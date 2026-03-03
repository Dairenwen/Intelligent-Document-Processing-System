package com.team.docai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.docai.entity.Document;
import com.team.docai.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

/**
 * 自动填表服务 - 竞赛核心功能
 * 将多个源文档的数据自动提取并填充到Word/Excel模板中
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoFillService {

    private final AIService aiService;
    private final DocParseService docParseService;
    private final DocumentMapper documentMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 执行自动填表 - 主入口
     *
     * @param sourceDocIds 源文档ID列表
     * @param templateFile 模板文件
     * @return 填充后的文件字节数组
     */
    public byte[] autoFill(List<Long> sourceDocIds, MultipartFile templateFile) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("开始自动填表, 源文档数量={}, 模板文件={}", sourceDocIds.size(), templateFile.getOriginalFilename());

        // 1. 收集所有源文档内容
        String sourceContent = collectSourceContent(sourceDocIds);
        log.info("源文档内容收集完成, 总字符数={}", sourceContent.length());

        // 2. 分析模板结构
        String templateFilename = templateFile.getOriginalFilename();
        if (templateFilename == null) templateFilename = "template.docx";
        String ext = templateFilename.substring(templateFilename.lastIndexOf(".")).toLowerCase();
        String templateStructure = analyzeTemplate(templateFile, ext);
        log.info("模板结构分析完成: {}", templateStructure.substring(0, Math.min(200, templateStructure.length())));

        // 3. AI提取结构化数据
        // 如果源文档内容太长，进行分段摘要
        String processedContent = sourceContent;
        if (sourceContent.length() > 15000) {
            processedContent = summarizeContent(sourceContent);
            log.info("源文档内容已摘要化, 摘要长度={}", processedContent.length());
        }

        String aiResponse = aiService.extractStructuredData(templateStructure, processedContent);
        log.info("AI数据提取完成, 响应长度={}", aiResponse.length());

        // 4. 解析AI返回的JSON
        Map<String, String> extractedData = parseAIResponse(aiResponse);
        log.info("解析出{}个字段值", extractedData.size());

        // 5. 填充模板
        byte[] result;
        try (InputStream templateIs = templateFile.getInputStream()) {
            if (ext.equals(".docx")) {
                result = docParseService.fillWordTemplate(templateIs, extractedData);
            } else if (ext.equals(".xlsx")) {
                result = docParseService.fillExcelTemplate(templateIs, extractedData);
            } else {
                throw new IllegalArgumentException("模板文件格式不支持: " + ext + "，仅支持 .docx 和 .xlsx");
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("自动填表完成, 耗时={}ms, 填充字段数={}", elapsed, extractedData.size());
        return result;
    }

    /**
     * 批量自动填表 - 多个模板一次性填充
     */
    public Map<String, byte[]> batchAutoFill(List<Long> sourceDocIds, List<MultipartFile> templateFiles) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("开始批量自动填表, 源文档数={}, 模板数={}", sourceDocIds.size(), templateFiles.size());

        // 统一收集源文档内容（避免重复解析）
        String sourceContent = collectSourceContent(sourceDocIds);

        // 摘要化
        String processedContent = sourceContent;
        if (sourceContent.length() > 15000) {
            processedContent = summarizeContent(sourceContent);
        }

        Map<String, byte[]> results = new LinkedHashMap<>();

        for (MultipartFile templateFile : templateFiles) {
            String filename = templateFile.getOriginalFilename();
            if (filename == null) filename = "template.docx";
            try {
                String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();

                // 分析模板
                String templateStructure = analyzeTemplate(templateFile, ext);

                // AI提取数据
                String aiResponse = aiService.extractStructuredData(templateStructure, processedContent);
                Map<String, String> data = parseAIResponse(aiResponse);

                // 填充
                byte[] filled;
                try (InputStream is = templateFile.getInputStream()) {
                    if (ext.equals(".docx")) {
                        filled = docParseService.fillWordTemplate(is, data);
                    } else if (ext.equals(".xlsx")) {
                        filled = docParseService.fillExcelTemplate(is, data);
                    } else {
                        log.warn("跳过不支持的模板格式: {}", filename);
                        continue;
                    }
                }
                results.put(filename, filled);
                log.info("模板 {} 填充完成, 字段数={}", filename, data.size());
            } catch (Exception e) {
                log.error("模板 {} 填充失败: {}", filename, e.getMessage());
                // 继续处理其他模板，不中断
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("批量自动填表完成, 总耗时={}ms, 成功={}/{}", elapsed, results.size(), templateFiles.size());
        return results;
    }

    /**
     * 收集所有源文档内容
     */
    private String collectSourceContent(List<Long> docIds) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Long id : docIds) {
            Document doc = documentMapper.selectById(id);
            if (doc == null) {
                log.warn("文档ID={}不存在，跳过", id);
                continue;
            }

            sb.append("\n\n========== 文档: ").append(doc.getTitle()).append(" ==========\n");

            // 优先使用数据库中已解析的文本
            if (doc.getContentText() != null && !doc.getContentText().isBlank()) {
                sb.append(doc.getContentText());
            } else {
                // 尝试重新解析文件
                File file = new File(uploadDir + doc.getFilePath());
                if (file.exists()) {
                    String content = docParseService.parseLocalFile(file, doc.getFileType());
                    sb.append(content);
                } else {
                    log.warn("文档文件不存在: {}", file.getAbsolutePath());
                }
            }
        }
        return sb.toString();
    }

    /**
     * 分析模板结构
     */
    private String analyzeTemplate(MultipartFile templateFile, String ext) throws IOException {
        try (InputStream is = templateFile.getInputStream()) {
            if (ext.equals(".docx")) {
                return docParseService.analyzeWordTemplate(is);
            } else if (ext.equals(".xlsx")) {
                return docParseService.analyzeExcelTemplate(is);
            }
        }
        throw new IllegalArgumentException("无法分析的模板格式: " + ext);
    }

    /**
     * 对过长的内容进行分段摘要
     */
    private String summarizeContent(String content) {
        // 分段处理，每段约5000字
        int segmentSize = 5000;
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < content.length(); i += segmentSize) {
            segments.add(content.substring(i, Math.min(i + segmentSize, content.length())));
        }

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i < 3) {
                // 前3段直接保留
                summary.append(segments.get(i));
            } else {
                // 后续段落通过AI摘要
                try {
                    String seg = aiService.chat(
                            "请将以下文本压缩摘要，保留所有关键数据(数字、日期、人名、机构名等)，删除冗余描述。直接输出摘要内容。",
                            segments.get(i));
                    summary.append("\n[摘要] ").append(seg);
                } catch (Exception e) {
                    // 如果摘要失败，截取前1000字
                    summary.append(segments.get(i), 0, Math.min(1000, segments.get(i).length()));
                }
            }
        }
        return summary.toString();
    }

    /**
     * 解析AI返回的JSON响应
     */
    private Map<String, String> parseAIResponse(String aiResponse) {
        try {
            // 清理AI返回的可能包含的markdown标记
            String cleaned = aiResponse.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            // 找到第一个 { 和最后一个 }
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            Map<String, Object> raw = objectMapper.readValue(cleaned, new TypeReference<>() {});
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                result.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
            }
            return result;
        } catch (Exception e) {
            log.error("AI响应JSON解析失败: {}", aiResponse, e);
            // 尝试用正则提取 key-value 对
            return fallbackParse(aiResponse);
        }
    }

    /**
     * JSON解析失败时的兜底方案 - 正则提取
     */
    private Map<String, String> fallbackParse(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*[:：]\\s*\"([^\"]*?)\"");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.put(matcher.group(1), matcher.group(2));
        }
        log.info("兜底解析提取到{}个字段", result.size());
        return result;
    }
}
