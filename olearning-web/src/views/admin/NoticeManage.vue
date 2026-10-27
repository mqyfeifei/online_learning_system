<template>
  <div class="admin-page">
    <div class="ap-head">
      <div>
        <h1>系统公告</h1>
        <p>公告会展示在学员端首页与顶栏通知里，置顶公告排在列表最前。</p>
      </div>
      <div class="ap-actions">
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">发布公告</el-button>
      </div>
    </div>

    <div class="ap-stats">
      <div class="ap-stat">
        <div class="ico ico--brand"><el-icon><Bell /></el-icon></div>
        <div><b>{{ list.length }}</b><span>公告总数</span></div>
      </div>
      <div class="ap-stat">
        <div class="ico ico--green"><el-icon><View /></el-icon></div>
        <div><b>{{ list.filter((n) => n.status === 1).length }}</b><span>展示中</span></div>
      </div>
      <div class="ap-stat">
        <div class="ico ico--amber"><el-icon><Top /></el-icon></div>
        <div><b>{{ list.filter((n) => n.topFlag === 1).length }}</b><span>置顶</span></div>
      </div>
      <div class="ap-stat">
        <div class="ico ico--violet"><el-icon><Hide /></el-icon></div>
        <div><b>{{ list.filter((n) => n.status !== 1).length }}</b><span>已撤下</span></div>
      </div>
    </div>

    <div class="ap-toolbar">
      <div class="grow">
        <button
          v-for="f in filters"
          :key="f.key"
          class="chip-btn"
          :class="{ 'is-active': filter === f.key }"
          @click="filter = f.key"
        >
          {{ f.label }}
          <em>{{ countOf(f.key) }}</em>
        </button>
      </div>
      <el-input v-model="keyword" placeholder="搜索标题或内容" clearable style="width: 240px">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <div class="notice-list" v-loading="loading">
      <div
        v-for="n in view"
        :key="n.id"
        class="notice-card fade-up"
        :class="{ 'is-off': n.status !== 1, 'is-top': n.topFlag === 1 }"
      >
        <div class="n-bar"></div>
        <div class="n-main">
          <div class="n-head">
            <span v-if="n.topFlag === 1" class="chip chip--amber"><el-icon><Top /></el-icon>置顶</span>
            <span class="chip" :class="n.status === 1 ? 'chip--green' : ''">
              {{ n.status === 1 ? '展示中' : '已撤下' }}
            </span>
            <b class="n-title">{{ n.title }}</b>
          </div>
          <p class="n-content">{{ n.content }}</p>
          <div class="n-meta">
            <span><el-icon><Clock /></el-icon>{{ fmt(n.createTime) }}</span>
            <span>ID {{ n.id }}</span>
          </div>
        </div>
        <div class="n-ops">
          <el-button link type="primary" :icon="EditPen" @click="openEdit(n)">编辑</el-button>
          <el-button link type="warning" :icon="n.status === 1 ? Hide : View" @click="toggle(n)">
            {{ n.status === 1 ? '撤下' : '显示' }}
          </el-button>
          <el-button link :type="n.topFlag === 1 ? 'info' : 'warning'" :icon="Top" @click="toggleTop(n)">
            {{ n.topFlag === 1 ? '取消置顶' : '置顶' }}
          </el-button>
          <el-button link type="danger" :icon="Delete" @click="remove(n)">删除</el-button>
        </div>
      </div>

      <div v-if="!view.length && !loading" class="ol-empty">
        <div class="icon"><el-icon><Bell /></el-icon></div>
        <p>{{ keyword ? '没有匹配的公告' : '还没有公告，点右上角发布一条' }}</p>
      </div>
    </div>

    <el-dialog append-to-body v-model="dialog" :title="form.id ? '编辑公告' : '发布公告'" width="560px">
      <el-form label-width="70px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="一句话说清公告主题" maxlength="40" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            placeholder="补充具体安排，如停课时间、作业截止调整等"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.topFlag" :active-value="1" :inactive-value="0" />
          <span class="ol-muted" style="margin-left: 10px; font-size: 12px">置顶公告会显示在最前面</span>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, Clock, Delete, EditPen, Hide, Plus, Search, Top, View
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const list = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const filter = ref('all')
const keyword = ref('')
// status 要一起带上：后端 save 对 status 为空会默认置 1，编辑「已撤下」公告时会把它放出去
const form = reactive<any>({ id: null, title: '', content: '', topFlag: 0, status: 1 })

const filters = [
  { key: 'all', label: '全部' },
  { key: 'on', label: '展示中' },
  { key: 'off', label: '已撤下' },
  { key: 'top', label: '置顶' }
]

function countOf(key: string) {
  if (key === 'all') return list.value.length
  if (key === 'on') return list.value.filter((n) => n.status === 1).length
  if (key === 'off') return list.value.filter((n) => n.status !== 1).length
  return list.value.filter((n) => n.topFlag === 1).length
}

const view = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let arr = list.value
  if (filter.value === 'on') arr = arr.filter((n) => n.status === 1)
  if (filter.value === 'off') arr = arr.filter((n) => n.status !== 1)
  if (filter.value === 'top') arr = arr.filter((n) => n.topFlag === 1)
  if (kw) arr = arr.filter((n) => (n.title + n.content).toLowerCase().includes(kw))
  // 置顶优先，其次新发布的在前
  return [...arr].sort(
    (a, b) => (b.topFlag ?? 0) - (a.topFlag ?? 0) || String(b.id).localeCompare(String(a.id)) || b.id - a.id
  )
})

function fmt(t: string) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    list.value = ((await request.get('/api/admin/notices')) as any[]) || []
  } finally {
    loading.value = false
  }
}

function openEdit(row: any) {
  Object.assign(
    form,
    row
      ? { id: row.id, title: row.title, content: row.content, topFlag: row.topFlag ?? 0, status: row.status ?? 1 }
      : { id: null, title: '', content: '', topFlag: 0, status: 1 }
  )
  dialog.value = true
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  await request.post('/api/admin/notices', form)
  ElMessage.success(form.id ? '已更新' : '已发布')
  dialog.value = false
  load()
}

async function toggle(row: any) {
  const next = row.status === 1 ? 0 : 1
  await request.post('/api/admin/notices/' + row.id + '/status', null, { params: { status: next } })
  ElMessage.success(next === 1 ? '已显示' : '已撤下')
  load()
}

async function toggleTop(row: any) {
  // 置顶位存在公告本体上，走同一个 save 接口（带 id 即为更新）。
  // 必须带上 status：后端对 status 为空会默认置为 1，会把「已撤下」的公告又放出去。
  const next = row.topFlag === 1 ? 0 : 1
  await request.post('/api/admin/notices', {
    id: row.id,
    title: row.title,
    content: row.content,
    topFlag: next,
    status: row.status ?? 1
  })
  ElMessage.success(next === 1 ? '已置顶' : '已取消置顶')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除公告「${row.title}」？`, '删除确认', { type: 'warning' })
  await request.delete('/api/admin/notices/' + row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.notice-card {
  position: relative;
  display: flex;
  gap: 16px;
  padding: 16px 18px 16px 22px;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  box-shadow: var(--sh-sm);
  transition: box-shadow 0.18s, transform 0.18s;
}
.notice-card:hover {
  box-shadow: var(--sh-md);
  transform: translateY(-1px);
}
.notice-card.is-off {
  background: var(--surface-sunken);
}
.notice-card.is-off .n-title,
.notice-card.is-off .n-content {
  color: var(--ink-400);
}

/* 左侧色条：置顶为琥珀色，撤下为灰 */
.n-bar {
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 4px;
  border-radius: 0 4px 4px 0;
  background: var(--brand);
}
.notice-card.is-top .n-bar {
  background: var(--amber);
}
.notice-card.is-off .n-bar {
  background: var(--ink-200);
}

.n-main {
  flex: 1;
  min-width: 0;
}
.n-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.n-title {
  font-size: 15px;
  color: var(--ink-900);
}
.n-content {
  margin: 8px 0 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-700);
  white-space: pre-wrap;
  word-break: break-word;
}
.n-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--ink-400);
}
.n-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.n-ops {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 2px;
  flex: none;
}

@media (max-width: 900px) {
  .notice-card {
    flex-direction: column;
  }
  .n-ops {
    flex-direction: row;
    align-items: center;
  }
}
</style>
