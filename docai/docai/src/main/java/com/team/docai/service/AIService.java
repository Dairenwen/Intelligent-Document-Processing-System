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
import java.util.concurrent.Semaphore;
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

    /** 并发限制：最多同时3个AI请求，防止API限流 */
    private final Semaphore aiSemaphore = new Semaphore(3);

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
     * AI调用带重试机制和并发控制 - 核心方法
     */
    private String invokeWithRetry(List<ChatMessage> messages) {
        Exception lastException = null;
        boolean acquired = false;
        try {
            // 获取信号量，最多等待60秒
            acquired = aiSemaphore.tryAcquire(60, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("AI服务繁忙，请稍后重试（并发请求过多）");
            }

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
                        var choice = response.getData().getChoices().get(0);
                        if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                            String content = choice.getMessage().getContent().toString();
                            log.info("AI调用成功, attempt={}, responseLength={}", attempt, content.length());
                            return content;
                        }
                    }
                    log.warn("AI返回空响应, attempt={}", attempt);
                } catch (Exception e) {
                    lastException = e;
                    log.warn("AI调用失败, attempt={}/{}, error={}", attempt, maxRetries, e.getMessage());
                    if (attempt < maxRetries) {
                        try {
                            // 指数退避：2s, 4s, 8s...
                            long waitMs = (long) Math.pow(2, attempt) * 1000;
                            Thread.sleep(waitMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI服务调用被中断");
        } finally {
            if (acquired) {
                aiSemaphore.release();
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
        String systemPrompt = "你是一名高效精准的文档信息提取专家。请从文档中快速提取以下关键信息，以结构化格式输出：\n"
                + "1. 文档类型（通知、报告、公报、合同、简历、统计数据等）\n"
                + "2. 文档主题/标题\n"
                + "3. 关键实体（人名、机构名、地名等）\n"
                + "4. 关键数值数据（日期、金额、百分比、统计数字等，保留原始数值和单位）\n"
                + "5. 核心内容摘要（200字以内，涵盖文档主旨和要点）\n\n"
                + "要求：\n"
                + "- 信息必须来自文档原文，不得编造\n"
                + "- 数字数据务必精确，保留原文格式\n"
                + "- 以清晰分类的结构化格式输出\n"
                + "- 输出要全面但精炼，便于后续数据检索和表格填写";
        return chat(systemPrompt, documentContent);
    }

    /**
     * AI从原始文档文本中提取结构化信息（用于文档上传时自动提取）
     * 优化提取速度和准确率，提取最全面最简洁的信息
     */
    public String extractDocumentInfo(String rawText, String filename) {
        // 截断过长文本以加快AI处理速度
        String processedText = rawText;
        if (rawText.length() > 12000) {
            processedText = rawText.substring(0, 12000) + "\n...(内容已截断)";
        }

        String systemPrompt = "你是高精度文档信息提取引擎。请从文档原文中提取所有关键数据和信息，输出结构化摘要。\n\n"
                + "提取规则：\n"
                + "1. 提取所有数值型数据（统计数字、金额、百分比、年份、日期等），保留精确数值和单位\n"
                + "2. 提取所有实体信息（机构名称、人名、地名、项目名等）\n"
                + "3. 提取关键事实和结论\n"
                + "4. 保持数据的上下文关联（如：某指标=某数值）\n"
                + "5. 输出格式清晰、分类明确，便于后续自动匹配和表格填写\n\n"
                + "输出格式要求：\n"
                + "- 使用分类标题组织信息\n"
                + "- 每条信息独立一行\n"
                + "- 数值数据格式：指标名称：数值 单位\n"
                + "- 不要输出无关说明，只输出提取的信息";

        String userMessage = "文档名：" + filename + "\n\n文档原文：\n" + processedText;
        return chat(systemPrompt, userMessage);
    }
}
