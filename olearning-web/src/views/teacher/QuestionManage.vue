<template>
  <div class="page-card">
    <el-page-header content="题库管理" @back="router.push('/teacher/courses')" style="margin-bottom: 16px" />
    <el-row justify="space-between" style="margin-bottom: 12px">
      <div class="muted">共 {{ list.length }} 道题</div>
      <el-button type="primary" @click="openEdit(null)">新增题目</el-button>
    </el-row>

    <el-table :data="list" stripe>
      <el-table-column label="题型" width="90">
        <template #default="scope">
          <el-tag size="small">{{ typeName(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="题干" show-overflow-tooltip />
      <el-table-column prop="answer" label="答案" width="100" />
      <el-table-column label="难度" width="90">
        <template #default="scope">{{ ['简单', '中等', '困难'][scope.row.difficulty - 1] }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="list.length === 0" description="还没有题目" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑题目' : '新增题目'" width="640px">
      <el-form label-width="80px">
        <el-form-item label="题型">
          <el-radio-group v-model="form.type" @change="resetOptions">
            <el-radio :value="1">单选</el-radio>
            <el-radio :value="2">多选</el-radio>
            <el-radio :value="3">判断</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干"><el-input v-model="form.title" type="textarea" :rows="2" /></el-form-item>

        <template v-if="form.type !== 3">
          <el-form-item v-for="(o, i) in options" :key="i" :label="'选项 ' + o.key">
            <el-input v-model="o.text" />
          </el-form-item>
        </template>

        <el-form-item label="答案">
          <el-input v-model="form.answer"
                    :placeholder="form.type === 3 ? '正确填 T，错误填 F' : (form.type === 2 ? '多选如 A,C' : '如 A')" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio :value="1">简单</el-radio>
            <el-radio :value="2">中等</el-radio>
            <el-radio :value="3">困难</el-radio>
          </el-radio-group>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const list = ref<any[]>([])
const dialog = ref(false)
const form = reactive<any>({ id: null, type: 1, title: '', answer: '', analysis: '', difficulty: 1 })
const options = reactive<any[]>([
  { key: 'A', text: '' }, { key: 'B', text: '' }, { key: 'C', text: '' }, { key: 'D', text: '' }
])

function typeName(t: number) {
  return t === 1 ? '单选' : t === 2 ? '多选' : '判断'
}

function resetOptions() {
  options.forEach((o) => (o.text = ''))
}

async function load() {
  list.value = (await request.get('/api/teacher/courses/' + courseId + '/questions')) as any
}

function openEdit(row: any) {
  if (row) {
    Object.assign(form, { ...row })
    resetOptions()
    try {
      const arr = row.optionsJson ? JSON.parse(row.optionsJson) : []
      arr.forEach((o: any, i: number) => {
        if (options[i]) options[i].text = o.text
      })
    } catch (e) {
      // 选项解析失败不影响编辑
    }
  } else {
    Object.assign(form, { id: null, type: 1, title: '', answer: '', analysis: '', difficulty: 1 })
    resetOptions()
  }
  dialog.value = true
}

async function save() {
  if (!form.title) {
    ElMessage.warning('题干不能为空')
    return
  }
  const optionsJson = form.type === 3
    ? null
    : JSON.stringify(options.filter((o) => o.text).map((o) => ({ key: o.key, text: o.text })))
  await request.post('/api/teacher/questions', { ...form, courseId, optionsJson })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该题目？', '提示', { type: 'warning' })
  await request.delete('/api/teacher/questions/' + row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
