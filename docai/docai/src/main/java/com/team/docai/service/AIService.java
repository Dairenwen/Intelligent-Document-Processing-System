package com.team.docai.service;

import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIService {

    @Value("${ai.api-key}")
    private String apiKey;

    private ClientV4 client;

    @PostConstruct
    public void init() {
        client = new ClientV4.Builder(apiKey).build();
    }

    /**
     * 通用AI对话
     */
    public String chat(String systemPrompt, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), userMessage));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("glm-4-flash")  // 免费模型
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();

        ModelApiResponse response = client.invokeModelApi(request);
        return response.getData().getChoices().get(0).getMessage().getContent().toString();
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
}