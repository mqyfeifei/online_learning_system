<template>
  <div v-loading="loading">
    <div class="page-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="page-title">章节目录</div>
          <div v-for="(ch, i) in chapters" :key="ch.id"
               :class="['ch-item', { active: current?.id === ch.id }]"
               @click="current = ch">
            <span class="idx">{{ i + 1 }}</span>
            <span class="ch-title">{{ ch.title }}</span>
            <el-icon v-if="finishedIds.includes(ch.id)" color="#67c23a"><CircleCheck /></el-icon>
          </div>
        </el-col>

        <el-col :span="18">
          <template v-if="current">
            <h2 class="ch-head">{{ current.title }}</h2>
            <div class="content">{{ current.content || '本章暂无图文内容，请查看下方课件资源。' }}</div>
            <video v-if="current.videoUrl" :src="current.videoUrl" controls style="width: 100%; margin-top: 12px" />
            <div style="margin-top: 20px">
              <el-button v-if="!finishedIds.includes(current.id)" type="success" @click="finish(current)">
                学完本章
              </el-button>
              <el-tag v-else type="success">本章已打卡</el-tag>
            </div>
          </template>
          <el-empty v-else description="请选择左侧章节" />
        </el-col>
      </el-row>
    </div>

    <div class="page-card">
      <el-tabs v-model="tab">
        <el-tab-pane label="课件资源" name="res">
          <el-empty v-if="resources.length === 0" description="暂无课件" />
          <div v-for="r in resources" :key="r.id" class="res-item">
            <el-icon><Document /></el-icon>
            <a :href="r.fileUrl" target="_blank">{{ r.name }}</a>
            <span class="muted">{{ r.fileType }}</span>
          </div>
        </el-tab-pane>

        <el-tab-pane label="作业" name="hw">
          <el-empty v-if="assignments.length === 0" description="暂无作业" />
          <div v-for="item in assignments" :key="item.assignment.id" class="hw-item">
            <div>
              <div class="hw-title">{{ item.assignment.title }}</div>
              <div class="muted">{{ item.assignment.requirement }}</div>
              <div class="muted">截止：{{ fmt(item.assignment.deadline) }}</div>
              <el-tag v-if="item.submission" size="small" :type="item.submission.status === 1 ? 'success' : 'warning'">
                {{ item.submission.status === 1 ? `已批阅 ${item.submission.score} 分` : '已提交待批阅' }}
              </el-tag>
            </div>
            <el-button size="small" type="primary" @click="openSubmit(item)">提交作业</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="在线测验" name="exam">
          <el-empty v-if="papers.length === 0" description="暂无测验" />
          <div v-for="p in papers" :key="p.id" class="hw-item">
            <div>
              <div class="hw-title">{{ p.title }}</div>
              <div class="muted">总分 {{ p.totalScore }} · 及格 {{ p.passScore }} · 限时 {{ p.duration }} 分钟</div>
            </div>
            <el-button size="small" type="primary" @click="router.push(`/exam/${p.id}`)">开始答题</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="dialog" title="提交作业" width="520px">
      <el-form label-position="top">
        <el-form-item label="作业说明">
          <el-input v-model="submitForm.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload :http-request="doUpload" :limit="1" :on-success="onUploaded">
            <el-button>选择文件</el-button>
          </el-upload>
          <div v-if="submitForm.attachUrl" class="muted">已上传：{{ submitForm.attachUrl }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const loading = ref(false)
const chapters = ref<any[]>([])
const current = ref<any>(null)
const finishedIds = ref<number[]>([])
const resources = ref<any[]>([])
const assignments = ref<any[]>([])
const papers = ref<any[]>([])
const tab = ref('res')

const dialog = ref(false)
const submitForm = ref<any>({ assignmentId: null, content: '', attachUrl: '' })

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '无'
}

async function load() {
  loading.value = true
  try {
    const data: any = await request.get(`/api/public/courses/${courseId}`)
    chapters.value = data.chapters
    finishedIds.value = data.finishedChapterIds || []
    current.value = chapters.value[0] || null

    resources.value = (await request.get(`/api/courses/${courseId}/resources`)) as any
    assignments.value = (await request.get(`/api/courses/${courseId}/assignments`)) as any
    papers.value = (await request.get(`/api/courses/${courseId}/papers`)) as any
  } finally {
    loading.value = false
  }
}

async function finish(ch: any) {
  await request.post(`/api/chapters/${ch.id}/finish`, null, { params: { courseId } })
  ElMessage.success('打卡成功')
  finishedIds.value.push(ch.id)
}

function openSubmit(item: any) {
  submitForm.value = {
    assignmentId: item.assignment.id,
    content: item.submission?.content || '',
    attachUrl: item.submission?.attachUrl || ''
  }
  dialog.value = true
}

async function doUpload(options: any) {
  const fd = new FormData()
  fd.append('file', options.file)
  const data: any = await request.post('/api/files/upload', fd)
  options.onSuccess(data)
}

function onUploaded(data: any) {
  submitForm.value.attachUrl = data.url
  ElMessage.success('附件上传成功')
}

async function submit() {
  await request.post(`/api/assignments/${submitForm.value.assignmentId}/submit`, {
    content: submitForm.value.content,
    attachUrl: submitForm.value.attachUrl
  })
  ElMessage.success('提交成功')
  dialog.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.ch-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
  border-radius: 6px;
  cursor: pointer;
}
.ch-item:hover {
  background: #f5f7fa;
}
.ch-item.active {
  background: #ecf5ff;
  color: #409eff;
}
.idx {
  font-size: 12px;
  color: #909399;
}
.ch-title {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ch-head {
  margin: 0 0 12px;
}
.content {
  line-height: 1.9;
  color: #606266;
  white-space: pre-wrap;
}
.res-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid #f2f6fc;
}
.hw-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f6fc;
}
.hw-title {
  font-weight: 600;
  margin-bottom: 4px;
}
</style>
