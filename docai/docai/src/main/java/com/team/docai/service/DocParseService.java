package com.team.docai.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DocParseService {

    // ============================== 文档解析 ==============================

    /**
     * 根据文件扩展名自动选择解析方式
     */
    public String parseDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new IllegalArgumentException("文件名无效");

        String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        return switch (ext) {
            case ".docx" -> parseWord(file);
            case ".xlsx" -> parseExcel(file);
            case ".txt", ".md" -> parseText(file);
            default -> throw new IllegalArgumentException("不支持的文件格式: " + ext);
        };
    }

    /**
     * 根据文件路径和扩展名解析本地文件
     */
    public String parseLocalFile(File file, String ext) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return switch (ext.toLowerCase()) {
                case "docx" -> parseWordStream(fis);
                case "xlsx" -> parseExcelStream(fis);
                case "txt", "md" -> new String(fis.readAllBytes(), StandardCharsets.UTF_8);
                default -> throw new IllegalArgumentException("不支持的文件格式: " + ext);
            };
        }
    }

    /** 解析Word文档提取文本 */
    public String parseWord(MultipartFile file) throws IOException {
        return parseWordStream(file.getInputStream());
    }

    private String parseWordStream(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
            for (XWPFTable table : doc.getTables()) {
                sb.append("\n[表格开始]\n");
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cellTexts = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cellTexts.add(cell.getText().trim());
                    }
                    sb.append(String.join("\t|\t", cellTexts)).append("\n");
                }
                sb.append("[表格结束]\n\n");
            }
            return sb.toString().trim();
        }
    }

    /** 解析Excel提取数据为结构化文本描述 */
    public String parseExcel(MultipartFile file) throws IOException {
        return parseExcelStream(file.getInputStream());
    }

    private String parseExcelStream(InputStream is) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                sb.append("【Sheet: ").append(sheet.getSheetName()).append("】\n");
                for (var row : sheet) {
                    List<String> cellValues = new ArrayList<>();
                    for (var cell : row) {
                        cellValues.add(getCellValueAsString(cell));
                    }
                    sb.append(String.join("\t|\t", cellValues)).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        }
    }

    /** 解析纯文本文件(txt/md) */
    public String parseText(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    // ============================== Word生成 ==============================

    /** 根据AI生成的文本创建Word文档 */
    public byte[] generateWord(String title, String content) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            // 标题
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setFontFamily("方正小标宋简体");

            doc.createParagraph(); // 空行

            // 正文段落
            String[] paragraphs = content.split("\n");
            for (String text : paragraphs) {
                if (!text.trim().isEmpty()) {
                    XWPFParagraph para = doc.createParagraph();
                    para.setAlignment(ParagraphAlignment.LEFT);
                    XWPFRun run = para.createRun();
                    run.setText(text.trim());
                    run.setFontSize(14);
                    run.setFontFamily("仿宋");
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    // ============================== 模板分析 ==============================

    /**
     * 分析Word模板结构，提取需要填写的字段
     * 支持 {{字段名}} 占位符 和 表格空单元格
     */
    public String analyzeWordTemplate(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            sb.append("【Word模板结构】\n");

            // 分析段落中的占位符
            List<String> placeholders = new ArrayList<>();
            Pattern pattern = Pattern.compile("\\{\\{(.+?)\\}\\}");

            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append("段落: ").append(text).append("\n");
                    Matcher matcher = pattern.matcher(text);
                    while (matcher.find()) {
                        placeholders.add(matcher.group(1));
                    }
                }
            }

            // 分析表格结构
            for (XWPFTable table : doc.getTables()) {
                sb.append("\n[模板表格]\n");
                for (int rowIdx = 0; rowIdx < table.getRows().size(); rowIdx++) {
                    XWPFTableRow row = table.getRows().get(rowIdx);
                    List<String> cellTexts = new ArrayList<>();
                    for (int cellIdx = 0; cellIdx < row.getTableCells().size(); cellIdx++) {
                        XWPFTableCell cell = row.getTableCells().get(cellIdx);
                        String cellText = cell.getText().trim();
                        cellTexts.add(cellText.isEmpty() ? "[待填写]" : cellText);
                        // 检查占位符
                        Matcher matcher = pattern.matcher(cellText);
                        while (matcher.find()) {
                            placeholders.add(matcher.group(1));
                        }
                    }
                    sb.append("行").append(rowIdx).append(": ").append(String.join(" | ", cellTexts)).append("\n");
                }
            }

            if (!placeholders.isEmpty()) {
                sb.append("\n【需要填写的字段】\n");
                for (String p : placeholders) {
                    sb.append("- ").append(p).append("\n");
                }
            }

            return sb.toString();
        }
    }

    /**
     * 分析Excel模板结构，提取表头和需要填写的单元格
     */
    public String analyzeExcelTemplate(InputStream is) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            StringBuilder sb = new StringBuilder();
            sb.append("【Excel模板结构】\n");

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                sb.append("\nSheet: ").append(sheet.getSheetName()).append("\n");

                List<String> headers = new ArrayList<>();

                for (var row : sheet) {
                    List<String> cellValues = new ArrayList<>();
                    boolean hasData = false;

                    for (var cell : row) {
                        String value = getCellValueAsString(cell);
                        cellValues.add(value.isEmpty() ? "[待填写]" : value);
                        if (!value.isEmpty()) hasData = true;
                    }

                    if (row.getRowNum() == 0 && hasData) {
                        headers = cellValues;
                        sb.append("表头: ").append(String.join(" | ", cellValues)).append("\n");
                    } else {
                        sb.append("行").append(row.getRowNum()).append(": ")
                                .append(String.join(" | ", cellValues)).append("\n");
                    }
                }

                if (!headers.isEmpty()) {
                    sb.append("\n【列字段说明】\n");
                    for (int h = 0; h < headers.size(); h++) {
                        String header = headers.get(h).replace("[待填写]", "");
                        if (!header.isEmpty()) {
                            sb.append("列").append(h).append(": ").append(header).append("\n");
                        }
                    }
                }
            }

            return sb.toString();
        }
    }

    // ============================== 模板填充 ==============================

    /**
     * 填充Word模板 - 替换占位符 {{字段名}} 并填充空表格单元格
     */
    public byte[] fillWordTemplate(InputStream templateIs, Map<String, String> data) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(templateIs)) {
            // 替换段落中的占位符
            for (XWPFParagraph para : doc.getParagraphs()) {
                replacePlaceholdersInParagraph(para, data);
            }

            // 替换表格中的占位符和填充空单元格
            for (XWPFTable table : doc.getTables()) {
                fillWordTable(table, data);
            }

            // 替换页眉页脚中的占位符
            for (XWPFHeader header : doc.getHeaderList()) {
                for (XWPFParagraph para : header.getParagraphs()) {
                    replacePlaceholdersInParagraph(para, data);
                }
            }
            for (XWPFFooter footer : doc.getFooterList()) {
                for (XWPFParagraph para : footer.getParagraphs()) {
                    replacePlaceholdersInParagraph(para, data);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 填充Excel模板 - 填充空单元格和替换占位符
     */
    public byte[] fillExcelTemplate(InputStream templateIs, Map<String, String> data) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(templateIs)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                fillExcelSheet(sheet, data);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ============================== 私有辅助方法 ==============================

    private void replacePlaceholdersInParagraph(XWPFParagraph para, Map<String, String> data) {
        String fullText = para.getText();
        if (fullText == null || fullText.isEmpty()) return;

        Pattern pattern = Pattern.compile("\\{\\{(.+?)\\}\\}");
        Matcher matcher = pattern.matcher(fullText);
        if (!matcher.find()) return;

        // 需要处理跨Run的占位符情况
        // 先尝试在每个Run中替换
        for (XWPFRun run : para.getRuns()) {
            String runText = run.getText(0);
            if (runText == null) continue;
            Matcher runMatcher = pattern.matcher(runText);
            StringBuffer sb = new StringBuffer();
            while (runMatcher.find()) {
                String fieldName = runMatcher.group(1).trim();
                String value = data.getOrDefault(fieldName, "");
                runMatcher.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
            runMatcher.appendTail(sb);
            run.setText(sb.toString(), 0);
        }

        // 处理跨Run的占位符 - 合并所有Run的文本再替换
        String newFullText = para.getText();
        Matcher newMatcher = pattern.matcher(newFullText);
        if (newMatcher.find()) {
            // 还有未处理的占位符，需要合并Run处理
            String mergedText = newFullText;
            StringBuffer sb = new StringBuffer();
            Matcher m = pattern.matcher(mergedText);
            while (m.find()) {
                String fieldName = m.group(1).trim();
                String value = data.getOrDefault(fieldName, "");
                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
            m.appendTail(sb);

            // 清除所有现有run并创建新run
            while (para.getRuns().size() > 1) {
                para.removeRun(para.getRuns().size() - 1);
            }
            if (!para.getRuns().isEmpty()) {
                para.getRuns().get(0).setText(sb.toString(), 0);
            }
        }
    }

    private void fillWordTable(XWPFTable table, Map<String, String> data) {
        Pattern pattern = Pattern.compile("\\{\\{(.+?)\\}\\}");

        // 获取表头行（第一行）用于关联字段名
        List<String> headerNames = new ArrayList<>();
        if (!table.getRows().isEmpty()) {
            XWPFTableRow headerRow = table.getRows().get(0);
            for (XWPFTableCell cell : headerRow.getTableCells()) {
                headerNames.add(cell.getText().trim());
            }
        }

        for (int rowIdx = 0; rowIdx < table.getRows().size(); rowIdx++) {
            XWPFTableRow row = table.getRows().get(rowIdx);
            for (int cellIdx = 0; cellIdx < row.getTableCells().size(); cellIdx++) {
                XWPFTableCell cell = row.getTableCells().get(cellIdx);
                String cellText = cell.getText().trim();

                // 替换占位符
                Matcher matcher = pattern.matcher(cellText);
                if (matcher.find()) {
                    for (XWPFParagraph para : cell.getParagraphs()) {
                        replacePlaceholdersInParagraph(para, data);
                    }
                }
                // 如果单元格为空且有对应的表头字段名，尝试从数据中填充
                else if (cellText.isEmpty() && cellIdx < headerNames.size() && rowIdx > 0) {
                    String header = headerNames.get(cellIdx);
                    if (data.containsKey(header)) {
                        // 清除并填入数据
                        cell.removeParagraph(0);
                        XWPFParagraph newPara = cell.addParagraph();
                        XWPFRun run = newPara.createRun();
                        run.setText(data.get(header));
                        run.setFontSize(10);
                    }
                }
            }
        }
    }

    private void fillExcelSheet(XSSFSheet sheet, Map<String, String> data) {
        Pattern pattern = Pattern.compile("\\{\\{(.+?)\\}\\}");

        // 获取表头
        List<String> headers = new ArrayList<>();
        var headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (var cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }
        }

        // 遍历所有单元格
        for (var row : sheet) {
            for (var cell : row) {
                String value = getCellValueAsString(cell);

                // 替换占位符
                Matcher matcher = pattern.matcher(value);
                if (matcher.find()) {
                    StringBuffer sb = new StringBuffer();
                    matcher.reset();
                    while (matcher.find()) {
                        String fieldName = matcher.group(1).trim();
                        String replacement = data.getOrDefault(fieldName, "");
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    }
                    matcher.appendTail(sb);
                    cell.setCellValue(sb.toString());
                }
                // 如果为空且有对应表头，从数据填充
                else if (value.isEmpty() && row.getRowNum() > 0) {
                    int colIdx = cell.getColumnIndex();
                    if (colIdx < headers.size() && data.containsKey(headers.get(colIdx))) {
                        String fillValue = data.get(headers.get(colIdx));
                        // 尝试数值类型
                        try {
                            double num = Double.parseDouble(fillValue);
                            cell.setCellValue(num);
                        } catch (NumberFormatException e) {
                            cell.setCellValue(fillValue);
                        }
                    }
                }
            }
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    yield sdf.format(cell.getDateCellValue());
                } else {
                    double val = cell.getNumericCellValue();
                    if (val == Math.floor(val) && !Double.isInfinite(val)) {
                        yield String.valueOf((long) val);
                    }
                    yield String.valueOf(val);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }
}
