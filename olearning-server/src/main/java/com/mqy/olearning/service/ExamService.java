package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.dto.ExamResultVO;
import com.mqy.olearning.dto.ExamSubmitDTO;
import com.mqy.olearning.dto.QuestionVO;
import com.mqy.olearning.entity.*;
import com.mqy.olearning.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 测验：题库、组卷、答题、自动判分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final QuestionMapper questionMapper;
    private final ExamPaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final ExamRecordMapper recordMapper;
    private final CourseMapper courseMapper;
    private final EnrollmentService enrollmentService;
    private final ObjectMapper objectMapper;

    // ---------------- 题库 ----------------

    public List<Question> questions(Long courseId) {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .orderByDesc(Question::getId));
    }

    public void saveQuestion(Question question, Long teacherId) {
        checkOwner(question.getCourseId(), teacherId);
        if (question.getDifficulty() == null) {
            question.setDifficulty(1);
        }
        if (question.getId() == null) {
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
        }
    }

    public void removeQuestion(Long id, Long teacherId) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            return;
        }
        checkOwner(question.getCourseId(), teacherId);
        questionMapper.deleteById(id);
        paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getQuestionId, id));
    }

    // ---------------- 试卷 ----------------

    public List<ExamPaper> papers(Long courseId) {
        return paperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getCourseId, courseId)
                .orderByDesc(ExamPaper::getId));
    }

    public Long savePaper(ExamPaper paper, Long teacherId) {
        checkOwner(paper.getCourseId(), teacherId);
        if (paper.getTotalScore() == null) {
            paper.setTotalScore(100);
        }
        if (paper.getPassScore() == null) {
            paper.setPassScore(60);
        }
        if (paper.getDuration() == null) {
            paper.setDuration(30);
        }
        if (paper.getStatus() == null) {
            paper.setStatus(1);
        }
        if (paper.getId() == null) {
            paperMapper.insert(paper);
        } else {
            paperMapper.updateById(paper);
        }
        return paper.getId();
    }

    public void removePaper(Long paperId, Long teacherId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            return;
        }
        checkOwner(paper.getCourseId(), teacherId);
        paperMapper.deleteById(paperId);
        paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId));
    }

    /** 组卷：把题目挂到试卷上并指定分值 */
    public void addQuestion(Long paperId, Long questionId, Integer score) {
        Long exists = paperQuestionMapper.selectCount(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .eq(PaperQuestion::getQuestionId, questionId));
        if (exists != null && exists > 0) {
            throw new BusinessException("该题目已在试卷中");
        }
        PaperQuestion pq = new PaperQuestion();
        pq.setPaperId(paperId);
        pq.setQuestionId(questionId);
        pq.setScore(score == null ? 10 : score);
        pq.setSort(0);
        paperQuestionMapper.insert(pq);
    }

    public void removePaperQuestion(Long id) {
        paperQuestionMapper.deleteById(id);
    }

    /** 教师视角看试卷题目（带答案） */
    public List<QuestionVO> paperQuestions(Long paperId, boolean withAnswer) {
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .orderByAsc(PaperQuestion::getSort)
                .orderByAsc(PaperQuestion::getId));
        List<QuestionVO> result = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            QuestionVO vo = new QuestionVO();
            vo.setId(q.getId());
            vo.setType(q.getType());
            vo.setTitle(q.getTitle());
            vo.setScore(pq.getScore());
            vo.setOptions(parseOptions(q.getOptionsJson()));
            if (withAnswer) {
                vo.setAnswer(q.getAnswer());
                vo.setAnalysis(q.getAnalysis());
            }
            result.add(vo);
        }
        return result;
    }

    /** 学生开始答题：不返回答案和解析 */
    public Map<String, Object> startExam(Long paperId, Long userId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null || paper.getStatus() == null || paper.getStatus() != 1) {
            throw new BusinessException("试卷不存在或未启用");
        }
        if (!enrollmentService.isEnrolled(userId, paper.getCourseId())) {
            throw new BusinessException("请先选修该课程再参加测验");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("paper", paper);
        data.put("questions", paperQuestions(paperId, false));
        data.put("startTime", LocalDateTime.now().toString());
        return data;
    }

    /** 交卷判分：客观题自动判，多选需完全一致 */
    @Transactional
    public ExamResultVO submit(Long userId, ExamSubmitDTO dto) {
        ExamPaper paper = paperMapper.selectById(dto.getPaperId());
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        Map<String, String> answers = dto.getAnswers() == null ? Map.of() : dto.getAnswers();

        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paper.getId()));

        int score = 0;
        List<ExamResultVO.AnswerDetail> details = new ArrayList<>();

        for (PaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            String your = answers.getOrDefault(String.valueOf(q.getId()), "");
            boolean correct = normalize(your).equals(normalize(q.getAnswer()));
            if (correct) {
                score += pq.getScore() == null ? 0 : pq.getScore();
            }
            ExamResultVO.AnswerDetail detail = new ExamResultVO.AnswerDetail();
            detail.setQuestionId(q.getId());
            detail.setTitle(q.getTitle());
            detail.setYourAnswer(your);
            detail.setRightAnswer(q.getAnswer());
            detail.setCorrect(correct);
            detail.setScore(correct ? pq.getScore() : 0);
            detail.setAnalysis(q.getAnalysis());
            details.add(detail);
        }

        ExamRecord record = new ExamRecord();
        record.setPaperId(paper.getId());
        record.setUserId(userId);
        record.setAnswersJson(writeJson(answers));
        record.setScore(score);
        record.setPassed(score >= (paper.getPassScore() == null ? 60 : paper.getPassScore()) ? 1 : 0);
        record.setStartTime(LocalDateTime.now());
        record.setSubmitTime(LocalDateTime.now());
        recordMapper.insert(record);

        ExamResultVO vo = new ExamResultVO();
        vo.setRecordId(record.getId());
        vo.setScore(score);
        vo.setTotalScore(paper.getTotalScore());
        vo.setPassScore(paper.getPassScore());
        vo.setPassed(record.getPassed() == 1);
        vo.setDetails(details);
        return vo;
    }

    public List<Map<String, Object>> myRecords(Long userId) {
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .orderByDesc(ExamRecord::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord r : records) {
            ExamPaper paper = paperMapper.selectById(r.getPaperId());
            Map<String, Object> item = new HashMap<>();
            item.put("record", r);
            item.put("paperTitle", paper == null ? "已删除试卷" : paper.getTitle());
            item.put("totalScore", paper == null ? 100 : paper.getTotalScore());
            result.add(item);
        }
        return result;
    }

    /** 查看某次答卷的错题与解析 */
    public ExamResultVO recordDetail(Long recordId, Long userId) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能查看自己的答卷");
        }
        ExamPaper paper = paperMapper.selectById(record.getPaperId());
        Map<String, String> answers = readJson(record.getAnswersJson());

        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, record.getPaperId()));
        List<ExamResultVO.AnswerDetail> details = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            String your = answers.getOrDefault(String.valueOf(q.getId()), "");
            boolean correct = normalize(your).equals(normalize(q.getAnswer()));
            ExamResultVO.AnswerDetail detail = new ExamResultVO.AnswerDetail();
            detail.setQuestionId(q.getId());
            detail.setTitle(q.getTitle());
            detail.setYourAnswer(your);
            detail.setRightAnswer(q.getAnswer());
            detail.setCorrect(correct);
            detail.setScore(correct ? pq.getScore() : 0);
            detail.setAnalysis(q.getAnalysis());
            details.add(detail);
        }

        ExamResultVO vo = new ExamResultVO();
        vo.setRecordId(record.getId());
        vo.setScore(record.getScore());
        vo.setTotalScore(paper == null ? 100 : paper.getTotalScore());
        vo.setPassScore(paper == null ? 60 : paper.getPassScore());
        vo.setPassed(record.getPassed() != null && record.getPassed() == 1);
        vo.setDetails(details);
        return vo;
    }

    /** 教师端：本课程所有考试记录 */
    public List<Map<String, Object>> courseRecords(Long courseId, Long teacherId) {
        checkOwner(courseId, teacherId);
        List<ExamPaper> papers = papers(courseId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamPaper p : papers) {
            List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getPaperId, p.getId()));
            for (ExamRecord r : records) {
                Map<String, Object> item = new HashMap<>();
                item.put("record", r);
                item.put("paperTitle", p.getTitle());
                result.add(item);
            }
        }
        return result;
    }

    // ---------------- 工具方法 ----------------

    /** 答案标准化：去空格、转大写、多选按字母排序后比较 */
    private String normalize(String answer) {
        if (answer == null) {
            return "";
        }
        String[] parts = answer.toUpperCase().replace(" ", "").split(",");
        Arrays.sort(parts);
        return String.join(",", parts);
    }

    public List<Map<String, String>> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {
            });
        } catch (Exception e) {
            log.warn("选项 JSON 解析失败: {}", json);
            return List.of();
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, String> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void checkOwner(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (teacherId != null && !teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(403, "只能操作自己的课程");
        }
    }
}
