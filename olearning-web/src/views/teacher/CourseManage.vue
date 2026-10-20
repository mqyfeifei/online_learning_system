<template>
  <div class="page-card">
    <el-row justify="space-between" align="middle" style="margin-bottom: 16px">
      <div class="page-title" style="margin: 0">我的课程</div>
      <el-button type="primary" @click="openEdit(null)">新建课程</el-button>
    </el-row>

    <el-table :data="list" stripe>
      <el-table-column prop="title" label="课程名称" min-width="180" />
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column label="状态" width="110">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chapterCount" label="章节" width="80" />
      <el-table-column prop="studentCount" label="选课人数" width="100" />
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column label="操作" width="420">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="primary" @click="go(scope.row, 'chapters')">章节</el-button>
          <el-button link type="primary" @click="go(scope.row, 'questions')">题库</el-button>
          <el-button link type="primary" @click="go(scope.row, 'papers')">试卷</el-button>
          <el-button link type="primary" @click="go(scope.row, 'assignments')">作业</el-button>
          <el-button link type="primary" @click="go(scope.row, 'students')">学生</el-button>
          <el-button v-if="scope.row.status !== 2 && scope.row.status !== 1"
                     link type="success" @click="submit(scope.row)">提交审核</el-button>
          <el-button v-if="scope.row.status === 2" link type="warning" @click="offline(scope.row)">下架</el-button>
          <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="rejectReason" class="muted" style="margin-top: 12px">
      驳回理由：{{ rejectReason }}
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑课程' : '新建课程'" width="560px">
      <el-form label-width="80px">
        <el-form-item label="课程名称">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="form.cover" placeholder="可先上传图片拿地址" />
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const list = ref<any[]>([])
const categories = ref<any[]>([])
const dialog = ref(false)
const form = reactive<any>({ id: null, title: '', categoryId: null, cover: '', description: '' })
const rejectReason = ref('')

function statusText(s: number) {
  return ['草稿', '待审核', '已发布', '已下架', '审核驳回'][s] || '未知'
}

function statusType(s: number) {
  return (['info', 'warning', 'success', 'info', 'danger'] as const)[s] || 'info'
}

async function load() {
  list.value = (await request.get('/api/teacher/courses')) as any
  const rejected = list.value.find((c) => c.status === 4)
  rejectReason.value = rejected ? rejected.rejectReason || '' : ''
}

function openEdit(row: any) {
  Object.assign(form, row || { id: null, title: '', categoryId: null, cover: '', description: '' })
  dialog.value = true
}

async function save() {
  if (!form.title) {
    ElMessage.warning('课程名称不能为空')
    return
  }
  await request.post('/api/teacher/courses', {
    id: form.id, title: form.title, categoryId: form.categoryId,
    cover: form.cover, description: form.description
  })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function submit(row: any) {
  await request.post('/api/teacher/courses/' + row.id + '/submit')
  ElMessage.success('已提交审核')
  load()
}

async function offline(row: any) {
  await request.post('/api/teacher/courses/' + row.id + '/offline')
  ElMessage.success('已下架')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除课程《' + row.title + '》？', '提示', { type: 'warning' })
  await request.delete('/api/teacher/courses/' + row.id)
  ElMessage.success('已删除')
  load()
}

function go(row: any, tab: string) {
  router.push('/teacher/courses/' + row.id + '/' + tab)
}

onMounted(async () => {
  categories.value = (await request.get('/api/teacher/categories')) as any
  load()
})
</script>
