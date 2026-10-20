<template>
  <div class="page-card">
    <el-page-header content="作业管理与批阅" @back="router.push('/teacher/courses')" style="margin-bottom: 16px" />
    <el-row justify="space-between" style="margin-bottom: 12px">
      <div class="muted">共 {{ list.length }} 次作业</div>
      <el-button type="primary" @click="openEdit(null)">布置作业</el-button>
    </el-row>

    <el-table :data="list" stripe>
      <el-table-column label="标题" min-width="180">
        <template #default="scope">{{ scope.row.assignment.title }}</template>
      </el-table-column>
      <el-table-column label="截止时间" width="180">
        <template #default="scope">{{ fmt(scope.row.assignment.deadline) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button link type="primary" @click="openSubmissions(scope.row.assignment)">提交情况</el-button>
          <el-button link type="primary" @click="openEdit(scope.row.assignment)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row.assignment)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="list.length === 0" description="还没有布置作业" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑作业' : '布置作业'" width="560px">
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="要求">
          <el-input v-model="form.requirement" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="选择截止时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer" title="提交情况" size="640px">
      <el-table :data="submissions" stripe>
        <el-table-column prop="realName" label="学生" width="100" />
        <el-table-column prop="submission.content" label="作业说明" show-overflow-tooltip />
        <el-table-column label="附件" width="90">
          <template #default="scope">
            <el-link v-if="scope.row.submission.attachUrl" type="primary"
                     @click="openUrl(scope.row.submission.attachUrl)">下载</el-link>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.submission.status === 1 ? 'success' : 'warning'">
              {{ scope.row.submission.status === 1 ? '已批阅' : '待批阅' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submission.score" label="分数" width="80" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button link type="primary" @click="openGrade(scope.row)">批阅</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="submissions.length === 0" description="还没有学生提交" />
    </el-drawer>

    <el-dialog v-model="gradeDialog" title="批阅" width="420px">
      <el-form label-width="70px">
        <el-form-item label="分数">
          <el-input-number v-model="gradeForm.score" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="gradeForm.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gradeDialog = false">取消</el-button>
        <el-button type="primary" @click="doGrade">提交</el-button>
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

const list = ref<any[]>([])
const dialog = ref(false)
const form = reactive<any>({ id: null, title: '', requirement: '', deadline: null })

const drawer = ref(false)
const submissions = ref<any[]>([])
const gradeDialog = ref(false)
const gradeForm = reactive<any>({ id: null, score: 80, comment: '' })

function fmt(t: string) {
  return t ? t.replace('T', ' ') : '不限'
}

function openUrl(url: string) {
  window.open(url, '_blank')
}

async function load() {
  list.value = (await request.get('/api/teacher/courses/' + courseId + '/assignments')) as any
}

function openEdit(row: any) {
  Object.assign(form, row || { id: null, title: '', requirement: '', deadline: null })
  dialog.value = true
}

async function save() {
  await request.post('/api/teacher/assignments', { ...form, courseId })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('删除作业会同时删除学生提交记录，确认？', '提示', { type: 'warning' })
  await request.delete('/api/teacher/assignments/' + row.id)
  ElMessage.success('已删除')
  load()
}

async function openSubmissions(assignment: any) {
  submissions.value = (await request.get('/api/teacher/assignments/' + assignment.id + '/submissions')) as any
  drawer.value = true
}

function openGrade(row: any) {
  gradeForm.id = row.submission.id
  gradeForm.score = row.submission.score || 80
  gradeForm.comment = row.submission.comment || ''
  gradeDialog.value = true
}

async function doGrade() {
  await request.post('/api/teacher/submissions/' + gradeForm.id + '/grade', {
    score: gradeForm.score,
    comment: gradeForm.comment
  })
  ElMessage.success('批阅完成')
  gradeDialog.value = false
  const current = submissions.value.find((s) => s.submission.id === gradeForm.id)
  if (current) {
    current.submission.score = gradeForm.score
    current.submission.comment = gradeForm.comment
    current.submission.status = 1
  }
}

onMounted(load)
</script>
