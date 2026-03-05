package com.team.docai.controller;

import com.team.docai.common.Result;
import com.team.docai.entity.Document;
import com.team.docai.mapper.DocumentMapper;
import com.team.docai.service.AIService;
import com.team.docai.service.DocParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final DocParseService docParseService;
    private final DocumentMapper documentMapper;

    /** AI对话（可带文档上下文） - 使用rawText提供完整文档内容 */
    @PostMapping("/chat")
    public Result<?> chat(@RequestBody Map<String, Object> body) {
        try {
            String message = (String) body.get("message");
            if (message == null || message.isBlank()) {
                return Result.error("消息内容不能为空");
            }

            Long documentId = body.get("documentId") != null ?
                    Long.valueOf(body.get("documentId").toString()) : null;

            String response;
            if (documentId != null) {
                Document doc = documentMapper.selectById(documentId);
                if (doc == null) return Result.error("文档不存在");
                // 优先使用 rawText（完整原始文本），若为空则回退到 contentText
                String docContent = doc.getRawText();
                if (docContent == null || docContent.isBlank()) {
                    docContent = doc.getContentText();
                }
                if (docContent == null || docContent.isBlank()) {
                    return Result.error("文档内容为空，请先确保文档已正确解析");
                }
                response = aiService.analyzeDocument(docContent, message);
            } else {
                response = aiService.chat("你是一个智能公文处理助手，请帮助用户处理公文相关问题。请用简洁专业的语言回答。", message);
            }
            return Result.success(Map.of("reply", response));
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return Result.error("AI对话失败: " + e.getMessage());
        }
    }

    /** AI编辑文档 - 根据指令对关联文档进行编辑并返回结果 */
    @PostMapping("/edit-document")
    public Result<?> editDocument(@RequestBody Map<String, Object> body) {
        try {
            Long documentId = body.get("documentId") != null ?
                    Long.valueOf(body.get("documentId").toString()) : null;
            String instruction = (String) body.get("instruction");
            if (documentId == null) return Result.error("请指定文档ID");
            if (instruction == null || instruction.isBlank()) return Result.error("请输入编辑指令");

            Document doc = documentMapper.selectById(documentId);
            if (doc == null) return Result.error("文档不存在");

            String docContent = doc.getRawText();
            if (docContent == null || docContent.isBlank()) {
                docContent = doc.getContentText();
            }
            if (docContent == null || docContent.isBlank()) {
                return Result.error("文档内容为空");
            }

            String editedContent = aiService.editDocumentContent(docContent, instruction);
            return Result.success(Map.of(
                    "editedContent", editedContent,
                    "originalLength", docContent.length(),
                    "editedLength", editedContent.length(),
                    "title", doc.getTitle()
            ));
        } catch (Exception e) {
            log.error("AI编辑文档失败", e);
            return Result.error("编辑失败: " + e.getMessage());
        }
    }

    /** AI生成公文 */
    @PostMapping("/generate")
    public Result<?> generate(@RequestBody Map<String, String> body) {
        try {
            String docType = body.get("docType");
            String requirement = body.get("requirement");
            if (requirement == null || requirement.isBlank()) {
                return Result.error("请填写需求描述");
            }
            String content = aiService.generateDocument(docType, requirement);
            return Result.success(Map.of("content", content));
        } catch (Exception e) {
            log.error("公文生成失败", e);
            return Result.error("公文生成失败: " + e.getMessage());
        }
    }

    /** AI生成并下载Word文件 */
    @PostMapping("/generate-word")
    public ResponseEntity<byte[]> generateWord(@RequestBody Map<String, String> body) {
        try {
            String title = body.getOrDefault("title", "公文");
            String content = body.get("content");
            if (content == null || content.isBlank()) {
                return ResponseEntity.badRequest().body("内容不能为空".getBytes(StandardCharsets.UTF_8));
            }

            byte[] wordBytes = docParseService.generateWord(title, content);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(URLEncoder.encode(title + ".docx", StandardCharsets.UTF_8)).build());
            return new ResponseEntity<>(wordBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Word生成失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("生成失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** AI润色 */
    @PostMapping("/polish")
    public Result<?> polish(@RequestBody Map<String, String> body) {
        try {
            String text = body.get("text");
            if (text == null || text.isBlank()) return Result.error("文本不能为空");
            String result = aiService.polish(text);
            return Result.success(Map.of("polished", result));
        } catch (Exception e) {
            log.error("AI润色失败", e);
            return Result.error("润色失败: " + e.getMessage());
        }
    }

    /** AI分析数据 */
    @PostMapping("/analyze-data")
    public Result<?> analyzeData(@RequestBody Map<String, String> body) {
        try {
            String data = body.get("data");
            if (data == null || data.isBlank()) return Result.error("数据不能为空");
            String result = aiService.analyzeData(data);
            return Result.success(Map.of("analysis", result));
        } catch (Exception e) {
            log.error("数据分析失败", e);
            return Result.error("数据分析失败: " + e.getMessage());
        }
    }

    /** AI提取文档关键信息 - 优先使用rawText */
    @PostMapping("/extract-info")
    public Result<?> extractInfo(@RequestBody Map<String, Object> body) {
        try {
            Long documentId = body.get("documentId") != null ?
                    Long.valueOf(body.get("documentId").toString()) : null;
            if (documentId == null) return Result.error("请指定文档ID");

            Document doc = documentMapper.selectById(documentId);
            if (doc == null) return Result.error("文档不存在");

            String docContent = doc.getRawText();
            if (docContent == null || docContent.isBlank()) {
                docContent = doc.getContentText();
            }
            if (docContent == null || docContent.isBlank()) {
                return Result.error("文档内容为空");
            }

            String keyInfo = aiService.extractKeyInfo(docContent);
            return Result.success(Map.of("info", keyInfo, "title", doc.getTitle()));
        } catch (Exception e) {
            log.error("信息提取失败", e);
            return Result.error("信息提取失败: " + e.getMessage());
        }
    }
}
