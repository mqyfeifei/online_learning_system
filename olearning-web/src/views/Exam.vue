<template>
  <div v-loading="loading" class="exam">
    <!-- ==================== 结果页 ==================== -->
    <div v-if="result" class="result-wrap fade-up">
      <section class="result-card">
        <div class="rs-ring">
          <svg viewBox="0 0 160 160">
            <circle cx="80" cy="80" r="66" class="rs-track" />
            <circle
              cx="80"
              cy="80"
              r="66"
              class="rs-bar"
              :class="{ fail: !result.passed }"
              :stroke-dasharray="rsLen"
              :stroke-dashoffset="rsOffset"
            />
          </svg>
          <div class="rs-text">
            <b>{{ result.score }}</b>
            <span>/ {{ result.totalScore }}</span>
          </div>
        </div>

        <h2>{{ result.passed ? '测验通过 🎉' : '这次没过，复习一遍吧' }}</h2>
        <p class="rs-tip">
          共 {{ result.details?.length || 0 }} 题，答对 {{ correctCount }} 题，及格线 {{ result.passScore }} 分
        </p>
        <div class="rs-done">
          <span class="chip chip--green">已完成</span>
          <span class="ol-muted">本次测验已提交，不能重复作答</span>
        </div>

        <div class="rs-actions">
          <el-button type="primary" size="large" @click="goGrades">
            查看我的成绩
          </el-button>
          <el-button size="large" @click="openWorkNote">
            <el-icon><Notebook /></el-icon> 测验笔记
          </el-button>
          <el-button size="large" @click="close">
            {{ embedded ? '关闭' : '返回课程' }}
          </el-button>
        </div>
      </section>

      <section class="ol-card">
        <div class="ol-card-title">
          <h2 class="ol-h2">逐题解析</h2>
          <span class="ol-muted">答错的题重点看解析</span>
        </div>
        <div class="q-list">
          <div
            v-for="(d, i) in result.details"
            :key="d.questionId"
            class="q-item"
            :class="{ wrong: !d.correct }"
          >
            <div class="q-head">
              <span class="q-no" :class="{ ok: d.correct }">{{ i + 1 }}</span>
              <b>{{ d.title }}</b>
              <span
                v-if="d.needManual"
                class="chip chip--violet"
              >
                待教师批阅
              </span>
              <span v-else class="chip" :class="d.correct ? 'chip--green' : 'chip--brand'">
                {{ d.correct ? '正确' : '错误' }}
              </span>
            </div>
            <div class="q-ans">
              <template v-if="d.type === 5">
                <div class="ans-line">我的作答：</div>
                <div
                  v-if="plainOf(d.yourAnswer)"
                  class="ans-rich"
                  v-html="d.yourAnswer"
                />
                <div v-else class="ans-line">（未作答）</div>
                <div v-if="d.referenceAnswer" class="ans-ref">
                  参考答案：{{ d.referenceAnswer }}
                </div>
              </template>
              <template v-else>
                你的答案：<b :class="{ bad: !d.correct }">{{ d.yourAnswer || '未作答' }}</b>
                <span v-if="d.rightAnswer" class="sep">|</span>
                <template v-if="d.rightAnswer">
                  正确答案：<b class="ok-txt">{{ d.rightAnswer }}</b>
                </template>
              </template>
            </div>
            <div v-if="d.needManual" class="q-analysis q-pending">
              <el-icon><Clock /></el-icon>
              这道问答题已提交，教师批阅后可在「成绩与测验」看到得分与评语
            </div>
            <div v-if="d.analysis" class="q-analysis">
              <el-icon><InfoFilled /></el-icon>{{ d.analysis }}
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- ==================== 答题页 ==================== -->
    <template v-else>
      <header class="bar">
        <div class="bar-left">
          <button class="back" @click="leave">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <div>
            <h1>{{ paper.title }}</h1>
            <p>{{ questions.length }} 题 · 总分 {{ paper.totalScore }} · 及格 {{ paper.passScore }}</p>
          </div>
        </div>

        <div class="bar-right">
          <div class="progress-mini">
            <span>已答 <b>{{ answeredCount }}</b>/{{ questions.length }}</span>
            <div class="bar" style="width: 108px">
              <i :style="{ width: (answeredCount / Math.max(1, questions.length)) * 100 + '%' }" />
            </div>
          </div>

          <div class="timer" :class="{ urgent: seconds <= 60 }">
            <el-icon><Timer /></el-icon>
            <b>{{ leftText }}</b>
          </div>

          <el-button @click="openWorkNote">
            <el-icon><Notebook /></el-icon> 笔记
          </el-button>

          <el-button type="primary" size="large" @click="trySubmit">交卷</el-button>
        </div>
      </header>

      <div class="body">
        <!-- 题目 -->
        <main class="questions">
          <section
            v-for="(q, i) in questions"
            :id="`q-${i}`"
            :key="q.id"
            class="q-card"
          >
            <div class="q-top">
              <span class="q-idx">{{ i + 1 }}</span>
              <span class="chip" :class="typeClass(q.type)">{{ typeName(q.type) }}</span>
              <span class="chip">{{ q.score }} 分</span>
              <span v-if="isAnswered(q)" class="chip chip--green">
                <el-icon><Select /></el-icon> 已答
              </span>
            </div>

            <h3 class="q-title">{{ q.title }}</h3>

            <!-- 单选 -->
            <div v-if="q.type === 1" class="opts">
              <div
                v-for="o in q.options"
                :key="o.key"
                class="opt"
                :class="{ on: single[q.id] === o.key }"
                @click="pickSingle(q, o.key)"
              >
                <span class="opt-key">{{ o.key }}</span>
                <span class="opt-text">{{ o.text }}</span>
              </div>
            </div>

            <!-- 多选 -->
            <div v-else-if="q.type === 2" class="opts">
              <div
                v-for="o in q.options"
                :key="o.key"
                class="opt"
                :class="{ on: (multi[q.id] || []).includes(o.key) }"
                @click="pickMulti(q, o.key)"
              >
                <span class="opt-key">{{ o.key }}</span>
                <span class="opt-text">{{ o.text }}</span>
              </div>
              <p class="multi-tip">多选题：选出所有正确项</p>
            </div>

            <!-- 判断 -->
            <div v-else-if="q.type === 3" class="opts opts--row">
              <div
                class="opt judge"
                :class="{ on: judge[q.id] === 'T' }"
                @click="pickJudge(q, 'T')"
              >
                <span class="opt-key">√</span>
                <span class="opt-text">正确</span>
              </div>
              <div
                class="opt judge"
                :class="{ on: judge[q.id] === 'F' }"
                @click="pickJudge(q, 'F')"
              >
                <span class="opt-key">×</span>
                <span class="opt-text">错误</span>
              </div>
            </div>

            <!-- 填空：几个空就几个输入框 -->
            <div v-else-if="q.type === 4" class="blanks">
              <div v-for="(_, bi) in blankCount(q)" :key="bi" class="blank-row">
                <span class="blank-no">第 {{ bi + 1 }} 空</span>
                <el-input
                  :model-value="(blank[q.id] || [])[bi] || ''"
                  placeholder="填写答案"
                  @update:model-value="(v: string) => setBlank(q, bi, v)"
                />
              </div>
              <p class="multi-tip">填空题按空判分，全部填对才得分</p>
            </div>

            <!-- 问答：主观题，交卷后由教师批阅 -->
            <div v-else class="essay">
              <RichEditor
                :model-value="essay[q.id] || ''"
                :min-height="150"
                placeholder="按要点作答，支持加粗、列表与粘贴截图，答案会提交给教师批阅"
                @update:model-value="(v: string) => (essay[q.id] = v)"
              />
              <p class="multi-tip">
                <el-icon><InfoFilled /></el-icon>
                问答题不自动判分，交卷后由教师批阅给分，可在「成绩与测验」查看最终得分
              </p>
            </div>
          </section>

          <div class="submit-foot">
            <el-button type="primary" size="large" @click="trySubmit">交卷并查看成绩</el-button>
          </div>
        </main>

        <!-- 答题卡 -->
        <aside class="sheet">
          <div class="sheet-card">
            <h3>答题卡</h3>
            <div class="sheet-grid">
              <button
                v-for="(q, i) in questions"
                :key="q.id"
                class="sheet-cell"
                :class="{ done: isAnswered(q), current: activeIdx === i }"
                @click="scrollTo(i)"
              >
                {{ i + 1 }}
              </button>
            </div>
            <div class="sheet-legend">
              <span><i class="lg-done" />已答</span>
              <span><i class="lg-todo" />未答</span>
            </div>
            <p class="sheet-tip">
              剩余 {{ questions.length - answeredCount }} 题未作答
            </p>
            <p v-if="draftTip" class="sheet-draft">
              <el-icon><CircleCheck /></el-icon> 草稿 {{ draftTip }} 已保存
            </p>
          </div>
        </aside>
      </div>
    </template>

    <!-- 测验笔记 -->
    <el-dialog append-to-body v-model="workNote.visible" title="测验笔记" width="560px">
      <RichEditor
        v-model="workNote.content"
        :min-height="160"
        placeholder="记录这道题/这套卷的思路与错因，截图可直接粘贴"
      />
      <template #footer>
        <el-button @click="workNote.visible = false">取消</el-button>
        <el-button type="primary" @click="saveWorkNote">保存笔记</el-button>
      </template>
    </el-dialog>

    <!-- 交卷确认 -->
    <el-dialog append-to-body v-model="confirm" title="确认交卷" width="420px" align-center>
      <p class="confirm-text">
        你已作答 <b>{{ answeredCount }}</b> / {{ questions.length }} 题。
        <template v-if="questions.length - answeredCount > 0">
          还有 <b class="warn">{{ questions.length - answeredCount }}</b> 题未作答，未答按 0 分计。
        </template>
        交卷后不能修改。
      </p>
      <template #footer>
        <el-button @click="confirm = false">继续答题</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">
          确认交卷
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import RichEditor from '@/components/RichEditor.vue'

const route = useRoute()
const router = useRouter()

/**
 * 两种用法：
 * - 路由页 /exam/:paperId（embedded=false）
 * - 被课程详情页以全屏弹窗嵌进来（embedded=true，paperId 由 props 传）
 * 课程详情里点「去测验」走后者，避免弹窗背后跳到别的页面。
 */
const props = withDefaults(
  defineProps<{
    paperId?: string | number
    embedded?: boolean
  }>(),
  { paperId: undefined, embedded: false }
)
const emit = defineEmits<{ (e: 'close'): void }>()

const pid = computed(() => (props.paperId != null ? props.paperId : route.params.paperId))
const embedded = computed(() => props.embedded)

/** 关闭：嵌在弹窗里时通知父组件收起弹窗，独立页面时退回上一页 */
function close() {
  if (embedded.value) {
    emit('close')
  } else {
    close()
  }
}

function goGrades() {
  if (embedded.value) {
    emit('close')
  }
  router.push('/grades')
}

const loading = ref(false)
const submitting = ref(false)
const paper = ref<any>({})
const questions = ref<any[]>([])
const result = ref<any>(null)
const confirm = ref(false)
const activeIdx = ref(0)

const single = reactive<Record<string, string>>({})
const multi = reactive<Record<string, string[]>>({})
const judge = reactive<Record<string, string>>({})
/** 填空题按空存，提交时用 | 拼起来 */
const blank = reactive<Record<string, string[]>>({})
/** 问答题原文 */
const essay = reactive<Record<string, string>>({})

const seconds = ref(0)
let timer: number | undefined

const rsLen = 2 * Math.PI * 66
const rsOffset = computed(() => {
  if (!result.value) return rsLen
  const rate = Math.min(100, ((result.value.score || 0) / (result.value.totalScore || 100)) * 100)
  return rsLen * (1 - rate / 100)
})

const correctCount = computed(
  () => (result.value?.details || []).filter((d: any) => d.correct).length
)

const leftText = computed(() => {
  const m = Math.floor(seconds.value / 60)
  const s = seconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const answeredCount = computed(
  () => questions.value.filter((q) => isAnswered(q)).length
)

function typeName(t: number) {
  return ['', '单选题', '多选题', '判断题', '填空题', '问答题'][t] || '题目'
}
function typeClass(t: number) {
  return ['', 'chip--blue', 'chip--violet', 'chip--amber', 'chip--green', 'chip--brand'][t] || ''
}

/** 填空题的空数：题干里 ______ 的数量，至少 1 个 */
function blankCount(q: any) {
  const matched = (q.title || '').match(/_{2,}/g)
  return Math.max(1, matched ? matched.length : 1)
}

function setBlank(q: any, index: number, value: string) {
  const arr = blank[q.id] || []
  while (arr.length < blankCount(q)) arr.push('')
  arr[index] = value
  blank[q.id] = [...arr]
}

function isAnswered(q: any) {
  if (q.type === 1) return !!single[q.id]
  if (q.type === 2) return (multi[q.id] || []).length > 0
  if (q.type === 3) return !!judge[q.id]
  if (q.type === 4) return (blank[q.id] || []).some((v) => (v || '').trim())
  return !!(essay[q.id] || '').trim()
}

function pickSingle(q: any, key: string) {
  single[q.id] = key
}
function pickMulti(q: any, key: string) {
  const arr = multi[q.id] || []
  multi[q.id] = arr.includes(key) ? arr.filter((k) => k !== key) : [...arr, key]
}
function pickJudge(q: any, key: string) {
  judge[q.id] = key
}

function scrollTo(i: number) {
  activeIdx.value = i
  document.getElementById(`q-${i}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function load() {
  loading.value = true
  try {
    let data: any
    try {
      data = await request.get(`/api/papers/${pid.value}/start`)
    } catch (e: any) {
      // 已交过卷 / 未到时间 / 已结束：能拿到成绩就直接展示成绩，否则留在提示页
      await showRecordIfAny()
      return
    }
    paper.value = data.paper || {}
    questions.value = data.questions || []
    questions.value.forEach((q) => {
      if (q.type === 2) multi[q.id] = []
    })
    restoreDraft()
    seconds.value = (paper.value.duration || 30) * 60
    timer = window.setInterval(() => {
      seconds.value--
      if (seconds.value <= 0) {
        window.clearInterval(timer)
        ElMessage.warning('时间到，已自动交卷')
        submitNow()
      }
    }, 1000)
  } finally {
    loading.value = false
  }
}

/** 已提交过的话，直接展示那次答卷的成绩与解析 */
async function showRecordIfAny() {
  try {
    const records: any = await request.get('/api/my/records')
    const hit = (records || []).find(
      (r: any) => Number(r.record?.paperId) === Number(pid.value)
    )
    if (!hit) {
      ElMessage.warning('该测验当前不可作答')
      close()
      return
    }
    result.value = await request.get(`/api/records/${hit.record.id}`)
    // 笔记要挂课程，答卷本身不带课程信息，从列表项里取
    if (hit.courseId) paper.value = { ...paper.value, courseId: hit.courseId }
    if (hit.paperTitle) paper.value = { ...paper.value, title: hit.paperTitle }
  } catch {
    close()
  }
}

function trySubmit() {
  if (!answeredCount.value) {
    ElMessage.warning('还没有作答任何题目')
    return
  }
  confirm.value = true
}

async function doSubmit() {
  confirm.value = false
  await submitNow()
}

/** 实际交卷：先把三种题型的答案归一成 {题目ID: "A,C"} */
async function submitNow() {
  if (submitting.value) return
  submitting.value = true
  try {
    const answers: Record<string, string> = {}
    questions.value.forEach((q) => {
      const key = String(q.id)
      if (q.type === 1) answers[key] = single[key] || ''
      else if (q.type === 2) answers[key] = (multi[key] || []).slice().sort().join(',')
      else if (q.type === 3) answers[key] = judge[key] || ''
      else if (q.type === 4) {
        const arr = blank[key] || []
        answers[key] = Array.from({ length: blankCount(q) }, (_, i) => (arr[i] || '').trim()).join('|')
      } else answers[key] = essay[key] || ''
    })
    result.value = await request.post('/api/exams/submit', {
      paperId: paper.value.id,
      answers
    })
    if (timer) window.clearInterval(timer)
    clearDraft()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } finally {
    submitting.value = false
  }
}

async function leave() {
  try {
    await ElMessageBox.confirm('离开后作答进度会以草稿保存在本机，下次进入可以继续，确定退出？', '提示', {
      type: 'warning',
      confirmButtonText: '保存并退出',
      cancelButtonText: '继续答题'
    })
    saveDraft()
    if (timer) window.clearInterval(timer)
    close()
  } catch {
    /* 取消就继续答题 */
  }
}

// ---------------- 作答草稿 ----------------
const draftTip = ref('')

function draftKey() {
  return `ol-draft:exam:${pid.value}`
}

/** 一个字都没填过就不必存草稿，免得答题卡一直提示「草稿已保存」 */
function hasAnyAnswer() {
  if (Object.keys(single).length || Object.keys(judge).length || Object.keys(blank).length) return true
  if (Object.values(multi).some((v: any) => Array.isArray(v) && v.length)) return true
  return Object.values(essay).some((v: any) =>
    String(v || '').replace(/<img[^>]*>/gi, 'IMG').replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
  )
}

function saveDraft() {
  if (result.value || !questions.value.length) return
  if (!hasAnyAnswer()) {
    clearDraft()
    draftTip.value = ''
    return
  }
  const payload = {
    single: { ...single },
    multi: { ...multi },
    judge: { ...judge },
    blank: { ...blank },
    essay: { ...essay },
    at: Date.now()
  }
  try {
    localStorage.setItem(draftKey(), JSON.stringify(payload))
    draftTip.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch {
    /* 忽略 */
  }
}

function restoreDraft() {
  try {
    const raw = localStorage.getItem(draftKey())
    if (!raw) return
    const d = JSON.parse(raw)
    Object.assign(single, d.single || {})
    Object.assign(multi, d.multi || {})
    Object.assign(judge, d.judge || {})
    Object.assign(blank, d.blank || {})
    Object.assign(essay, d.essay || {})
    draftTip.value = new Date(d.at).toLocaleTimeString('zh-CN', { hour12: false })
    ElMessage.success('已恢复上次未提交的作答草稿')
  } catch {
    /* 草稿损坏忽略 */
  }
}

function clearDraft() {
  localStorage.removeItem(draftKey())
}

let saveTimer: number | null = null
watch(
  [single, multi, judge, blank, essay],
  () => {
    if (!questions.value.length || result.value) return
    if (saveTimer) window.clearTimeout(saveTimer)
    saveTimer = window.setTimeout(saveDraft, 1000)
  },
  { deep: true }
)

// ---------------- 测验笔记 ----------------
const workNote = ref<any>({ visible: false, content: '', existingId: null })

async function openWorkNote() {
  workNote.value = { visible: true, content: '', existingId: null }
  try {
    const list: any = await request.get('/api/my/notes')
    const hit = (list || []).find(
      (n: any) =>
        n.refType === 'EXAM' && n.refId === Number(pid.value)
    )
    if (hit) {
      workNote.value.content = hit.content
      workNote.value.existingId = hit.id
    }
  } catch {
    /* 没有课程信息时也允许写 */
  }
}

async function saveWorkNote() {
  const text = (workNote.value.content || '').replace(/<[^>]+>/g, '').trim()
  if (!text && !workNote.value.content.includes('<img')) {
    ElMessage.warning('笔记内容不能为空')
    return
  }
  await request.post('/api/my/notes', {
    id: workNote.value.existingId,
    courseId: paper.value.courseId,
    refType: 'EXAM',
    refId: Number(pid.value),
    title: '',
    content: workNote.value.content
  })
  ElMessage.success('笔记已保存')
  workNote.value.visible = false
}

/** 富文本是否有内容，用于结果页判断「未作答」 */
function plainOf(html: string) {
  return (html || '').replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
}

onMounted(load)
onUnmounted(() => {
  if (timer) window.clearInterval(timer)
  if (saveTimer) window.clearTimeout(saveTimer)
  saveDraft()
})
</script>

<style scoped>
.exam {
  min-height: calc(100vh - 64px);
  background: var(--bg);
}

/* ---------------- 顶部条 ---------------- */
.bar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 30px 28px;
  background: rgba(255, 255, 255, 0.24);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--ink-100);
}
.bar-left {
  display: flex;
  align-items: center;
  gap: 13px;
  min-width: 0;
}
.back {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  border: 1px solid var(--ink-100);
  background: var(--surface);
  color: var(--ink-500);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all 0.18s;
}
.back:hover {
  color: var(--brand);
  border-color: var(--brand-200);
}
.bar-left h1 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bar-left p {
  margin: 2px 0 0;
  font-size: 11.5px;
  color: var(--ink-400);
}
.bar-right {
  display: flex;
  align-items: center;
  gap: 18px;
  flex: none;
}
.progress-mini {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
  color: var(--ink-400);
}
.progress-mini b {
  color: var(--ink-900);
  font-weight: 700;
}
.timer {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: var(--r-pill);
  background: var(--surface-sunken);
  color: var(--ink-700);
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.timer.urgent {
  background: #fdecec;
  color: #c0392b;
  animation: pulse 1s infinite;
}
@keyframes pulse {
  50% {
    opacity: 0.62;
  }
}

/* ---------------- 主体 ---------------- */
.body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 244px;
  gap: 22px;
  max-width: 1120px;
  margin: 0 auto;
  padding: 22px 24px 48px;
  align-items: start;
}
.questions {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}
.q-card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 20px 22px 22px;
  scroll-margin-top: 84px;
}
.q-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.q-idx {
  width: 26px;
  height: 26px;
  border-radius: 9px;
  background: var(--ink-900);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 12.5px;
  font-weight: 700;
}
.q-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 620;
  line-height: 1.65;
}

.opts {
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.opts--row {
  flex-direction: row;
}
.opt {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 15px;
  border: 1.5px solid var(--ink-100);
  border-radius: var(--r-md);
  cursor: pointer;
  transition: all 0.16s;
  background: var(--surface);
}
.opt:hover {
  border-color: var(--brand-300);
  background: var(--brand-50);
}
.opt.on {
  border-color: var(--brand);
  background: var(--brand-50);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.1);
}
.opt-key {
  width: 26px;
  height: 26px;
  flex: none;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--surface-sunken);
  color: var(--ink-500);
  font-size: 12.5px;
  font-weight: 700;
  transition: all 0.16s;
}
.opt.on .opt-key {
  background: var(--brand);
  color: #fff;
}
.opt-text {
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--ink-700);
}
.opt.on .opt-text {
  color: var(--brand-700);
  font-weight: 550;
}
.judge {
  flex: 1;
}
.multi-tip {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: var(--ink-300);
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 填空题 */
.blanks {
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.blank-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.blank-no {
  flex: none;
  width: 62px;
  font-size: 12px;
  color: var(--ink-400);
  font-weight: 550;
}

/* 问答题 */
.essay {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.q-pending {
  background: var(--violet-soft);
  color: #5a4bcf;
}

.submit-foot {
  display: flex;
  justify-content: center;
  padding: 10px 0 0;
}

/* ---------------- 答题卡 ---------------- */
.sheet {
  position: sticky;
  top: 92px;
}
.sheet-card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 18px;
}
.sheet-card h3 {
  margin: 0 0 13px;
  font-size: 13.5px;
  font-weight: 680;
}
.sheet-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 7px;
}
.sheet-cell {
  aspect-ratio: 1;
  border-radius: 9px;
  border: 1px solid var(--ink-100);
  background: var(--surface);
  color: var(--ink-400);
  font-size: 12.5px;
  font-weight: 650;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.16s;
}
.sheet-cell:hover {
  border-color: var(--brand-300);
  color: var(--brand);
}
.sheet-cell.done {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}
.sheet-cell.current {
  outline: 2px solid var(--ink-900);
  outline-offset: 1px;
}
.sheet-legend {
  display: flex;
  gap: 14px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--ink-100);
  font-size: 11px;
  color: var(--ink-400);
}
.sheet-legend span {
  display: flex;
  align-items: center;
  gap: 5px;
}
.sheet-legend i {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}
.lg-done {
  background: var(--brand);
}
.lg-todo {
  background: var(--surface);
  border: 1px solid var(--ink-200);
}
.sheet-tip {
  margin: 10px 0 0;
  font-size: 11.5px;
  color: var(--ink-400);
}

/* ---------------- 结果 ---------------- */
.result-wrap {
  max-width: 860px;
  margin: 0 auto;
  padding: 28px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.result-card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-xl);
  padding: 34px;
  text-align: center;
}
.rs-ring {
  position: relative;
  width: 160px;
  height: 160px;
  margin: 0 auto 14px;
}
.rs-ring svg {
  width: 160px;
  height: 160px;
  transform: rotate(-90deg);
}
.rs-track {
  fill: none;
  stroke: #f3e7da;
  stroke-width: 12;
}
.rs-bar {
  fill: none;
  stroke: var(--green);
  stroke-width: 12;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.9s cubic-bezier(0.22, 1, 0.36, 1);
}
.rs-bar.fail {
  stroke: var(--brand);
}
.rs-text {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.rs-text b {
  font-size: 44px;
  font-weight: 800;
  letter-spacing: -2px;
  line-height: 1;
}
.rs-text span {
  font-size: 12px;
  color: var(--ink-400);
  margin-top: 4px;
}
.result-card h2 {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 720;
}
.rs-tip {
  margin: 0 0 20px;
  font-size: 13px;
  color: var(--ink-500);
}
.rs-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.q-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.q-item {
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 13px 15px;
}
.q-item.wrong {
  border-color: #f6d5d0;
  background: #fffbfa;
}
.q-head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 8px;
}
.q-head b {
  flex: 1;
  font-size: 13.5px;
  font-weight: 620;
  line-height: 1.55;
}
.q-no {
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  background: var(--brand-50);
  color: var(--brand-700);
}
.q-no.ok {
  background: var(--green-soft);
  color: var(--green);
}
.q-ans {
  font-size: 12.5px;
  color: var(--ink-500);
}
.q-ans b {
  font-weight: 650;
  color: var(--ink-900);
}
.q-ans .bad {
  color: #c0392b;
}
.ok-txt {
  color: var(--green) !important;
}
.ans-line {
  font-size: 12.5px;
  color: var(--ink-500);
  margin-bottom: 4px;
}
.ans-rich {
  background: var(--surface-soft);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-sm);
  padding: 10px 12px;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--ink-700);
  margin-bottom: 8px;
  word-break: break-word;
}
.ans-rich :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 6px 0;
}
.ans-ref {
  font-size: 12.5px;
  color: var(--green);
  background: var(--green-soft);
  border-radius: 8px;
  padding: 6px 10px;
}
.rs-done {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 4px 0 10px;
  font-size: 12px;
}
.sheet-draft {
  margin: 8px 0 0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--green);
}
.sep {
  margin: 0 8px;
  color: var(--ink-200);
}
.q-analysis {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 9px;
  font-size: 12px;
  line-height: 1.7;
  color: #1f7a4f;
  background: var(--green-soft);
  padding: 9px 11px;
  border-radius: var(--r-sm);
}
.q-analysis .el-icon {
  margin-top: 2px;
  flex: none;
}

.confirm-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ink-700);
}
.confirm-text b {
  font-weight: 700;
}
.confirm-text .warn {
  color: #c0392b;
}

@media (max-width: 980px) {
  .body {
    grid-template-columns: 1fr;
  }
  .sheet {
    display: none;
  }
  .progress-mini {
    display: none;
  }
}
</style>
