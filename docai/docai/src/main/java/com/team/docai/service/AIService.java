package com.team.docai.service;

import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AIService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model:glm-4-flash}")
    private String model;

    @Value("${ai.max-retries:3}")
    private int maxRetries;

    private ClientV4 client;

    @PostConstruct
    public void init() {
        client = new ClientV4.Builder(apiKey)
                .networkConfig(60, 120, 120, 120, TimeUnit.SECONDS)
                .build();
        log.info("智谱AI客户端初始化完成, model={}", model);
    }

    /**
     * 带重试的通用AI对话
     */
    public String chat(String systemPrompt, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), userMessage));
        return invokeWithRetry(messages);
    }

    /**
     * AI调用带重试机制 - 核心方法
     */
    private String invokeWithRetry(List<ChatMessage> messages) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ChatCompletionRequest request = ChatCompletionRequest.builder()
                        .model(model)
                        .stream(Boolean.FALSE)
                        .invokeMethod(Constants.invokeMethod)
                        .messages(messages)
                        .build();

                ModelApiResponse response = client.invokeModelApi(request);
                if (response != null && response.getData() != null
                        && response.getData().getChoices() != null
                        && !response.getData().getChoices().isEmpty()) {
                    String content = response.getData().getChoices().get(0)
                            .getMessage().getContent().toString();
                    log.info("AI调用成功, attempt={}, responseLength={}", attempt, content.length());
                    return content;
                }
                log.warn("AI返回空响应, attempt={}", attempt);
            } catch (Exception e) {
                lastException = e;
                log.warn("AI调用失败, attempt={}/{}, error={}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep((long) Math.pow(2, attempt - 1) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("AI调用全部失败, maxRetries={}", maxRetries, lastException);
        throw new RuntimeException("AI服务暂时不可用，请稍后重试: " +
                (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    /** AI生成公文 */
    public String generateDocument(String docType, String requirement) {
        String systemPrompt = "你是一个专业的公文写作助手。请根据用户需求生成规范的" + docType
                + "。要求：格式规范、用语正式、结构完整、内容详实。直接输出公文内容，不要输出多余说明。";
        return chat(systemPrompt, requirement);
    }

    /** AI润色 */
    public String polish(String text) {
        String systemPrompt = "你是公文润色专家。请对以下文本进行润色优化，保持公文风格，使语言更加规范流畅。直接输出润色后的内容。";
        return chat(systemPrompt, text);
    }

    /** AI分析文档 */
    public String analyzeDocument(String content, String question) {
        String systemPrompt = "你是一个智能文档分析助手。以下是文档内容，请根据用户的问题进行分析回答。\n\n文档内容：\n" + content;
        return chat(systemPrompt, question);
    }

    /** AI分析Excel数据 */
    public String analyzeData(String dataDescription) {
        String systemPrompt = "你是数据分析专家。请根据以下表格数据进行分析，给出关键发现和建议。如果适合可视化，请建议合适的图表类型和数据维度。";
        return chat(systemPrompt, dataDescription);
    }

    /**
     * AI结构化数据提取 - 用于自动填写表格（核心竞赛功能）
     */
    public String extractStructuredData(String templateDescription, String sourceDocuments) {
        String systemPrompt = "你是一个高精度的数据提取专家。你的任务是从源文档中提取数据来填充模板。\n\n"
                + "【重要规则】\n"
                + "1. 严格按照模板中的字段名提取对应数据\n"
                + "2. 提取的数据必须准确、来自源文档原文\n"
                + "3. 如果某字段在源文档中确实找不到，值设为空字符串\n"
                + "4. 数字类型保持数字格式，不要添加多余文字\n"
                + "5. 日期保持源文档中的原始格式\n"
                + "6. 只返回纯JSON，不要包含任何markdown标记或代码块标记\n\n"
                + "【输出格式】\n"
                + "直接返回JSON对象: {\"字段名1\": \"值1\", \"字段名2\": \"值2\", ...}";

        String userMessage = "【模板结构】\n" + templateDescription
                + "\n\n【源文档内容】\n" + sourceDocuments;

        return chat(systemPrompt, userMessage);
    }

    /**
     * AI提取关键信息摘要
     */
    public String extractKeyInfo(String documentContent) {
        String systemPrompt = "你是文档信息提取专家。请从文档中提取以下关键信息：\n"
                + "1. 文档类型（通知、报告、合同、简历等）\n"
                + "2. 文档主题/标题\n"
                + "3. 关键实体（人名、机构名、日期、金额等）\n"
                + "4. 核心内容摘要（100字以内）\n\n"
                + "请以清晰的结构化格式输出。";
        return chat(systemPrompt, documentContent);
    }
}
