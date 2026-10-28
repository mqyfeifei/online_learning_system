<template>
  <div v-loading="loading" class="detail fade-up">
    <!-- 面包屑 -->
    <div class="crumb">
      <router-link to="/courses">课程广场</router-link>
      <el-icon><ArrowRight /></el-icon>
      <span>{{ course.categoryName || '课程' }}</span>
      <el-icon><ArrowRight /></el-icon>
      <b>{{ course.title }}</b>
    </div>

    <div class="layout">
      <!-- ==================== 主区 ==================== -->
      <div class="main">
        <section class="banner" :class="{ 'has-cover': !!course.cover }" :style="coverStyle(course)">
          <div class="banner-inner">
            <div class="tags">
              <span v-if="course.categoryName" class="tag">{{ course.categoryName }}</span>
              <span class="tag tag--ghost">{{ course.chapterCount || 0 }} 章节</span>
            </div>
            <h1>{{ course.title }}</h1>
            <p>{{ course.description }}</p>
            <div class="banner-meta">
              <span><el-icon><User /></el-icon>{{ course.teacherName }}</span>
              <span><el-icon><Avatar /></el-icon>{{ course.studentCount || 0 }} 人在学</span>
              <span><el-icon><View /></el-icon>{{ course.viewCount || 0 }} 次浏览</span>
            </div>
          </div>
        </section>

        <!-- Tab -->
        <section class="ol-card tabs-card">
          <div class="tabs">
            <button
              v-for="t in tabs"
              :key="t.key"
              class="tab"
              :class="{ active: tab === t.key }"
              @click="tab = t.key"
            >
              {{ t.label }}<i v-if="t.count !== undefined">{{ t.count }}</i>
            </button>
          </div>

          <!-- 介绍 -->
          <div v-show="tab === 'intro'" class="pane">
            <h3 class="pane-title">课程简介</h3>
            <p class="paragraph">{{ course.description || '暂无课程简介' }}</p>

            <h3 class="pane-title">学完你将获得</h3>
            <ul class="gain">
              <li v-for="(g, i) in gains" :key="i">
                <el-icon><CircleCheckFilled /></el-icon>{{ g }}
              </li>
            </ul>

            <h3 class="pane-title">适用人群</h3>
            <p class="paragraph">
              计算机相关专业本科在读学生；准备考研或找工作需要系统复习的同学；已经会写代码但想把底层原理补齐的开发者。
            </p>
          </div>

          <!-- 目录 -->
          <div v-show="tab === 'chapters'" class="pane">
            <div v-if="chapters.length" class="chapter-list">
              <div
                v-for="(ch, i) in chapters"
                :key="ch.id"
                class="chapter-row"
                :class="{ done: finishedIds.includes(ch.id) }"
                @click="openChapter(ch)"
              >
                <span class="ch-idx">
                  <el-icon v-if="finishedIds.includes(ch.id)"><Check /></el-icon>
                  <template v-else>{{ i + 1 }}</template>
                </span>
                <div class="ch-body">
                  <b>{{ ch.title }}</b>
                  <p>{{ ch.content || '本节暂无文字说明' }}</p>
                </div>
                <span v-if="ch.hasResource" class="ch-badge">
                  <el-icon><Paperclip /></el-icon> 课件
                </span>
                <el-icon class="ch-arrow"><ArrowRight /></el-icon>
              </div>
            </div>
            <div v-else class="ol-empty">该课程还没有章节</div>
          </div>

          <!-- 课件 -->
          <div v-show="tab === 'files'" class="pane">
            <div v-if="resources.length" class="file-list">
              <div
                v-for="f in resources"
                :key="f.id"
                class="file-row"
                @click="openPreview(f)"
              >
                <span class="file-ico" :class="`type-${(f.fileType || '').slice(0, 4)}`">
                  {{ (f.fileType || 'FILE').toUpperCase().slice(0, 4) }}
                </span>
                <div class="file-body">
                  <b>{{ f.name }}</b>
                  <p>{{ sizeText(f.fileSize) }} · {{ chapterName(f.chapterId) }}</p>
                </div>
                <span class="pv-tip">在线查看</span>
                <el-icon
                  v-if="downloadable"
                  class="dl"
                  title="下载"
                  @click.stop="downloadFile(f)"
                >
                  <Download />
                </el-icon>
              </div>
            </div>
            <div v-else class="ol-empty">该课程还没有上传课件</div>
          </div>

          <!-- 作业测试 -->
          <div v-show="tab === 'works'" class="pane">
            <div class="wk-head">
              <div class="wk-stat">
                <b>{{ worksStats.done }}</b>
                <span>/ {{ worksStats.total }} 已完成</span>
              </div>
              <div class="wk-legend">
                <span class="chip cs-ongoing">进行中</span>
                <span class="chip cs-done">已完成</span>
                <span class="chip cs-wait">未开始</span>
                <span class="chip cs-expired">已截止</span>
              </div>
            </div>

            <template v-if="worksStats.total">
              <h3 class="pane-title">课程作业<i>{{ worksStats.assignmentCount }}</i></h3>
              <div v-if="works.assignments.length" class="wk-list">
                <div v-for="item in works.assignments" :key="item.assignment.id" class="wk-row">
                  <span class="wk-ico wk-ico--hw"><el-icon><EditPen /></el-icon></span>
                  <div class="wk-body">
                    <b>{{ item.assignment.title }}</b>
                    <p>
                      <template v-if="item.assignment.chapterTitle">
                        {{ item.assignment.chapterTitle }} ·
                      </template>
                      截止 {{ fmtTime(item.assignment.deadline) || '不限' }}
                      <template v-if="item.assignment.startTime">
                        · 开始 {{ fmtTime(item.assignment.startTime) }}
                      </template>
                      <template v-if="item.submission?.score != null">
                        · 得分 {{ item.submission.score }}
                      </template>
                    </p>
                  </div>
                  <span class="chip" :class="stateClass(item.state)">{{ stateText(item.state) }}</span>
                  <div class="wk-ops">
                    <button
                      v-if="item.state !== 'NOT_START'"
                      class="wk-btn"
                      @click="goWork(item)"
                    >
                      {{ isDone(item.state) ? '查看详情' : '去作答' }}
                    </button>
                    <button
                      v-if="canExport && item.state !== 'NOT_START'"
                      class="wk-btn wk-btn--ghost"
                      title="导出为 docx"
                      @click.stop="exportWork(item)"
                    >
                      导出
                    </button>
                    <button
                      v-if="isDone(item.state)"
                      class="wk-btn wk-btn--ghost"
                      title="给这次作业写笔记"
                      @click.stop="openWorkNote(item)"
                    >
                      笔记
                    </button>
                  </div>
                </div>
              </div>
              <div v-else class="ol-empty">暂无作业</div>

              <h3 class="pane-title">课程测验<i>{{ worksStats.examCount }}</i></h3>
              <div v-if="works.exams.length" class="wk-list">
                <div v-for="item in works.exams" :key="item.paper.id" class="wk-row">
                  <span class="wk-ico wk-ico--ex"><el-icon><Document /></el-icon></span>
                  <div class="wk-body">
                    <b>{{ item.paper.title }}</b>
                    <p>
                      {{ examWindowText(item.paper) }}
                      <template v-if="item.paper.chapterTitle">
                        · {{ item.paper.chapterTitle }}
                      </template>
                      <template v-if="isDone(item.state) && item.record?.score != null">
                        · 得分 {{ item.record.score }}
                      </template>
                      <template v-else-if="item.historyRecord">
                        · 试卷已更新，需重新作答
                      </template>
                    </p>
                  </div>
                  <span class="chip" :class="stateClass(item.state)">{{ stateText(item.state) }}</span>
                  <div class="wk-ops">
                    <button
                      v-if="item.state !== 'NOT_START'"
                      class="wk-btn"
                      @click="goExam(item)"
                    >
                      {{ isDone(item.state) ? '查看成绩' : '去测验' }}
                    </button>
                    <button
                      v-if="canExport && item.state !== 'NOT_START'"
                      class="wk-btn wk-btn--ghost"
                      title="导出为 docx"
                      @click.stop="exportExam(item)"
                    >
                      导出
                    </button>
                    <button
                      v-if="isDone(item.state)"
                      class="wk-btn wk-btn--ghost"
                      title="给这次测验写笔记"
                      @click.stop="openExamNote(item)"
                    >
                      笔记
                    </button>
                  </div>
                </div>
              </div>
              <div v-else class="ol-empty">暂无测验</div>
            </template>
            <div v-else class="ol-empty">本课程还没有布置作业或测验</div>
          </div>
        </section>
      </div>

      <!-- ==================== 侧栏 ==================== -->
      <aside class="side">
        <section class="ol-card action-card">
          <template v-if="course.enrolled">
            <div class="ring-box">
              <svg viewBox="0 0 110 110">
                <circle cx="55" cy="55" r="44" class="ring-track" />
                <circle
                  cx="55"
                  cy="55"
                  r="44"
                  class="ring-bar"
                  :stroke-dasharray="ringLen"
                  :stroke-dashoffset="ringOffset"
                />
              </svg>
              <div class="ring-num">
                <b>{{ course.progress || 0 }}%</b>
                <span>已学完</span>
              </div>
            </div>
            <p class="hint">
              已学完 {{ finishedIds.length }} / {{ chapters.length }} 章
            </p>
            <el-button type="primary" size="large" class="full" @click="continueLearn">
              继续学习
            </el-button>
          </template>
          <template v-else>
            <div class="price-box">
              <b>免费</b>
              <span>{{ course.enrollAudit === 1 ? '选课后需教师审核通过' : '校内课程，选课即可学习' }}</span>
            </div>
            <el-button
              v-if="course.enrollStatus === 2"
              size="large"
              class="full"
              disabled
            >
              选课申请已提交，等待教师审核
            </el-button>
            <el-button v-else type="primary" size="large" class="full" @click="enroll">
              立即选课
            </el-button>
            <p class="hint">选课后可学习章节、下载课件并参加测验</p>
          </template>

          <button class="fav-btn" :class="{ on: favored }" @click="toggleFav">
            <el-icon><component :is="favored ? 'StarFilled' : 'Star'" /></el-icon>
            {{ favored ? '已收藏' : '收藏课程' }}
          </button>

          <div class="info-rows">
            <div class="info-row">
              <span>课程章节</span>
              <b>{{ chapters.length }} 章</b>
            </div>
            <div class="info-row">
              <span>课件资料</span>
              <b>{{ resources.length }} 份</b>
            </div>
            <div class="info-row">
              <span>授课教师</span>
              <b>{{ course.teacherName }}</b>
            </div>
            <div class="info-row">
              <span>课程分类</span>
              <b>{{ course.categoryName || '未分类' }}</b>
            </div>
          </div>
        </section>

        <section class="ol-card teacher-card">
          <div class="tc-head">
            <el-avatar :size="46" class="tc-avatar">{{ (course.teacherName || '师').slice(0, 1) }}</el-avatar>
            <div>
              <b>{{ course.teacherName }}</b>
              <span>课程主讲</span>
            </div>
          </div>
          <p class="tc-desc">
            负责本课程的教学设计与答疑，课件与作业要求均可在课程内查看。有问题欢迎在讨论区发帖。
          </p>
          <el-button text class="full" @click="goDiscuss">
            去讨论区提问 →
          </el-button>
        </section>
      </aside>
    </div>

    <!-- 课件在线预览 -->
    <FilePreview v-model="previewVisible" :rid="previewRid" />

    <!-- 作业：查看详情 / 提交作答，都开在课程详情里，不跳页 -->
    <el-dialog
      append-to-body
      v-model="work.visible"
      :title="work.done ? '作业详情' : '提交作业'"
      width="620px"
      @closed="onWorkClosed"
    >
      <div v-if="work.item" class="hw-brief">
        <div class="hw-brief-top">
          <b>{{ work.item.assignment.title }}</b>
          <span class="chip" :class="stateClass(work.item.state)">
            {{ stateText(work.item.state) }}
          </span>
        </div>
        <p>{{ work.item.assignment.requirement }}</p>
        <span class="ol-muted">
          开放 {{ fmtTime(work.item.assignment.startTime) || '不限' }} · 截止
          {{ fmtTime(work.item.assignment.deadline) || '不限' }}
        </span>
        <div v-if="work.item.assignment.attachUrl" class="hw-att">
          <el-icon><Paperclip /></el-icon>
          <button type="button" @click="previewAttach(work.item.assignment.attachUrl)">
            查看作业附件
          </button>
        </div>
      </div>

      <!-- 已提交：只读 -->
      <div v-if="work.done" class="done-box">
        <div class="done-line">
          <span class="chip cs-done">已完成</span>
          <span class="ol-muted">
            提交于 {{ fmtTime(work.item.submission?.submitTime) }}
          </span>
          <span v-if="work.item.submission?.status === 1" class="chip cs-ongoing">
            得分 {{ work.item.submission.score }} 分
          </span>
        </div>
        <RichEditor
          v-if="hasRich(work.item.submission?.content)"
          :model-value="work.item.submission?.content || ''"
          readonly
          :min-height="60"
        />
        <p v-else class="empty-ans">（没有填写文字作答，仅提交了附件）</p>
        <div v-if="work.item.submission?.attachUrl" class="hw-att">
          <el-icon><Paperclip /></el-icon>
          <button type="button" @click="previewAttach(work.item.submission.attachUrl)">
            查看我提交的附件
          </button>
        </div>
        <p v-if="work.item.submission?.comment" class="teacher-comment">
          教师评语：{{ work.item.submission.comment }}
        </p>
        <p class="lock-tip">
          <el-icon><Lock /></el-icon>
          作业已提交，不能再次提交或修改；需要补充可以写一条作业笔记。
        </p>
      </div>

      <!-- 未提交：作答 -->
      <template v-else>
        <p v-if="work.item?.state === 'EXPIRED'" class="lock-tip lock-tip--warn">
          <el-icon><Clock /></el-icon>
          已过截止时间，无法再提交；仍可以写一条作业笔记做小结。
        </p>
        <div class="form-label">我的作答</div>
        <RichEditor
          v-model="workForm.content"
          :min-height="160"
          placeholder="写下思路、结论或说明；支持加粗、列表，截图可直接粘贴"
        />
        <div class="form-label" style="margin-top: 14px">附件（可选）</div>
        <div class="up-row">
          <FileUploader label="选择文件上传" :max-size="50" @uploaded="onWorkAttach" />
          <span v-if="workForm.attachUrl" class="att-chip">
            <el-icon><Paperclip /></el-icon>
            <button type="button" @click="previewAttach(workForm.attachUrl)">预览</button>
            <button type="button" class="rm" @click="workForm.attachUrl = ''">移除</button>
          </span>
        </div>
      </template>

      <template #footer>
        <el-button @click="openWorkNote(work.item)">
          <el-icon><Notebook /></el-icon> 作业笔记
        </el-button>
        <el-button @click="work.visible = false">关闭</el-button>
        <el-button
          v-if="!work.done"
          type="primary"
          :disabled="work.item?.state === 'EXPIRED'"
          :loading="workSaving"
          @click="submitWork"
        >
          提交作业
        </el-button>
      </template>
    </el-dialog>

    <!-- 作业 / 测验笔记 -->
    <el-dialog append-to-body v-model="noteDialog.visible" :title="noteDialog.title" width="560px">
      <RichEditor
        v-model="noteDialog.content"
        :min-height="160"
        placeholder="记录思路、易错点；截图可直接粘贴"
      />
      <template #footer>
        <el-button @click="noteDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="noteDialog.saving" @click="saveNote">保存笔记</el-button>
      </template>
    </el-dialog>

    <!-- 测验：原地全屏答题，关闭后仍停在课程详情 -->
    <el-dialog
      v-model="exam.visible"
      fullscreen
      append-to-body
      :with-header="false"
      class="exam-dialog"
      @closed="onExamClosed"
    >
      <ExamView
        v-if="exam.paperId"
        :paper-id="exam.paperId"
        embedded
        @close="exam.visible = false"
      />
    </el-dialog>

    <!-- 附件预览 -->
    <FilePreview v-model="pv.visible" :url="pv.url" :name="pv.name" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { coverOf, coverStyle } from '@/utils/cover'
import FilePreview from '@/components/FilePreview.vue'
import FileUploader from '@/components/FileUploader.vue'
import RichEditor from '@/components/RichEditor.vue'
import ExamView from '@/views/Exam.vue'
import { openFile, triggerDownload } from '@/utils/file'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const course = ref<any>({})
const chapters = ref<any[]>([])
const resources = ref<any[]>([])
const finishedIds = ref<number[]>([])
const favored = ref(false)
const tab = ref<'intro' | 'chapters' | 'files' | 'works'>('intro')
const works = ref<{ assignments: any[]; exams: any[]; stats: any }>({
  assignments: [],
  exams: [],
  stats: { total: 0, done: 0, assignmentCount: 0, examCount: 0 }
})
const worksStats = computed(() => works.value.stats || { total: 0, done: 0 })

/** 课程是否允许下载课件（教师可在课程设置里关闭） */
const downloadable = computed(() => course.value.resourceDownloadable !== 0)
const previewVisible = ref(false)
const previewRid = ref<number | null>(null)

function openPreview(f: any) {
  previewRid.value = f.id
  previewVisible.value = true
}

function downloadFile(f: any) {
  if (!downloadable.value) {
    ElMessage.warning('本课程资料仅支持在线查看')
    return
  }
  openFile(`/api/public/files/download?rid=${f.id}`)
}

const tabs = computed(() => [
  { key: 'intro' as const, label: '课程介绍' },
  { key: 'chapters' as const, label: '课程目录', count: chapters.value.length },
  { key: 'files' as const, label: '课件资料', count: resources.value.length },
  { key: 'works' as const, label: '作业测试', count: worksStats.value.total }
])

/** 教师设置的导出范围：NONE 不开放 / ENROLLED 仅选课学生 / ALL 平台用户 */
const canExport = computed(() => {
  const scope = course.value.exportScope || 'ENROLLED'
  if (scope === 'NONE') return false
  if (scope === 'ALL') return true
  return !!course.value.enrolled
})

/** 作业/测验状态：未开始 / 进行中 / 已完成 / 已批阅 / 已截止 */
const STATE_TEXT: Record<string, string> = {
  NOT_START: '未开始',
  ONGOING: '进行中',
  SUBMITTED: '已完成',
  GRADED: '已批阅',
  EXPIRED: '已截止'
}
const STATE_CLASS: Record<string, string> = {
  NOT_START: 'cs-wait',
  ONGOING: 'cs-ongoing',
  SUBMITTED: 'cs-done',
  GRADED: 'cs-done',
  EXPIRED: 'cs-expired'
}
function stateText(s: string) {
  return STATE_TEXT[s] || '进行中'
}
function stateClass(s: string) {
  return STATE_CLASS[s] || 'cs-ongoing'
}
function isDone(s: string) {
  return s === 'SUBMITTED' || s === 'GRADED'
}

function fmtTime(v: string) {
  if (!v) return ''
  return String(v).replace('T', ' ').slice(0, 16)
}

function examWindowText(p: any) {
  const s = fmtTime(p.startTime)
  const e = fmtTime(p.endTime)
  if (!s && !e) return '时间不限'
  return `${s || '不限'} ~ ${e || '不限'}`
}

// ---------------- 作业：原地弹窗，不再跳到学习页 ----------------
const work = ref<any>({ visible: false, item: null, done: false })
const workForm = ref<any>({ content: '', attachUrl: '' })
const workSaving = ref(false)

function goWork(item: any) {
  if (!userStore.isLogin) return needLogin()
  if (!course.value.enrolled) {
    ElMessage.info('选课后才能查看作业')
    return
  }
  const done = isDone(item.state)
  work.value = { visible: true, item, done }
  workForm.value = { content: '', attachUrl: '' }
  if (!done) {
    // 草稿：关掉弹窗也不丢，重新点开还能接着写
    const draft = localStorage.getItem(`ol-draft:assignment:${item.assignment.id}`)
    if (draft) {
      try {
        const d = JSON.parse(draft)
        workForm.value = { content: d.content || '', attachUrl: d.attachUrl || '' }
      } catch {
        /* 草稿坏了就当没有 */
      }
    }
  }
}

function onWorkClosed() {
  // 未提交就关掉：把内容存成草稿，下次点开还在
  if (!work.value.done && (workForm.value.content || workForm.value.attachUrl)) {
    localStorage.setItem(
      `ol-draft:assignment:${work.value.item?.assignment?.id}`,
      JSON.stringify({ ...workForm.value, savedAt: new Date().toISOString() })
    )
  }
  work.value = { visible: false, item: null, done: false }
  loadWorks()
}

function onWorkAttach(data: { url: string }) {
  workForm.value.attachUrl = data.url
}

async function submitWork() {
  const item = work.value.item
  if (!item) return
  if (!workForm.value.content && !workForm.value.attachUrl) {
    ElMessage.warning('请填写作答内容或上传附件')
    return
  }
  workSaving.value = true
  try {
    await request.post(`/api/assignments/${item.assignment.id}/submit`, {
      content: workForm.value.content,
      attachUrl: workForm.value.attachUrl
    })
    ElMessage.success('作业已提交')
    localStorage.removeItem(`ol-draft:assignment:${item.assignment.id}`)
    work.value.visible = false
    work.value.done = true
    await loadWorks()
    const fresh = works.value.assignments.find(
      (a: any) => a.assignment.id === item.assignment.id
    )
    if (fresh) work.value.item = fresh
  } finally {
    workSaving.value = false
  }
}

// ---------------- 测验：原地全屏弹窗 ----------------
const exam = ref<any>({ visible: false, paperId: null as number | null })

function goExam(item: any) {
  if (!userStore.isLogin) return needLogin()
  if (!course.value.enrolled) {
    ElMessage.info('选课后才能参加测验')
    return
  }
  exam.value = { visible: true, paperId: item.paper.id }
}

/** 交完卷关闭弹窗后刷新一次，页面上就能看到成绩与「已完成」 */
function onExamClosed() {
  exam.value = { visible: false, paperId: null }
  loadWorks()
}

// ---------------- 作业 / 测验笔记 ----------------
const noteDialog = ref<any>({
  visible: false,
  title: '',
  content: '',
  refType: '',
  refId: null as number | null,
  saving: false
})

function openWorkNote(item: any) {
  if (!userStore.isLogin) return needLogin()
  if (!item?.assignment) return
  noteDialog.value = {
    visible: true,
    title: `作业笔记 · ${item.assignment.title}`,
    content: '',
    refType: 'ASSIGNMENT',
    refId: item.assignment.id,
    saving: false
  }
  loadWorkNote()
}

function openExamNote(item: any) {
  if (!userStore.isLogin) return needLogin()
  if (!item?.paper) return
  noteDialog.value = {
    visible: true,
    title: `测验笔记 · ${item.paper.title}`,
    content: '',
    refType: 'EXAM',
    refId: item.paper.id,
    saving: false
  }
  loadWorkNote()
}

/** 已经写过笔记就回填，避免重复建多条 */
async function loadWorkNote() {
  const nd = noteDialog.value
  if (!nd.refId) return
  const list: any = await request.get('/api/my/notes').catch(() => [])
  const hit = (list || []).find(
    (n: any) => n.refType === nd.refType && Number(n.refId) === Number(nd.refId)
  )
  if (hit) {
    nd.content = hit.content || ''
  }
}

async function saveNote() {
  const nd = noteDialog.value
  const plain = (nd.content || '')
    .replace(/<img[^>]*>/gi, ' [图片] ')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
  if (!plain && !(nd.content || '').includes('<img')) {
    ElMessage.warning('笔记内容不能为空')
    return
  }
  nd.saving = true
  try {
    await request.post('/api/my/notes', {
      courseId: course.value.id,
      refType: nd.refType,
      refId: nd.refId,
      content: nd.content
    })
    ElMessage.success('笔记已保存')
    nd.visible = false
  } finally {
    nd.saving = false
  }
}

// ---------------- 附件预览 ----------------
const pv = ref<any>({ visible: false, url: '', name: '' })
function previewAttach(url: string) {
  pv.value = { visible: true, url, name: '' }
}

/** 富文本里是否真的有内容（图片也算） */
function hasRich(html: any): boolean {
  if (!html) return false
  const text = String(html)
    .replace(/<img[^>]*>/gi, 'IMG')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
  return text.length > 0
}

function exportWork(item: any) {
  if (item.state === 'NOT_START') {
    ElMessage.warning('作业尚未开始，暂不能导出')
    return
  }
  triggerDownload(`/api/courses/${course.value.id}/assignments/${item.assignment.id}/export`)
}

function exportExam(item: any) {
  if (item.state === 'NOT_START') {
    ElMessage.warning('测验尚未开放，暂不能导出')
    return
  }
  triggerDownload(`/api/courses/${course.value.id}/papers/${item.paper.id}/export`)
}

async function loadWorks() {
  works.value = { assignments: [], exams: [], stats: { total: 0, done: 0, assignmentCount: 0, examCount: 0 } }
  try {
    const data: any = await request.get(`/api/public/courses/${route.params.id}/works`)
    works.value = {
      assignments: data?.assignments || [],
      exams: data?.exams || [],
      stats: data?.stats || { total: 0, done: 0, assignmentCount: 0, examCount: 0 }
    }
  } catch {
    /* 未登录或课程无作业时保持空 */
  }
}

const gains = computed(() => {
  const name = course.value.title || '本课程'
  return [
    `系统掌握《${name}》的核心概念与知识框架`,
    '能独立完成配套作业与章节测验，检验学习效果',
    '获得可离线查阅的完整课件，方便期末复习',
    '在讨论区与同学、教师一起拆解疑难问题'
  ]
})

const RING_R = 44
const ringLen = 2 * Math.PI * RING_R
const ringOffset = computed(() => {
  const rate = Math.min(100, Math.max(0, course.value.progress || 0))
  return ringLen * (1 - rate / 100)
})

function sizeText(bytes: number) {
  if (!bytes) return '未知大小'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function chapterName(chapterId: number) {
  const ch = chapters.value.find((c) => c.id === chapterId)
  return ch ? ch.title : '课程级资料'
}

async function load() {
  loading.value = true
  try {
    const data: any = await request.get(`/api/public/courses/${route.params.id}`)
    course.value = data.course || {}
    chapters.value = data.chapters || []
    finishedIds.value = data.finishedChapterIds || []

    const files: any = await request.get(`/api/public/courses/${route.params.id}/resources`)
    resources.value = files || []
    // 标记哪些章节挂了课件，目录里好显示
    const withFile = new Set(resources.value.map((f) => f.chapterId))
    chapters.value = chapters.value.map((c) => ({ ...c, hasResource: withFile.has(c.id) }))

    if (userStore.isLogin) {
      try {
        const st: any = await request.get(`/api/courses/${route.params.id}/favorite`)
        favored.value = !!st.favored
      } catch {
        favored.value = false
      }
    }

    await loadWorks()
  } finally {
    loading.value = false
  }
}

function needLogin() {
  ElMessage.warning('请先登录')
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function enroll() {
  if (!userStore.isLogin) return needLogin()
  const res: any = await request.post(`/api/courses/${course.value.id}/enroll`)
  if (res?.pending) {
    ElMessage.success('选课申请已提交，等待教师审核通过')
  } else {
    ElMessage.success('选课成功，开始学习吧')
  }
  load()
}

async function cancel() {
  await ElMessageBox.confirm('确认退课？学习进度会保留。', '退课确认', {
    type: 'warning',
    confirmButtonText: '确认退课',
    cancelButtonText: '再想想'
  })
  await request.delete(`/api/courses/${course.value.id}/enroll`)
  ElMessage.success('已退课')
  load()
}

async function toggleFav() {
  if (!userStore.isLogin) return needLogin()
  const res: any = await request.post(`/api/courses/${course.value.id}/favorite`)
  favored.value = !!res.favored
  ElMessage.success(favored.value ? '已加入收藏' : '已取消收藏')
}

function continueLearn() {
  if (!userStore.isLogin) return needLogin()
  router.push(`/learn/${course.value.id}`)
}

/** 带课程 id 进讨论区，那边会自动选中这门课的标签 */
function goDiscuss() {
  router.push({ path: '/discussions', query: { courseId: String(course.value.id) } })
}

function openChapter(ch: any) {
  if (!course.value.enrolled) {
    ElMessage.info('选课后才能进入章节学习')
    return
  }
  router.push({ path: `/learn/${course.value.id}`, query: { chapterId: ch.id } })
}

onMounted(async () => {
  // 从学习页「全部作业与测验」跳过来时直接定位到作业测试页签
  if (route.query.tab === 'works') tab.value = 'works'
  await load()
})
</script>

<style scoped>
.detail {
  max-width: 1360px;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--ink-400);
  margin-bottom: 16px;
}
.crumb a:hover {
  color: var(--brand);
}
.crumb b {
  color: var(--ink-700);
  font-weight: 600;
}
.crumb .el-icon {
  font-size: 10px;
  color: var(--ink-300);
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 332px;
  gap: 18px;
  align-items: start;
}

/* ---------- Banner ---------- */
.banner {
  border-radius: var(--r-xl);
  padding: 30px 32px;
  color: #fff;
  position: relative;
  overflow: hidden;
  min-height: 196px;
  display: flex;
  align-items: center;
  margin-bottom: 18px;
}
.banner::after {
  content: '';
  position: absolute;
  right: -40px;
  bottom: -70px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}
/* 有封面图时压一层暗色渐变，保证白字仍然可读 */
.banner.has-cover::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, rgba(28, 18, 10, 0.72) 0%, rgba(28, 18, 10, 0.34) 58%, rgba(28, 18, 10, 0.12) 100%);
}
.banner-inner {
  position: relative;
  z-index: 1;
  max-width: 620px;
}
.tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.tag {
  font-size: 11.5px;
  font-weight: 600;
  padding: 4px 11px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.94);
  color: var(--ink-700);
}
.tag--ghost {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.4);
}
.banner h1 {
  font-size: 30px;
  font-weight: 780;
  letter-spacing: -0.8px;
  margin: 0 0 10px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.14);
}
.banner p {
  margin: 0 0 18px;
  font-size: 13.5px;
  line-height: 1.7;
  opacity: 0.94;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.banner-meta {
  display: flex;
  gap: 20px;
  font-size: 12.5px;
  opacity: 0.95;
}
.banner-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ---------- Tabs ---------- */
.tabs-card {
  padding: 8px 22px 22px;
}
.tabs {
  display: flex;
  gap: 26px;
  border-bottom: 1px solid var(--ink-100);
  margin-bottom: 18px;
}
.tab {
  border: 0;
  background: transparent;
  padding: 14px 0 13px;
  font-size: 14px;
  font-weight: 600;
  font-family: var(--font);
  color: var(--ink-400);
  cursor: pointer;
  position: relative;
  transition: color 0.18s;
}
.tab:hover {
  color: var(--ink-700);
}
.tab.active {
  color: var(--ink-900);
}
.tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 3px;
  border-radius: var(--r-pill);
  background: var(--brand);
}
.tab i {
  font-style: normal;
  font-size: 11px;
  margin-left: 5px;
  padding: 1px 6px;
  border-radius: var(--r-pill);
  background: var(--surface-sunken);
  color: var(--ink-400);
}

.pane-title {
  font-size: 15px;
  font-weight: 680;
  margin: 22px 0 10px;
}
.pane-title:first-child {
  margin-top: 4px;
}
.pane-title i {
  font-style: normal;
  font-size: 11px;
  font-weight: 600;
  margin-left: 6px;
  padding: 1px 7px;
  border-radius: var(--r-pill);
  background: var(--surface-sunken);
  color: var(--ink-400);
}
.paragraph {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.85;
  color: var(--ink-500);
}

.gain {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 20px;
}
.gain li {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  font-size: 13px;
  color: var(--ink-700);
  line-height: 1.6;
}
.gain .el-icon {
  color: var(--green);
  font-size: 15px;
  margin-top: 2px;
  flex: none;
}

/* ---------- 章节 ---------- */
.chapter-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.chapter-row {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 13px 14px;
  border-radius: var(--r-md);
  cursor: pointer;
  transition: all 0.18s;
  border: 1px solid transparent;
}
.chapter-row:hover {
  background: var(--surface-soft);
  border-color: var(--brand-100);
}
.ch-idx {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: var(--surface-sunken);
  color: var(--ink-500);
  font-size: 12.5px;
  font-weight: 700;
}
.chapter-row.done .ch-idx {
  background: var(--green-soft);
  color: var(--green);
}
.ch-body {
  flex: 1;
  min-width: 0;
}
.ch-body b {
  font-size: 13.5px;
  font-weight: 620;
  display: block;
  margin-bottom: 2px;
}
.ch-body p {
  margin: 0;
  font-size: 12px;
  color: var(--ink-400);
  line-height: 1.55;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ch-badge {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--brand-700);
  background: var(--brand-50);
  padding: 3px 8px;
  border-radius: var(--r-pill);
  flex: none;
}
.ch-arrow {
  color: var(--ink-300);
  font-size: 12px;
  flex: none;
}

/* ---------- 课件 ---------- */
.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.file-row {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 12px 14px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  transition: all 0.18s;
  cursor: pointer;
}
.file-row:hover {
  border-color: var(--brand-200);
  background: var(--surface-soft);
}
.pv-tip {
  font-size: 11.5px;
  color: var(--brand);
  background: var(--brand-50);
  padding: 3px 9px;
  border-radius: var(--r-pill);
  flex: none;
}
.file-row:hover .dl {
  color: var(--brand);
}
.file-ico {
  width: 42px;
  height: 42px;
  flex: none;
  border-radius: 11px;
  display: grid;
  place-items: center;
  font-size: 10.5px;
  font-weight: 750;
  letter-spacing: 0.3px;
  background: var(--surface-sunken);
  color: var(--ink-500);
}
.type-pptx,
.type-ppt {
  background: #fff0e8;
  color: #d2691e;
}
.type-pdf {
  background: #ffeaea;
  color: #c9302c;
}
.type-docx,
.type-doc {
  background: var(--blue-soft);
  color: #2b6cb0;
}
.type-md {
  background: var(--violet-soft);
  color: #5a4bcf;
}
.file-body {
  flex: 1;
  min-width: 0;
}
.file-body b {
  font-size: 13.5px;
  font-weight: 620;
  display: block;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-body p {
  margin: 0;
  font-size: 11.5px;
  color: var(--ink-400);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dl {
  color: var(--ink-300);
  font-size: 16px;
  flex: none;
  transition: color 0.18s;
}

/* ---------- 作业测试 ---------- */
.wk-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 14px;
  background: var(--surface-soft);
  border: 1px solid var(--brand-100);
  border-radius: var(--r-md);
  margin-bottom: 14px;
}
.wk-stat {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 12.5px;
  color: var(--ink-500);
}
.wk-stat b {
  font-size: 20px;
  font-weight: 760;
  color: var(--brand);
  letter-spacing: -0.5px;
}
.wk-legend {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.chip {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: var(--r-pill);
  flex: none;
}
.cs-ongoing {
  background: var(--brand-50);
  color: var(--brand-700);
}
.cs-done {
  background: var(--green-soft);
  color: var(--green);
}
.cs-wait {
  background: var(--surface-sunken);
  color: var(--ink-400);
}
.cs-expired {
  background: var(--red-soft, #fdecec);
  color: var(--red, #c0392b);
}

.wk-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.wk-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  transition: all 0.18s;
}
.wk-row:hover {
  border-color: var(--brand-200);
  background: var(--surface-soft);
}
.wk-ico {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 11px;
  display: grid;
  place-items: center;
  font-size: 17px;
}
.wk-ico--hw {
  background: var(--brand-50);
  color: var(--brand);
}
.wk-ico--ex {
  background: var(--violet-soft, #eeecff);
  color: var(--violet, #5a4bcf);
}
.wk-body {
  flex: 1;
  min-width: 0;
}
.wk-body b {
  font-size: 13.5px;
  font-weight: 620;
  display: block;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wk-body p {
  margin: 0;
  font-size: 11.5px;
  color: var(--ink-400);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wk-ops {
  display: flex;
  gap: 6px;
  flex: none;
}
.wk-btn {
  height: 28px;
  padding: 0 12px;
  border-radius: var(--r-sm);
  border: 1px solid var(--brand);
  background: var(--brand);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.18s;
}
.wk-btn:hover {
  filter: brightness(1.05);
}
.wk-btn--ghost {
  background: var(--surface);
  border-color: var(--ink-100);
  color: var(--ink-500);
}
.wk-btn--ghost:hover {
  border-color: var(--brand-200);
  color: var(--brand);
}

/* ---------- 作业弹窗 ---------- */
.hw-brief {
  background: var(--surface-soft);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 12px 14px;
  margin-bottom: 14px;
}
.hw-brief-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.hw-brief-top b {
  font-size: 14px;
  font-weight: 650;
}
.hw-brief p {
  margin: 0 0 6px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--ink-700);
  white-space: pre-wrap;
}
.hw-brief .ol-muted {
  font-size: 11.5px;
}
.hw-att {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  font-size: 12px;
}
.hw-att button {
  border: 0;
  background: transparent;
  padding: 0;
  color: var(--brand);
  font-size: 12px;
  font-family: var(--font);
  cursor: pointer;
  text-decoration: underline;
}
.done-box .done-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 12px;
}
.done-box .ol-muted {
  font-size: 11.5px;
}
.empty-ans {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-400);
}
.teacher-comment {
  margin: 10px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-700);
  background: var(--brand-50);
  border-radius: var(--r-sm);
  padding: 8px 11px;
}
.lock-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 10px 0 0;
  font-size: 11.5px;
  color: var(--ink-400);
}
.lock-tip--warn {
  color: #b4453b;
}
.form-label {
  font-size: 12.5px;
  font-weight: 620;
  margin-bottom: 6px;
}
.up-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.att-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--ink-500);
}
.att-chip button {
  border: 0;
  background: transparent;
  padding: 0;
  font-size: 12px;
  font-family: var(--font);
  color: var(--brand);
  cursor: pointer;
}
.att-chip .rm {
  color: var(--ink-400);
}

/* ---------- 侧栏 ---------- */
.side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.action-card {
  padding: 20px;
}
.ring-box {
  position: relative;
  width: 118px;
  height: 118px;
  margin: 2px auto 10px;
}
.ring-box svg {
  width: 118px;
  height: 118px;
  transform: rotate(-90deg);
}
.ring-track {
  fill: none;
  stroke: #f3e7da;
  stroke-width: 9;
}
.ring-bar {
  fill: none;
  stroke: var(--brand);
  stroke-width: 9;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.8s ease;
}
.ring-num {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.ring-num b {
  font-size: 24px;
  font-weight: 760;
  letter-spacing: -0.8px;
}
.ring-num span {
  font-size: 11px;
  color: var(--ink-400);
}

.price-box {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 16px;
}
.price-box b {
  font-size: 26px;
  font-weight: 780;
  color: var(--brand);
  letter-spacing: -0.5px;
}
.price-box span {
  font-size: 12px;
  color: var(--ink-400);
}

.full {
  width: 100%;
}
.hint {
  margin: 10px 0 0;
  font-size: 11.5px;
  color: var(--ink-400);
  text-align: center;
  line-height: 1.6;
}

.fav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 40px;
  margin-top: 10px;
  border-radius: var(--r-sm);
  border: 1px solid var(--ink-100);
  background: var(--surface);
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 550;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.18s;
}
.fav-btn:hover {
  border-color: var(--amber);
  color: var(--amber);
}
.fav-btn.on {
  background: var(--amber-soft);
  border-color: #f3d9a8;
  color: #a86a08;
}

.info-rows {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--ink-100);
  display: flex;
  flex-direction: column;
  gap: 11px;
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
}
.info-row span {
  color: var(--ink-400);
}
.info-row b {
  font-weight: 620;
}

.teacher-card {
  padding: 20px;
}
.tc-head {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 10px;
}
.tc-avatar {
  background: linear-gradient(135deg, var(--brand-300), var(--brand));
  font-weight: 700;
}
.tc-head b {
  font-size: 14px;
  font-weight: 650;
  display: block;
}
.tc-head span {
  font-size: 11.5px;
  color: var(--ink-400);
}
.tc-desc {
  margin: 0 0 6px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-500);
}

@media (max-width: 1180px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .gain {
    grid-template-columns: 1fr;
  }
}

/* 全屏答题弹窗：去掉内边距，让 Exam 自己撑满 */
:deep(.exam-dialog .el-dialog__body) {
  padding: 0;
  height: 100vh;
  overflow: hidden;
}
</style>
