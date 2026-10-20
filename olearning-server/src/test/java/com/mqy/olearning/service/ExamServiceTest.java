package com.mqy.olearning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mqy.olearning.dto.ExamResultVO;
import com.mqy.olearning.dto.ExamSubmitDTO;
import com.mqy.olearning.entity.ExamPaper;
import com.mqy.olearning.entity.PaperQuestion;
import com.mqy.olearning.entity.Question;
import com.mqy.olearning.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 测验自动判分单元测试（对应测试用例 TC-06）
 */
@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock private QuestionMapper questionMapper;
    @Mock private ExamPaperMapper paperMapper;
    @Mock private PaperQuestionMapper paperQuestionMapper;
    @Mock private ExamRecordMapper recordMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private EnrollmentService enrollmentService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ExamService examService;

    @BeforeEach
    void setUp() {
        ExamPaper paper = new ExamPaper();
        paper.setId(1L);
        paper.setCourseId(1L);
        paper.setTotalScore(30);
        paper.setPassScore(20);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        // 三道题：单选 A / 多选 A,C / 判断 T，各 10 分
        when(paperQuestionMapper.selectList(any())).thenReturn(List.of(
                pq(1L, 10), pq(2L, 10), pq(3L, 10)));

        when(questionMapper.selectById(1L)).thenReturn(question(1L, 1, "A"));
        when(questionMapper.selectById(2L)).thenReturn(question(2L, 2, "A,C"));
        when(questionMapper.selectById(3L)).thenReturn(question(3L, 3, "T"));
    }

    private PaperQuestion pq(Long questionId, Integer score) {
        PaperQuestion item = new PaperQuestion();
        item.setPaperId(1L);
        item.setQuestionId(questionId);
        item.setScore(score);
        return item;
    }

    private Question question(Long id, Integer type, String answer) {
        Question q = new Question();
        q.setId(id);
        q.setType(type);
        q.setTitle("题目" + id);
        q.setAnswer(answer);
        q.setAnalysis("解析" + id);
        return q;
    }

    @Test
    @DisplayName("TC-06 全对得满分并判定及格")
    void allCorrect() {
        ExamSubmitDTO dto = new ExamSubmitDTO();
        dto.setPaperId(1L);
        dto.setAnswers(Map.of("1", "A", "2", "A,C", "3", "T"));

        ExamResultVO result = examService.submit(4L, dto);
        assertEquals(30, result.getScore());
        assertTrue(result.getPassed());
        assertTrue(result.getDetails().stream().allMatch(ExamResultVO.AnswerDetail::getCorrect));
    }

    @Test
    @DisplayName("多选答案顺序颠倒（C,A）也应判对")
    void multiChoiceOrderShouldNotMatter() {
        ExamSubmitDTO dto = new ExamSubmitDTO();
        dto.setPaperId(1L);
        dto.setAnswers(Map.of("1", "A", "2", "C,A", "3", "T"));

        ExamResultVO result = examService.submit(4L, dto);
        assertEquals(30, result.getScore());
    }

    @Test
    @DisplayName("答案带空格和小写也应判对")
    void answerShouldBeNormalized() {
        ExamSubmitDTO dto = new ExamSubmitDTO();
        dto.setPaperId(1L);
        dto.setAnswers(Map.of("1", " a ", "2", "c, a", "3", "t"));

        ExamResultVO result = examService.submit(4L, dto);
        assertEquals(30, result.getScore());
    }

    @Test
    @DisplayName("答对 2 题（20 分）刚好达到及格线，且错题带解析")
    void partialCorrect() {
        ExamSubmitDTO dto = new ExamSubmitDTO();
        dto.setPaperId(1L);
        // 第 3 题判断题答错
        dto.setAnswers(Map.of("1", "A", "2", "A,C", "3", "F"));

        ExamResultVO result = examService.submit(4L, dto);
        assertEquals(20, result.getScore());
        assertTrue(result.getPassed(), "20 分应达到 20 分及格线");
        assertFalse(result.getDetails().get(2).getCorrect());
        assertEquals("解析3", result.getDetails().get(2).getAnalysis());
    }

    @Test
    @DisplayName("漏答的题目按错处理，得 0 分")
    void missingAnswerCountsWrong() {
        ExamSubmitDTO dto = new ExamSubmitDTO();
        dto.setPaperId(1L);
        dto.setAnswers(Map.of("1", "A"));

        ExamResultVO result = examService.submit(4L, dto);
        assertEquals(10, result.getScore());
        assertFalse(result.getPassed());
    }
}
