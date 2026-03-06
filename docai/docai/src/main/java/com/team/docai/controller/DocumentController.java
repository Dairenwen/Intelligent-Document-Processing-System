package com.team.docai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.team.docai.common.Result;
import com.team.docai.entity.Document;
import com.team.docai.mapper.DocumentMapper;
import com.team.docai.service.AIService;
import com.team.docai.service.DocParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentMapper documentMapper;
    private final DocParseService docParseService;
    private final AIService aiService;
    private final com.team.docai.service.FileParserService fileParserService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".docx", ".xlsx", ".txt", ".md", ".pdf", ".doc", ".xls", ".pptx", ".ppt", ".csv", ".html");

    /** 上传单个文档 */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                            @RequestAttribute(value = "userId", required = false) Long userId) {
        try {
            Document doc = processUpload(file, userId);
            return Result.success(doc);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /** 批量上传文档 */
    @PostMapping("/upload/batch")
    public Result<?> batchUpload(@RequestParam("files") List<MultipartFile> files,
                                 @RequestAttribute(value = "userId", required = false) Long userId) {
        List<Document> uploaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                Document doc = processUpload(file, userId);
                uploaded.add(doc);
            } catch (Exception e) {
                String msg = file.getOriginalFilename() + ": " + e.getMessage();
                errors.add(msg);
                log.error("批量上传-单文件失败: {}", msg);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("uploaded", uploaded);
        result.put("successCount", uploaded.size());
        result.put("failCount", errors.size());
        result.put("errors", errors);
        return Result.success(result);
    }

    /** 文档列表 */
    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String fileType,
                          @RequestAttribute(value = "userId", required = false) Long userId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Document::getUserId, userId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Document::getTitle, keyword);
        }
        if (fileType != null && !fileType.isEmpty()) {
            wrapper.eq(Document::getFileType, fileType);
        }
        wrapper.orderByDesc(Document::getCreatedAt);
        return Result.success(documentMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 获取文档详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) return Result.error("文档不存在");
        return Result.success(doc);
    }

    /** 获取文档提取的信息摘要 */
    @GetMapping("/{id}/summary")
    public Result<?> summary(@PathVariable Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) return Result.error("文档不存在");
        if (doc.getContentText() == null || doc.getContentText().isBlank()) {
            return Result.error("文档内容为空，无法生成摘要");
        }
        try {
            String keyInfo = aiService.extractKeyInfo(doc.getContentText());
            return Result.success(Map.of("summary", keyInfo, "title", doc.getTitle()));
        } catch (Exception e) {
            log.error("提取文档摘要失败", e);
            return Result.error("摘要生成失败: " + e.getMessage());
        }
    }

    /** 下载文档 */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        File file = Paths.get(uploadDir).toAbsolutePath().resolve(doc.getFilePath()).toFile();
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(URLEncoder.encode(doc.getTitle(), StandardCharsets.UTF_8)).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** 删除文档 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc != null) {
            try {
                Paths.get(uploadDir).toAbsolutePath().resolve(doc.getFilePath()).toFile().delete();
            } catch (Exception e) {
                log.warn("删除文件失败: {}", e.getMessage());
            }
            documentMapper.deleteById(id);
        }
        return Result.success("删除成功");
    }

    /** 批量删除文档 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        int deleted = 0;
        for (Long id : ids) {
            Document doc = documentMapper.selectById(id);
            if (doc != null) {
                try {
                    Paths.get(uploadDir).toAbsolutePath().resolve(doc.getFilePath()).toFile().delete();
                } catch (Exception e) {
                    log.warn("删除文件失败: {}", e.getMessage());
                }
                documentMapper.deleteById(id);
                deleted++;
            }
        }
        return Result.success(Map.of("deleted", deleted));
    }

    /** 更新文档内容（AI编辑后保存） */
    @PutMapping("/{id}/content")
    public Result<?> updateContent(@PathVariable Long id,
                                   @RequestBody Map<String, String> body,
                                   @RequestAttribute(value = "userId", required = false) Long userId) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) return Result.error("文档不存在");
        // 检查所有权
        if (userId != null && !userId.equals(doc.getUserId())) {
            return Result.error("无权修改此文档");
        }
        String newContent = body.get("content");
        if (newContent == null || newContent.isBlank()) {
            return Result.error("内容不能为空");
        }
        // 更新 rawText 为新内容，contentText 同步更新
        doc.setRawText(newContent);
        doc.setContentText(newContent);
        documentMapper.updateById(doc);
        log.info("文档内容已更新: id={}, title={}, contentLength={}", id, doc.getTitle(), newContent.length());
        return Result.success("文档内容已保存");
    }

    /** 获取文档统计信息 */
    @GetMapping("/stats")
    public Result<?> stats(@RequestAttribute(value = "userId", required = false) Long userId) {
        LambdaQueryWrapper<Document> baseWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            baseWrapper.eq(Document::getUserId, userId);
        }
        Long total = documentMapper.selectCount(new LambdaQueryWrapper<Document>()
                .eq(userId != null, Document::getUserId, userId));
        Long docxCount = documentMapper.selectCount(new LambdaQueryWrapper<Document>()
                .eq(userId != null, Document::getUserId, userId)
                .eq(Document::getFileType, "docx"));
        Long xlsxCount = documentMapper.selectCount(new LambdaQueryWrapper<Document>()
                .eq(userId != null, Document::getUserId, userId)
                .eq(Document::getFileType, "xlsx"));
        Long txtCount = documentMapper.selectCount(new LambdaQueryWrapper<Document>()
                .eq(userId != null, Document::getUserId, userId)
                .eq(Document::getFileType, "txt"));
        Long mdCount = documentMapper.selectCount(new LambdaQueryWrapper<Document>()
                .eq(userId != null, Document::getUserId, userId)
                .eq(Document::getFileType, "md"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("docx", docxCount);
        stats.put("xlsx", xlsxCount);
        stats.put("txt", txtCount);
        stats.put("md", mdCount);
        return Result.success(stats);
    }

    // ============ 私有方法 ============

    private Document processUpload(MultipartFile file, Long userId) throws IOException {
        Path dir = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.exists(dir)) Files.createDirectories(dir);

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new IllegalArgumentException("文件名无效");
        }

        String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件格式: " + ext + "，仅支持: " + SUPPORTED_EXTENSIONS);
        }

        // Step 1: 先读取文件字节（transferTo后InputStream不可用）
        byte[] fileBytes = file.getBytes();

        // Step 2: 保存文件到磁盘
        String savedName = UUID.randomUUID() + ext;
        Path filePath = dir.resolve(savedName);
        Files.write(filePath, fileBytes);

        // Step 3: 解析文档 —— 优先使用智谱AI文件解析API（同步/异步策略）
        // 失败时自动回退到本地Apache POI解析
        String rawText = "";
        try {
            rawText = fileParserService.parseFile(filePath.toFile(), originalName);
            log.info("文件解析完成(云端/本地): {}, rawTextLength={}", originalName, rawText.length());
        } catch (Exception e) {
            log.warn("文件解析整体失败: {}, error={}", originalName, e.getMessage());
            // 最终回退：尝试本地解析
            try {
                rawText = docParseService.parseLocalFile(filePath.toFile(), ext.replace(".", ""));
            } catch (Exception e2) {
                log.warn("本地解析也失败: {}", e2.getMessage());
            }
        }

        // Step 4: AI提取结构化关键信息（用于自动填表和检索）
        String contentText = rawText;
        if (rawText != null && !rawText.isBlank()) {
            try {
                contentText = aiService.extractDocumentInfo(rawText, originalName);
                log.info("AI信息提取完成: {}, 原文长度={}, 提取结果长度={}",
                        originalName, rawText.length(), contentText.length());
            } catch (Exception e) {
                log.warn("AI信息提取失败，保留原始文本: {}, error={}", originalName, e.getMessage());
                contentText = rawText;
            }
        }

        // Step 5: 创建文档记录并入库
        Document doc = new Document();
        doc.setUserId(userId != null ? userId : 1L);
        doc.setTitle(originalName);
        doc.setFileType(ext.replace(".", ""));
        doc.setFilePath(savedName);
        doc.setFileSize(file.getSize());
        doc.setContentText(contentText);  // AI提取的结构化信息（供自动填表使用）
        doc.setRawText(rawText);          // 完整的原始解析文本（供AI对话和编辑使用）
        documentMapper.insert(doc);

        log.info("文档上传成功: {}, type={}, size={}", originalName, ext, file.getSize());
        return doc;
    }
}
