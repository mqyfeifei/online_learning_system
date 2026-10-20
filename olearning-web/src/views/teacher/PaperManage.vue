<template>
  <div class="page-card">
    <el-page-header content="试卷与组卷" @back="router.push('/teacher/courses')" style="margin-bottom: 16px" />
    <el-row justify="space-between" style="margin-bottom: 12px">
      <div class="muted">共 {{ papers.length }} 份试卷</div>
      <el-button type="primary" @click="openEdit(null)">新建试卷</el-button>
    </el-row>

    <el-table :data="papers" stripe>
      <el-table-column prop="title" label="试卷名称" />
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="passScore" label="及格分" width="90" />
      <el-table-column prop="duration" label="时长(分钟)" width="110" />
      <el-table-column label="操作" width="240">
        <template #default="scope">
          <el-button link type="primary" @click="openCompose(scope.row)">组卷</el-button>
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="papers.length === 0" description="还没有试卷" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑试卷' : '新建试卷'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="试卷名称"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="总分"><el-input-number v-model="form.totalScore" :min="10" :max="200" /></el-form-item>
        <el-form-item label="及格分"><el-input-number v-model="form.passScore" :min="0" :max="200" /></el-form-item>
        <el-form-item label="时长(分钟)"><el-input-number v-model="form.duration" :min="5" :max="180" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer" :title="'组卷：' + currentPaper.title" size="720px">
      <el-row :gutter="12" style="margin-bottom: 12px">
        <el-col :span="14">
          <el-select v-model="selectedQuestion" placeholder="从题库选择题目" filterable style="width: 100%">
            <el-option v-for="q in questions" :key="q.id" :label="q.title" :value="q.id" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-input-number v-model="questionScore" :min="1" :max="100" style="width: 100%" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="addQuestion">添加</el-button>
        </el-col>
      </el-row>

      <el-table :data="paperQuestions" stripe>
        <el-table-column label="题目" min-width="260">
          <template #default="scope">{{ scope.row.title }}</template>
        </el-table-column>
        <el-table-column label="题型" width="80">
          <template #default="scope">{{ ['', '单选', '多选', '判断'][scope.row.type] }}</template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column prop="answer" label="答案" width="90" />
      </el-table>
      <el-empty v-if="paperQuestions.length === 0" description="试卷还是空的，从上方选题目加入" />
    </el-drawer>
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

const papers = ref<any[]>([])
const questions = ref<any[]>([])
const dialog = ref(false)
const form = reactive<any>({ id: null, title: '', totalScore: 100, passScore: 60, duration: 30 })

const drawer = ref(false)
const currentPaper = ref<any>({})
const paperQuestions = ref<any[]>([])
const selectedQuestion = ref<number | null>(null)
const questionScore = ref(10)

async function load() {
  papers.value = (await request.get('/api/teacher/courses/' + courseId + '/papers')) as any
  questions.value = (await request.get('/api/teacher/courses/' + courseId + '/questions')) as any
}

function openEdit(row: any) {
  Object.assign(form, row || { id: null, title: '', totalScore: 100, passScore: 60, duration: 30 })
  dialog.value = true
}

async function save() {
  await request.post('/api/teacher/papers', { ...form, courseId })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该试卷？', '提示', { type: 'warning' })
  await request.delete('/api/teacher/papers/' + row.id)
  ElMessage.success('已删除')
  load()
}

async function openCompose(row: any) {
  currentPaper.value = row
  await loadPaperQuestions()
  drawer.value = true
}

async function loadPaperQuestions() {
  paperQuestions.value = (await request.get(
    '/api/teacher/papers/' + currentPaper.value.id + '/questions'
  )) as any
}

async function addQuestion() {
  if (!selectedQuestion.value) {
    ElMessage.warning('请先选择题目')
    return
  }
  await request.post('/api/teacher/papers/' + currentPaper.value.id + '/questions', {
    questionId: selectedQuestion.value,
    score: questionScore.value
  })
  ElMessage.success('已加入试卷')
  selectedQuestion.value = null
  loadPaperQuestions()
}

onMounted(load)
</script>
