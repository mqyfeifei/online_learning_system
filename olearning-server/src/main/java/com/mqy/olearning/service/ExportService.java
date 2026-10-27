package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.entity.Assignment;
import com.mqy.olearning.entity.Course;
import com.mqy.olearning.entity.Enrollment;
import com.mqy.olearning.entity.ExamPaper;
import com.mqy.olearning.entity.PaperQuestion;
import com.mqy.olearning.entity.Question;
import com.mqy.olearning.entity.Submission;
import com.mqy.olearning.entity.SysUser;
import com.mqy.olearning.mapper.AssignmentMapper;
import com.mqy.olearning.mapper.CourseMapper;
import com.mqy.olearning.mapper.EnrollmentMapper;
import com.mqy.olearning.mapper.ExamPaperMapper;
import com.mqy.olearning.mapper.PaperQuestionMapper;
import com.mqy.olearning.mapper.QuestionMapper;
import com.mqy.olearning.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作业 / 试卷导出为 docx。
 * <p>导出范围由课程设置控制：
 * <ul>
 *   <li>NONE：不开放导出（教师本人除外）</li>
 *   <li>ENROLLED：仅已选课学生可导出</li>
 *   <li>ALL：平台登录用户均可导出</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CourseMapper courseMapper;
    private final AssignmentMapper assignmentMapper;
    private final com.mqy.olearning.mapper.SubmissionMapper submissionMapper;
    private final ExamPaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final SysUserMapper userMapper;

    // ---------------- 作业导出 ----------------

    public byte[] exportAssignment(Long courseId, Long assignmentId, Long userId, String role) {
        Course course = requireCourse(courseId);
        checkScope(course, userId, role, "作业");

        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null || !courseId.equals(assignment.getCourseId())) {
            throw new BusinessException("作业不存在");
        }

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            title(doc, assignment.getTitle());
            subtitle(doc, "课程：" + course.getTitle()
                    + "　　导出时间：" + java.time.LocalDateTime.now().format(DT));

            heading(doc, "一、作业要求");
            body(doc, blankTo(assignment.getRequirement(), "（教师未填写具体要求）"));
            heading(doc, "二、截止时间");
            body(doc, assignment.getDeadline() == null ? "未设置" : assignment.getDeadline().format(DT));

            List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                    .eq(Submission::getAssignmentId, assignmentId)
                    .orderByAsc(Submission::getId));
            heading(doc, "三、提交情况（共 " + submissions.size() + " 人）");
            if (submissions.isEmpty()) {
                body(doc, "暂无学生提交。");
            } else {
                XWPFTable table = doc.createTable();
                String[] header = {"序号", "学生", "提交时间", "得分", "状态"};
                fillRow(table.getRow(0), header);
                int index = 1;
                for (Submission s : submissions) {
                    SysUser user = userMapper.selectById(s.getUserId());
                    XWPFTableRow row = table.createRow();
                    fillRow(row, new String[]{
                            String.valueOf(index++),
                            user == null ? "未知用户" : displayName(user),
                            s.getSubmitTime() == null ? "" : s.getSubmitTime().format(DT),
                            s.getScore() == null ? "未批阅" : s.getScore().toString(),
                            s.getStatus() != null && s.getStatus() == 1 ? "已批阅" : "待批阅"
                    });
                }
            }

            heading(doc, "四、学生作答摘录");
            if (submissions.isEmpty()) {
                body(doc, "暂无内容。");
            }
            for (Submission s : submissions) {
                SysUser user = userMapper.selectById(s.getUserId());
                body(doc, "【" + (user == null ? "未知用户" : displayName(user)) + "】");
                body(doc, blankTo(s.getContent(), "（仅提交附件，无文字说明）"));
                if (s.getComment() != null && !s.getComment().isBlank()) {
                    body(doc, "教师评语：" + s.getComment());
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("导出作业失败：" + e.getMessage());
        }
    }

    // ---------------- 试卷导出 ----------------

    public byte[] exportPaper(Long courseId, Long paperId, Long userId, String role, boolean withAnswer) {
        Course course = requireCourse(courseId);
        checkScope(course, userId, role, "试卷");

        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null || !courseId.equals(paper.getCourseId())) {
            throw new BusinessException("试卷不存在");
        }

        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .orderByAsc(PaperQuestion::getSort)
                .orderByAsc(PaperQuestion::getId));

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            title(doc, paper.getTitle());
            subtitle(doc, "课程：" + course.getTitle()
                    + "　　总分：" + paper.getTotalScore()
                    + "　　时长：" + paper.getDuration() + " 分钟"
                    + (withAnswer ? "　　（含参考答案，教师用）" : ""));

            int no = 1;
            for (PaperQuestion pq : pqs) {
                Question q = questionMapper.selectById(pq.getQuestionId());
                if (q == null) {
                    continue;
                }
                String typeName = typeName(q.getType());
                paragraph(doc, no + "．[ " + typeName + " · " + pq.getScore() + " 分 ] " + q.getTitle(), true);

                // 选择题选项
                if (q.getOptionsJson() != null && !q.getOptionsJson().isBlank()) {
                    for (Map<String, String> option : parseOptions(q.getOptionsJson())) {
                        body(doc, "　　" + option.get("key") + "．" + option.get("text"));
                    }
                }
                if (q.getType() != null && q.getType() == 4) {
                    body(doc, "　　答：______________________________________________");
                }
                if (q.getType() != null && q.getType() == 5) {
                    for (int i = 0; i < 4; i++) {
                        body(doc, "　　________________________________________________________________");
                    }
                }
                if (withAnswer) {
                    if (q.getAnswer() != null && !q.getAnswer().isBlank()) {
                        body(doc, "　　【标准答案】" + q.getAnswer());
                    }
                    if (q.getReferenceAnswer() != null && !q.getReferenceAnswer().isBlank()) {
                        body(doc, "　　【参考答案】" + q.getReferenceAnswer());
                    }
                    if (q.getAnalysis() != null && !q.getAnalysis().isBlank()) {
                        body(doc, "　　【解析】" + q.getAnalysis());
                    }
                }
                no++;
            }

            doc.write(out);
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("导出试卷失败：" + e.getMessage());
        }
    }

    // ---------------- 权限与格式化 ----------------

    private Course requireCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }

    private void checkScope(Course course, Long userId, String role, String what) {
        // 授课教师与管理员不受课程开关限制
        if ("ADMIN".equals(role) || (userId != null && userId.equals(course.getTeacherId()))) {
            return;
        }
        String scope = course.getExportScope() == null ? "ENROLLED" : course.getExportScope();
        if ("NONE".equals(scope)) {
            throw new BusinessException(403, "本课程未开放" + what + "导出");
        }
        if (!"ALL".equals(scope)) {
            Long count = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                    .eq(Enrollment::getUserId, userId)
                    .eq(Enrollment::getCourseId, course.getId())
                    .eq(Enrollment::getStatus, 1));
            if (count == null || count == 0) {
                throw new BusinessException(403, "选修本课程后才可导出" + what);
            }
        }
    }

    private String typeName(Integer type) {
        if (type == null) {
            return "题目";
        }
        return switch (type) {
            case 1 -> "单选题";
            case 2 -> "多选题";
            case 3 -> "判断题";
            case 4 -> "填空题";
            case 5 -> "问答题";
            default -> "题目";
        };
    }

    private String displayName(SysUser user) {
        return user.getRealName() == null || user.getRealName().isBlank()
                ? user.getUsername() : user.getRealName() + "（" + user.getUsername() + "）";
    }

    private String blankTo(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private List<Map<String, String>> parseOptions(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {
                    });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void title(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(18);
        run.setFontFamily("微软雅黑");
    }

    private void subtitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(10);
        run.setColor("6B6259");
        run.setFontFamily("微软雅黑");
        p.setSpacingAfter(240);
    }

    private void heading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(13);
        run.setFontFamily("微软雅黑");
        p.setSpacingBefore(200);
        p.setSpacingAfter(80);
    }

    private void body(XWPFDocument doc, String text) {
        paragraph(doc, text, false);
    }

    private void paragraph(XWPFDocument doc, String text, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(11);
        run.setFontFamily("微软雅黑");
        p.setSpacingAfter(60);
        p.setIndentationFirstLine(0);
    }

    private void fillRow(XWPFTableRow row, String[] values) {
        for (int i = 0; i < values.length; i++) {
            XWPFTableCell cell = i < row.getTableCells().size()
                    ? row.getCell(i) : row.addNewTableCell();
            cell.setText(values[i] == null ? "" : values[i]);
        }
    }

    /** 供控制器校验用：返回课程导出范围，前端据此决定是否显示导出按钮 */
    public String scopeOf(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        return course == null || course.getExportScope() == null ? "ENROLLED" : course.getExportScope();
    }

    public List<Long> teacherCourseIds(Long teacherId) {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getTeacherId, teacherId))
                .stream().map(Course::getId).collect(Collectors.toList());
    }
}
