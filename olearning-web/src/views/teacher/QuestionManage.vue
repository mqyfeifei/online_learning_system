<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>题库管理</h1>
        <p>{{ courseTitle }} · 共 {{ list.length }} 道题，试卷从题库里挑题组卷</p>
      </div>
      <div class="ap-actions">
        <el-button @click="router.push('/teacher/courses')">
          <el-icon><ArrowLeft /></el-icon> 返回课程
        </el-button>
        <el-button @click="router.push(`/teacher/courses/${courseId}/papers`)">
          <el-icon><EditPen /></el-icon> 去组卷
        </el-button>
        <el-button @click="exportBank">
          <el-icon><Download /></el-icon> 导出题库
        </el-button>
        <el-button @click="importDialog = true">
          <el-icon><Upload /></el-icon> 导入题库
        </el-button>
        <el-button type="primary" @click="openEdit(null)">
          <el-icon><Plus /></el-icon> 新增题目
        </el-button>
      </div>
    </div>

    <!-- 筛选 -->
    <section class="ap-toolbar">
      <span
        v-for="t in typeFilters"
        :key="String(t.key)"
        class="chip chip-btn"
        :class="{ 'is-active': typeFilter === t.key }"
        @click="typeFilter = t.key"
      >
        {{ t.label }}
      </span>
      <span class="grow" />
      <el-select v-model="diffFilter" placeholder="全部难度" clearable style="width: 140px">
        <el-option label="简单" :value="1" />
        <el-option label="中等" :value="2" />
        <el-option label="困难" :value="3" />
      </el-select>
    </section>

    <!-- 题目列表 -->
    <div class="q-list">
      <article v-for="(q, i) in filtered" :key="q.id" class="q-card">
        <div class="q-head">
          <span class="q-no">{{ i + 1 }}</span>
          <span class="chip" :class="typeClass(q.type)">{{ typeName(q.type) }}</span>
          <span class="chip" :class="diffClass(q.difficulty)">{{ diffName(q.difficulty) }}</span>
          <span class="grow" />
          <el-button link type="primary" @click="openEdit(q)">编辑</el-button>
          <el-button link type="danger" @click="remove(q)">删除</el-button>
        </div>

        <h3>{{ q.title }}</h3>

        <div v-if="q.type === 1 || q.type === 2" class="opts">
          <div
            v-for="o in parseOptions(q.optionsJson)"
            :key="o.key"
            class="opt"
            :class="{ right: isRight(q, o.key) }"
          >
            <span class="ok">{{ o.key }}</span>{{ o.text }}
          </div>
        </div>
        <div v-else-if="q.type === 3" class="opts">
          <div class="opt" :class="{ right: true }">
            <span class="ok">{{ q.answer === 'T' ? '√' : '×' }}</span>
            {{ q.answer === 'T' ? '该说法正确' : '该说法错误' }}
          </div>
        </div>
        <div v-else-if="q.type === 4" class="blank-answer">
          <span class="ba-label">标准答案</span>
          <span class="ba-text">{{ q.answer || '未设置' }}</span>
          <span class="ba-tip">多个空用 | 分隔，同一空多种写法用 ~ 分隔</span>
        </div>
        <div v-else-if="q.type === 5" class="blank-answer blank-answer--essay">
          <span class="ba-label">参考答案</span>
          <span class="ba-text">{{ q.referenceAnswer || '未填写参考答案（问答题不设标准答案，由教师批阅给分）' }}</span>
        </div>

        <div v-if="q.analysis" class="analysis">
          <el-icon><InfoFilled /></el-icon>{{ q.analysis }}
        </div>
      </article>

      <div v-if="!filtered.length" class="ol-empty ol-card">
        <span class="icon">❓</span>
        这个筛选下没有题目
      </div>
    </div>

    <!-- 编辑 -->
    <el-dialog append-to-body v-model="dialog" :title="form.id ? '编辑题目' : '新增题目'" width="680px" top="6vh">
      <el-form label-position="top">
        <div class="three">
          <el-form-item label="题型">
            <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
              <el-option label="单选题" :value="1" />
              <el-option label="多选题" :value="2" />
              <el-option label="判断题" :value="3" />
              <el-option label="填空题" :value="4" />
              <el-option label="问答题" :value="5" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty" style="width: 100%">
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.type <= 3" label="正确答案">
            <el-select
              v-if="form.type !== 3"
              v-model="answerArr"
              multiple
              collapse-tags
              style="width: 100%"
              placeholder="选择正确项"
            >
              <el-option v-for="o in form.options" :key="o.key" :label="o.key" :value="o.key" />
            </el-select>
            <el-select v-else v-model="form.answer" style="width: 100%">
              <el-option label="正确" value="T" />
              <el-option label="错误" value="F" />
            </el-select>
          </el-form-item>
          <el-form-item v-else-if="form.type === 4" label="标准答案">
            <el-input v-model="form.answer" placeholder="如 80|443" />
          </el-form-item>
          <el-form-item v-else label="批阅方式">
            <div class="auto-tip">主观题，由教师批阅给分</div>
          </el-form-item>
        </div>

        <el-form-item label="题干">
          <el-input
            v-model="form.title"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="例如：OSI 参考模型中负责端到端可靠传输的是哪一层？"
          />
        </el-form-item>

        <el-form-item v-if="form.type === 4" label="填空说明">
          <div class="hint-line">
            题干里用 ______ 表示空位；多个空用 <code>|</code> 分隔答案，同一空的多种正确写法用
            <code>~</code> 分隔（例：<code>80~eighty|443</code>），判分时忽略大小写与首尾空格。
          </div>
        </el-form-item>

        <el-form-item v-if="form.type === 5" label="参考答案（可选）">
          <el-input
            v-model="form.referenceAnswer"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="问答题一般没有标准答案，这里可以写评分要点，供自己和其它教师批阅时参考"
          />
        </el-form-item>

        <el-form-item v-if="form.type === 1 || form.type === 2" label="选项">
          <div class="opt-editor">
            <div v-for="(o, i) in form.options" :key="i" class="opt-row">
              <span class="opt-key">{{ o.key }}</span>
              <el-input v-model="o.text" :placeholder="`选项 ${o.key} 的内容`" />
              <el-button
                link
                type="danger"
                :disabled="form.options.length <= 2"
                @click="removeOption(i)"
              >
                删除
              </el-button>
            </div>
            <el-button
              v-if="form.options.length < 6"
              link
              type="primary"
              @click="addOption"
            >
              + 添加选项
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="解析（可选）">
          <el-input
            v-model="form.analysis"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="解释为什么选它，学生答错时会看到这段"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题库导入 -->
    <el-dialog append-to-body v-model="importDialog" title="导入题库" width="580px">
      <div class="imp-tips">
        <b>格式要求</b>
        <ul>
          <li>只支持 <code>.xlsx</code>，表头固定为：题型、题干、选项A~F、答案、参考答案、解析、难度</li>
          <li>题型填「单选 / 多选 / 判断 / 填空 / 问答」；难度填「简单 / 中等 / 困难」</li>
          <li>单选答案填一个字母（如 <code>B</code>），多选填多个字母用英文逗号分隔（如 <code>A,C</code>）</li>
          <li>判断题答案填 <code>T</code> 或 <code>F</code>；填空题多个空用 <code>|</code> 分隔</li>
          <li>问答题不需要标准答案，「参考答案」可选填</li>
        </ul>
      </div>

      <div class="imp-actions">
        <el-button @click="downloadTemplate">
          <el-icon><Download /></el-icon> 下载模板
        </el-button>
        <el-upload
          :http-request="doImport"
          :show-file-list="false"
          accept=".xlsx"
          :before-upload="beforeImport"
        >
          <el-button type="primary" :loading="importing">
            <el-icon><Upload /></el-icon>
            {{ importing ? '导入中…' : '选择文件导入' }}
          </el-button>
        </el-upload>
        <span class="imp-note">导入过程中会逐行校验，失败的条目不会影响其它行</span>
      </div>

      <div v-if="importResult" class="imp-result">
        <div class="ir-head">
          <span class="chip chip--green">成功 {{ importResult.success }} 条</span>
          <span v-if="importResult.failed?.length" class="chip chip--brand">
            失败 {{ importResult.failed.length }} 条
          </span>
        </div>
        <ul v-if="importResult.failed?.length" class="ir-list">
          <li v-for="(f, i) in importResult.failed" :key="i">
            第 {{ f.row }} 行：{{ f.message }}
          </li>
        </ul>
      </div>

      <template #footer>
        <el-button @click="importDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { triggerDownload } from '@/utils/file'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const courseTitle = ref('课程')
const list = ref<any[]>([])
const dialog = ref(false)
const typeFilter = ref<number | null>(null)
const diffFilter = ref<number | null>(null)
const answerArr = ref<string[]>([])

// ---------------- 题库导入导出 ----------------
const importDialog = ref(false)
const importing = ref(false)
const importResult = ref<any>(null)

function exportBank() {
  triggerDownload(`/api/teacher/courses/${courseId}/questions/export`)
}

function downloadTemplate() {
  triggerDownload('/api/teacher/questions/import-template')
}

function beforeImport(file: File) {
  if (!/\.xlsx$/i.test(file.name)) {
    ElMessage.warning('只支持 .xlsx 文件，请先下载模板')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 10MB')
    return false
  }
  return true
}

async function doImport(options: any) {
  importing.value = true
  importResult.value = null
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const res: any = await request.post(
      `/api/teacher/courses/${courseId}/questions/import`,
      fd
    )
    importResult.value = res
    if (res.failed?.length) {
      ElMessage.warning(`导入完成：成功 ${res.success} 条，失败 ${res.failed.length} 条`)
    } else {
      ElMessage.success(`导入完成，共 ${res.success} 条`)
    }
    load()
  } catch {
    // 拦截器已提示
  } finally {
    importing.value = false
  }
}

const form = ref<any>({
  id: null,
  type: 1,
  title: '',
  options: [
    { key: 'A', text: '' },
    { key: 'B', text: '' },
    { key: 'C', text: '' },
    { key: 'D', text: '' }
  ],
  answer: '',
  referenceAnswer: '',
  analysis: '',
  difficulty: 1
})

const typeFilters = [
  { key: null, label: '全部题型' },
  { key: 1, label: '单选题' },
  { key: 2, label: '多选题' },
  { key: 3, label: '判断题' },
  { key: 4, label: '填空题' },
  { key: 5, label: '问答题' }
]

const filtered = computed(() =>
  list.value.filter(
    (q) =>
      (typeFilter.value === null || q.type === typeFilter.value) &&
      (!diffFilter.value || q.difficulty === diffFilter.value)
  )
)

function typeName(t: number) {
  return ['', '单选题', '多选题', '判断题', '填空题', '问答题'][t] || '题目'
}
function typeClass(t: number) {
  return ['', 'chip--blue', 'chip--violet', 'chip--amber', 'chip--green', 'chip--brand'][t] || ''
}
function diffName(d: number) {
  return d === 1 ? '简单' : d === 2 ? '中等' : '困难'
}
function diffClass(d: number) {
  return d === 1 ? 'chip--green' : d === 2 ? 'chip--amber' : 'chip--brand'
}

function parseOptions(json: string) {
  if (!json) return []
  try {
    return JSON.parse(json) as { key: string; text: string }[]
  } catch {
    return []
  }
}

function isRight(q: any, key: string) {
  return (q.answer || '').split(',').includes(key)
}

async function load() {
  const courses: any = await request.get('/api/teacher/courses')
  const target = (courses || []).find((c: any) => c.id === courseId)
  courseTitle.value = target?.title || '课程'
  list.value = ((await request.get(`/api/teacher/courses/${courseId}/questions`)) as any) || []
}

function onTypeChange() {
  const type = form.value.type
  if (type === 3) {
    form.value.options = []
    form.value.answer = 'T'
    answerArr.value = []
  } else if (type === 4) {
    // 填空题：清空选项，答案手填
    form.value.options = []
    form.value.answer = ''
    answerArr.value = []
  } else if (type === 5) {
    // 问答题：没有标准答案，只保留参考答案
    form.value.options = []
    form.value.answer = ''
    answerArr.value = []
  } else {
    if (!form.value.options?.length) {
      form.value.options = [
        { key: 'A', text: '' },
        { key: 'B', text: '' },
        { key: 'C', text: '' },
        { key: 'D', text: '' }
      ]
    }
    form.value.answer = ''
    answerArr.value = []
  }
}

function addOption() {
  const keys = ['A', 'B', 'C', 'D', 'E', 'F']
  form.value.options.push({ key: keys[form.value.options.length], text: '' })
}

function removeOption(i: number) {
  form.value.options.splice(i, 1)
  // 删完要重排 key，否则会出现 A、C、D 这种断号
  const keys = ['A', 'B', 'C', 'D', 'E', 'F']
  form.value.options.forEach((o: any, idx: number) => (o.key = keys[idx]))
  answerArr.value = answerArr.value.filter((k) => form.value.options.some((o: any) => o.key === k))
}

function openEdit(row: any) {
  const isChoice = row && (row.type === 1 || row.type === 2)
  if (row) {
    form.value = {
      id: row.id,
      type: row.type,
      title: row.title,
      options: isChoice ? parseOptions(row.optionsJson) : [],
      answer: row.answer || '',
      referenceAnswer: row.referenceAnswer || '',
      analysis: row.analysis || '',
      difficulty: row.difficulty || 1
    }
    answerArr.value = isChoice ? (row.answer || '').split(',').filter(Boolean) : []
  } else {
    form.value = {
      id: null,
      type: 1,
      title: '',
      options: [
        { key: 'A', text: '' },
        { key: 'B', text: '' },
        { key: 'C', text: '' },
        { key: 'D', text: '' }
      ],
      answer: '',
      referenceAnswer: '',
      analysis: '',
      difficulty: 1
    }
    answerArr.value = []
  }
  dialog.value = true
}

async function save() {
  const f = form.value
  if (!f.title.trim()) {
    ElMessage.warning('题干不能为空')
    return
  }
  if (f.type === 1 || f.type === 2) {
    if (f.options.some((o: any) => !o.text.trim())) {
      ElMessage.warning('选项内容不能为空')
      return
    }
    if (!answerArr.value.length) {
      ElMessage.warning('请选择正确答案')
      return
    }
    if (f.type === 1 && answerArr.value.length > 1) {
      ElMessage.warning('单选题只能有一个正确答案')
      return
    }
    f.answer = [...answerArr.value].sort().join(',')
  } else if (f.type === 3) {
    if (!f.answer) {
      ElMessage.warning('请选择判断结果')
      return
    }
  } else if (f.type === 4) {
    if (!f.answer.trim()) {
      ElMessage.warning('请填写填空题的标准答案')
      return
    }
  } else if (f.type === 5) {
    // 问答题不强制参考答案：没有标准答案，教师批阅时给分
    if (!f.referenceAnswer.trim()) {
      const go = await ElMessageBox.confirm(
        '问答题将不设参考答案，教师批卷时按要点给分。确定继续吗？',
        '提示',
        { type: 'info', confirmButtonText: '继续保存' }
      ).catch(() => false)
      if (!go) return
    }
  }

  const isChoice = f.type === 1 || f.type === 2
  await request.post('/api/teacher/questions', {
    id: f.id,
    courseId,
    type: f.type,
    title: f.title,
    optionsJson: isChoice ? JSON.stringify(f.options) : null,
    answer: f.type === 5 ? null : f.answer,
    referenceAnswer: f.type === 5 ? f.referenceAnswer || null : null,
    analysis: f.analysis,
    difficulty: f.difficulty
  })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('删除这道题？已组卷的试卷里也会移除。', '提示', { type: 'warning' })
  await request.delete(`/api/teacher/questions/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.grow {
  flex: 1;
}
.q-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.q-card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 16px 20px 18px;
}
.q-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.q-no {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 11.5px;
  font-weight: 700;
  background: var(--ink-900);
  color: #fff;
}
.q-card h3 {
  margin: 0 0 12px;
  font-size: 14.5px;
  font-weight: 620;
  line-height: 1.6;
}
.opts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 7px;
}
.opt {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 11px;
  border-radius: var(--r-sm);
  background: var(--surface-sunken);
  font-size: 12.5px;
  color: var(--ink-500);
  line-height: 1.55;
}
.opt.right {
  background: var(--green-soft);
  color: #1f7a4f;
  font-weight: 550;
}
.ok {
  font-weight: 750;
  flex: none;
}
.analysis {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 11px;
  padding: 9px 11px;
  background: var(--amber-soft);
  border-radius: var(--r-sm);
  font-size: 12px;
  line-height: 1.7;
  color: #8a5a06;
}
.analysis .el-icon {
  margin-top: 2px;
  flex: none;
}

.three {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}
/* 填空 / 问答题的答案展示 */
.blank-answer {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px 13px;
  border-radius: var(--r-sm);
  background: var(--green-soft);
  font-size: 12.5px;
  line-height: 1.7;
}
.blank-answer--essay {
  background: var(--violet-soft);
}
.ba-label {
  font-weight: 700;
  color: var(--ink-700);
  flex: none;
}
.ba-text {
  color: var(--ink-700);
  white-space: pre-wrap;
}
.ba-tip {
  font-size: 11px;
  color: var(--ink-400);
}
.auto-tip {
  width: 100%;
  height: 32px;
  display: flex;
  align-items: center;
  padding: 0 11px;
  border-radius: var(--r-sm);
  background: var(--violet-soft);
  color: #5a4bcf;
  font-size: 12px;
}
.hint-line {
  font-size: 12px;
  color: var(--ink-500);
  line-height: 1.8;
}
.hint-line code {
  background: var(--surface-sunken);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 11.5px;
  color: var(--brand-700);
}
.opt-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.opt-row {
  display: flex;
  align-items: center;
  gap: 9px;
}
.opt-key {
  width: 24px;
  height: 24px;
  flex: none;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 11.5px;
  font-weight: 700;
  background: var(--brand-50);
  color: var(--brand-700);
}

@media (max-width: 900px) {
  .opts {
    grid-template-columns: 1fr;
  }
  .three {
    grid-template-columns: 1fr;
  }
}

/* ---------- 导入 ---------- */
.imp-tips {
  background: var(--surface-soft);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 12px 16px;
}
.imp-tips b {
  font-size: 13px;
}
.imp-tips ul {
  margin: 8px 0 0;
  padding-left: 18px;
  font-size: 12.5px;
  line-height: 1.9;
  color: var(--ink-500);
}
.imp-tips code {
  background: var(--brand-50);
  color: var(--brand-700);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
}
.imp-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}
.imp-note {
  font-size: 11.5px;
  color: var(--ink-300);
}
.imp-result {
  margin-top: 16px;
  border-top: 1px dashed var(--ink-100);
  padding-top: 12px;
}
.ir-head {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.ir-list {
  margin: 0;
  padding-left: 18px;
  max-height: 180px;
  overflow: auto;
  font-size: 12.5px;
  line-height: 1.9;
  color: #c0392b;
}
</style>
