<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>学生与学习进度</h1>
        <p>{{ courseTitle }} · {{ list.length }} 人选课</p>
      </div>
      <div class="ap-actions">
        <el-button @click="router.push('/teacher/courses')">
          <el-icon><ArrowLeft /></el-icon> 返回课程
        </el-button>
        <el-button type="primary" @click="openImport">
          <el-icon><Upload /></el-icon> 导入学生名单
        </el-button>
      </div>
    </div>

    <!-- 待审核的选课申请 -->
    <section v-if="applies.length" class="ap-card apply-card">
      <div class="ap-card-head">
        <h2>选课申请 · {{ applies.length }} 人待处理</h2>
        <span class="ol-muted">本课程开启了选课审核</span>
      </div>
      <div class="apply-list">
        <div v-for="a in applies" :key="a.id" class="apply-row">
          <span class="s-avatar" :style="{ background: avatarColor(a.userId) }">
            {{ (a.realName || a.username || '?').slice(0, 1) }}
          </span>
          <div class="ap-info">
            <b>{{ a.realName || a.username }}</b>
            <span>@{{ a.username }}<template v-if="a.phone"> · {{ a.phone }}</template> · {{ fmt(a.applyTime) }}</span>
          </div>
          <span class="grow" />
          <el-button size="small" type="primary" @click="auditApply(a, true)">通过</el-button>
          <el-button size="small" @click="auditApply(a, false)">拒绝</el-button>
        </div>
      </div>
    </section>

    <section class="ap-stats">
      <div class="ap-stat">
        <span class="ico ico--brand"><el-icon><UserFilled /></el-icon></span>
        <b>{{ list.length }}<i>人</i></b>
        <span>选课人数</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--green"><el-icon><TrendCharts /></el-icon></span>
        <b>{{ avgProgress }}<i>%</i></b>
        <span>平均进度</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--violet"><el-icon><CircleCheckFilled /></el-icon></span>
        <b>{{ finishedCount }}<i>人</i></b>
        <span>已学完</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--amber"><el-icon><WarningFilled /></el-icon></span>
        <b>{{ laggingCount }}<i>人</i></b>
        <span>进度偏低（&lt;30%）</span>
      </div>
    </section>

    <section class="ap-card">
      <div class="ap-card-head">
        <h2>学生名单</h2>
        <el-select v-model="sortBy" size="small" style="width: 148px">
          <el-option label="按进度降序" value="desc" />
          <el-option label="按进度升序" value="asc" />
          <el-option label="按选课时间" value="time" />
        </el-select>
      </div>
      <div class="ap-card-body">
        <el-table :data="sorted" stripe style="width: 100%">
          <el-table-column label="学生" min-width="180">
            <template #default="{ row }">
              <div class="stu">
                <span class="s-avatar" :style="{ background: avatarColor(row.userId) }">
                  {{ (row.realName || row.username || '?').slice(0, 1) }}
                </span>
                <div>
                  <b>{{ row.realName || row.username }}</b>
                  <span>@{{ row.username }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="150" />
          <el-table-column label="学习进度" min-width="220">
            <template #default="{ row }">
              <div class="prog">
                <div class="bar" :class="{ 'bar--green': row.progress >= 100 }">
                  <i :style="{ width: (row.progress || 0) + '%' }" />
                </div>
                <span :class="{ done: row.progress >= 100 }">{{ row.progress || 0 }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span class="chip" :class="statusClass(row.progress)">
                {{ statusText(row.progress) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="选课时间" width="170">
            <template #default="{ row }">{{ fmt(row.enrollTime) }}</template>
          </el-table-column>
        </el-table>

        <div v-if="!list.length" class="ol-empty" style="padding: 40px 0">
          还没有学生选这门课
        </div>
      </div>
    </section>

    <!-- 导入学生名单 -->
    <el-dialog append-to-body v-model="importDialog" title="导入学生名单" width="620px" top="6vh">
      <el-alert type="info" :closable="false" show-icon class="imp-tip">
        每行一个<strong>已注册的登录名</strong>，也支持「登录名,姓名」两列；
        账号不存在或不是学生角色会跳过，并在下方列出原因。
      </el-alert>

      <div class="imp-toolbar">
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".txt,.csv"
          :on-change="readImportFile"
        >
          <el-button plain>
            <el-icon><Document /></el-icon> 选择文件（txt / csv）
          </el-button>
        </el-upload>
        <span class="ol-muted">导入后学生直接进入名单，无需再次选课</span>
      </div>

      <el-input
        v-model="importText"
        type="textarea"
        :rows="9"
        placeholder="student1&#10;student2&#10;student3,张三"
      />

      <div v-if="importResult" class="imp-result">
        <div class="ir-head">
          <span class="chip chip--green">成功 {{ importResult.successCount }}</span>
          <span class="chip chip--brand">失败 {{ importResult.failedCount }}</span>
        </div>
        <div v-if="importResult.failed.length" class="ir-list">
          <div v-for="(f, i) in importResult.failed" :key="i" class="ir-row">
            第 {{ f.line }} 行 · {{ f.username || '（空）' }} —— {{ f.reason }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="importDialog = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { avatarColor } from '@/utils/cover'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const courseTitle = ref('课程')
const list = ref<any[]>([])
const applies = ref<any[]>([])
const sortBy = ref<'desc' | 'asc' | 'time'>('desc')

// ---------------- 导入学生名单 ----------------
const importDialog = ref(false)
const importText = ref('')
const importing = ref(false)
const importResult = ref<any>(null)

function openImport() {
  importText.value = ''
  importResult.value = null
  importDialog.value = true
}

/** 选本地 txt/csv 后读成文本填进输入框，用户可以先改再提交 */
function readImportFile(file: any) {
  const raw: File = file.raw
  if (!raw) return
  const reader = new FileReader()
  reader.onload = () => {
    importText.value = String(reader.result || '').replace(/^\uFEFF/, '')
    ElMessage.success(`已读取 ${raw.name}`)
  }
  reader.readAsText(raw, 'utf-8')
}

async function doImport() {
  if (!importText.value.trim()) {
    ElMessage.warning('请粘贴或选择要导入的名单')
    return
  }
  importing.value = true
  try {
    importResult.value = await request.post(`/api/teacher/courses/${courseId}/students/import`, {
      text: importText.value
    })
    ElMessage.success(`导入完成：成功 ${importResult.value.successCount} 人`)
    await loadStudents()
  } finally {
    importing.value = false
  }
}

// ---------------- 选课申请 ----------------
async function loadApplies() {
  applies.value = ((await request.get(`/api/teacher/courses/${courseId}/enroll-applies`)) as any) || []
}

async function auditApply(row: any, pass: boolean) {
  const name = row.realName || row.username
  if (!pass) {
    await ElMessageBox.confirm(`拒绝 ${name} 的选课申请？`, '提示', { type: 'warning' })
  }
  await request.post(`/api/teacher/enroll-applies/${row.id}/audit`, { pass })
  ElMessage.success(pass ? '已通过' : '已拒绝')
  await Promise.all([loadApplies(), loadStudents()])
}

const avgProgress = computed(() =>
  list.value.length
    ? Math.round(list.value.reduce((s, r) => s + (r.progress || 0), 0) / list.value.length)
    : 0
)
const finishedCount = computed(() => list.value.filter((r) => (r.progress || 0) >= 100).length)
const laggingCount = computed(() => list.value.filter((r) => (r.progress || 0) < 30).length)

const sorted = computed(() => {
  const arr = [...list.value]
  if (sortBy.value === 'asc') return arr.sort((a, b) => (a.progress || 0) - (b.progress || 0))
  if (sortBy.value === 'time') return arr.sort((a, b) => (a.enrollTime < b.enrollTime ? 1 : -1))
  return arr.sort((a, b) => (b.progress || 0) - (a.progress || 0))
})

function statusText(p: number) {
  if (p >= 100) return '已学完'
  if (p >= 60) return '进展顺利'
  if (p >= 30) return '正常'
  if (p > 0) return '进度偏低'
  return '未开始'
}
function statusClass(p: number) {
  if (p >= 100) return 'chip--green'
  if (p >= 30) return 'chip--blue'
  if (p > 0) return 'chip--amber'
  return ''
}

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

async function loadStudents() {
  list.value = ((await request.get(`/api/teacher/courses/${courseId}/students`)) as any) || []
}

onMounted(async () => {
  const courses: any = await request.get('/api/teacher/courses')
  const target = (courses || []).find((c: any) => c.id === courseId)
  courseTitle.value = target?.title || '课程'
  await Promise.all([loadStudents(), loadApplies()])
})
</script>

<style scoped>
.stu {
  display: flex;
  align-items: center;
  gap: 10px;
}
.s-avatar {
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}
.stu b {
  display: block;
  font-size: 13px;
  font-weight: 620;
}
.stu span {
  font-size: 11px;
  color: var(--ink-400);
}
.prog {
  display: flex;
  align-items: center;
  gap: 10px;
}
.prog .bar {
  flex: 1;
}
.prog span {
  width: 40px;
  text-align: right;
  font-size: 12px;
  font-weight: 650;
  color: var(--brand-700);
}
.prog span.done {
  color: var(--green);
}

/* ---------- 选课申请 ---------- */
.apply-card {
  border-color: var(--violet);
}
.apply-list {
  display: flex;
  flex-direction: column;
}
.apply-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--ink-100);
}
.apply-row:last-child {
  border-bottom: 0;
}
.ap-info b {
  display: block;
  font-size: 13px;
  font-weight: 620;
}
.ap-info span {
  font-size: 11.5px;
  color: var(--ink-400);
}
.grow {
  flex: 1;
}

/* ---------- 导入名单 ---------- */
.imp-tip {
  margin-bottom: 12px;
}
.imp-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.imp-result {
  margin-top: 14px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 12px 14px;
  background: var(--surface-soft);
}
.ir-head {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.ir-list {
  max-height: 160px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ir-row {
  font-size: 12px;
  color: #b4453b;
  line-height: 1.7;
}
</style>
