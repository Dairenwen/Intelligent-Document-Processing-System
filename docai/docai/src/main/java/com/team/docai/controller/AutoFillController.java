package com.team.docai.controller;

import com.team.docai.common.Result;
import com.team.docai.service.AutoFillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/**
 * 自动填表控制器 - 竞赛核心接口
 */
@Slf4j
@RestController
@RequestMapping("/api/autofill")
@RequiredArgsConstructor
public class AutoFillController {

    private final AutoFillService autoFillService;

    /**
     * 单模板自动填充
     * 上传模板文件 + 指定源文档ID列表 → 返回填充后的文件
     */
    @PostMapping("/single")
    public ResponseEntity<byte[]> fillSingle(
            @RequestParam("template") MultipartFile templateFile,
            @RequestParam("sourceDocIds") List<Long> sourceDocIds) {
        try {
            long start = System.currentTimeMillis();
            byte[] filledBytes = autoFillService.autoFill(sourceDocIds, templateFile);
            long elapsed = System.currentTimeMillis() - start;

            String originalName = templateFile.getOriginalFilename();
            String outputName = "filled_" + originalName;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(URLEncoder.encode(outputName, StandardCharsets.UTF_8)).build());
            headers.set("X-Fill-Time-Ms", String.valueOf(elapsed));

            log.info("单模板填充成功: {}, 耗时={}ms", outputName, elapsed);
            return new ResponseEntity<>(filledBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("自动填充失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("填充失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 批量模板自动填充
     * 上传多个模板文件 + 指定源文档ID列表 → 返回ZIP压缩包
     */
    @PostMapping("/batch")
    public ResponseEntity<byte[]> fillBatch(
            @RequestParam("templates") List<MultipartFile> templateFiles,
            @RequestParam("sourceDocIds") List<Long> sourceDocIds) {
        try {
            long start = System.currentTimeMillis();

            Map<String, byte[]> results = autoFillService.batchAutoFill(sourceDocIds, templateFiles);

            if (results.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("所有模板填充均失败".getBytes(StandardCharsets.UTF_8));
            }

            // 打包为ZIP
            byte[] zipBytes = createZip(results);
            long elapsed = System.currentTimeMillis() - start;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(URLEncoder.encode("filled_templates.zip", StandardCharsets.UTF_8)).build());
            headers.set("X-Fill-Time-Ms", String.valueOf(elapsed));
            headers.set("X-Fill-Count", String.valueOf(results.size()));

            log.info("批量填充完成: 成功{}/{}个, 总耗时={}ms",
                    results.size(), templateFiles.size(), elapsed);
            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("批量自动填充失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("批量填充失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 预览模式 - 返回AI提取的数据而不填充
     * 用于让用户确认数据再填充
     */
    @PostMapping("/preview")
    public Result<?> preview(
            @RequestParam("template") MultipartFile templateFile,
            @RequestParam("sourceDocIds") List<Long> sourceDocIds) {
        try {
            // 复用自动填充逻辑但只返回中间数据
            long start = System.currentTimeMillis();
            byte[] filledBytes = autoFillService.autoFill(sourceDocIds, templateFile);
            long elapsed = System.currentTimeMillis() - start;

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("fillTimeMs", elapsed);
            resultData.put("templateName", templateFile.getOriginalFilename());
            resultData.put("sourceDocCount", sourceDocIds.size());
            resultData.put("fileSize", filledBytes.length);

            return Result.success(resultData);
        } catch (Exception e) {
            log.error("预览失败", e);
            return Result.error("预览失败: " + e.getMessage());
        }
    }

    /**
     * 打包文件为ZIP
     */
    private byte[] createZip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                ZipEntry ze = new ZipEntry("filled_" + entry.getKey());
                zos.putNextEntry(ze);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
