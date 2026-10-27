<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>试卷与组卷</h1>
        <p>{{ courseTitle }} · 共 {{ papers.length }} 份试卷</p>
      </div>
      <div class="ap-actions">
        <el-button @click="router.push('/teacher/courses')">
          <el-icon><ArrowLeft /></el-icon> 返回课程
        </el-button>
        <el-button @click="router.push(`/teacher/courses/${courseId}/questions`)">
          <el-icon><Collection /></el-icon> 去题库
        </el-button>
        <el-button type="primary" @click="openEdit(null)">
          <el-icon><Plus /></el-icon> 新建试卷
        </el-button>
      </div>
    </div>

    <!-- 试卷卡片 -->
    <div class="paper-grid">
      <article v-for="p in papers" :key="p.id" class="paper">
        <div class="p-head">
          <span class="p-ico"><el-icon><Document /></el-icon></span>
          <div class="p-title">
            <b>{{ p.title }}</b>
            <span>限时 {{ p.duration }} 分钟</span>
          </div>
        </div>

        <div class="p-time">
          <span :class="{ muted: !p.startTime }">
            开始 {{ p.startTime ? fmt(p.startTime) : '不限' }}
          </span>
          <span :class="{ muted: !p.endTime }">
            结束 {{ p.endTime ? fmt(p.endTime) : '不限' }}
          </span>
        </div>

        <div class="p-stats">
          <div>
            <b>{{ p.totalScore }}</b>
            <span>总分</span>
          </div>
          <div>
            <b>{{ p.passScore }}</b>
            <span>及格</span>
          </div>
          <div>
            <b>{{ p.duration }}</b>
            <span>分钟</span>
          </div>
        </div>

        <div class="p-ops">
          <el-button type="primary" size="small" @click="openCompose(p)">
            组卷 / 调分
          </el-button>
          <el-button size="small" @click="openRecords(p)">
            <el-icon><EditPen /></el-icon> 答卷批阅
          </el-button>
          <el-dropdown trigger="click" @command="(c: string) => exportPaper(p, c)">
            <el-button size="small">
              <el-icon><Download /></el-icon> 导出
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="plain">导出试卷（docx）</el-dropdown-item>
                <el-dropdown-item command="answer">导出含答案（docx）</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button size="small" @click="openEdit(p)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(p)">删除</el-button>
        </div>
      </article>

      <div v-if="!papers.length" class="ol-empty ol-card" style="grid-column: 1 / -1">
        <span class="icon">📝</span>
        还没有试卷，新建一份并从题库挑题组卷
      </div>
    </div>

    <!-- 新建 / 编辑 -->
    <el-dialog append-to-body v-model="dialog" :title="form.id ? '编辑试卷' : '新建试卷'" width="520px">
      <el-form label-position="top">
        <el-form-item label="试卷名称">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="例如：计算机网络分层与协议测验" />
        </el-form-item>
        <div class="three">
          <el-form-item label="总分">
            <el-input-number v-model="form.totalScore" :min="10" :max="200" style="width: 100%" />
          </el-form-item>
          <el-form-item label="及格分">
            <el-input-number v-model="form.passScore" :min="0" :max="200" style="width: 100%" />
          </el-form-item>
          <el-form-item label="限时（分钟）">
            <el-input-number v-model="form.duration" :min="5" :max="180" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="two-time">
          <el-form-item label="开始时间（可选）">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="留空表示立即开放"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间（可选）">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="留空表示不关闭"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <p class="time-tip">学生只能在「开始时间 ~ 结束时间」之间作答，每张试卷只能考一次</p>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 组卷抽屉 -->
    <el-drawer v-model="drawer" append-to-body size="860px" :with-header="false">
      <div class="compose">
        <div class="c-head">
          <div>
            <h2>{{ currentPaper.title }}</h2>
            <p class="ol-muted">
              已选 {{ paperQuestions.length }} 题，合计
              <b :class="{ over: sumScore > (currentPaper.totalScore || 100) }">{{ sumScore }}</b>
              / {{ currentPaper.totalScore || 100 }} 分
            </p>
          </div>
          <el-button @click="drawer = false">完成</el-button>
        </div>

        <div class="c-body">
          <!-- 题库 -->
          <section class="bank">
            <div class="bank-head">
              <h3>题库</h3>
              <el-input
                v-model="qKeyword"
                size="small"
                placeholder="搜索题干"
                :prefix-icon="Search"
                clearable
              />
            </div>

            <div class="bank-list">
              <div
                v-for="q in bankFiltered"
                :key="q.id"
                class="bank-item"
                :class="{ used: inPaper(q.id) }"
                @click="addToPaper(q)"
              >
                <div class="bi-top">
                  <span class="chip" :class="typeClass(q.type)">{{ typeName(q.type) }}</span>
                  <span class="grow" />
                  <span v-if="inPaper(q.id)" class="chip chip--green">已加入</span>
                  <el-icon v-else class="bi-add"><Plus /></el-icon>
                </div>
                <p>{{ q.title }}</p>
              </div>

              <div v-if="!bankFiltered.length" class="ol-empty" style="padding: 24px 0">
                题库为空，先去题库新增题目
              </div>
            </div>
          </section>

          <!-- 已选 -->
          <section class="picked">
            <div class="picked-head">
              <h3>试卷题目</h3>
              <span class="ol-muted">{{ paperQuestions.length }} 题</span>
            </div>

            <div class="picked-list">
              <div v-for="(q, i) in paperQuestions" :key="q.paperQuestionId" class="picked-item">
                <span class="pi-no">{{ i + 1 }}</span>
                <div class="pi-body">
                  <p>{{ q.title }}</p>
                  <div class="pi-meta">
                    <el-input-number
                      v-model="q.score"
                      :min="1"
                      :max="100"
                      size="small"
                      controls-position="right"
                      style="width: 92px"
                      @change="(v: number) => updateScore(q, v)"
                    />
                    <span class="ol-muted">分</span>
                    <span class="chip">{{ typeName(q.type) }}</span>
                    <span class="ol-muted">答案 {{ q.answer }}</span>
                  </div>
                </div>
                <el-icon class="pi-del" @click="removeFromPaper(q)"><Delete /></el-icon>
              </div>

              <div v-if="!paperQuestions.length" class="ol-empty" style="padding: 30px 0">
                左侧点题目即可加入试卷
              </div>
            </div>
          </section>
        </div>
      </div>
    </el-drawer>

    <!-- 答卷列表 -->
    <el-dialog
      append-to-body
      v-model="recordsDialog"
      :title="`答卷与批阅 · ${recordsPaper?.title || ''}`"
      width="860px"
      top="6vh"
    >
      <el-table :data="records" stripe empty-text="还没有学生作答">
        <el-table-column label="学生" min-width="140">
          <template #default="{ row }">{{ row.studentName }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">
            {{ (row.record.submitTime || '').replace('T', ' ').slice(0, 16) }}
          </template>
        </el-table-column>
        <el-table-column label="得分" width="110">
          <template #default="{ row }">
            <b>{{ row.record.score ?? '—' }}</b>
            <span class="ol-muted"> / {{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span
              class="chip"
              :class="markChip(row.record.markStatus)"
            >
              {{ markText(row.record.markStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openMark(row)">
              {{ row.record.markStatus === 1 ? '批阅' : '查看/修改' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 主观题批阅 -->
    <el-drawer append-to-body v-model="markDrawer" size="720px" :with-header="false">
      <div class="mark-panel">
        <div class="c-head">
          <div>
            <h2>批阅答卷</h2>
            <p class="ol-muted">
              {{ markData.studentName }} · 客观题自动得分
              <b>{{ markData.objectiveScore ?? 0 }}</b> 分
            </p>
          </div>
          <el-button @click="markDrawer = false">关闭</el-button>
        </div>

        <div class="mark-body">
          <div
            v-for="(d, i) in essayItems"
            :key="d.questionId"
            class="mark-item"
          >
            <div class="m-head">
              <span class="m-no">{{ i + 1 }}</span>
              <b>{{ d.title }}</b>
              <span class="chip chip--violet">满分 {{ d.fullScore }} 分</span>
            </div>
            <div class="m-answer">
              <span class="m-label">学生作答</span>
              <p>{{ d.yourAnswer || '（未作答）' }}</p>
            </div>
            <div v-if="d.referenceAnswer" class="m-ref">
              <span class="m-label">参考答案</span>
              <p>{{ d.referenceAnswer }}</p>
            </div>
            <div class="m-form">
              <el-form-item label="本题得分">
                <el-input-number
                  v-model="markForm[d.questionId].score"
                  :min="0"
                  :max="d.fullScore"
                  style="width: 140px"
                />
              </el-form-item>
              <el-form-item label="评语（可选）" class="grow">
                <el-input
                  v-model="markForm[d.questionId].comment"
                  placeholder="指出亮点与需要改进的地方"
                />
              </el-form-item>
            </div>
          </div>

          <div v-if="!essayItems.length" class="ol-empty">
            这份答卷没有主观题，无需批阅
          </div>
        </div>

        <div class="mark-foot">
          <span class="ol-muted">
            预计总分：{{ (markData.objectiveScore || 0) + markSum }} 分
          </span>
          <el-button type="primary" :disabled="!essayItems.length" @click="saveMark">
            保存批阅结果
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { triggerDownload } from '@/utils/file'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const courseTitle = ref('课程')
const papers = ref<any[]>([])
const questions = ref<any[]>([])
const dialog = ref(false)
const form = ref<any>({ id: null, title: '', totalScore: 100, passScore: 60, duration: 30 })

const drawer = ref(false)
const currentPaper = ref<any>({})
const paperQuestions = ref<any[]>([])
const qKeyword = ref('')

/** 时间展示：2026-10-27 14:30 */
function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

const sumScore = computed(() =>
  paperQuestions.value.reduce((s, q) => s + Number(q.score || 0), 0)
)

// ---------------- 答卷与主观题批阅 ----------------
const recordsDialog = ref(false)
const recordsPaper = ref<any>(null)
const records = ref<any[]>([])
const markDrawer = ref(false)
const markData = ref<any>({})
const markForm = reactive<Record<string, { score: number; comment: string }>>({})

/** 需要人工批阅的题目（问答题） */
const essayItems = computed(() =>
  (markData.value.details || []).filter((d: any) => d.needManual)
)
const markSum = computed(() =>
  Object.values(markForm).reduce((s, v) => s + Number(v?.score || 0), 0)
)

function markText(status: number) {
  if (status === 1) return '待批阅'
  if (status === 2) return '已批阅'
  return '无需批阅'
}
function markChip(status: number) {
  if (status === 1) return 'chip--violet'
  if (status === 2) return 'chip--green'
  return ''
}

async function openRecords(p: any) {
  recordsPaper.value = p
  records.value = ((await request.get(`/api/teacher/papers/${p.id}/records`)) as any) || []
  recordsDialog.value = true
}

async function openMark(row: any) {
  const recordId = row.record.id
  const detail: any = await request.get(`/api/teacher/records/${recordId}`)
  detail.studentName = row.studentName
  markData.value = detail
  // 把已有的批阅结果回填，便于二次修改
  Object.keys(markForm).forEach((k) => delete markForm[k])
  ;(detail.details || [])
    .filter((d: any) => d.needManual)
    .forEach((d: any) => {
      markForm[d.questionId] = { score: d.score ?? 0, comment: d.comment || '' }
    })
  markDrawer.value = true
}

async function saveMark() {
  const marks = essayItems.value.map((d: any) => ({
    questionId: d.questionId,
    score: markForm[d.questionId]?.score ?? 0,
    comment: markForm[d.questionId]?.comment || ''
  }))
  await request.post(`/api/teacher/records/${markData.value.recordId}/mark`, { marks })
  ElMessage.success('批阅结果已保存')
  markDrawer.value = false
  if (recordsPaper.value) {
    records.value =
      ((await request.get(`/api/teacher/papers/${recordsPaper.value.id}/records`)) as any) || []
  }
}

/** 导出试卷：plain 不含答案，answer 含参考答案（教师用） */
function exportPaper(p: any, mode: string) {
  const withAnswer = mode === 'answer'
  triggerDownload(`/api/courses/${courseId}/papers/${p.id}/export?withAnswer=${withAnswer}`)
}

const bankFiltered = computed(() => {
  const kw = qKeyword.value.trim()
  if (!kw) return questions.value
  return questions.value.filter((q) => (q.title || '').includes(kw))
})

function typeName(t: number) {
  return t === 1 ? '单选' : t === 2 ? '多选' : '判断'
}
function typeClass(t: number) {
  return t === 1 ? 'chip--blue' : t === 2 ? 'chip--violet' : 'chip--amber'
}

function inPaper(questionId: number) {
  return paperQuestions.value.some((q) => q.id === questionId)
}

async function load() {
  const courses: any = await request.get('/api/teacher/courses')
  const target = (courses || []).find((c: any) => c.id === courseId)
  courseTitle.value = target?.title || '课程'

  papers.value = ((await request.get(`/api/teacher/courses/${courseId}/papers`)) as any) || []
  questions.value = ((await request.get(`/api/teacher/courses/${courseId}/questions`)) as any) || []
}

function openEdit(row: any) {
  form.value = row
    ? {
        id: row.id,
        title: row.title,
        totalScore: row.totalScore,
        passScore: row.passScore,
        duration: row.duration,
        startTime: row.startTime || null,
        endTime: row.endTime || null
      }
    : {
        id: null,
        title: '',
        totalScore: 100,
        passScore: 60,
        duration: 30,
        startTime: null,
        endTime: null
      }
  dialog.value = true
}

async function save() {
  if (!form.value.title.trim()) {
    ElMessage.warning('试卷名称不能为空')
    return
  }
  if (
    form.value.startTime &&
    form.value.endTime &&
    new Date(form.value.startTime) >= new Date(form.value.endTime)
  ) {
    ElMessage.warning('开始时间要早于结束时间')
    return
  }
  await request.post('/api/teacher/papers', { ...form.value, courseId })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`删除试卷《${row.title}》？已有的考试记录会保留但无法再作答。`, '提示', {
    type: 'warning'
  })
  await request.delete(`/api/teacher/papers/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function openCompose(row: any) {
  currentPaper.value = { ...row }
  qKeyword.value = ''
  await loadPaperQuestions()
  drawer.value = true
}

async function loadPaperQuestions() {
  paperQuestions.value =
    ((await request.get(`/api/teacher/papers/${currentPaper.value.id}/questions`)) as any) || []
}

async function addToPaper(q: any) {
  if (inPaper(q.id)) {
    ElMessage.info('这道题已经在试卷里了')
    return
  }
  await request.post(`/api/teacher/papers/${currentPaper.value.id}/questions`, {
    questionId: q.id,
    score: 10
  })
  await loadPaperQuestions()
}

async function removeFromPaper(q: any) {
  await request.delete(`/api/teacher/paper-questions/${q.paperQuestionId}`)
  await loadPaperQuestions()
}

/**
 * 分值调整：后端没有单独改分接口，做法是「删掉关联行再按新分值加回去」。
 * 关联表对 (paper_id, question_id) 有唯一索引，不先删会直接报重复键。
 */
async function updateScore(q: any, value: number) {
  if (!value) return
  await request.delete(`/api/teacher/paper-questions/${q.paperQuestionId}`)
  await request.post(`/api/teacher/papers/${currentPaper.value.id}/questions`, {
    questionId: q.id,
    score: value
  })
  await loadPaperQuestions()
}

onMounted(load)
</script>

<style scoped>
.paper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}
.paper {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 17px 18px;
  transition: all 0.2s;
}
.paper:hover {
  border-color: var(--brand-200);
  box-shadow: var(--sh-sm);
}
.p-head {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 15px;
}
.p-ico {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 11px;
  background: var(--violet-soft);
  color: var(--violet);
  display: grid;
  place-items: center;
  font-size: 17px;
}
.p-title b {
  display: block;
  font-size: 14px;
  font-weight: 650;
  margin-bottom: 2px;
  line-height: 1.4;
}
.p-title span {
  font-size: 11.5px;
  color: var(--ink-400);
}
.p-stats {
  display: flex;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid var(--ink-100);
  border-bottom: 1px solid var(--ink-100);
  margin-bottom: 13px;
}
.p-stats > div {
  flex: 1;
  text-align: center;
}
.p-stats b {
  display: block;
  font-size: 17px;
  font-weight: 750;
  letter-spacing: -0.4px;
}
.p-stats span {
  font-size: 10.5px;
  color: var(--ink-400);
}
.p-ops {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  align-items: center;
}
/* 卡片窄，5 个按钮放不下时换行，避免最后一个被裁掉 */
.p-ops :deep(.el-button) {
  margin-left: 0;
}
.three {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.two-time {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.time-tip {
  margin: -8px 0 4px;
  font-size: 11.5px;
  color: var(--ink-300);
}
.p-time {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 11.5px;
  color: var(--ink-500);
  padding: 0 2px 10px;
}
.p-time .muted {
  color: var(--ink-300);
}

/* ---------- 组卷 ---------- */
.compose {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.c-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--ink-100);
}
.c-head h2 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 720;
  letter-spacing: -0.3px;
}
.c-body {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  gap: 18px;
  padding-top: 16px;
  min-height: 0;
}
.over {
  color: #c0392b;
}

.bank,
.picked {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.bank-head,
.picked-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}
.bank-head h3,
.picked-head h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 680;
}
.bank-head :deep(.el-input) {
  width: 170px;
}
.bank-list,
.picked-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 4px;
}
.bank-item {
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 11px 13px;
  cursor: pointer;
  transition: all 0.16s;
}
.bank-item:hover {
  border-color: var(--brand-300);
  background: var(--brand-50);
}
.bank-item.used {
  opacity: 0.55;
  cursor: default;
}
.bank-item.used:hover {
  border-color: var(--ink-100);
  background: var(--surface);
}
.bi-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.grow {
  flex: 1;
}
.bi-add {
  color: var(--brand);
  font-size: 14px;
}
.bank-item p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--ink-700);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.picked-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 11px 12px;
}
.pi-no {
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  background: var(--ink-900);
  color: #fff;
}
.pi-body {
  flex: 1;
  min-width: 0;
}
.pi-body p {
  margin: 0 0 7px;
  font-size: 12.5px;
  line-height: 1.55;
  color: var(--ink-700);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pi-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}
.pi-del {
  color: var(--ink-300);
  font-size: 14px;
  cursor: pointer;
  flex: none;
  margin-top: 3px;
}
.pi-del:hover {
  color: #c0392b;
}

@media (max-width: 1100px) {
  .c-body {
    grid-template-columns: 1fr;
  }
}
</style>
