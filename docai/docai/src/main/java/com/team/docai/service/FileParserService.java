package com.team.docai.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * 智谱AI文件解析服务
 *
 * 集成智谱AI新文件解析API，支持两种解析策略：
 * - 小文件(< 5MB): 使用 prime-sync 同步解析，一次请求立即返回结果
 * - 大文件(>= 5MB): 使用 lite 异步解析，创建任务 → 保存task_id → 轮询查询结果
 *
 * 如果云端解析失败，自动回退到本地 Apache POI 解析
 *
 * API文档参考: https://docs.bigmodel.cn/cn/guide/tools/file-parser
 */
@Slf4j
@Service
public class FileParserService {

    private static final String PARSER_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/files/parser";

    /** 同步/异步阈值：5MB以下使用同步解析，5MB以上使用异步解析 */
    private static final long SYNC_THRESHOLD = 5 * 1024 * 1024;

    /** 异步解析最大轮询次数（每次间隔3秒，最长约3分钟） */
    private static final int MAX_POLL_ATTEMPTS = 60;
    private static final long POLL_INTERVAL_MS = 3000;

    /** 智谱文件解析API支持的文件类型 */
    private static final Set<String> CLOUD_SUPPORTED_TYPES = Set.of(
            "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt",
            "csv", "txt", "md", "html",
            "png", "jpg", "jpeg", "bmp", "gif", "webp"
    );

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.file-parser.enabled:true}")
    private boolean parserEnabled;

    private final DocParseService docParseService;

    public FileParserService(DocParseService docParseService) {
        this.docParseService = docParseService;
    }

    // ======================== 公开接口 ========================

    /**
     * 智能解析文件 - 根据文件大小自动选择同步/异步策略
     *
     * @param file             已保存到磁盘的文件
     * @param originalFilename 原始文件名（用于提取扩展名）
     * @return 解析后的文本内容
     */
    public String parseFile(File file, String originalFilename) {
        String ext = getFileExtension(originalFilename);

        // 未启用云端解析或不支持的格式 → 本地解析
        if (!parserEnabled || !CLOUD_SUPPORTED_TYPES.contains(ext)) {
            log.info("使用本地解析: file={}, parserEnabled={}, cloudSupported={}",
                    originalFilename, parserEnabled, CLOUD_SUPPORTED_TYPES.contains(ext));
            return parseLocal(file, ext);
        }

        try {
            long fileSize = file.length();
            if (fileSize <= SYNC_THRESHOLD) {
                log.info("使用同步云端解析(prime-sync): file={}, size={}KB",
                        originalFilename, fileSize / 1024);
                return parseSyncFile(file, ext);
            } else {
                log.info("使用异步云端解析(lite): file={}, size={}KB",
                        originalFilename, fileSize / 1024);
                return parseAsyncFile(file, ext);
            }
        } catch (Exception e) {
            log.warn("云端文件解析失败，回退到本地解析: file={}, error={}", originalFilename, e.getMessage());
            return parseLocal(file, ext);
        }
    }

    // ======================== 同步解析 ========================

    /**
     * 同步解析 - 使用 prime-sync 服务
     * POST /api/paas/v4/files/parser/sync
     *
     * 适用场景：小文件、需要低延迟即时返回结果
     * 支持格式：pdf,docx,doc,xls,xlsx,ppt,pptx,md,txt,csv,html,...
     */
    private String parseSyncFile(File file, String fileType) {
        String token = generateApiToken();

        HttpResponse response = HttpRequest.post(PARSER_BASE_URL + "/sync")
                .header("Authorization", "Bearer " + token)
                .form("file", file)
                .form("tool_type", "prime-sync")
                .form("file_type", fileType.toUpperCase())
                .timeout(120000)  // 2分钟超时
                .execute();

        String body = response.body();
        log.debug("同步解析响应: status={}, bodyLength={}",
                response.getStatus(), body != null ? body.length() : 0);

        if (response.getStatus() != 200) {
            throw new RuntimeException("同步解析HTTP错误: status=" + response.getStatus());
        }

        JSONObject json = JSONUtil.parseObj(body);
        String status = json.getStr("status");

        if ("succeeded".equals(status)) {
            String content = json.getStr("content");
            if (content != null && !content.isBlank()) {
                log.info("同步解析成功: contentLength={}", content.length());
                return content;
            }
            throw new RuntimeException("同步解析返回空内容");
        }

        throw new RuntimeException("同步解析失败: status=" + status
                + ", message=" + json.getStr("message"));
    }

    // ======================== 异步解析 ========================

    /**
     * 异步解析 - 使用 lite 服务
     * Step 1: POST /api/paas/v4/files/parser/create → 获取 task_id
     * Step 2: GET /api/paas/v4/files/parser/result/{taskId}/text → 轮询获取结果
     *
     * 适用场景：大文件、复杂文档、批量处理
     * 优势：成本极低（0.01元/次）
     */
    private String parseAsyncFile(File file, String fileType) throws InterruptedException {
        String token = generateApiToken();

        // Step 1: 创建解析任务
        HttpResponse createResp = HttpRequest.post(PARSER_BASE_URL + "/create")
                .header("Authorization", "Bearer " + token)
                .form("file", file)
                .form("tool_type", "lite")
                .form("file_type", fileType.toUpperCase())
                .timeout(60000)
                .execute();

        if (createResp.getStatus() != 200) {
            throw new RuntimeException("创建异步任务HTTP错误: status=" + createResp.getStatus());
        }

        JSONObject createJson = JSONUtil.parseObj(createResp.body());
        if (!createJson.getBool("success", false)) {
            throw new RuntimeException("创建异步任务失败: " + createJson.getStr("message"));
        }

        String taskId = createJson.getStr("task_id");
        log.info("异步解析任务已创建: taskId={}", taskId);

        // Step 2: 轮询获取结果
        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            Thread.sleep(POLL_INTERVAL_MS);

            // 每次轮询重新生成token以防过期
            String pollToken = generateApiToken();

            HttpResponse resultResp = HttpRequest
                    .get(PARSER_BASE_URL + "/result/" + taskId + "/text")
                    .header("Authorization", "Bearer " + pollToken)
                    .timeout(30000)
                    .execute();

            if (resultResp.getStatus() != 200) {
                log.warn("轮询异步结果HTTP错误: status={}, attempt={}/{}",
                        resultResp.getStatus(), attempt, MAX_POLL_ATTEMPTS);
                continue;
            }

            JSONObject resultJson = JSONUtil.parseObj(resultResp.body());
            String status = resultJson.getStr("status");

            if ("succeeded".equals(status)) {
                String content = resultJson.getStr("content");
                log.info("异步解析成功: taskId={}, contentLength={}, attempts={}",
                        taskId, content != null ? content.length() : 0, attempt);
                return content != null ? content : "";
            } else if ("failed".equals(status)) {
                throw new RuntimeException("异步解析失败: " + resultJson.getStr("message"));
            }

            // status == "processing" → 继续轮询
            if (attempt % 10 == 0) {
                log.info("异步解析进行中: taskId={}, attempt={}/{}", taskId, attempt, MAX_POLL_ATTEMPTS);
            }
        }

        throw new RuntimeException("异步解析超时(>3分钟): taskId=" + taskId);
    }

    // ======================== 本地解析回退 ========================

    /**
     * 本地解析 - 使用 Apache POI 等本地工具
     * 当云端解析不可用或失败时的回退方案
     */
    private String parseLocal(File file, String ext) {
        try {
            return docParseService.parseLocalFile(file, ext);
        } catch (Exception e) {
            log.error("本地解析也失败了: file={}, error={}", file.getName(), e.getMessage());
            return "";
        }
    }

    // ======================== JWT Token 生成 ========================

    /**
     * 生成智谱AI API认证Token (JWT格式)
     *
     * API Key格式: {id}.{secret}
     * JWT Header: {"alg":"HS256","sign_type":"SIGN"}
     * JWT Payload: {"api_key":"{id}","exp":{过期时间戳ms},"timestamp":{当前时间戳ms}}
     * 签名算法: HMAC-SHA256
     */
    private String generateApiToken() {
        String[] parts = apiKey.split("\\.", 2);
        if (parts.length != 2) {
            throw new RuntimeException("API Key格式无效，期望格式: {id}.{secret}");
        }
        String id = parts[0];
        String secret = parts[1];

        long nowMs = System.currentTimeMillis();
        long expMs = nowMs + 3600 * 1000; // 1小时有效期

        // 手动构建JWT（避免第三方库的header字段差异）
        String headerJson = "{\"alg\":\"HS256\",\"sign_type\":\"SIGN\"}";
        String payloadJson = String.format(
                "{\"api_key\":\"%s\",\"exp\":%d,\"timestamp\":%d}", id, expMs, nowMs);

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String content = header + "." + payload;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
            return content + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("JWT Token生成失败", e);
        }
    }

    // ======================== 工具方法 ========================

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
