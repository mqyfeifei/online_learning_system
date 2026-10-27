package com.mqy.olearning.service;

import com.mqy.olearning.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Office 文档解析：把 docx / pptx / xlsx 拆成前端可渲染的结构。
 * <p>项目不引入 LibreOffice，纯 POI 解析能保证版式之外的内容（文字、表格、图片）完整可见，
 * 对课件「在线查看」这个场景够用，也避免了额外服务依赖。
 */
@Slf4j
@Service
public class DocPreviewService {

    /** 单张内嵌图片最大 4MB，超过就跳过，避免响应体过大 */
    private static final long MAX_IMAGE_BYTES = 4L * 1024 * 1024;

    public Map<String, Object> preview(File file, String ext) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", file.getName());
        switch (ext) {
            case "docx":
                data.put("kind", "docx");
                data.put("blocks", parseDocx(file));
                break;
            case "doc":
                // 旧版二进制 Word：POI 只能取到纯段落，没有版式，但内容完整
                data.put("kind", "docx");
                data.put("blocks", parseDoc(file));
                break;
            case "pptx":
                data.put("kind", "pptx");
                data.put("slides", parsePptx(file));
                break;
            case "ppt":
                data.put("kind", "pptx");
                data.put("slides", parsePpt(file));
                break;
            case "xlsx":
                data.put("kind", "xlsx");
                data.put("sheets", parseXlsx(file));
                break;
            case "xls":
                data.put("kind", "xlsx");
                data.put("sheets", parseXls(file));
                break;
            default:
                throw new BusinessException("暂不支持该格式的在线解析，请下载后查看");
        }
        return data;
    }

    // ---------------- Word ----------------

    private List<Map<String, Object>> parseDocx(File file) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        try (InputStream in = new FileInputStream(file); XWPFDocument doc = new XWPFDocument(in)) {
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        Map<String, Object> block = new LinkedHashMap<>();
                        block.put("type", "paragraph");
                        block.put("text", text);
                        block.put("heading", isHeading(paragraph));
                        block.put("align", paragraph.getAlignment() == null
                                ? "left" : paragraph.getAlignment().name().toLowerCase());
                        blocks.add(block);
                    }
                    // 段落里的图片单独成块，保持顺序
                    for (XWPFRun run : paragraph.getRuns()) {
                        for (XWPFPicture picture : run.getEmbeddedPictures()) {
                            byte[] bytes = picture.getPictureData().getData();
                            String dataUrl = toDataUrl(bytes, picture.getPictureData().suggestFileExtension());
                            if (dataUrl != null) {
                                Map<String, Object> img = new LinkedHashMap<>();
                                img.put("type", "image");
                                img.put("src", dataUrl);
                                blocks.add(img);
                            }
                        }
                    }
                } else if (element instanceof XWPFTable table) {
                    List<List<String>> rows = new ArrayList<>();
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            cells.add(cell.getText());
                        }
                        rows.add(cells);
                    }
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("type", "table");
                    block.put("rows", rows);
                    blocks.add(block);
                }
            }
        } catch (Exception e) {
            log.warn("docx 解析失败: {}", file.getName(), e);
            throw new BusinessException("文档解析失败，可能文件已损坏，请下载后查看");
        }
        return blocks;
    }

    private boolean isHeading(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        if (style != null && style.toLowerCase().contains("heading")) {
            return true;
        }
        String text = paragraph.getText();
        return text != null && text.length() <= 40 && paragraph.getRuns().stream()
                .anyMatch(r -> r.getFontSizeAsDouble() != null && r.getFontSizeAsDouble() >= 16);
    }

    // ---------------- PowerPoint ----------------

    private List<Map<String, Object>> parsePptx(File file) {
        List<Map<String, Object>> slides = new ArrayList<>();
        try (InputStream in = new FileInputStream(file); XMLSlideShow show = new XMLSlideShow(in)) {
            int index = 1;
            for (XSLFSlide slide : show.getSlides()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", index++);
                List<String> lines = new ArrayList<>();
                List<String> images = new ArrayList<>();
                String title = null;

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text == null || text.isBlank()) {
                            continue;
                        }
                        boolean placeholderTitle = textShape.getPlaceholder() != null
                                && textShape.getPlaceholder() == org.apache.poi.sl.usermodel.Placeholder.TITLE;
                        if (placeholderTitle || title == null) {
                            title = text.trim();
                        }
                        for (String line : text.split("\\R")) {
                            if (!line.isBlank()) {
                                lines.add(line.trim());
                            }
                        }
                    } else if (shape instanceof XSLFPictureShape pictureShape) {
                        PictureData pictureData = pictureShape.getPictureData();
                        String dataUrl = pictureData == null ? null
                                : toDataUrl(pictureData.getData(), pictureData.getType() == null
                                ? null : pictureData.getType().extension);
                        if (dataUrl != null) {
                            images.add(dataUrl);
                        }
                    }
                }
                item.put("title", title == null ? ("第 " + (index - 1) + " 页") : title);
                item.put("lines", lines);
                item.put("images", images);
                slides.add(item);
            }
        } catch (Exception e) {
            log.warn("pptx 解析失败: {}", file.getName(), e);
            throw new BusinessException("幻灯片解析失败，可能文件已损坏，请下载后查看");
        }
        return slides;
    }

    // ---------------- Excel ----------------
    private List<Map<String, Object>> parseXlsx(File file) {
        List<Map<String, Object>> sheets = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream in = new FileInputStream(file); Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(in)) {
            sheets.addAll(readSheets(workbook, formatter));
        } catch (Exception e) {
            log.warn("xlsx 解析失败: {}", file.getName(), e);
            throw new BusinessException("表格解析失败，可能文件已损坏，请下载后查看");
        }
        return sheets;
    }

    // ---------------- 旧版二进制格式（doc / xls / ppt） ----------------

    private List<Map<String, Object>> parseDoc(File file) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        try (InputStream in = new FileInputStream(file);
             org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(in)) {
            org.apache.poi.hwpf.extractor.WordExtractor extractor =
                    new org.apache.poi.hwpf.extractor.WordExtractor(doc);
            for (String text : extractor.getParagraphText()) {
                String line = text == null ? "" : text.replaceAll("[\\r\\n\\u0007]", "").trim();
                if (line.isEmpty()) {
                    continue;
                }
                Map<String, Object> block = new LinkedHashMap<>();
                block.put("type", "paragraph");
                block.put("text", line);
                block.put("heading", line.length() <= 30 && !line.endsWith("。"));
                blocks.add(block);
            }
        } catch (Exception e) {
            log.warn("doc 解析失败: {}", file.getName(), e);
            throw new BusinessException("文档解析失败，可能文件已损坏，请下载后查看");
        }
        return blocks;
    }

    private List<Map<String, Object>> parseXls(File file) {
        List<Map<String, Object>> sheets = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream in = new FileInputStream(file);
             Workbook workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook(in)) {
            sheets.addAll(readSheets(workbook, formatter));
        } catch (Exception e) {
            log.warn("xls 解析失败: {}", file.getName(), e);
            throw new BusinessException("表格解析失败，可能文件已损坏，请下载后查看");
        }
        return sheets;
    }

    private List<Map<String, Object>> parsePpt(File file) {
        List<Map<String, Object>> slides = new ArrayList<>();
        try (InputStream in = new FileInputStream(file);
             org.apache.poi.hslf.usermodel.HSLFSlideShow show =
                     new org.apache.poi.hslf.usermodel.HSLFSlideShow(in)) {
            int index = 1;
            for (org.apache.poi.hslf.usermodel.HSLFSlide slide : show.getSlides()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", index);
                List<String> lines = new ArrayList<>();
                List<String> images = new ArrayList<>();
                String title = null;
                for (org.apache.poi.hslf.usermodel.HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof org.apache.poi.hslf.usermodel.HSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text == null || text.isBlank()) {
                            continue;
                        }
                        if (title == null) {
                            title = text.trim();
                        }
                        for (String line : text.split("\\R")) {
                            if (!line.isBlank()) {
                                lines.add(line.trim());
                            }
                        }
                    } else if (shape instanceof org.apache.poi.hslf.usermodel.HSLFPictureShape pic) {
                        PictureData pictureData = pic.getPictureData();
                        String dataUrl = pictureData == null ? null
                                : toDataUrl(pictureData.getData(), pictureData.getType() == null
                                ? null : pictureData.getType().extension);
                        if (dataUrl != null) {
                            images.add(dataUrl);
                        }
                    }
                }
                item.put("title", title == null ? ("第 " + index + " 页") : title);
                item.put("lines", lines);
                item.put("images", images);
                item.put("index", index++);
                slides.add(item);
            }
        } catch (Exception e) {
            log.warn("ppt 解析失败: {}", file.getName(), e);
            throw new BusinessException("幻灯片解析失败，可能文件已损坏，请下载后查看");
        }
        return slides;
    }

    /** xlsx / xls 共用：把工作簿的所有工作表读成二维字符串 */
    private List<Map<String, Object>> readSheets(Workbook workbook, DataFormatter formatter) {
        List<Map<String, Object>> sheets = new ArrayList<>();
        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);
            List<List<String>> rows = new ArrayList<>();
            int last = Math.min(sheet.getLastRowNum(), 200);
            for (int r = 0; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                List<String> cells = new ArrayList<>();
                int lastCell = Math.min(row.getLastCellNum(), 30);
                for (int c = 0; c < lastCell; c++) {
                    Cell cell = row.getCell(c);
                    cells.add(cell == null ? "" : formatter.formatCellValue(cell));
                }
                rows.add(cells);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", sheet.getSheetName());
            item.put("rows", rows);
            sheets.add(item);
        }
        return sheets;
    }

    // ---------------- 工具 ----------------

    private String toDataUrl(byte[] bytes, String ext) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            return null;
        }
        String mime = switch (ext == null ? "" : ext.toLowerCase()) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            default -> "image/jpeg";
        };
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
