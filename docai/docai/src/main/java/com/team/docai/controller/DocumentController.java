package com.team.docai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.team.docai.common.Result;
import com.team.docai.entity.Document;
import com.team.docai.mapper.DocumentMapper;
import com.team.docai.service.DocParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentMapper documentMapper;
    private final DocParseService docParseService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /** 上传文档 */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        // 创建上传目录
        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        // 保存文件
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            return Result.error("文件名无效");
        }
        String ext = originalName.substring(originalName.lastIndexOf("."));
        String savedName = UUID.randomUUID() + ext;
        Path filePath = dir.resolve(savedName);
        file.transferTo(filePath.toFile());

        // 解析文本内容
        String contentText = "";
        if (ext.equals(".docx")) {
            contentText = docParseService.parseWord(file);
        } else if (ext.equals(".xlsx")) {
            contentText = docParseService.parseExcel(file);
        } else if (ext.equals(".txt")) {
            contentText = new String(file.getBytes(), "UTF-8");
        }

        // 保存到数据库
        Document doc = new Document();
        doc.setUserId(1L); // Demo阶段写死用户ID
        doc.setTitle(originalName);
        doc.setFileType(ext.replace(".", ""));
        doc.setFilePath(savedName);
        doc.setFileSize(file.getSize());
        doc.setContentText(contentText);
        documentMapper.insert(doc);

        return Result.success(doc);
    }

    /** 文档列表 */
    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Document::getTitle, keyword);
        }
        wrapper.orderByDesc(Document::getCreatedAt);
        return Result.success(documentMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 获取文档详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(documentMapper.selectById(id));
    }

    /** 下载文档 */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Document doc = documentMapper.selectById(id);
        File file = new File(uploadDir + doc.getFilePath());
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getTitle() + "\"")
                .body(resource);
    }

    /** 删除文档 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc != null) {
            new File(uploadDir + doc.getFilePath()).delete();
            documentMapper.deleteById(id);
        }
        return Result.success("删除成功");
    }
}