<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>章节与课件</h1>
        <p>{{ courseTitle }} · 共 {{ chapters.length }} 节，{{ resources.length }} 份课件</p>
      </div>
      <div class="ap-actions">
        <el-button @click="router.push('/teacher/courses')">
          <el-icon><ArrowLeft /></el-icon> 返回课程列表
        </el-button>
        <el-button type="primary" @click="openChapter(null)">
          <el-icon><Plus /></el-icon> 新增章节
        </el-button>
      </div>
    </div>

    <div class="layout">
      <!-- 左：章节列表 -->
      <section class="ap-card side-list">
        <div class="ap-card-head">
          <h2>章节目录</h2>
          <span class="ol-muted">{{ chapters.length }} 节</span>
        </div>
        <div class="chapters">
          <div
            v-for="(ch, i) in chapters"
            :key="ch.id"
            class="ch"
            :class="{ active: current?.id === ch.id }"
            @click="selectChapter(ch)"
          >
            <span class="ch-no">{{ i + 1 }}</span>
            <div class="ch-text">
              <b>{{ ch.title }}</b>
              <span>{{ fileCount(ch.id) }} 份课件</span>
            </div>
            <el-icon class="ch-del" @click.stop="removeChapter(ch)">
              <Delete />
            </el-icon>
          </div>

          <div v-if="!chapters.length" class="ol-empty" style="padding: 30px 0">
            还没有章节，先新增一节
          </div>
        </div>
      </section>

      <!-- 右：章节详情 + 课件 -->
      <section v-if="current" class="ap-card detail">
        <div class="ap-card-head">
          <h2>{{ current.title }}</h2>
          <el-button size="small" @click="openChapter(current)">编辑章节</el-button>
        </div>

        <div class="detail-body">
          <div class="field">
            <label>章节序号</label>
            <b>{{ current.sort }}</b>
          </div>
          <div class="field">
            <label>视频地址</label>
            <span class="mono">{{ current.videoUrl || '未设置' }}</span>
          </div>
          <div class="field field--block">
            <label>章节内容</label>
            <p class="content">{{ current.content || '暂无图文内容' }}</p>
          </div>
        </div>

        <!-- 课件 -->
        <div class="files-head">
          <h3>课件资料</h3>
          <FileUploader
            size="small"
            :max-size="100"
            accept=".pdf,.ppt,.pptx,.doc,.docx,.xls,.xlsx,.md,.txt,.zip,.mp4,.png,.jpg,.jpeg"
            label="选择文件上传"
            @uploaded="(d) => saveResource(d, current?.id)"
          />
        </div>

        <div class="file-list">
          <div v-for="f in currentFiles" :key="f.id" class="file">
            <span class="fico" :class="`type-${(f.fileType || '').slice(0, 4)}`">
              {{ (f.fileType || 'FILE').toUpperCase().slice(0, 4) }}
            </span>
            <div class="fbody">
              <b>{{ f.name }}</b>
              <span>{{ sizeText(f.fileSize) }}</span>
            </div>
            <button class="fop" @click="openPreview(f)">预览</button>
            <button class="fop" @click="downloadFile(f)">下载</button>
            <button class="fop danger" @click="removeFile(f)">删除</button>
          </div>
          <div v-if="!currentFiles.length" class="ol-empty" style="padding: 22px 0">
            本节还没有课件
          </div>
        </div>

        <!-- 课程级资料 -->
        <div class="files-head">
          <h3>课程级资料（不归属某一节）</h3>
          <FileUploader
            size="small"
            :max-size="100"
            accept=".pdf,.ppt,.pptx,.doc,.docx,.xls,.xlsx,.md,.txt,.zip,.mp4,.png,.jpg,.jpeg"
            label="选择文件上传"
            @uploaded="(d) => saveResource(d, null)"
          />
        </div>
        <div class="file-list">
          <div v-for="f in courseLevelFiles" :key="f.id" class="file">
            <span class="fico" :class="`type-${(f.fileType || '').slice(0, 4)}`">
              {{ (f.fileType || 'FILE').toUpperCase().slice(0, 4) }}
            </span>
            <div class="fbody">
              <b>{{ f.name }}</b>
              <span>{{ sizeText(f.fileSize) }}</span>
            </div>
            <button class="fop" @click="openPreview(f)">预览</button>
            <button class="fop" @click="downloadFile(f)">下载</button>
            <button class="fop danger" @click="removeFile(f)">删除</button>
          </div>
          <div v-if="!courseLevelFiles.length" class="ol-empty" style="padding: 22px 0">
            暂无课程级资料
          </div>
        </div>
      </section>

      <section v-else class="ap-card empty-card">
        <div class="ol-empty">
          <span class="icon">📂</span>
          左侧选择一节，查看与维护其内容
        </div>
      </section>
    </div>

    <!-- 课件预览 -->
    <FilePreview v-model="previewVisible" :rid="previewRid" />

    <!-- 章节编辑 -->
    <el-dialog append-to-body v-model="dialog" :title="form.id ? '编辑章节' : '新增章节'" width="580px">
      <el-form label-position="top">
        <el-form-item label="章节标题">
          <el-input v-model="form.title" maxlength="80" show-word-limit placeholder="例如：第 1 章 计算机网络概述" />
        </el-form-item>
        <div class="two">
          <el-form-item label="排序序号">
            <el-input-number v-model="form.sort" :min="1" :max="99" style="width: 100%" />
          </el-form-item>
          <el-form-item label="章节视频（可选）">
            <div class="video-row">
              <el-input v-model="form.videoUrl" placeholder="上传视频或粘贴 mp4 地址" />
              <FileUploader
                size="default"
                accept="video/*"
                label="选择视频"
                :max-size="100"
                @uploaded="(d) => (form.videoUrl = d.url)"
              />
            </div>
          </el-form-item>
        </div>
        <el-form-item label="章节内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="本节的要点说明，支持换行分段"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveChapter">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import FileUploader from '@/components/FileUploader.vue'
import FilePreview from '@/components/FilePreview.vue'
import { openFile } from '@/utils/file'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const courseTitle = ref('课程')
const chapters = ref<any[]>([])
const resources = ref<any[]>([])
const current = ref<any>(null)
const dialog = ref(false)
const uploading = ref(false)
const previewVisible = ref(false)
const previewRid = ref<number | null>(null)
const form = ref<any>({ id: null, title: '', sort: 1, videoUrl: '', content: '' })

const currentFiles = computed(() =>
  resources.value.filter((r) => r.chapterId === current.value?.id)
)
const courseLevelFiles = computed(() => resources.value.filter((r) => !r.chapterId))

function fileCount(chapterId: number) {
  return resources.value.filter((r) => r.chapterId === chapterId).length
}

function sizeText(bytes: number) {
  if (!bytes) return '未知大小'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

async function load() {
  const courses: any = await request.get('/api/teacher/courses')
  const target = (courses || []).find((c: any) => c.id === courseId)
  courseTitle.value = target?.title || '课程'

  chapters.value = ((await request.get(`/api/teacher/courses/${courseId}/chapters`)) as any) || []
  resources.value = ((await request.get(`/api/teacher/courses/${courseId}/resources`)) as any) || []

  // 保持当前选中项；首次进入默认选第一节
  if (current.value) {
    current.value = chapters.value.find((c) => c.id === current.value.id) || chapters.value[0] || null
  } else {
    current.value = chapters.value[0] || null
  }
}

function selectChapter(ch: any) {
  current.value = ch
}

function openChapter(row: any) {
  form.value = row
    ? {
        id: row.id,
        title: row.title,
        sort: row.sort,
        videoUrl: row.videoUrl || '',
        content: row.content || ''
      }
    : {
        id: null,
        title: '',
        sort: chapters.value.length + 1,
        videoUrl: '',
        content: ''
      }
  dialog.value = true
}

async function saveChapter() {
  if (!form.value.title.trim()) {
    ElMessage.warning('章节标题不能为空')
    return
  }
  await request.post('/api/teacher/chapters', { ...form.value, courseId })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function removeChapter(ch: any) {
  await ElMessageBox.confirm(`删除章节《${ch.title}》？该章节的课件也会一起删除。`, '提示', {
    type: 'warning'
  })
  await request.delete(`/api/teacher/chapters/${ch.id}`)
  ElMessage.success('已删除')
  if (current.value?.id === ch.id) current.value = null
  load()
}

/** 上传成功后落库：chapterId 为空表示课程级资料 */
async function saveResource(data: any, chapterId: number | null) {
  const ext = (data.name.split('.').pop() || '').toLowerCase()
  await request.post('/api/teacher/resources', {
    courseId,
    chapterId: chapterId || null,
    name: data.name,
    fileUrl: data.url,
    fileType: ext,
    fileSize: data.size
  })
  resources.value = ((await request.get(`/api/teacher/courses/${courseId}/resources`)) as any) || []
}

function downloadFile(f: any) {
  openFile(`/api/public/files/download?rid=${f.id}`)
}

function openPreview(f: any) {
  previewRid.value = f.id
  previewVisible.value = true
}

async function removeFile(f: any) {
  await ElMessageBox.confirm(`删除课件《${f.name}》？`, '提示', { type: 'warning' })
  await request.delete(`/api/teacher/resources/${f.id}`)
  ElMessage.success('已删除')
  resources.value = ((await request.get(`/api/teacher/courses/${courseId}/resources`)) as any) || []
}

onMounted(load)
</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

/* ---------- 左列表 ---------- */
.chapters {
  padding: 8px;
  max-height: 640px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ch {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 11px;
  cursor: pointer;
  transition: all 0.16s;
  border: 1px solid transparent;
}
.ch:hover {
  background: var(--surface-sunken);
}
.ch.active {
  background: var(--brand-50);
  border-color: var(--brand-100);
}
.ch-no {
  width: 24px;
  height: 24px;
  flex: none;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 11.5px;
  font-weight: 700;
  background: var(--surface-sunken);
  color: var(--ink-400);
}
.ch.active .ch-no {
  background: var(--brand);
  color: #fff;
}
.ch-text {
  flex: 1;
  min-width: 0;
}
.ch-text b {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ch-text span {
  font-size: 11px;
  color: var(--ink-400);
}
.ch-del {
  color: var(--ink-300);
  font-size: 14px;
  opacity: 0;
  transition: all 0.16s;
}
.ch:hover .ch-del {
  opacity: 1;
}
.ch-del:hover {
  color: #c0392b;
}

/* ---------- 右详情 ---------- */
.detail-body {
  padding: 16px 20px;
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 12px 20px;
}
.field label {
  display: block;
  font-size: 11.5px;
  color: var(--ink-400);
  margin-bottom: 3px;
}
.field b {
  font-size: 13.5px;
  font-weight: 650;
}
.mono {
  font-size: 12.5px;
  color: var(--ink-500);
  word-break: break-all;
}
.field--block {
  grid-column: 1 / -1;
}
.content {
  margin: 0;
  font-size: 13px;
  line-height: 1.85;
  color: var(--ink-700);
  white-space: pre-wrap;
  background: var(--surface-sunken);
  padding: 12px 14px;
  border-radius: var(--r-sm);
}

.files-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px 6px;
  border-top: 1px solid var(--ink-100);
}
.files-head h3 {
  margin: 0;
  font-size: 13.5px;
  font-weight: 680;
}
.file-list {
  padding: 4px 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.file {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 9px 12px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-sm);
  transition: border-color 0.16s;
}
.file:hover {
  border-color: var(--brand-200);
}
.fico {
  width: 36px;
  height: 36px;
  flex: none;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 9.5px;
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
.fbody {
  flex: 1;
  min-width: 0;
}
.fbody b {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.fbody span {
  font-size: 11px;
  color: var(--ink-400);
}
.fop {
  border: 0;
  background: transparent;
  color: var(--brand);
  font-size: 12px;
  font-weight: 550;
  font-family: var(--font);
  cursor: pointer;
  padding: 2px 6px;
  flex: none;
}
.fop.danger {
  color: var(--ink-400);
}
.fop.danger:hover {
  color: #c0392b;
}

.empty-card {
  min-height: 300px;
  display: grid;
  place-items: center;
}
.two {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 12px;
}
.video-row {
  display: flex;
  gap: 8px;
  width: 100%;
  align-items: center;
}
.video-row :deep(.el-input) {
  flex: 1;
}

@media (max-width: 1100px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
