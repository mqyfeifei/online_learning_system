<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>作业与批阅</h1>
        <p>{{ courseTitle }} · 共 {{ list.length }} 次作业</p>
      </div>
      <div class="ap-actions">
        <el-button @click="router.push('/teacher/courses')">
          <el-icon><ArrowLeft /></el-icon> 返回课程
        </el-button>
        <el-button type="primary" @click="openEdit(null)">
          <el-icon><Plus /></el-icon> 布置作业
        </el-button>
      </div>
    </div>

    <!-- 作业列表 -->
    <div class="hw-list">
      <article v-for="item in list" :key="item.assignment.id" class="hw">
        <div class="hw-head">
          <span class="hw-ico"><el-icon><Document /></el-icon></span>
          <div class="hw-title">
            <b>{{ item.assignment.title }}</b>
            <span v-if="item.assignment.chapterTitle" class="chip chip--blue">
              {{ item.assignment.chapterTitle }}
            </span>
            <span class="chip" :class="deadlineClass(item.assignment.deadline)">
              {{ deadlineText(item.assignment.deadline) }}
            </span>
          </div>
          <div class="hw-ops">
            <el-button size="small" type="primary" @click="openSubmissions(item.assignment)">
              提交与批阅
            </el-button>
            <el-button size="small" @click="exportAssignment(item.assignment)">
              <el-icon><Download /></el-icon> 导出
            </el-button>
            <el-button size="small" @click="openEdit(item.assignment)">编辑</el-button>
            <el-button size="small" text type="danger" @click="remove(item.assignment)">
              删除
            </el-button>
          </div>
        </div>

        <p class="hw-req">{{ item.assignment.requirement }}</p>

        <div class="hw-meta">
          <span v-if="item.assignment.startTime">
            <el-icon><Timer /></el-icon>开始 {{ fmt(item.assignment.startTime) }}
          </span>
          <span><el-icon><Clock /></el-icon>截止 {{ fmt(item.assignment.deadline) }}</span>
          <span v-if="item.assignment.attachUrl" class="att-link" @click="previewFile(item.assignment.attachUrl)">
            <el-icon><Paperclip /></el-icon>附件模板（点击预览）
          </span>
        </div>
      </article>

      <div v-if="!list.length" class="ol-empty ol-card">
        <span class="icon">📄</span>
        还没有布置作业
      </div>
    </div>

    <!-- 编辑 -->
    <el-dialog append-to-body v-model="dialog" :title="form.id ? '编辑作业' : '布置作业'" width="580px">
      <el-form label-position="top">
        <el-form-item label="作业标题">
          <el-input v-model="form.title" maxlength="80" show-word-limit placeholder="例如：作业一：子网划分与 CIDR 计算" />
        </el-form-item>
        <el-form-item label="作业要求">
          <el-input
            v-model="form.requirement"
            type="textarea"
            :rows="6"
            placeholder="写清要交什么、格式要求、评分侧重"
          />
        </el-form-item>
        <el-form-item label="归属章节（可选）">
          <el-select
            v-model="form.chapterId"
            placeholder="不选表示整门课的作业"
            clearable
            style="width: 100%"
          >
            <el-option v-for="c in chapters" :key="c.id" :label="c.title" :value="c.id" />
          </el-select>
          <span class="tip">挂到章节上，学生在该章节里就能直接看到这次作业</span>
        </el-form-item>
        <div class="time-row">
          <el-form-item label="开始时间（可选）">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="留空表示立即开放"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker
              v-model="form.deadline"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择截止时间"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <p class="time-tip">学生只能在「开始时间 ~ 截止时间」之间提交作业</p>
        <el-form-item label="附件模板（可选）">
          <div class="attach-row">
            <el-input v-model="form.attachUrl" placeholder="上传附件模板，或粘贴文件地址" />
            <FileUploader
              label="选择文件"
              :max-size="50"
              @uploaded="(d) => (form.attachUrl = d.url)"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提交与批阅 -->
    <el-drawer v-model="drawer" append-to-body size="720px" :with-header="false">
      <div class="sub-wrap">
        <div class="sub-head">
          <div>
            <h2>{{ currentAssignment.title }}</h2>
            <p class="ol-muted">
              共 {{ submissions.length }} 人提交，待批阅
              {{ pendingCount }} 份
            </p>
          </div>
          <el-button @click="drawer = false">关闭</el-button>
        </div>

        <div class="sub-list">
          <div v-for="s in submissions" :key="s.submission.id" class="sub-item">
            <div class="s-avatar" :style="{ background: avatarColor(s.submission.id) }">
              {{ (s.realName || '同').slice(0, 1) }}
            </div>

            <div class="s-body">
              <div class="s-top">
                <b>{{ s.realName }}</b>
                <span class="chip" :class="s.submission.status === 1 ? 'chip--green' : 'chip--amber'">
                  {{ s.submission.status === 1 ? `已批阅 ${s.submission.score} 分` : '待批阅' }}
                </span>
                <span class="ol-muted">{{ fmt(s.submission.submitTime) }}</span>
              </div>

              <div
                v-if="s.submission.content"
                class="s-content rich-body-view"
                v-html="s.submission.content"
              />
              <p v-else class="s-content">（无文字说明）</p>

              <div class="s-foot">
                <button
                  v-if="s.submission.attachUrl"
                  type="button"
                  class="s-file"
                  @click="previewFile(s.submission.attachUrl)"
                >
                  <el-icon><Paperclip /></el-icon> 预览附件
                </button>
                <span v-else class="ol-muted">未上传附件</span>

                <div v-if="s.submission.comment" class="s-comment">
                  评语：{{ s.submission.comment }}
                </div>

                <el-button size="small" type="primary" plain @click="openGrade(s)">
                  {{ s.submission.status === 1 ? '重新评分' : '批阅' }}
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="!submissions.length" class="ol-empty" style="padding: 40px 0">
            还没有学生提交
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 评分 -->
    <el-dialog append-to-body v-model="gradeDialog" title="批阅作业" width="480px">
      <div v-if="grading" class="grade-brief">
        <b>{{ grading.realName }}</b>
        <p>{{ grading.submission.content || '（无文字说明）' }}</p>
      </div>
      <el-form label-position="top" style="margin-top: 14px">
        <el-form-item label="分数（0-100）">
          <el-input-number v-model="gradeForm.score" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input
            v-model="gradeForm.comment"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="指出做得好的地方与需要改进的点"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gradeDialog = false">取消</el-button>
        <el-button type="primary" @click="doGrade">提交评分</el-button>
      </template>
    </el-dialog>

    <FilePreview v-model="pv.visible" :url="pv.url" :name="pv.name" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { avatarColor } from '@/utils/cover'
import { triggerDownload } from '@/utils/file'
import FileUploader from '@/components/FileUploader.vue'
import FilePreview from '@/components/FilePreview.vue'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

/** 导出作业为 docx（含提交名单与作答摘录） */
function exportAssignment(assignment: any) {
  triggerDownload(`/api/courses/${courseId}/assignments/${assignment.id}/export`)
}

const courseTitle = ref('课程')
const list = ref<any[]>([])
const dialog = ref(false)
const drawer = ref(false)
const gradeDialog = ref(false)

const form = ref<any>({
  id: null,
  title: '',
  requirement: '',
  chapterId: null,
  startTime: null,
  deadline: null
})
const chapters = ref<any[]>([])
const submissions = ref<any[]>([])
const currentAssignment = ref<any>({})
const grading = ref<any>(null)
const gradeForm = ref<any>({ score: 80, comment: '' })

/** 附件预览（所有上传的文件都能直接看） */
const pv = ref<any>({ visible: false, url: '', name: '' })
function previewFile(url: string, name?: string) {
  pv.value = { visible: true, url, name: name || '' }
}

const pendingCount = computed(
  () => submissions.value.filter((s) => s.submission.status !== 1).length
)

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '未设置'
}

function deadlineText(t: string) {
  if (!t) return '长期有效'
  const d = new Date(t.replace('T', ' ').replace(/-/g, '/'))
  const diff = d.getTime() - Date.now()
  if (diff < 0) return '已截止'
  const days = Math.floor(diff / 86400000)
  if (days === 0) return '今天截止'
  return `还剩 ${days} 天`
}

function deadlineClass(t: string) {
  if (!t) return ''
  const d = new Date(t.replace('T', ' ').replace(/-/g, '/'))
  const diff = d.getTime() - Date.now()
  if (diff < 0) return 'chip--brand'
  if (diff < 2 * 86400000) return 'chip--amber'
  return 'chip--green'
}

async function load() {
  const courses: any = await request.get('/api/teacher/courses')
  const target = (courses || []).find((c: any) => c.id === courseId)
  courseTitle.value = target?.title || '课程'
  list.value = ((await request.get(`/api/teacher/courses/${courseId}/assignments`)) as any) || []
  chapters.value = ((await request.get(`/api/teacher/courses/${courseId}/chapters`)) as any) || []
}

function openEdit(row: any) {
  form.value = row
    ? {
        id: row.id,
        title: row.title,
        requirement: row.requirement || '',
        chapterId: row.chapterId || null,
        startTime: row.startTime || null,
        deadline: row.deadline,
        attachUrl: row.attachUrl || ''
      }
    : {
        id: null,
        title: '',
        requirement: '',
        chapterId: null,
        startTime: null,
        deadline: null,
        attachUrl: ''
      }
  dialog.value = true
}

async function save() {
  if (!form.value.title.trim()) {
    ElMessage.warning('作业标题不能为空')
    return
  }
  if (
    form.value.startTime &&
    form.value.deadline &&
    new Date(form.value.startTime) >= new Date(form.value.deadline)
  ) {
    ElMessage.warning('开始时间要早于截止时间')
    return
  }
  await request.post('/api/teacher/assignments', { ...form.value, courseId })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`删除作业《${row.title}》？学生的提交记录会一起删除。`, '提示', {
    type: 'warning'
  })
  await request.delete(`/api/teacher/assignments/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function openSubmissions(assignment: any) {
  currentAssignment.value = assignment
  submissions.value = ((await request.get(
    `/api/teacher/assignments/${assignment.id}/submissions`
  )) as any) || []
  drawer.value = true
}

function openGrade(s: any) {
  grading.value = s
  gradeForm.value = {
    score: s.submission.score ?? 80,
    comment: s.submission.comment || ''
  }
  gradeDialog.value = true
}

async function doGrade() {
  await request.post(`/api/teacher/submissions/${grading.value.submission.id}/grade`, gradeForm.value)
  ElMessage.success('已提交评分')
  gradeDialog.value = false
  await openSubmissions(currentAssignment.value)
}

onMounted(load)
</script>

<style scoped>
.hw-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.hw {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 17px 20px;
  transition: border-color 0.2s;
}
.hw:hover {
  border-color: var(--brand-200);
}
.hw-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.hw-ico {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 11px;
  background: var(--amber-soft);
  color: var(--amber);
  display: grid;
  place-items: center;
  font-size: 17px;
}
.hw-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}
.hw-title b {
  font-size: 14.5px;
  font-weight: 650;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hw-ops {
  display: flex;
  gap: 7px;
  align-items: center;
  flex: none;
}
.hw-req {
  margin: 0 0 10px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--ink-500);
  white-space: pre-wrap;
}
.hw-meta {
  display: flex;
  align-items: center;
  gap: 18px;
  font-size: 11.5px;
  color: var(--ink-400);
}
.hw-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}
.hw-meta a {
  color: var(--brand);
}

/* ---------- 提交抽屉 ---------- */
.sub-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.sub-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--ink-100);
  margin-bottom: 14px;
}
.sub-head h2 {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 720;
}
.sub-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}
.sub-item {
  display: flex;
  gap: 12px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 13px 15px;
}
.s-avatar {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}
.s-body {
  flex: 1;
  min-width: 0;
}
.s-top {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 7px;
  flex-wrap: wrap;
}
.s-top b {
  font-size: 13.5px;
  font-weight: 650;
}
.s-content {
  margin: 0 0 9px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--ink-700);
  white-space: pre-wrap;
}
.s-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.s-file {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--brand);
  font-weight: 550;
  border: 0;
  background: transparent;
  padding: 0;
  font-family: var(--font);
  cursor: pointer;
}
.s-file:hover {
  text-decoration: underline;
}
/* 富文本作答回显 */
.rich-body-view {
  white-space: normal;
  max-height: 260px;
  overflow: auto;
}
.rich-body-view :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 6px 0;
  border: 1px solid var(--ink-100);
}
.rich-body-view :deep(h2),
.rich-body-view :deep(h3) {
  font-size: 14px;
  margin: 6px 0 4px;
}
.rich-body-view :deep(blockquote) {
  margin: 6px 0;
  padding: 4px 10px;
  border-left: 3px solid var(--brand-200);
  background: var(--brand-50);
  border-radius: 0 6px 6px 0;
}
.rich-body-view :deep(pre) {
  background: var(--surface-sunken);
  padding: 8px 10px;
  border-radius: 8px;
  font-family: 'Consolas', monospace;
  font-size: 12px;
  white-space: pre-wrap;
}
.rich-body-view :deep(ul),
.rich-body-view :deep(ol) {
  padding-left: 20px;
  margin: 4px 0;
}

/* 时间区间 */
.time-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.time-tip {
  margin: -8px 0 14px;
  font-size: 11.5px;
  color: var(--ink-300);
}
.time-tip b {
  color: var(--brand);
}
.tip {
  font-size: 11.5px;
  color: var(--ink-300);
  line-height: 1.6;
  display: block;
  margin-top: 3px;
}
.att-link {
  cursor: pointer;
  color: var(--brand);
}
.att-link:hover {
  text-decoration: underline;
}
.s-comment {
  flex: 1;
  font-size: 11.5px;
  color: var(--ink-400);
  background: var(--surface-sunken);
  padding: 5px 10px;
  border-radius: 8px;
}
.grade-brief {
  background: var(--surface-sunken);
  border-radius: var(--r-md);
  padding: 13px 15px;
}
.grade-brief b {
  font-size: 13.5px;
  display: block;
  margin-bottom: 5px;
}
.grade-brief p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-500);
  white-space: pre-wrap;
}

.attach-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.attach-row :deep(.el-input) {
  flex: 1;
}
</style>
