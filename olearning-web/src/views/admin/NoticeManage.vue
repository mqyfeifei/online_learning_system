<template>
  <div class="page-card">
    <el-row justify="space-between" style="margin-bottom: 16px">
      <div class="page-title" style="margin: 0">系统公告</div>
      <el-button type="primary" @click="openEdit(null)">发布公告</el-button>
    </el-row>

    <el-table :data="list" stripe>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
      <el-table-column label="置顶" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.topFlag === 1" type="danger" size="small">置顶</el-tag>
          <span v-else class="muted">否</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '显示' : '已撤下' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="warning" @click="toggle(scope.row)">
            {{ scope.row.status === 1 ? '撤下' : '显示' }}
          </el-button>
          <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="list.length === 0" description="还没有公告" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑公告' : '发布公告'" width="560px">
      <el-form label-width="70px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.topFlag" :active-value="1" :inactive-value="0" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const list = ref<any[]>([])
const dialog = ref(false)
const form = reactive<any>({ id: null, title: '', content: '', topFlag: 0 })

async function load() {
  list.value = (await request.get('/api/admin/notices')) as any
}

function openEdit(row: any) {
  Object.assign(form, row || { id: null, title: '', content: '', topFlag: 0 })
  dialog.value = true
}

async function save() {
  if (!form.title) {
    ElMessage.warning('标题不能为空')
    return
  }
  await request.post('/api/admin/notices', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function toggle(row: any) {
  const next = row.status === 1 ? 0 : 1
  await request.post('/api/admin/notices/' + row.id + '/status', null, { params: { status: next } })
  ElMessage.success('已更新')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除公告？', '提示', { type: 'warning' })
  await request.delete('/api/admin/notices/' + row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
