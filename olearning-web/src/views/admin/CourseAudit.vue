<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>课程审核</h1>
        <p>教师提交的课程需要审核通过后才会出现在课程广场</p>
      </div>
      <div class="ap-actions">
        <el-button @click="load(1)">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- 状态筛选 -->
    <section class="ap-toolbar">
      <span
        v-for="s in statusFilters"
        :key="String(s.key)"
        class="chip chip-btn"
        :class="{ 'is-active': status === s.key }"
        @click="setStatus(s.key)"
      >
        {{ s.label }}
      </span>
      <span class="grow" />
      <span class="ol-muted">共 {{ total }} 门</span>
    </section>

    <!-- 课程审核卡 -->
    <div class="list">
      <article v-for="c in list" :key="c.id" class="row">
        <div class="cover" :style="coverStyle(c)">
          <span v-if="!c.cover">{{ c.title }}</span>
        </div>

        <div class="body">
          <div class="r-top">
            <h3>{{ c.title }}</h3>
            <span class="chip" :class="statusClass(c.status)">{{ statusText(c.status) }}</span>
          </div>

          <div class="meta">
            <span><el-icon><User /></el-icon>{{ c.teacherName }}</span>
            <span><el-icon><Files /></el-icon>{{ c.categoryName || '未分类' }}</span>
            <span><el-icon><Notebook /></el-icon>{{ c.chapterCount || 0 }} 章</span>
            <span><el-icon><View /></el-icon>{{ c.viewCount || 0 }}</span>
          </div>

          <p class="desc">{{ c.description || '教师未填写课程简介' }}</p>

          <div v-if="c.status === 4 && c.rejectReason" class="reject">
            <el-icon><WarningFilled /></el-icon>驳回理由：{{ c.rejectReason }}
          </div>
        </div>

        <div class="ops">
          <template v-if="c.status === 1">
            <el-button type="primary" size="small" @click="audit(c, true)">
              <el-icon><Check /></el-icon> 通过
            </el-button>
            <el-button size="small" @click="openReject(c)">
              <el-icon><Close /></el-icon> 驳回
            </el-button>
          </template>
          <span v-else class="ol-muted">无需操作</span>
        </div>
      </article>

      <div v-if="!list.length" class="ol-empty ol-card">
        <span class="icon">✅</span>
        {{ status === 1 ? '没有待审核的课程，都处理完了' : '这个状态下没有课程' }}
      </div>
    </div>

    <div v-if="total > query.size" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.current"
        @current-change="load"
      />
    </div>

    <!-- 驳回 -->
    <el-dialog append-to-body v-model="rejectDialog" title="驳回课程" width="480px">
      <p class="dlg-tip">
        驳回理由会显示在教师的课程管理页，写清楚需要改什么，避免来回拉扯。
      </p>
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="例如：缺少章节内容与课件，请补充后再提交"
      />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" @click="doReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { coverOf, coverStyle } from '@/utils/cover'

const status = ref<number>(1)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive<any>({ current: 1, size: 10 })
const rejectDialog = ref(false)
const rejectReason = ref('')
const currentCourse = ref<any>(null)

const statusFilters = [
  { key: 1, label: '待审核' },
  { key: 2, label: '已发布' },
  { key: 4, label: '已驳回' },
  { key: 3, label: '已下架' },
  { key: 0, label: '草稿' }
]

function statusText(s: number) {
  return ['草稿', '待审核', '已发布', '已下架', '已驳回'][s] || '未知'
}
function statusClass(s: number) {
  return ['', 'chip--amber', 'chip--green', '', 'chip--brand'][s] || ''
}

function setStatus(s: number) {
  status.value = s
  load(1)
}

async function load(page?: number) {
  if (page) query.current = page
  const data: any = await request.get('/api/admin/courses', {
    params: { status: status.value, current: query.current, size: query.size }
  })
  list.value = data.records || []
  total.value = data.total || 0
}

async function audit(row: any, pass: boolean) {
  await ElMessageBox.confirm(
    pass ? `通过《${row.title}》的审核？通过后课程立即上架。` : '',
    '通过审核',
    { type: 'info' }
  )
  await request.post(`/api/admin/courses/${row.id}/audit`, { pass: true })
  ElMessage.success('已通过，课程已上架')
  load()
}

function openReject(row: any) {
  currentCourse.value = row
  rejectReason.value = ''
  rejectDialog.value = true
}

async function doReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  await request.post(`/api/admin/courses/${currentCourse.value.id}/audit`, {
    pass: false,
    reason: rejectReason.value
  })
  ElMessage.success('已驳回')
  rejectDialog.value = false
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.grow {
  flex: 1;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.row {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 16px;
  transition: border-color 0.2s;
}
.row:hover {
  border-color: var(--brand-200);
}
.cover {
  width: 140px;
  height: 92px;
  flex: none;
  border-radius: var(--r-md);
  display: flex;
  align-items: flex-end;
  padding: 11px;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}
.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.r-top {
  display: flex;
  align-items: center;
  gap: 10px;
}
.r-top h3 {
  margin: 0;
  font-size: 15.5px;
  font-weight: 680;
}
.meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: var(--ink-400);
}
.meta span {
  display: flex;
  align-items: center;
  gap: 3px;
}
.desc {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--ink-500);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.reject {
  display: flex;
  align-items: center;
  gap: 5px;
  align-self: flex-start;
  font-size: 11.5px;
  color: #c0392b;
  background: #fdecec;
  padding: 5px 10px;
  border-radius: 8px;
}
.ops {
  display: flex;
  flex-direction: column;
  gap: 7px;
  flex: none;
  min-width: 92px;
  align-items: stretch;
}
.ops :deep(.el-button) {
  margin-left: 0;
}
.pager {
  display: flex;
  justify-content: center;
  padding: 6px 0;
}
.dlg-tip {
  margin: 0 0 12px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-400);
}

@media (max-width: 1000px) {
  .row {
    flex-wrap: wrap;
  }
  .cover {
    width: 100%;
  }
}
</style>
