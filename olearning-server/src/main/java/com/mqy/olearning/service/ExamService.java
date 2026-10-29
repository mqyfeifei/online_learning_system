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
 * 测验：题库、组卷、答题、判分与主观题批阅。
 * <p>判分规则：
 * <ul>
 *   <li>单选 / 多选 / 判断：客观题，交卷即自动判分</li>
 *   <li>填空题：按空比对，忽略大小写与首尾空白，同一空多种写法用 ~ 分隔</li>
 *   <li>问答题：不自动判分，交卷后进入「待批阅」，教师按点给分并可写评语</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    /** 题型常量 */
    public static final int TYPE_SINGLE = 1;
    public static final int TYPE_MULTI = 2;
    public static final int TYPE_JUDGE = 3;
    public static final int TYPE_BLANK = 4;
    public static final int TYPE_ESSAY = 5;

    private final QuestionMapper questionMapper;
    private final ExamPaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamMarkMapper markMapper;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final SysUserMapper userMapper;
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
        Integer type = question.getType() == null ? TYPE_SINGLE : question.getType();
        question.setType(type);
        if (type == TYPE_ESSAY) {
            // 问答题不设标准答案，参考答案可为空
            question.setAnswer(null);
            question.setOptionsJson(null);
        } else if (type == TYPE_BLANK) {
            if (question.getAnswer() == null || question.getAnswer().isBlank()) {
                throw new BusinessException("填空题需要填写标准答案，多个空用 | 分隔");
            }
            question.setOptionsJson(null);
        } else if (type == TYPE_JUDGE) {
            question.setOptionsJson(null);
            if (question.getAnswer() == null || question.getAnswer().isBlank()) {
                throw new BusinessException("请选择判断题的正确答案");
            }
        } else if (question.getAnswer() == null || question.getAnswer().isBlank()) {
            throw new BusinessException("请设置正确答案");
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

    /**
     * 教师端试卷列表：补上题目数、题目分值合计与章节标题。
     * 题目分值合计用来提示「能不能发布」——必须等于试卷设置的总分。
     */
    public List<ExamPaper> papers(Long courseId) {
        List<ExamPaper> list = paperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getCourseId, courseId)
                .orderByDesc(ExamPaper::getId));
        for (ExamPaper p : list) {
            fillPaperStats(p);
        }
        return list;
    }

    /** 学生端可见的试卷：必须已发布，未发布的试卷教师自己还在调 */
    public List<ExamPaper> publishedPapers(Long courseId) {
        return paperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getCourseId, courseId)
                .eq(ExamPaper::getPublishStatus, 1)
                .eq(ExamPaper::getStatus, 1)
                .orderByDesc(ExamPaper::getId));
    }

    /** 回填题目数与分值合计 */
    private void fillPaperStats(ExamPaper paper) {
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paper.getId()));
        paper.setQuestionCount(pqs.size());
        paper.setQuestionScore(pqs.stream()
                .mapToInt(pq -> pq.getScore() == null ? 0 : pq.getScore()).sum());
        paper.setChapterTitle(chapterTitle(paper.getChapterId()));
    }

    private String chapterTitle(Long chapterId) {
        if (chapterId == null) {
            return "";
        }
        Chapter chapter = chapterMapper.selectById(chapterId);
        return chapter == null ? "" : chapter.getTitle();
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
            // 新建的试卷一律是「未发布」，教师组好卷再点发布
            paper.setPublishStatus(0);
            paper.setVersion(1);
            paperMapper.insert(paper);
        } else {
            // 编辑已发布的试卷 → 打回未发布，学生端暂时看不到，需重新发布
            ExamPaper old = paperMapper.selectById(paper.getId());
            if (old != null && Integer.valueOf(1).equals(old.getPublishStatus())) {
                paper.setPublishStatus(0);
            } else {
                paper.setPublishStatus(old == null ? 0 : old.getPublishStatus());
            }
            paper.setVersion(old == null || old.getVersion() == null ? 1 : old.getVersion());
            paperMapper.updateById(paper);
        }
        return paper.getId();
    }

    /**
     * 发布试卷（或重新发布）。
     * <p>发布条件：试卷里至少有 1 道题，且「所有题目分值之和」必须等于试卷设置的总分。
     * <p>重新发布会让版本号 +1：学生那一侧的已提交答卷变成「未完成，需要重做」，
     * 但旧答卷记录与作答内容全部保留，教师仍能在批阅列表里查到历史作答。
     */
    @Transactional
    public void publishPaper(Long paperId, Long teacherId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        checkOwner(paper.getCourseId(), teacherId);

        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId));
        if (pqs.isEmpty()) {
            throw new BusinessException("试卷还没有题目，先组卷再发布");
        }
        int sum = pqs.stream().mapToInt(pq -> pq.getScore() == null ? 0 : pq.getScore()).sum();
        int total = paper.getTotalScore() == null ? 100 : paper.getTotalScore();
        if (sum != total) {
            throw new BusinessException(
                    "题目分值合计 " + sum + " 分，与试卷总分 " + total + " 分不一致，请调整后再发布");
        }

        ExamPaper update = new ExamPaper();
        update.setId(paperId);
        update.setPublishStatus(1);
        update.setPublishedAt(LocalDateTime.now());
        update.setVersion((paper.getVersion() == null ? 0 : paper.getVersion()) + 1);
        paperMapper.updateById(update);
    }

    /** 试卷内容有变动（加题/删题/改分）时，已发布的试卷打回未发布 */
    private void markDirty(Long paperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null || !Integer.valueOf(1).equals(paper.getPublishStatus())) {
            return;
        }
        ExamPaper update = new ExamPaper();
        update.setId(paperId);
        update.setPublishStatus(0);
        paperMapper.updateById(update);
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
        markDirty(paperId);
    }

    public void removePaperQuestion(Long id) {
        PaperQuestion pq = paperQuestionMapper.selectById(id);
        if (pq == null) {
            return;
        }
        paperQuestionMapper.deleteById(id);
        markDirty(pq.getPaperId());
    }

    /** 直接在试卷里改某题分值（前端不必再走「删了再加」） */
    public void updatePaperQuestionScore(Long paperQuestionId, Integer score) {
        PaperQuestion pq = paperQuestionMapper.selectById(paperQuestionId);
        if (pq == null) {
            throw new BusinessException("题目不在试卷中");
        }
        PaperQuestion update = new PaperQuestion();
        update.setId(paperQuestionId);
        update.setScore(score == null || score < 1 ? 1 : score);
        paperQuestionMapper.updateById(update);
        markDirty(pq.getPaperId());
    }

    /** 教师 / 学生视角看试卷题目；withAnswer=false 时不返回答案 */
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
            // 关联表主键单独返回：前端移除题目时按它删，用题干 id 会删错关联行
            vo.setPaperQuestionId(pq.getId());
            vo.setType(q.getType());
            vo.setTitle(q.getTitle());
            vo.setScore(pq.getScore());
            vo.setOptions(parseOptions(q.getOptionsJson()));
            if (withAnswer) {
                vo.setAnswer(q.getAnswer());
                vo.setReferenceAnswer(q.getReferenceAnswer());
                vo.setAnalysis(q.getAnalysis());
            }
            result.add(vo);
        }
        return result;
    }

    /** 学生开始答题：不返回答案和解析；只能在开放时间窗内、且每张试卷只能考一次 */
    public Map<String, Object> startExam(Long paperId, Long userId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null || paper.getStatus() == null || paper.getStatus() != 1) {
            throw new BusinessException("试卷不存在或未启用");
        }
        if (!Integer.valueOf(1).equals(paper.getPublishStatus())) {
            throw new BusinessException("该试卷教师尚未发布，暂时不能作答");
        }
        if (!enrollmentService.isEnrolled(userId, paper.getCourseId())) {
            throw new BusinessException("请先选修该课程再参加测验");
        }
        checkWindow(paper);

        ExamRecord done = latestRecord(paperId, userId, paper.getVersion());
        if (done != null) {
            throw new BusinessException("该测验已完成，不能重复作答");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("paper", paper);
        data.put("questions", paperQuestions(paperId, false));
        data.put("startTime", LocalDateTime.now().toString());
        return data;
    }

    /** 时间窗校验：未开始 / 已结束都不允许作答 */
    private void checkWindow(ExamPaper paper) {
        LocalDateTime now = LocalDateTime.now();
        if (paper.getStartTime() != null && now.isBefore(paper.getStartTime())) {
            throw new BusinessException("测验尚未开始，开放时间：" + fmtMinute(paper.getStartTime()));
        }
        if (paper.getEndTime() != null && now.isAfter(paper.getEndTime())) {
            throw new BusinessException("测验已于 " + fmtMinute(paper.getEndTime()) + " 结束");
        }
    }

    private String fmtMinute(LocalDateTime time) {
        return time.toString().replace('T', ' ').substring(0, 16);
    }

    /**
     * 当前有效答卷：只看「试卷当前发布版本」的记录。
     * 教师重新发布后版本号变了，旧记录就不再是「本次已完成」，
     * 学生需要重新作答，但旧记录仍留在库里供教师查阅。
     */
    private ExamRecord latestRecord(Long paperId, Long userId, Integer version) {
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getPaperId, paperId)
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getPaperVersion, version == null ? 1 : version)
                .orderByDesc(ExamRecord::getId));
        return records.isEmpty() ? null : records.get(0);
    }

    /** 交卷判分：客观题自动判，多选需完全一致；问答题留给教师批阅 */
    @Transactional
    public ExamResultVO submit(Long userId, ExamSubmitDTO dto) {
        ExamPaper paper = paperMapper.selectById(dto.getPaperId());
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        if (!Integer.valueOf(1).equals(paper.getPublishStatus())) {
            throw new BusinessException("该试卷已撤下发布，无法交卷");
        }
        checkWindow(paper);
        ExamRecord submitted = latestRecord(paper.getId(), userId, paper.getVersion());
        if (submitted != null) {
            throw new BusinessException("该测验已提交，不能重复交卷");
        }
        Map<String, String> answers = dto.getAnswers() == null ? Map.of() : dto.getAnswers();

        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paper.getId()));

        int objectiveScore = 0;
        boolean hasEssay = false;
        List<ExamResultVO.AnswerDetail> details = new ArrayList<>();

        for (PaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            int type = q.getType() == null ? TYPE_SINGLE : q.getType();
            String your = answers.getOrDefault(String.valueOf(q.getId()), "");
            int fullScore = pq.getScore() == null ? 0 : pq.getScore();

            ExamResultVO.AnswerDetail detail = new ExamResultVO.AnswerDetail();
            detail.setQuestionId(q.getId());
            detail.setType(type);
            detail.setTitle(q.getTitle());
            detail.setOptions(parseOptions(q.getOptionsJson()));
            detail.setYourAnswer(your);
            detail.setFullScore(fullScore);
            detail.setRightAnswer(q.getAnswer());
            detail.setReferenceAnswer(q.getReferenceAnswer());
            detail.setAnalysis(q.getAnalysis());

            if (type == TYPE_ESSAY) {
                hasEssay = true;
                detail.setNeedManual(true);
                detail.setCorrect(null);
                detail.setScore(0);
            } else {
                boolean correct = isCorrect(q, your);
                detail.setNeedManual(false);
                detail.setCorrect(correct);
                detail.setScore(correct ? fullScore : 0);
                if (correct) {
                    objectiveScore += fullScore;
                }
            }
            details.add(detail);
        }

        ExamRecord record = new ExamRecord();
        record.setPaperId(paper.getId());
        record.setPaperVersion(paper.getVersion() == null ? 1 : paper.getVersion());
        record.setUserId(userId);
        record.setAnswersJson(writeJson(answers));
        record.setObjectiveScore(objectiveScore);
        record.setSubjectiveScore(0);
        record.setScore(objectiveScore);
        // 有问答题 → 待批阅；否则无需批阅
        record.setMarkStatus(hasEssay ? 1 : 0);
        record.setPassed(passFlag(objectiveScore, paper));
        record.setStartTime(LocalDateTime.now());
        record.setSubmitTime(LocalDateTime.now());
        recordMapper.insert(record);

        ExamResultVO vo = new ExamResultVO();
        vo.setRecordId(record.getId());
        vo.setScore(objectiveScore);
        vo.setObjectiveScore(objectiveScore);
        vo.setSubjectiveScore(0);
        vo.setMarkStatus(record.getMarkStatus());
        vo.setTotalScore(paper.getTotalScore());
        vo.setPassScore(paper.getPassScore());
        vo.setPassed(record.getPassed() == 1);
        vo.setDetails(details);
        return vo;
    }

    /**
     * 我的测验记录：带上课程、章节、试卷版本信息。
     * <p>教师重新发布试卷后版本会变，旧答卷的 current=false（已作废、需要重做），
     * 但记录本身保留，学生仍能看到当时的作答与得分。
     * @param courseId 传了就只返回该课程的记录，供成绩页按课程筛选
     */
    public List<Map<String, Object>> myRecords(Long userId, Long courseId) {
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .orderByDesc(ExamRecord::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord r : records) {
            ExamPaper paper = paperMapper.selectById(r.getPaperId());
            if (courseId != null && (paper == null || !courseId.equals(paper.getCourseId()))) {
                continue;
            }
            int version = paper == null || paper.getVersion() == null ? 1 : paper.getVersion();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("record", r);
            item.put("paperTitle", paper == null ? "已删除试卷" : paper.getTitle());
            item.put("courseId", paper == null ? null : paper.getCourseId());
            item.put("courseTitle", courseTitleOf(paper == null ? null : paper.getCourseId()));
            item.put("chapterId", paper == null ? null : paper.getChapterId());
            item.put("chapterTitle", chapterTitle(paper == null ? null : paper.getChapterId()));
            item.put("paperStartTime", paper == null ? null : paper.getStartTime());
            item.put("paperEndTime", paper == null ? null : paper.getEndTime());
            item.put("totalScore", paper == null ? 100 : paper.getTotalScore());
            item.put("passScore", paper == null ? 60 : paper.getPassScore());
            item.put("duration", paper == null ? 30 : paper.getDuration());
            item.put("publishStatus", paper == null ? 0 : paper.getPublishStatus());
            item.put("current", r.getPaperVersion() != null && r.getPaperVersion() == version);
            result.add(item);
        }
        return result;
    }

    private String courseTitleOf(Long courseId) {
        if (courseId == null) {
            return "";
        }
        Course course = courseMapper.selectById(courseId);
        return course == null ? "" : course.getTitle();
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
        return buildResult(record);
    }

    // ---------------- 教师批阅 ----------------

    /** 教师端：某份试卷的答卷列表 */
    public List<Map<String, Object>> paperRecords(Long paperId, Long teacherId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        checkOwner(paper.getCourseId(), teacherId);
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getPaperId, paperId)
                .orderByDesc(ExamRecord::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        int version = paper.getVersion() == null ? 1 : paper.getVersion();
        for (ExamRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("record", r);
            item.put("paperTitle", paper.getTitle());
            item.put("totalScore", paper.getTotalScore());
            item.put("userId", r.getUserId());
            SysUser user = userMapper.selectById(r.getUserId());
            item.put("studentName", user == null ? "未知学生"
                    : (user.getRealName() == null || user.getRealName().isBlank()
                    ? user.getUsername() : user.getRealName()));
            item.put("studentNo", user == null ? "" : nullToEmpty(user.getUserNo()));
            // 试卷重新发布后，旧版本答卷只是留档，不算本轮有效作答
            item.put("current", r.getPaperVersion() != null && r.getPaperVersion() == version);
            result.add(item);
        }
        return result;
    }

    /** 教师端：答卷详情（含参考答案与批阅记录） */
    public ExamResultVO teacherRecordDetail(Long recordId, Long teacherId) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        ExamPaper paper = paperMapper.selectById(record.getPaperId());
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        checkOwner(paper.getCourseId(), teacherId);
        return buildResult(record);
    }

    /**
     * 批阅主观题：一次提交这份答卷所有主观题的得分与评语。
     * marks 每项 {questionId, score, comment}
     */
    @Transactional
    public void markRecord(Long recordId, List<Map<String, Object>> marks, Long teacherId) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        ExamPaper paper = paperMapper.selectById(record.getPaperId());
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        checkOwner(paper.getCourseId(), teacherId);

        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paper.getId()));
        Map<Long, Integer> fullScoreMap = new HashMap<>();
        for (PaperQuestion pq : pqs) {
            fullScoreMap.put(pq.getQuestionId(), pq.getScore() == null ? 0 : pq.getScore());
        }

        int subjective = 0;
        List<Long> markedQuestionIds = new ArrayList<>();
        if (marks != null) {
            for (Map<String, Object> mark : marks) {
                if (mark.get("questionId") == null) {
                    continue;
                }
                Long questionId = Long.valueOf(mark.get("questionId").toString());
                int full = fullScoreMap.getOrDefault(questionId, 0);
                int score = mark.get("score") == null ? 0 : parseScore(mark.get("score"));
                if (score < 0) {
                    score = 0;
                }
                if (score > full) {
                    score = full;
                }
                subjective += score;
                markedQuestionIds.add(questionId);

                ExamMark exists = markMapper.selectOne(new LambdaQueryWrapper<ExamMark>()
                        .eq(ExamMark::getRecordId, recordId)
                        .eq(ExamMark::getQuestionId, questionId)
                        .last("limit 1"));
                ExamMark entity = exists == null ? new ExamMark() : exists;
                entity.setRecordId(recordId);
                entity.setQuestionId(questionId);
                entity.setScore(score);
                entity.setComment(mark.get("comment") == null ? null : mark.get("comment").toString());
                entity.setTeacherId(teacherId);
                if (exists == null) {
                    markMapper.insert(entity);
                } else {
                    markMapper.updateById(entity);
                }
            }
        }

        int objective = record.getObjectiveScore() == null ? 0 : record.getObjectiveScore();
        int total = objective + subjective;
        ExamRecord update = new ExamRecord();
        update.setId(recordId);
        update.setSubjectiveScore(subjective);
        update.setScore(total);
        update.setPassed(passFlag(total, paper));
        update.setMarkStatus(2);
        update.setMarkTime(LocalDateTime.now());
        update.setMarkerId(teacherId);
        recordMapper.updateById(update);
        log.debug("答卷 {} 批阅完成，主观题 {} 题，合计 {} 分", recordId, markedQuestionIds.size(), total);
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

    // ---------------- 判分与结果组装 ----------------

    private ExamResultVO buildResult(ExamRecord record) {
        ExamPaper paper = paperMapper.selectById(record.getPaperId());
        Map<String, String> answers = readJson(record.getAnswersJson());
        List<ExamMark> marks = markMapper.selectList(new LambdaQueryWrapper<ExamMark>()
                .eq(ExamMark::getRecordId, record.getId()));
        Map<Long, ExamMark> markMap = new HashMap<>();
        for (ExamMark mark : marks) {
            markMap.put(mark.getQuestionId(), mark);
        }

        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, record.getPaperId()));
        List<ExamResultVO.AnswerDetail> details = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            int type = q.getType() == null ? TYPE_SINGLE : q.getType();
            String your = answers.getOrDefault(String.valueOf(q.getId()), "");
            int fullScore = pq.getScore() == null ? 0 : pq.getScore();

            ExamResultVO.AnswerDetail detail = new ExamResultVO.AnswerDetail();
            detail.setQuestionId(q.getId());
            detail.setType(type);
            detail.setTitle(q.getTitle());
            detail.setOptions(parseOptions(q.getOptionsJson()));
            detail.setYourAnswer(your);
            detail.setFullScore(fullScore);
            detail.setRightAnswer(q.getAnswer());
            detail.setReferenceAnswer(q.getReferenceAnswer());
            detail.setAnalysis(q.getAnalysis());

            if (type == TYPE_ESSAY) {
                detail.setNeedManual(true);
                ExamMark mark = markMap.get(q.getId());
                detail.setScore(mark == null ? null : mark.getScore());
                detail.setComment(mark == null ? null : mark.getComment());
                detail.setCorrect(mark == null ? null
                        : (mark.getScore() != null && mark.getScore() >= fullScore));
            } else {
                boolean correct = isCorrect(q, your);
                detail.setNeedManual(false);
                detail.setCorrect(correct);
                detail.setScore(correct ? fullScore : 0);
            }
            details.add(detail);
        }

        ExamResultVO vo = new ExamResultVO();
        vo.setRecordId(record.getId());
        vo.setScore(record.getScore());
        vo.setObjectiveScore(record.getObjectiveScore());
        vo.setSubjectiveScore(record.getSubjectiveScore());
        vo.setMarkStatus(record.getMarkStatus());
        vo.setTotalScore(paper == null ? 100 : paper.getTotalScore());
        vo.setPassScore(paper == null ? 60 : paper.getPassScore());
        vo.setPassed(record.getPassed() != null && record.getPassed() == 1);
        vo.setDetails(details);
        return vo;
    }

    /** 判分入口：按题型分派 */
    private boolean isCorrect(Question q, String your) {
        int type = q.getType() == null ? TYPE_SINGLE : q.getType();
        String expected = q.getAnswer() == null ? "" : q.getAnswer();
        if (type == TYPE_BLANK) {
            return matchBlank(expected, your);
        }
        if (type == TYPE_ESSAY) {
            return false;
        }
        return normalize(your).equals(normalize(expected));
    }

    /**
     * 填空判分：按 | 拆空，逐空比对；同一空可以有多种正确写法，用 ~ 分隔。
     * 比较时忽略大小写、首尾空白与全角空格。
     */
    private boolean matchBlank(String expected, String actual) {
        if (expected.isBlank() || actual == null || actual.isBlank()) {
            return false;
        }
        String[] expectedParts = expected.split("\\|", -1);
        String[] actualParts = actual.split("\\|", -1);
        if (expectedParts.length != actualParts.length) {
            return false;
        }
        for (int i = 0; i < expectedParts.length; i++) {
            Set<String> accepted = new HashSet<>();
            for (String one : expectedParts[i].split("~")) {
                accepted.add(clean(one));
            }
            if (!accepted.contains(clean(actualParts[i]))) {
                return false;
            }
        }
        return true;
    }

    /** 清洗：去首尾空白、全角空格、字母统一小写 */
    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u3000', ' ').trim().toLowerCase();
    }

    private int parseScore(Object value) {
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (Exception e) {
            return 0;
        }
    }

    private int passFlag(int score, ExamPaper paper) {
        return score >= (paper.getPassScore() == null ? 60 : paper.getPassScore()) ? 1 : 0;
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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
