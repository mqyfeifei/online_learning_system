<template>
  <div class="page-card">
    <el-page-header content="章节与课件管理" @back="router.push('/teacher/courses')" style="margin-bottom: 16px" />

    <el-row justify="space-between" align="middle" style="margin-bottom: 12px">
      <el-tabs v-model="tab" style="flex: 1">
        <el-tab-pane label="章节管理" name="chapter" />
        <el-tab-pane label="课件资源" name="resource" />
      </el-tabs>
      <el-button type="primary" @click="tab === 'chapter' ? openChapter(null) : openResource()">
        新增{{ tab === 'chapter' ? '章节' : '课件' }}
      </el-button>
    </el-row>

    <template v-if="tab === 'chapter'">
      <el-table :data="chapters" stripe>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="title" label="章节标题" />
        <el-table-column prop="content" label="内容摘要" show-overflow-tooltip />
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button link type="primary" @click="openChapter(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="removeChapter(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="chapters.length === 0" description="还没有章节" />
    </template>

    <template v-else>
      <el-table :data="resources" stripe>
        <el-table-column prop="name" label="文件名" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column label="大小" width="120">
          <template #default="scope">{{ (scope.row.fileSize / 1024 / 1024).toFixed(2) }} MB</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button link type="primary" @click="openUrl(scope.row.fileUrl)">查看</el-button>
            <el-button link type="danger" @click="removeResource(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="resources.length === 0" description="还没有课件" />
    </template>

    <el-dialog v-model="chapterDialog" :title="chapterForm.id ? '编辑章节' : '新增章节'" width="560px">
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="chapterForm.title" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="chapterForm.sort" :min="1" /></el-form-item>
        <el-form-item label="视频地址"><el-input v-model="chapterForm.videoUrl" placeholder="可选" /></el-form-item>
        <el-form-item label="图文内容">
          <el-input v-model="chapterForm.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterDialog = false">取消</el-button>
        <el-button type="primary" @click="saveChapter">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resourceDialog" title="上传课件" width="520px">
      <el-form label-width="80px">
        <el-form-item label="文件">
          <el-upload :http-request="doUpload" :limit="1" :on-success="onUploaded">
            <el-button>选择文件</el-button>
          </el-upload>
          <div v-if="resourceForm.fileUrl" class="muted">已上传：{{ resourceForm.fileUrl }}</div>
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="resourceForm.name" />
        </el-form-item>
        <el-form-item label="所属章节">
          <el-select v-model="resourceForm.chapterId" clearable placeholder="课程级资源可不选" style="width: 100%">
            <el-option v-for="c in chapters" :key="c.id" :label="c.title" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resourceDialog = false">取消</el-button>
        <el-button type="primary" @click="saveResource">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const tab = ref('chapter')
const chapters = ref<any[]>([])
const resources = ref<any[]>([])

const chapterDialog = ref(false)
const chapterForm = reactive<any>({ id: null, title: '', sort: 1, videoUrl: '', content: '' })

const resourceDialog = ref(false)
const resourceForm = reactive<any>({ name: '', fileUrl: '', fileType: '', fileSize: 0, chapterId: null })

function openUrl(url: string) {
  window.open(url, '_blank')
}

async function load() {
  chapters.value = (await request.get('/api/teacher/courses/' + courseId + '/chapters')) as any
  resources.value = (await request.get('/api/teacher/courses/' + courseId + '/resources')) as any
}

function openChapter(row: any) {
  Object.assign(chapterForm, row || { id: null, title: '', sort: chapters.value.length + 1, videoUrl: '', content: '' })
  chapterDialog.value = true
}

async function saveChapter() {
  await request.post('/api/teacher/chapters', { ...chapterForm, courseId })
  ElMessage.success('已保存')
  chapterDialog.value = false
  load()
}

async function removeChapter(row: any) {
  await ElMessageBox.confirm('删除章节会同时删除其课件与学习记录，确认？', '提示', { type: 'warning' })
  await request.delete('/api/teacher/chapters/' + row.id)
  ElMessage.success('已删除')
  load()
}

function openResource() {
  Object.assign(resourceForm, { name: '', fileUrl: '', fileType: '', fileSize: 0, chapterId: null })
  resourceDialog.value = true
}

async function doUpload(options: any) {
  const fd = new FormData()
  fd.append('file', options.file)
  const data: any = await request.post('/api/files/upload', fd)
  options.onSuccess(data)
}

function onUploaded(data: any) {
  resourceForm.fileUrl = data.url
  resourceForm.fileType = data.type
  resourceForm.fileSize = data.size
  resourceForm.name = data.name
  ElMessage.success('上传成功')
}

async function saveResource() {
  await request.post('/api/teacher/resources', {
    courseId, chapterId: resourceForm.chapterId, name: resourceForm.name,
    fileUrl: resourceForm.fileUrl, fileType: resourceForm.fileType, fileSize: resourceForm.fileSize
  })
  ElMessage.success('已保存')
  resourceDialog.value = false
  load()
}

async function removeResource(row: any) {
  await request.delete('/api/teacher/resources/' + row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
