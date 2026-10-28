<template>
  <div v-loading="loading" class="learn">
    <!-- ==================== 左：章节目录 ==================== -->
    <aside class="nav">
      <div class="nav-head">
        <router-link :to="`/courses/${course.id}`" class="back">
          <el-icon><ArrowLeft /></el-icon> 返回课程
        </router-link>
        <h2>{{ course.title }}</h2>
        <div class="nav-progress">
          <div class="bar"><i :style="{ width: percent + '%' }" /></div>
          <span>{{ finishedIds.length }}/{{ chapters.length }}</span>
        </div>
      </div>

      <div class="nav-list">
        <div
          v-for="(ch, i) in chapters"
          :key="ch.id"
          class="nav-item"
          :class="{ active: current?.id === ch.id, done: finishedIds.includes(ch.id) }"
          @click="selectChapter(ch)"
        >
          <span class="ni-idx">
            <el-icon v-if="finishedIds.includes(ch.id)"><Check /></el-icon>
            <template v-else>{{ i + 1 }}</template>
          </span>
          <span class="ni-title">{{ ch.title }}</span>
          <el-icon v-if="hasFile(ch.id)" class="ni-file"><Paperclip /></el-icon>
        </div>
      </div>
    </aside>

    <!-- ==================== 中：内容 ==================== -->
    <main class="stage">
      <template v-if="current">
        <div class="stage-head">
          <div class="sh-meta">
            <span class="chip chip--brand">第 {{ currentIndex + 1 }} 节</span>
            <span v-if="finishedIds.includes(current.id)" class="chip chip--green">
              <el-icon><CircleCheckFilled /></el-icon> 已学完
            </span>
            <span class="ol-muted">共 {{ chapters.length }} 节</span>
          </div>
          <h1>{{ current.title }}</h1>
        </div>

        <div class="stage-body">
          <!-- 视频 -->
          <video
            v-if="current.videoUrl"
            :src="current.videoUrl"
            controls
            class="video"
            preload="metadata"
          />

          <!-- 图文正文 -->
          <article class="prose">
            <template v-if="paragraphs.length">
              <p v-for="(p, i) in paragraphs" :key="i">{{ p }}</p>
            </template>
            <div v-else class="ol-empty" style="padding: 28px 0">
              本节暂无图文内容，请查看下方课件
            </div>
          </article>

          <!-- 本节课件 -->
          <section v-if="chapterFiles.length" class="files">
            <h3 class="sec-title">
              <el-icon><FolderOpened /></el-icon> 本节课件
            </h3>
            <div class="file-grid">
              <div
                v-for="f in chapterFiles"
                :key="f.id"
                class="file-card"
                @click="openPreview(f)"
              >
                <span class="fc-ico" :class="`type-${(f.fileType || '').slice(0, 4)}`">
                  {{ (f.fileType || 'FILE').toUpperCase().slice(0, 4) }}
                </span>
                <div class="fc-body">
                  <b>{{ f.name }}</b>
                  <p>{{ sizeText(f.fileSize) }}</p>
                </div>
                <el-icon class="fc-view"><View /></el-icon>
                <el-icon
                  v-if="downloadable"
                  class="fc-dl"
                  title="下载"
                  @click.stop="downloadFile(f)"
                >
                  <Download />
                </el-icon>
              </div>
            </div>
          </section>

          <!-- 其他课程资料 -->
          <section v-if="courseFiles.length" class="files">
            <h3 class="sec-title">
              <el-icon><Files /></el-icon> 课程其他资料
            </h3>
            <div class="file-grid">
              <div
                v-for="f in courseFiles"
                :key="f.id"
                class="file-card"
                @click="openPreview(f)"
              >
                <span class="fc-ico" :class="`type-${(f.fileType || '').slice(0, 4)}`">
                  {{ (f.fileType || 'FILE').toUpperCase().slice(0, 4) }}
                </span>
                <div class="fc-body">
                  <b>{{ f.name }}</b>
                  <p>{{ sizeText(f.fileSize) }}</p>
                </div>
                <el-icon class="fc-view"><View /></el-icon>
                <el-icon
                  v-if="downloadable"
                  class="fc-dl"
                  title="下载"
                  @click.stop="downloadFile(f)"
                >
                  <Download />
                </el-icon>
              </div>
            </div>
          </section>
        </div>

        <!-- 底部操作条 -->
        <footer class="stage-foot">
          <button class="nav-btn" :disabled="currentIndex === 0" @click="goChapter(-1)">
            <el-icon><ArrowLeft /></el-icon> 上一节
          </button>

          <button
            v-if="!finishedIds.includes(current.id)"
            class="done-btn"
            @click="finish(current)"
          >
            <el-icon><Select /></el-icon> 标记本节已学完
          </button>
          <span v-else class="done-tag">
            <el-icon><CircleCheckFilled /></el-icon> 本节已完成
          </span>

          <button
            class="nav-btn"
            :disabled="currentIndex === chapters.length - 1"
            @click="goChapter(1)"
          >
            下一节 <el-icon><ArrowRight /></el-icon>
          </button>
        </footer>
      </template>
      <div v-else class="ol-empty" style="padding-top: 120px">该课程还没有章节</div>
    </main>

    <!-- ==================== 右：进度 / 笔记 / 任务 ==================== -->
    <aside class="panel">
      <section class="p-card">
        <div class="p-title">
          <h3>学习进度</h3>
          <b>{{ percent }}%</b>
        </div>
        <div class="bar bar--green"><i :style="{ width: percent + '%' }" /></div>
        <p class="p-hint">
          已完成 {{ finishedIds.length }} / {{ chapters.length }} 节
          <template v-if="percent === 100">，全部学完 🎉</template>
        </p>
      </section>

      <!-- 笔记 -->
      <section class="p-card">
        <div class="p-title">
          <h3>本节笔记</h3>
          <span class="ol-muted">{{ notes.length }} 条</span>
        </div>

        <RichEditor
          v-model="noteText"
          :min-height="110"
          placeholder="记下这一节的关键点，截图可以直接 Ctrl+V 粘贴进来；保存后可在「收藏与笔记」回看"
        />
        <div class="note-actions">
          <span class="ol-muted">支持富文本与图片</span>
          <button class="save-btn" :disabled="!plainNote" @click="saveNote">保存笔记</button>
        </div>

        <div v-if="notes.length" class="note-list">
          <div v-for="n in notes" :key="n.id" class="note-item">
            <div class="note-rich" v-html="n.content" />
            <div class="note-foot">
              <span class="ol-muted">{{ fmt(n.updateTime) }}</span>
              <button class="del" @click="removeNote(n)">删除</button>
            </div>
          </div>
        </div>
      </section>

      <!-- 测验：挂在本章的优先，其次是整门课程通用的 -->
      <section v-if="chapterPapers.length || coursePapers.length" class="p-card">
        <div class="p-title">
          <h3>本章测验</h3>
          <button class="mini-link" @click="router.push(`/courses/${courseId}?tab=works`)">
            全部作业与测验 →
          </button>
        </div>
        <template v-if="chapterPapers.length">
          <div
            v-for="p in chapterPapers"
            :key="p.id"
            class="task-row"
            :class="{ 'is-locked': paperNotOpen(p) }"
            @click="openPaper(p)"
          >
            <span class="task-ico task-ico--violet"><el-icon><EditPen /></el-icon></span>
            <div class="task-body">
              <b>{{ p.title }}</b>
              <p>
                {{ p.totalScore }} 分 · {{ p.duration }} 分钟
                <template v-if="paperNotOpen(p)"> · {{ fmt(p.startTime) }} 开放</template>
              </p>
            </div>
            <button
              v-if="canExport && !paperNotOpen(p)"
              class="mini-act mini-act--label"
              title="导出试卷（docx）"
              @click.stop="exportPaper(p)"
            >
              <el-icon><Download /></el-icon><em>导出</em>
            </button>
            <el-icon v-if="!paperNotOpen(p)" class="task-arrow"><ArrowRight /></el-icon>
          </div>
        </template>
        <p v-else class="ol-muted sm">本章没有随堂测验</p>
        <template v-if="coursePapers.length">
          <div class="sub-title">课程测验</div>
          <div
            v-for="p in coursePapers"
            :key="p.id"
            class="task-row"
            :class="{ 'is-locked': paperNotOpen(p) }"
            @click="openPaper(p)"
          >
            <span class="task-ico task-ico--violet"><el-icon><EditPen /></el-icon></span>
            <div class="task-body">
              <b>{{ p.title }}</b>
              <p>
                {{ p.totalScore }} 分 · {{ p.duration }} 分钟
                <template v-if="paperNotOpen(p)"> · {{ fmt(p.startTime) }} 开放</template>
              </p>
            </div>
            <button
              v-if="canExport && !paperNotOpen(p)"
              class="mini-act mini-act--label"
              title="导出试卷（docx）"
              @click.stop="exportPaper(p)"
            >
              <el-icon><Download /></el-icon><em>导出</em>
            </button>
            <el-icon v-if="!paperNotOpen(p)" class="task-arrow"><ArrowRight /></el-icon>
          </div>
        </template>
      </section>

      <!-- 作业 -->
      <section v-if="chapterAssignments.length || courseAssignments.length" class="p-card">
        <div class="p-title">
          <h3>本章作业</h3>
          <span class="ol-muted">已完成 {{ doneCount }}/{{ assignments.length }}</span>
        </div>
        <template v-if="chapterAssignments.length">
          <div
            v-for="item in chapterAssignments"
            :key="item.assignment.id"
            class="task-row task-row--stack"
            :class="{ 'is-done': isDone(item.state), 'is-locked': item.state === 'NOT_START' }"
            @click="openWork(item)"
          >
            <span class="task-ico task-ico--amber"><el-icon><Document /></el-icon></span>
            <div class="task-body">
              <b>{{ item.assignment.title }}</b>
              <p>
                <span class="chip" :class="stateClass(item.state)">{{ stateText(item.state) }}</span>
                <template v-if="item.state === 'SUBMITTED'">已提交，等待批阅</template>
                <template v-else-if="item.state === 'GRADED'">
                  得分 {{ item.submission?.score }} 分
                </template>
                <template v-else-if="item.state === 'NOT_START'">
                  {{ fmt(item.assignment.startTime) }} 开放
                </template>
                <template v-else-if="item.state === 'EXPIRED'">已截止</template>
                <template v-else>截止 {{ fmt(item.assignment.deadline) }}</template>
              </p>
            </div>
            <el-icon v-if="item.state !== 'NOT_START'" class="task-arrow"><ArrowRight /></el-icon>
            <div class="task-acts">
              <button
                v-if="canExport && item.state !== 'NOT_START'"
                class="mini-act mini-act--label"
                title="导出作业（docx）"
                @click.stop="exportAssignment(item)"
              >
                <el-icon><Download /></el-icon><em>导出</em>
              </button>
              <button
                class="mini-act mini-act--label"
                title="给这次作业添加笔记"
                @click.stop="openWorkNote(item)"
              >
                <el-icon><Notebook /></el-icon><em>笔记</em>
              </button>
            </div>
          </div>
        </template>
        <p v-else class="ol-muted sm">本章没有随堂作业</p>
        <template v-if="courseAssignments.length">
          <div class="sub-title">课程作业</div>
          <div
            v-for="item in courseAssignments"
            :key="item.assignment.id"
            class="task-row task-row--stack"
            :class="{ 'is-done': isDone(item.state), 'is-locked': item.state === 'NOT_START' }"
            @click="openWork(item)"
          >
            <span class="task-ico task-ico--amber"><el-icon><Document /></el-icon></span>
            <div class="task-body">
              <b>{{ item.assignment.title }}</b>
              <p>
                <span class="chip" :class="stateClass(item.state)">{{ stateText(item.state) }}</span>
                <template v-if="item.state === 'SUBMITTED'">已提交，等待批阅</template>
                <template v-else-if="item.state === 'GRADED'">
                  得分 {{ item.submission?.score }} 分
                </template>
                <template v-else-if="item.state === 'NOT_START'">
                  {{ fmt(item.assignment.startTime) }} 开放
                </template>
                <template v-else-if="item.state === 'EXPIRED'">已截止</template>
                <template v-else>截止 {{ fmt(item.assignment.deadline) }}</template>
              </p>
            </div>
            <el-icon v-if="item.state !== 'NOT_START'" class="task-arrow"><ArrowRight /></el-icon>
            <div class="task-acts">
              <button
                v-if="canExport && item.state !== 'NOT_START'"
                class="mini-act mini-act--label"
                title="导出作业（docx）"
                @click.stop="exportAssignment(item)"
              >
                <el-icon><Download /></el-icon><em>导出</em>
              </button>
              <button
                class="mini-act mini-act--label"
                title="给这次作业添加笔记"
                @click.stop="openWorkNote(item)"
              >
                <el-icon><Notebook /></el-icon><em>笔记</em>
              </button>
            </div>
          </div>
        </template>
      </section>
    </aside>

    <!-- 提交 / 查看作业 -->
    <el-dialog
      append-to-body
      v-model="dialog"
      :title="activeDone ? '作业详情' : '提交作业'"
      width="620px"
      @closed="onDialogClosed"
    >
      <div v-if="activeAssignment" class="hw-brief">
        <div class="hw-brief-top">
          <b>{{ activeAssignment.assignment.title }}</b>
          <span class="chip" :class="stateClass(activeAssignment.state)">
            {{ stateText(activeAssignment.state) }}
          </span>
        </div>
        <p>{{ activeAssignment.assignment.requirement }}</p>
        <span class="ol-muted">
          开放 {{ fmt(activeAssignment.assignment.startTime) }} · 截止
          {{ fmt(activeAssignment.assignment.deadline) }}
        </span>
        <div v-if="activeAssignment.assignment.attachUrl" class="hw-att">
          <el-icon><Paperclip /></el-icon>
          <button type="button" @click="previewFile(activeAssignment.assignment.attachUrl)">
            查看作业附件
          </button>
        </div>
      </div>

      <!-- 已提交：只读 -->
      <div v-if="activeDone" class="done-box">
        <div class="done-line">
          <span class="chip chip--green">已完成</span>
          <span class="ol-muted">提交于 {{ fmt(activeAssignment.submission?.submitTime) }}</span>
          <span
            v-if="activeAssignment.submission?.status === 1"
            class="chip chip--brand"
          >
            得分 {{ activeAssignment.submission.score }} 分
          </span>
        </div>
        <RichEditor
          v-if="hasRich(activeAssignment.submission?.content)"
          :model-value="activeAssignment.submission?.content || ''"
          readonly
          :min-height="60"
        />
        <p v-else class="empty-ans">（没有填写文字作答，仅提交了附件）</p>
        <div v-if="activeAssignment.submission?.attachUrl" class="hw-att">
          <el-icon><Paperclip /></el-icon>
          <button type="button" @click="previewFile(activeAssignment.submission.attachUrl)">
            查看我提交的附件
          </button>
        </div>
        <p v-if="activeAssignment.submission?.comment" class="teacher-comment">
          教师评语：{{ activeAssignment.submission.comment }}
        </p>
        <p class="lock-tip">
          <el-icon><Lock /></el-icon>
          作业已提交，不能再次提交或修改；需要补充可以写一条作业笔记。
        </p>
      </div>

      <!-- 未提交：作答 -->
      <template v-else>
        <p v-if="activeExpired" class="lock-tip lock-tip--warn">
          <el-icon><Clock /></el-icon>
          已过截止时间，无法再提交；仍可以写一条作业笔记做小结。
        </p>
        <p v-if="activeNotStart" class="lock-tip">
          <el-icon><Clock /></el-icon>
          作业将在 {{ fmt(activeAssignment.assignment.startTime) }} 开放，到时间后才能提交。
        </p>
        <div class="form-label">我的作答</div>
        <RichEditor
          v-model="submitForm.content"
          :min-height="160"
          placeholder="写下思路、结论或说明；支持加粗、列表，截图可直接粘贴"
        />
        <div class="form-label" style="margin-top: 14px">附件（可选）</div>
        <div class="up-row">
          <FileUploader
            label="选择文件上传"
            :max-size="50"
            @uploaded="onAttach"
          />
          <span v-if="submitForm.attachUrl" class="att-chip">
            <el-icon><Paperclip /></el-icon>
            <button type="button" @click="previewFile(submitForm.attachUrl)">预览</button>
            <button type="button" class="rm" @click="submitForm.attachUrl = ''">移除</button>
          </span>
          <span v-else class="ol-muted">支持任意格式，提交后仍可在线预览</span>
        </div>
        <p v-if="draftSavedAt" class="draft-tip">
          <el-icon><CircleCheck /></el-icon> 草稿已自动保存（{{ draftSavedAt }}），关掉页面也不会丢
        </p>
      </template>

      <template #footer>
        <el-button @click="openWorkNote(activeAssignment)">
          <el-icon><Notebook /></el-icon> 作业笔记
        </el-button>
        <el-button @click="dialog = false">关闭</el-button>
        <el-button
          v-if="!activeDone"
          type="primary"
          :disabled="activeExpired || activeNotStart"
          @click="submit"
        >
          提交作业
        </el-button>
      </template>
    </el-dialog>

    <!-- 作业/测验笔记 -->
    <el-dialog append-to-body v-model="workNote.visible" :title="workNote.title" width="560px">
      <RichEditor
        v-model="workNote.content"
        :min-height="160"
        placeholder="记录这道题的思路、易错点；截图可直接粘贴"
      />
      <template #footer>
        <el-button @click="workNote.visible = false">取消</el-button>
        <el-button type="primary" @click="saveWorkNote">保存笔记</el-button>
      </template>
    </el-dialog>

    <!-- 课件在线预览 -->
    <FilePreview v-model="previewVisible" :rid="previewRid" />
    <!-- 附件预览 -->
    <FilePreview v-model="pv.visible" :url="pv.url" :name="pv.name" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import FilePreview from '@/components/FilePreview.vue'
import FileUploader from '@/components/FileUploader.vue'
import RichEditor from '@/components/RichEditor.vue'
import { openFile, triggerDownload } from '@/utils/file'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const loading = ref(false)
const course = ref<any>({})
const chapters = ref<any[]>([])
const current = ref<any>(null)
const finishedIds = ref<number[]>([])
const resources = ref<any[]>([])
const assignments = ref<any[]>([])
const papers = ref<any[]>([])
const notes = ref<any[]>([])
const noteText = ref('')

const dialog = ref(false)
const activeAssignment = ref<any>(null)
const submitForm = ref<any>({ content: '', attachUrl: '' })

/** 已提交 / 已批阅 → 弹窗变只读 */
const activeDone = computed(() => isDone(activeAssignment.value?.state))
/** 已截止 / 未开放 → 可以看题但不能提交 */
const activeExpired = computed(() => activeAssignment.value?.state === 'EXPIRED')
const activeNotStart = computed(() => activeAssignment.value?.state === 'NOT_START')
const doneCount = computed(
  () => assignments.value.filter((a) => isDone(a.state)).length
)

/**
 * 教师创建作业/测验时可以挂到某一章节，也可以只挂到课程。
 * 章节学习页因此分成两组展示：挂在当前章节的 + 整门课程通用的，
 * 挂到别的章节的不在这里出现，避免跨章节串场。
 */
const chapterAssignments = computed(() =>
  assignments.value.filter(
    (a) => a.assignment?.chapterId && a.assignment.chapterId === current.value?.id
  )
)
const courseAssignments = computed(() =>
  assignments.value.filter((a) => !a.assignment?.chapterId)
)
const chapterPapers = computed(() =>
  papers.value.filter((p) => p.chapterId && p.chapterId === current.value?.id)
)
const coursePapers = computed(() => papers.value.filter((p) => !p.chapterId))

/**
 * 教师可在课程设置里控制作业/试卷导出的范围：
 * NONE 不开放 / ENROLLED 仅选课学生 / ALL 平台用户。
 * 学生端据此决定是否显示导出按钮，避免点了才报错。
 */
const canExport = computed(() => {
  const scope = course.value.exportScope || 'ENROLLED'
  if (scope === 'NONE') return false
  if (scope === 'ALL') return true
  return !!course.value.enrolled
})

/** 富文本里是否真的有内容（图片也算内容） */
function hasRich(html: any): boolean {
  if (!html) return false
  const text = String(html)
    .replace(/<img[^>]*>/gi, 'IMG')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
  return text.length > 0
}

/** 富文本判空 */
const plainNote = computed(() =>
  (noteText.value || '')
    .replace(/<img[^>]*>/gi, ' [图片] ')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
)

const pv = ref<any>({ visible: false, url: '', name: '' })
function previewFile(url: string, name?: string) {
  pv.value = { visible: true, url, name: name || '' }
}

const currentIndex = computed(() =>
  chapters.value.findIndex((c) => c.id === current.value?.id)
)
const percent = computed(() =>
  chapters.value.length
    ? Math.round((finishedIds.value.length / chapters.value.length) * 100)
    : 0
)

/** 正文按空行/换行切成段落，避免整段文字糊在一起 */
const paragraphs = computed(() => {
  const raw = (current.value?.content || '').trim()
  if (!raw) return []
  return raw
    .split(/\n+/)
    .map((s: string) => s.trim())
    .filter(Boolean)
})

const chapterFiles = computed(() =>
  resources.value.filter((r) => r.chapterId === current.value?.id)
)
const courseFiles = computed(() => resources.value.filter((r) => !r.chapterId))

/** 课程设置里关闭下载后，只保留「在线查看」入口 */
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

function hasFile(chapterId: number) {
  return resources.value.some((r) => r.chapterId === chapterId)
}

function sizeText(bytes: number) {
  if (!bytes) return '未知大小'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '无'
}

// ---------------- 作业状态 ----------------
const STATE_TEXT: Record<string, string> = {
  NOT_START: '未开始',
  ONGOING: '进行中',
  SUBMITTED: '已提交',
  GRADED: '已完成',
  EXPIRED: '已截止'
}
const STATE_CLASS: Record<string, string> = {
  NOT_START: '',
  ONGOING: 'chip--green',
  SUBMITTED: 'chip--green',
  GRADED: 'chip--brand',
  EXPIRED: ''
}

function stateText(state: string) {
  return STATE_TEXT[state] || '进行中'
}
function stateClass(state: string) {
  return STATE_CLASS[state] || ''
}
function isDone(state?: string) {
  return state === 'SUBMITTED' || state === 'GRADED'
}

/** 测验是否还没到开放时间（start_time 为空表示不限） */
function paperNotOpen(p: any): boolean {
  return !!p?.startTime && new Date(p.startTime).getTime() > Date.now()
}

/** 未开放的测验不允许进入作答页 */
function openPaper(p: any) {
  if (paperNotOpen(p)) {
    ElMessage.info(`测验将于 ${fmt(p.startTime)} 开放，到时间后才能作答`)
    return
  }
  router.push(`/exam/${p.id}`)
}

/** 未开始的作业不允许点进提交弹窗 */
function openWork(item: any) {
  if (item.state === 'NOT_START') {
    ElMessage.info(`作业将于 ${fmt(item.assignment.startTime)} 开放，到时间后才能作答`)
    return
  }
  openSubmit(item)
}

function onAttach(data: any) {
  submitForm.value.attachUrl = data.url
}

// ---------------- 导出 ----------------
function exportAssignment(item: any) {
  if (item.state === 'NOT_START') {
    ElMessage.warning('作业尚未开始，暂不能导出')
    return
  }
  triggerDownload(`/api/courses/${courseId}/assignments/${item.assignment.id}/export`)
}
function exportPaper(p: any) {
  if (paperNotOpen(p)) {
    ElMessage.warning('测验尚未开放，暂不能导出')
    return
  }
  triggerDownload(`/api/courses/${courseId}/papers/${p.id}/export`)
}

// ---------------- 作业笔记 ----------------
const workNote = ref<any>({
  visible: false,
  title: '作业笔记',
  content: '',
  refType: 'ASSIGNMENT',
  refId: null,
  existingId: null
})

async function openWorkNote(item: any) {
  const refId = item?.assignment?.id ?? null
  workNote.value = {
    visible: true,
    title: `作业笔记 · ${item?.assignment?.title || ''}`,
    content: '',
    refType: 'ASSIGNMENT',
    refId,
    existingId: null
  }
  // 已经写过的笔记直接读出来接着改
  try {
    const list: any = await request.get('/api/my/notes', { params: { courseId } })
    const hit = (list || []).find(
      (n: any) => n.refType === 'ASSIGNMENT' && n.refId === refId
    )
    if (hit) {
      workNote.value.content = hit.content
      workNote.value.existingId = hit.id
      workNote.value.title = `作业笔记 · ${item?.assignment?.title || ''}`
    }
  } catch {
    // 忽略
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
    courseId,
    refType: workNote.value.refType,
    refId: workNote.value.refId,
    title: '',
    content: workNote.value.content
  })
  ElMessage.success('笔记已保存')
  workNote.value.visible = false
}

async function load() {
  loading.value = true
  try {
    const data: any = await request.get(`/api/public/courses/${courseId}`)
    course.value = data.course || {}
    chapters.value = data.chapters || []
    finishedIds.value = data.finishedChapterIds || []

    // 支持从课程详情指定章节进入
    const want = Number(route.query.chapterId)
    current.value =
      chapters.value.find((c) => c.id === want) || chapters.value[0] || null

    resources.value = ((await request.get(`/api/courses/${courseId}/resources`)) as any) || []
    assignments.value = ((await request.get(`/api/courses/${courseId}/assignments`)) as any) || []
    papers.value = ((await request.get(`/api/courses/${courseId}/papers`)) as any) || []

    await loadNotes()

    // 支持从课程详情「作业测试」页签直接打开某份作业
    if (route.query.work === 'assignment') {
      const wantId = Number(route.query.id)
      const hit = assignments.value.find((a: any) => a.assignment?.id === wantId)
      if (hit) openWork(hit)
      else ElMessage.info('未找到该作业')
    }
  } finally {
    loading.value = false
  }
}

async function loadNotes() {
  if (!current.value) return
  try {
    const list: any = await request.get('/api/my/notes', { params: { courseId } })
    notes.value = (list || []).filter((n: any) => n.chapterId === current.value.id)
  } catch {
    notes.value = []
  }
}

function selectChapter(ch: any) {
  current.value = ch
  noteText.value = ''
  window.scrollTo({ top: 0 })
  loadNotes()
}

function goChapter(delta: number) {
  const next = chapters.value[currentIndex.value + delta]
  if (next) selectChapter(next)
}

async function finish(ch: any) {
  await request.post(`/api/chapters/${ch.id}/finish`, null, { params: { courseId } })
  if (!finishedIds.value.includes(ch.id)) finishedIds.value.push(ch.id)
  ElMessage.success('已标记完成')

  // 学完自动跳到下一节，符合「一路学下去」的预期
  const next = chapters.value[currentIndex.value + 1]
  if (next) setTimeout(() => selectChapter(next), 420)
}

async function saveNote() {
  const content = noteText.value
  if (!plainNote.value && !content.includes('<img')) {
    ElMessage.warning('笔记内容不能为空')
    return
  }
  await request.post('/api/my/notes', {
    courseId,
    chapterId: current.value.id,
    content
  })
  ElMessage.success('笔记已保存')
  noteText.value = ''
  loadNotes()
}

async function removeNote(n: any) {
  await request.delete(`/api/my/notes/${n.id}`)
  ElMessage.success('已删除')
  loadNotes()
}

// ---------------- 作答草稿：退出自动保存 ----------------
const draftSavedAt = ref('')

function draftKey(id: number) {
  return `ol-draft:assignment:${id}`
}

function saveDraft() {
  const item = activeAssignment.value
  if (!item || activeDone.value) return
  const { content, attachUrl } = submitForm.value
  if (!content && !attachUrl) return
  try {
    localStorage.setItem(
      draftKey(item.assignment.id),
      JSON.stringify({ content, attachUrl, at: Date.now() })
    )
    draftSavedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch {
    // 本地存储写满时静默失败，不影响作答
  }
}

let draftTimer: number | null = null
watch(
  () => [submitForm.value.content, submitForm.value.attachUrl],
  () => {
    if (!dialog.value || activeDone.value) return
    if (draftTimer) window.clearTimeout(draftTimer)
    draftTimer = window.setTimeout(saveDraft, 800)
  }
)

function restoreDraft(id: number) {
  try {
    const raw = localStorage.getItem(draftKey(id))
    if (!raw) return
    const data = JSON.parse(raw)
    if (data.content) submitForm.value.content = data.content
    if (data.attachUrl) submitForm.value.attachUrl = data.attachUrl
    draftSavedAt.value = new Date(data.at).toLocaleTimeString('zh-CN', { hour12: false })
  } catch {
    // 草稿损坏就当没有
  }
}

function clearDraft(id: number) {
  localStorage.removeItem(draftKey(id))
  draftSavedAt.value = ''
}

function onDialogClosed() {
  saveDraft()
}

onBeforeUnmount(() => {
  if (draftTimer) window.clearTimeout(draftTimer)
  saveDraft()
})

function openSubmit(item: any) {
  activeAssignment.value = item
  draftSavedAt.value = ''
  submitForm.value = {
    content: item.submission?.content || '',
    attachUrl: item.submission?.attachUrl || ''
  }
  if (!isDone(item.state)) {
    restoreDraft(item.assignment.id)
  }
  dialog.value = true
}

async function submit() {
  const text = (submitForm.value.content || '').replace(/<[^>]+>/g, '').trim()
  if (!text && !submitForm.value.content.includes('<img') && !submitForm.value.attachUrl) {
    ElMessage.warning('请先填写作答内容或上传附件')
    return
  }
  try {
    await request.post(`/api/assignments/${activeAssignment.value.assignment.id}/submit`, {
      content: submitForm.value.content,
      attachUrl: submitForm.value.attachUrl
    })
  } catch {
    return
  }
  clearDraft(activeAssignment.value.assignment.id)
  ElMessage.success('提交成功，作业已锁定')
  dialog.value = false
  assignments.value = ((await request.get(`/api/courses/${courseId}/assignments`)) as any) || []
}

onMounted(load)
</script>

<style scoped>
.learn {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 390px;
  height: calc(100vh - 64px);
  background: var(--bg);
}

/* ---------------- 左侧目录 ---------------- */
.nav {
  border-right: 1px solid var(--ink-100);
  background: var(--surface);
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.nav-head {
  padding: 20px 18px 14px;
  border-bottom: 1px solid var(--ink-100);
}
.back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--ink-400);
  margin-bottom: 10px;
  transition: color 0.18s;
}
.back:hover {
  color: var(--brand);
}
.nav-head h2 {
  font-size: 16px;
  font-weight: 700;
  margin: 0 0 12px;
  letter-spacing: -0.2px;
  line-height: 1.4;
}
.nav-progress {
  display: flex;
  align-items: center;
  gap: 9px;
}
.nav-progress .bar {
  flex: 1;
}
.nav-progress span {
  font-size: 11px;
  color: var(--ink-400);
  font-weight: 600;
  white-space: nowrap;
}

.nav-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 11px;
  cursor: pointer;
  transition: all 0.16s;
  border: 1px solid transparent;
}
.nav-item:hover {
  background: var(--surface-sunken);
}
.nav-item.active {
  background: var(--brand-50);
  border-color: var(--brand-100);
}
.ni-idx {
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  background: var(--surface-sunken);
  color: var(--ink-400);
}
.nav-item.active .ni-idx {
  background: var(--brand);
  color: #fff;
}
.nav-item.done .ni-idx {
  background: var(--green-soft);
  color: var(--green);
}
.nav-item.active.done .ni-idx {
  background: var(--green);
  color: #fff;
}
.ni-title {
  flex: 1;
  font-size: 12.5px;
  line-height: 1.45;
  color: var(--ink-700);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.nav-item.active .ni-title {
  color: var(--brand-700);
  font-weight: 620;
}
.ni-file {
  font-size: 12px;
  color: var(--ink-300);
  flex: none;
}

/* ---------------- 中间 ---------------- */
.stage {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  padding: 28px 34px 0;
}
.stage-inner {
  max-width: 820px;
}
.stage-head {
  max-width: 900px;
  width: 100%;
}
.sh-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.stage-head h1 {
  font-size: 25px;
  font-weight: 750;
  letter-spacing: -0.6px;
  margin: 0 0 18px;
  line-height: 1.35;
}
.stage-body {
  max-width: 900px;
  width: 100%;
  flex: 1;
}
.video {
  width: 100%;
  border-radius: var(--r-lg);
  margin-bottom: 20px;
  background: #000;
  max-height: 460px;
}
.prose {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 26px 28px;
}
.prose p {
  margin: 0 0 14px;
  font-size: 14.5px;
  line-height: 2;
  color: var(--ink-700);
  letter-spacing: 0.2px;
}
.prose p:last-child {
  margin-bottom: 0;
}

.sec-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14.5px;
  font-weight: 680;
  margin: 24px 0 12px;
  color: var(--ink-900);
}
.sec-title .el-icon {
  color: var(--brand);
  font-size: 16px;
}
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 10px;
}
.file-card {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 13px;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  transition: all 0.18s;
  cursor: pointer;
}
.file-card:hover {
  border-color: var(--brand-200);
  box-shadow: var(--sh-sm);
}
.file-card:hover .fc-dl,
.file-card:hover .fc-view {
  color: var(--brand);
}
.fc-view {
  color: var(--ink-300);
  font-size: 15px;
  flex: none;
  transition: color 0.18s;
}
.fc-ico {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 750;
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
.fc-body {
  flex: 1;
  min-width: 0;
}
.fc-body b {
  font-size: 12.5px;
  font-weight: 620;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.fc-body p {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--ink-400);
}
.fc-dl {
  color: var(--ink-300);
  font-size: 15px;
  flex: none;
  transition: color 0.18s;
}

.stage-foot {
  max-width: 900px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 20px 0 28px;
  margin-top: 10px;
  position: sticky;
  bottom: 0;
  background: linear-gradient(180deg, rgba(253, 247, 241, 0) 0%, var(--bg) 34%);
}
.nav-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 40px;
  padding: 0 16px;
  border-radius: var(--r-pill);
  border: 1px solid var(--ink-100);
  background: var(--surface);
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 550;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.18s;
}
.nav-btn:hover:not(:disabled) {
  border-color: var(--brand-200);
  color: var(--brand);
}
.nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.done-btn {
  flex: 1;
  max-width: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 44px;
  border: 0;
  border-radius: var(--r-pill);
  background: linear-gradient(100deg, var(--brand), var(--brand-600));
  color: #fff;
  font-size: 14px;
  font-weight: 650;
  font-family: var(--font);
  cursor: pointer;
  box-shadow: var(--sh-brand);
  transition: transform 0.16s, box-shadow 0.16s;
}
.done-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(255, 107, 53, 0.34);
}
.done-tag {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 620;
  color: var(--green);
  background: var(--green-soft);
  padding: 9px 18px;
  border-radius: var(--r-pill);
  flex: 1;
  max-width: 320px;
  justify-content: center;
}

/* ---------------- 右侧 ---------------- */
.panel {
  border-left: 1px solid var(--ink-100);
  background: var(--surface-soft);
  overflow-y: auto;
  padding: 18px 16px 28px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}
.p-card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 15px 16px;
}
.p-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 11px;
}
.p-title h3 {
  margin: 0;
  font-size: 13.5px;
  font-weight: 680;
}
/* 同一张卡片里区分「本章」与「课程级」作业/测验的小标题 */
.sub-title {
  margin: 10px 0 2px;
  padding-top: 8px;
  border-top: 1px dashed var(--ink-100);
  font-size: 11.5px;
  font-weight: 640;
  color: var(--ink-400);
}
.p-title b {
  font-size: 15px;
  font-weight: 750;
  color: var(--green);
}
.p-hint {
  margin: 8px 0 0;
  font-size: 11.5px;
  color: var(--ink-400);
}

.note-input {
  width: 100%;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-sm);
  padding: 10px 11px;
  font-family: var(--font);
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-700);
  resize: vertical;
  outline: none;
  transition: border-color 0.18s;
  background: var(--surface);
}
.note-input:focus {
  border-color: var(--brand-300);
  box-shadow: 0 0 0 3px var(--brand-50);
}
.note-input::placeholder {
  color: var(--ink-300);
}
.note-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.save-btn {
  height: 30px;
  padding: 0 14px;
  border: 0;
  border-radius: var(--r-pill);
  background: var(--brand);
  color: #fff;
  font-size: 12px;
  font-weight: 620;
  font-family: var(--font);
  cursor: pointer;
  transition: opacity 0.18s;
}
.save-btn:disabled {
  background: var(--ink-200);
  cursor: not-allowed;
}
.note-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.note-item {
  background: var(--surface-sunken);
  border-radius: var(--r-sm);
  padding: 10px 11px;
}
.note-item p {
  margin: 0 0 6px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-700);
  white-space: pre-wrap;
  word-break: break-word;
}
.note-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.del {
  border: 0;
  background: transparent;
  color: var(--ink-300);
  font-size: 11px;
  cursor: pointer;
  font-family: var(--font);
}
.del:hover {
  color: var(--brand);
}

.task-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 6px;
  border-radius: var(--r-sm);
  cursor: pointer;
  transition: background 0.16s;
}
.task-row:hover {
  background: var(--surface-sunken);
}
/* 未开放 / 未开始的测验、作业：整体弱化，提示还不能点进去 */
.task-row.is-locked {
  cursor: default;
  opacity: 0.66;
}
.task-row.is-locked:hover {
  background: transparent;
}
.task-row.is-locked .task-ico {
  filter: grayscale(0.4);
}
.task-ico {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 14px;
}
.task-ico--violet {
  background: var(--violet-soft);
  color: var(--violet);
}
.task-ico--amber {
  background: var(--amber-soft);
  color: var(--amber);
}
.task-row div {
  flex: 1;
  min-width: 0;
}
/* 侧栏窄：作业行的操作按钮另起一行右对齐，避免标题被挤成一两个字 */
.task-row--stack {
  flex-wrap: wrap;
}
.task-row .task-acts {
  flex: none;
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  padding-left: 40px;
  margin-top: 2px;
}
.task-row p {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
}
.task-row b {
  font-size: 12.5px;
  font-weight: 620;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.task-row p {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--ink-400);
}
.task-arrow {
  color: var(--ink-300);
  font-size: 11px;
  flex: none;
}

.hw-brief {
  background: var(--surface-sunken);
  border-radius: var(--r-md);
  padding: 13px 15px;
}
.hw-brief b {
  font-size: 13.5px;
  display: block;
  margin-bottom: 5px;
}
.hw-brief p {
  margin: 0 0 6px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--ink-500);
}

@media (max-width: 1280px) {
  .learn {
    grid-template-columns: 250px minmax(0, 1fr);
  }
  .panel {
    display: none;
  }
}

/* ---------- 作业状态与操作 ---------- */
.task-body {
  min-width: 0;
  flex: 1;
}
.task-row.is-done {
  opacity: 0.92;
}
.mini-act {
  width: 26px;
  height: 26px;
  flex: none;
  border: 1px solid var(--ink-100);
  border-radius: 7px;
  background: var(--surface);
  color: var(--ink-400);
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.15s;
}
.mini-act--label {
  width: auto;
  padding: 0 9px;
  gap: 4px;
  display: inline-flex;
  align-items: center;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--ink-500);
}
.mini-act--label em {
  font-style: normal;
}
.mini-act:hover {
  color: var(--brand);
  border-color: var(--brand-200);
  background: var(--brand-50);
}
.mini-link {
  border: 0;
  background: transparent;
  color: var(--brand);
  font-size: 11.5px;
  font-family: var(--font);
  cursor: pointer;
  padding: 0;
}
.mini-link:hover {
  text-decoration: underline;
}

/* ---------- 作业弹窗 ---------- */
.hw-brief-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.hw-att {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--brand);
}
.hw-att button {
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 12.5px;
  font-family: var(--font);
  cursor: pointer;
  padding: 0;
}
.hw-att button:hover {
  text-decoration: underline;
}
.done-box {
  margin-top: 14px;
  background: var(--surface-soft);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.done-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.lock-tip {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: var(--ink-400);
}
.lock-tip--warn {
  color: var(--red);
  background: var(--red-soft);
  padding: 8px 11px;
  border-radius: var(--r-sm);
  margin-bottom: 10px;
}
.empty-ans {
  margin: 0;
  padding: 12px 14px;
  border-radius: var(--r-md);
  background: var(--surface-sunken);
  font-size: 12.5px;
  color: var(--ink-400);
}
.teacher-comment {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-600);
  background: var(--amber-soft);
  border-radius: 8px;
  padding: 8px 12px;
}
.form-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-600);
  margin-bottom: 6px;
}
.up-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.att-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--brand-50);
  border-radius: var(--r-pill);
  padding: 4px 12px;
  font-size: 12px;
  color: var(--brand-700);
}
.att-chip button {
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 12px;
  font-family: var(--font);
  cursor: pointer;
  padding: 0;
}
.att-chip .rm {
  color: var(--ink-400);
}
.draft-tip {
  margin: 10px 0 0;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: var(--green);
}

/* ---------- 笔记富文本 ---------- */
.note-rich {
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--ink-600);
  word-break: break-word;
}
.note-rich :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 5px 0;
  border: 1px solid var(--ink-100);
}
.note-rich :deep(h2),
.note-rich :deep(h3) {
  font-size: 13px;
  margin: 4px 0;
}
.note-rich :deep(blockquote) {
  margin: 5px 0;
  padding: 4px 10px;
  border-left: 3px solid var(--brand-200);
  background: var(--brand-50);
}
.note-rich :deep(ul),
.note-rich :deep(ol) {
  padding-left: 18px;
  margin: 4px 0;
}
</style>
