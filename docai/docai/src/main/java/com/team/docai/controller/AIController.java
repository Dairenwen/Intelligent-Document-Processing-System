package com.team.docai.controller;

import com.team.docai.common.Result;
import com.team.docai.entity.Document;
import com.team.docai.mapper.DocumentMapper;
import com.team.docai.service.AIService;
import com.team.docai.service.DocParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final DocParseService docParseService;
    private final DocumentMapper documentMapper;

    /** AI对话（可带文档上下文） */
    @PostMapping("/chat")
    public Result<?> chat(@RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        Long documentId = body.get("documentId") != null ?
                Long.valueOf(body.get("documentId").toString()) : null;

        String response;
        if (documentId != null) {
            Document doc = documentMapper.selectById(documentId);
            response = aiService.analyzeDocument(doc.getContentText(), message);
        } else {
            response = aiService.chat("你是一个智能公文处理助手，请帮助用户处理公文相关问题。", message);
        }
        return Result.success(Map.of("reply", response));
    }

    /** AI生成公文 */
    @PostMapping("/generate")
    public Result<?> generate(@RequestBody Map<String, String> body) {
        String docType = body.get("docType");       // 如：通知、报告、请示
        String requirement = body.get("requirement"); // 用户需求描述
        String content = aiService.generateDocument(docType, requirement);
        return Result.success(Map.of("content", content));
    }

    /** AI生成并下载Word文件 */
    @PostMapping("/generate-word")
    public ResponseEntity<byte[]> generateWord(@RequestBody Map<String, String> body) throws IOException {
        String title = body.get("title");
        String content = body.get("content");
        byte[] wordBytes = docParseService.generateWord(title, content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(title + ".docx").build());
        return new ResponseEntity<>(wordBytes, headers, HttpStatus.OK);
    }

    /** AI润色 */
    @PostMapping("/polish")
    public Result<?> polish(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        String result = aiService.polish(text);
        return Result.success(Map.of("polished", result));
    }

    /** AI分析数据 */
    @PostMapping("/analyze-data")
    public Result<?> analyzeData(@RequestBody Map<String, String> body) {
        String data = body.get("data");
        String result = aiService.analyzeData(data);
        return Result.success(Map.of("analysis", result));
    }
}