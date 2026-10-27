package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.entity.Question;
import com.mqy.olearning.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 题库导入导出：统一用 xlsx 交换数据。
 * <p>导出的文件可以直接改完再导回来，模板与导出表头完全一致。
 * 导入逐行校验，任何一行出错都不影响其它行，最后统一返回明细。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionIOService {

    /** 表头必须与导入解析保持一致 */
    public static final String[] HEADER = {
            "题型", "题干", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F",
            "答案", "参考答案", "解析", "难度"
    };

    private static final Map<String, Integer> TYPES = new java.util.HashMap<>();

    static {
        TYPES.put("单选", ExamService.TYPE_SINGLE);
        TYPES.put("多选", ExamService.TYPE_MULTI);
        TYPES.put("判断", ExamService.TYPE_JUDGE);
        TYPES.put("填空", ExamService.TYPE_BLANK);
        TYPES.put("问答", ExamService.TYPE_ESSAY);
    }

    private static final Map<String, Integer> DIFFICULTIES = Map.of(
            "简单", 1, "中等", 2, "困难", 3
    );

    private static final String[] LETTERS = {"A", "B", "C", "D", "E", "F"};

    private final QuestionMapper questionMapper;
    private final ExamService examService;

    // ==================== 导出 ====================

    public byte[] export(Long courseId, Long teacherId) {
        List<Question> questions = examService.questions(courseId);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("题库");
            writeHeader(sheet);
            int rowNo = 1;
            for (Question q : questions) {
                Row row = sheet.createRow(rowNo++);
                writeQuestion(row, q);
            }
            autoWidth(sheet);
            return toBytes(wb);
        } catch (Exception e) {
            log.error("题库导出失败 courseId={}", courseId, e);
            throw new BusinessException("题库导出失败，请稍后重试");
        }
    }

    public byte[] template() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("题库");
            writeHeader(sheet);

            Row sample1 = sheet.createRow(1);
            fill(sample1, new String[]{
                    "单选", "Java 中 int 占用多少个字节？", "2", "4", "8", "16", "", "",
                    "B", "", "int 是 32 位有符号整数，占 4 个字节", "简单"
            });
            Row sample2 = sheet.createRow(2);
            fill(sample2, new String[]{
                    "填空", "HTTP 默认端口是 ____，HTTPS 默认端口是 ____。", "", "", "", "", "", "",
                    "80|443", "", "多空用 | 分隔；同一个空有多种写法时用 ~ 分隔，如 final~FINAL", "简单"
            });
            Row sample3 = sheet.createRow(3);
            fill(sample3, new String[]{
                    "问答", "简述 TCP 三次握手的过程。", "", "", "", "", "", "",
                    "", "三次握手：SYN → SYN+ACK → ACK，目的是确认双方收发能力并同步初始序列号。",
                    "答出三个步骤与目的即可给分", "中等"
            });

            Sheet guide = wb.createSheet("填写说明");
            String[] tips = {
                    "1. 表头不要改、不要删，第一行必须是列名。",
                    "2. 题型只能填：单选 / 多选 / 判断 / 填空 / 问答。",
                    "3. 单选答案填一个字母（如 B）；多选填多个字母并用英文逗号分隔（如 A,C）。",
                    "4. 判断题答案填 T 或 F。",
                    "5. 填空题多个空用 | 分隔；同一个空有多种正确写法用 ~ 分隔（如 final~FINAL）。",
                    "6. 问答题不需要标准答案，「参考答案」可选填，供教师批阅时对照。",
                    "7. 难度只能填：简单 / 中等 / 困难，留空按「简单」处理。",
                    "8. 从第 2 行开始填题目，一行一题，不要留空行。"
            };
            for (int i = 0; i < tips.length; i++) {
                guide.createRow(i).createCell(0).setCellValue(tips[i]);
            }
            guide.setColumnWidth(0, 120 * 256);
            autoWidth(sheet);
            return toBytes(wb);
        } catch (Exception e) {
            log.error("题库模板生成失败", e);
            throw new BusinessException("模板生成失败，请稍后重试");
        }
    }

    // ==================== 导入 ====================

    public Map<String, Object> importQuestions(Long courseId, Long teacherId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) {
            throw new BusinessException("只支持 .xlsx 格式，请先下载模板再填写");
        }

        int success = 0;
        List<Map<String, Object>> failed = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream in = file.getInputStream(); XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getLastRowNum() < 1) {
                throw new BusinessException("文件里没有题目数据，请从第 2 行开始填写");
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String typeText = cell(row, 0, formatter);
                String title = cell(row, 1, formatter);
                if (typeText.isBlank() && title.isBlank()) {
                    continue;
                }
                try {
                    Question q = buildQuestion(courseId, row, formatter);
                    examService.saveQuestion(q, teacherId);
                    success++;
                } catch (BusinessException e) {
                    failed.add(failRow(r + 1, e.getMessage()));
                } catch (Exception e) {
                    log.warn("题库导入第 {} 行失败", r + 1, e);
                    failed.add(failRow(r + 1, "该行数据无法解析"));
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("题库导入失败 courseId={}", courseId, e);
            throw new BusinessException("文件解析失败，请确认是模板格式的 .xlsx 文件");
        }

        if (success == 0 && failed.isEmpty()) {
            throw new BusinessException("没有读到有效题目，请检查文件内容");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("total", success + failed.size());
        return result;
    }

    private Question buildQuestion(Long courseId, Row row, DataFormatter formatter) {
        String typeText = cell(row, 0, formatter).replace("题", "");
        Integer type = TYPES.get(typeText);
        if (type == null) {
            throw new BusinessException("题型只能是 单选/多选/判断/填空/问答");
        }
        String title = cell(row, 1, formatter);
        if (title.isBlank()) {
            throw new BusinessException("题干不能为空");
        }

        Question q = new Question();
        q.setCourseId(courseId);
        q.setType(type);
        q.setTitle(title);

        List<String> options = new ArrayList<>();
        for (int i = 0; i < LETTERS.length; i++) {
            String text = cell(row, 2 + i, formatter);
            if (!text.isBlank()) {
                options.add(text);
            }
        }

        String answer = cell(row, 8, formatter).toUpperCase().replace("，", ",");
        String reference = cell(row, 9, formatter);
        String analysis = cell(row, 10, formatter);
        q.setAnalysis(analysis.isBlank() ? null : analysis);
        q.setDifficulty(DIFFICULTIES.getOrDefault(cell(row, 11, formatter), 1));

        if (type == ExamService.TYPE_SINGLE || type == ExamService.TYPE_MULTI) {
            if (options.size() < 2) {
                throw new BusinessException("选择题至少需要两个选项");
            }
            if (answer.isBlank()) {
                throw new BusinessException("选择题必须填写答案");
            }
            if (type == ExamService.TYPE_SINGLE && answer.contains(",")) {
                throw new BusinessException("单选题只能有一个答案");
            }
            for (String one : answer.split(",")) {
                int idx = "ABCDEF".indexOf(one.trim());
                if (idx < 0 || idx >= options.size()) {
                    throw new BusinessException("答案 " + one + " 超出选项范围");
                }
            }
            q.setAnswer(type == ExamService.TYPE_MULTI ? answer.replace(" ", "") : answer.trim());
            List<Map<String, String>> opts = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
                Map<String, String> opt = new LinkedHashMap<>();
                opt.put("key", LETTERS[i]);
                opt.put("text", options.get(i));
                opts.add(opt);
            }
            q.setOptionsJson(toJson(opts));
        } else if (type == ExamService.TYPE_JUDGE) {
            String normalized = switch (answer) {
                case "T", "TRUE", "对", "正确", "√" -> "T";
                case "F", "FALSE", "错", "错误", "×" -> "F";
                default -> "";
            };
            if (normalized.isEmpty()) {
                throw new BusinessException("判断题答案只能填 T 或 F");
            }
            q.setAnswer(normalized);
        } else if (type == ExamService.TYPE_BLANK) {
            if (answer.isBlank()) {
                throw new BusinessException("填空题必须填写答案，多个空用 | 分隔");
            }
            q.setAnswer(cell(row, 8, formatter));
        } else {
            // 问答题：不设标准答案
            q.setAnswer(null);
            q.setReferenceAnswer(reference.isBlank() ? null : reference);
        }
        return q;
    }

    // ==================== 工具 ====================

    private void writeHeader(Sheet sheet) {
        Row row = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < HEADER.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(HEADER[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeQuestion(Row row, Question q) {
        int type = q.getType() == null ? 1 : q.getType();
        String typeName = switch (type) {
            case 2 -> "多选";
            case 3 -> "判断";
            case 4 -> "填空";
            case 5 -> "问答";
            default -> "单选";
        };
        String[] values = new String[HEADER.length];
        values[0] = typeName;
        values[1] = q.getTitle();
        List<Map<String, String>> options = q.getOptionsJson() == null
                ? List.of() : examService.parseOptions(q.getOptionsJson());
        for (int i = 0; i < options.size() && i < LETTERS.length; i++) {
            Object text = options.get(i).get("text");
            values[2 + i] = text == null ? "" : text.toString();
        }
        values[8] = q.getAnswer() == null ? "" : q.getAnswer();
        values[9] = q.getReferenceAnswer() == null ? "" : q.getReferenceAnswer();
        values[10] = q.getAnalysis() == null ? "" : q.getAnalysis();
        values[11] = switch (q.getDifficulty() == null ? 1 : q.getDifficulty()) {
            case 2 -> "中等";
            case 3 -> "困难";
            default -> "简单";
        };
        fill(row, values);
    }

    private void fill(Row row, String[] values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i] == null ? "" : values[i]);
        }
    }

    private void autoWidth(Sheet sheet) {
        for (int i = 0; i < HEADER.length; i++) {
            sheet.setColumnWidth(i, (i == 1 ? 60 : 16) * 256);
        }
    }

    private String cell(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private Map<String, Object> failRow(int rowNo, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("row", rowNo);
        item.put("message", message);
        return item;
    }

    private byte[] toBytes(XSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 统计某课程的题目数，供导出文件名/提示使用 */
    public long count(Long courseId) {
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId));
        return count == null ? 0 : count;
    }
}
