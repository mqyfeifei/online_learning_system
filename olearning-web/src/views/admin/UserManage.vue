<template>
  <div class="page-card">
    <el-row :gutter="12" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-input v-model="query.keyword" placeholder="搜索用户名/姓名" clearable @keyup.enter="load(1)" />
      </el-col>
      <el-col :span="5">
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 100%" @change="load(1)">
          <el-option label="学生" value="STUDENT" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-button type="primary" @click="load(1)">查询</el-button>
      </el-col>
    </el-row>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="账号" width="130" />
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column label="角色" width="110">
        <template #default="scope">
          <el-tag size="small" :type="roleType(scope.row.role)">{{ roleText(scope.row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '正常' : '待审核/禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="scope">
          <el-button v-if="scope.row.status !== 1" link type="success" @click="setStatus(scope.row, 1)">启用</el-button>
          <el-button v-else link type="warning" @click="setStatus(scope.row, 0)">禁用</el-button>
          <el-button link type="primary" @click="reset(scope.row)">重置密码</el-button>
          <el-select :model-value="scope.row.role" size="small" style="width: 110px; margin-left: 8px"
                     @change="(v: string) => changeRole(scope.row, v)">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="query.current" :page-size="query.size" :total="total"
                   layout="prev, pager, next, total" style="justify-content: center; margin-top: 12px"
                   @current-change="load()" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const list = ref<any[]>([])
const total = ref(0)
const query = reactive<any>({ keyword: '', role: '', current: 1, size: 10 })

function roleText(r: string) {
  return ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' } as any)[r] || r
}

function roleType(r: string) {
  return ({ STUDENT: 'info', TEACHER: 'success', ADMIN: 'danger' } as any)[r] || 'info'
}

async function load(page?: number) {
  if (page) query.current = page
  const data: any = await request.get('/api/admin/users', { params: query })
  list.value = data.records
  total.value = data.total
}

async function setStatus(row: any, status: number) {
  await request.post('/api/admin/users/' + row.id + '/status', null, { params: { status } })
  ElMessage.success('已更新')
  load()
}

async function reset(row: any) {
  await ElMessageBox.confirm('将 ' + row.username + ' 的密码重置为 123456？', '提示', { type: 'warning' })
  await request.post('/api/admin/users/' + row.id + '/reset')
  ElMessage.success('已重置')
}

async function changeRole(row: any, role: string) {
  await request.post('/api/admin/users/' + row.id + '/role', null, { params: { role } })
  ElMessage.success('角色已更新')
  load()
}

onMounted(() => load())
</script>
