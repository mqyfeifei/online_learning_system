<template>
  <div class="page-card">
    <el-radio-group v-model="status" style="margin-bottom: 16px" @change="load(1)">
      <el-radio-button :value="1">待审核</el-radio-button>
      <el-radio-button :value="2">已发布</el-radio-button>
      <el-radio-button :value="4">已驳回</el-radio-button>
      <el-radio-button :value="0">草稿</el-radio-button>
    </el-radio-group>

    <el-table :data="list" stripe>
      <el-table-column prop="title" label="课程" min-width="180" />
      <el-table-column prop="teacherName" label="教师" width="110" />
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column prop="description" label="简介" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <template v-if="scope.row.status === 1">
            <el-button link type="success" @click="audit(scope.row, true)">通过</el-button>
            <el-button link type="danger" @click="openReject(scope.row)">驳回</el-button>
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="list.length === 0" description="没有符合条件的课程" />

    <el-pagination v-model:current-page="query.current" :page-size="query.size" :total="total"
                   layout="prev, pager, next, total" style="justify-content: center; margin-top: 12px"
                   @current-change="load()" />

    <el-dialog v-model="rejectDialog" title="驳回理由" width="460px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因，教师端可见" />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" @click="doReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const status = ref<number>(1)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive<any>({ current: 1, size: 10 })
const rejectDialog = ref(false)
const rejectReason = ref('')
const currentCourse = ref<any>(null)

function statusText(s: number) {
  return ['草稿', '待审核', '已发布', '已下架', '已驳回'][s] || '未知'
}

async function load(page?: number) {
  if (page) query.current = page
  const data: any = await request.get('/api/admin/courses', {
    params: { status: status.value, current: query.current, size: query.size }
  })
  list.value = data.records
  total.value = data.total
}

async function audit(row: any, pass: boolean) {
  await request.post('/api/admin/courses/' + row.id + '/audit', { pass })
  ElMessage.success(pass ? '已通过，课程已发布' : '已驳回')
  load()
}

function openReject(row: any) {
  currentCourse.value = row
  rejectReason.value = ''
  rejectDialog.value = true
}

async function doReject() {
  await request.post('/api/admin/courses/' + currentCourse.value.id + '/audit', {
    pass: false,
    reason: rejectReason.value
  })
  ElMessage.success('已驳回')
  rejectDialog.value = false
  load()
}

onMounted(() => load())
</script>
