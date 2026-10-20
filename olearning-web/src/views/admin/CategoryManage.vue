<template>
  <div class="page-card">
    <el-row justify="space-between" style="margin-bottom: 16px">
      <div class="page-title" style="margin: 0">课程分类</div>
      <el-button type="primary" @click="openEdit(null)">新增分类</el-button>
    </el-row>

    <el-table :data="list" stripe row-key="id">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名称" />
      <el-table-column label="上级分类" width="160">
        <template #default="scope">{{ parentName(scope.row.parentId) }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑分类' : '新增分类'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="分类名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option label="无（一级分类）" :value="0" />
            <el-option v-for="c in list.filter((x) => x.parentId === 0)" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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
const form = reactive<any>({ id: null, name: '', parentId: 0, sort: 0 })

function parentName(id: number) {
  if (!id) return '—'
  const p = list.value.find((c) => c.id === id)
  return p ? p.name : '—'
}

async function load() {
  list.value = (await request.get('/api/admin/categories')) as any
}

function openEdit(row: any) {
  Object.assign(form, row || { id: null, name: '', parentId: 0, sort: 0 })
  dialog.value = true
}

async function save() {
  if (!form.name) {
    ElMessage.warning('分类名称不能为空')
    return
  }
  await request.post('/api/admin/categories', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除分类「' + row.name + '」？', '提示', { type: 'warning' })
  await request.delete('/api/admin/categories/' + row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
