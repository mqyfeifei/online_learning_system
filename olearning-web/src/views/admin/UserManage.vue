<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>用户管理</h1>
        <p>共 {{ total }} 个账号 · 教师账号注册后需在此启用</p>
      </div>
      <div class="ap-actions">
        <el-button @click="load(1)">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
        <el-button type="primary" @click="openImport">
          <el-icon><UploadFilled /></el-icon> 批量导入
        </el-button>
      </div>
    </div>

    <!-- 筛选 -->
    <section class="ap-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索用户名或姓名"
        clearable
        style="width: 240px"
        :prefix-icon="Search"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <span
        v-for="r in roleFilters"
        :key="r.key"
        class="chip chip-btn"
        :class="{ 'is-active': query.role === r.key }"
        @click="setRole(r.key)"
      >
        {{ r.label }}
      </span>
      <span class="grow" />
      <el-button type="primary" @click="load(1)">查询</el-button>
    </section>

    <!-- 表格 -->
    <section class="ap-card">
      <div class="ap-card-body">
        <el-table v-loading="loading" :data="list" style="width: 100%">
          <el-table-column label="用户" min-width="190">
            <template #default="{ row }">
              <div class="user">
                <span class="avatar" :style="{ background: avatarColor(row.id) }">
                  {{ (row.realName || row.username || '?').slice(0, 1) }}
                </span>
                <div>
                  <b>{{ row.realName || '未填写姓名' }}</b>
                  <span>@{{ row.username }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="角色" width="130">
            <template #default="{ row }">
              <span class="chip" :class="roleClass(row.role)">{{ roleText(row.role) }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="phone" label="手机号" width="150">
            <template #default="{ row }">
              {{ row.phone || '—' }}
            </template>
          </el-table-column>

          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <span class="status" :class="{ off: row.status !== 1 }">
                <i />{{ row.status === 1 ? '正常' : (row.role === 'TEACHER' ? '待启用' : '已禁用') }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="注册时间" width="160">
            <template #default="{ row }">{{ fmt(row.createTime) }}</template>
          </el-table-column>

          <el-table-column label="操作" width="230" align="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status !== 1"
                link
                type="success"
                @click="setStatus(row, 1)"
              >
                启用
              </el-button>
              <el-button v-else link type="warning" @click="setStatus(row, 0)">禁用</el-button>
              <el-button link type="primary" @click="reset(row)">重置密码</el-button>
              <el-dropdown trigger="click">
                <el-button link type="primary">
                  改角色<el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="r in roleOptions"
                      :key="r.value"
                      :disabled="row.role === r.value"
                      @click="changeRole(row, r.value)"
                    >
                      设为{{ r.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!list.length && !loading" class="ol-empty" style="padding: 44px 0">
          没有匹配的用户
        </div>
      </div>

      <div v-if="total > query.size" class="ap-pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="query.size"
          :current-page="query.current"
          @current-change="load"
        />
      </div>
    </section>

    <!-- 批量导入用户 -->
    <el-dialog append-to-body v-model="importDialog" title="批量导入用户" width="680px" top="6vh">
      <div class="imp-spec">
        <b>格式要求</b>
        <ul>
          <li>每行一个用户，逗号分隔，列顺序固定：<code>用户名,真实姓名,角色,手机号,初始密码</code></li>
          <li>用户名：4–20 位字母、数字或下划线，不能与已有账号重复</li>
          <li>角色：只能填 <code>STUDENT</code> 或 <code>TEACHER</code>（教师导入后仍需在列表里启用）</li>
          <li>手机号可留空；初始密码可留空，默认 <code>123456</code></li>
          <li>首行表头可以保留，导入时会自动跳过</li>
        </ul>
        <el-button size="small" plain @click="downloadTemplate">
          <el-icon><Download /></el-icon> 下载 CSV 模板
        </el-button>
      </div>

      <div class="imp-toolbar">
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".csv,.txt"
          :on-change="readImportFile"
        >
          <el-button plain>
            <el-icon><Document /></el-icon> 选择文件（csv / txt）
          </el-button>
        </el-upload>
        <span class="ol-muted">也可以在下面直接粘贴内容</span>
      </div>

      <el-input
        v-model="importText"
        type="textarea"
        :rows="8"
        placeholder="student3,张三,STUDENT,13800000005,123456"
      />

      <div v-if="importResult" class="imp-result">
        <div class="ir-head">
          <span class="chip chip--green">成功 {{ importResult.successCount }}</span>
          <span class="chip chip--brand">失败 {{ importResult.failedCount }}</span>
        </div>
        <div v-if="importResult.failed.length" class="ir-list">
          <div v-for="(f, i) in importResult.failed" :key="i" class="ir-row">
            第 {{ f.line }} 行 · {{ f.username || '（空）' }} —— {{ f.reason }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="importDialog = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { avatarColor } from '@/utils/cover'
import { triggerDownload } from '@/utils/file'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive<any>({ keyword: '', role: '', current: 1, size: 10 })

const roleFilters = [
  { key: '', label: '全部角色' },
  { key: 'STUDENT', label: '学生' },
  { key: 'TEACHER', label: '教师' },
  { key: 'ADMIN', label: '管理员' }
]

const roleOptions = [
  { value: 'STUDENT', label: '学生' },
  { value: 'TEACHER', label: '教师' },
  { value: 'ADMIN', label: '管理员' }
]

// ---------------- 批量导入 ----------------
const importDialog = ref(false)
const importText = ref('')
const importing = ref(false)
const importResult = ref<any>(null)

function openImport() {
  importText.value = ''
  importResult.value = null
  importDialog.value = true
}

function downloadTemplate() {
  triggerDownload('/api/admin/users/import-template')
}

/** 选本地文件后读成文本填进输入框，便于用户先检查再提交 */
function readImportFile(file: any) {
  const raw: File = file.raw
  if (!raw) return
  const reader = new FileReader()
  reader.onload = () => {
    importText.value = String(reader.result || '').replace(/^\uFEFF/, '')
    ElMessage.success(`已读取 ${raw.name}`)
  }
  reader.readAsText(raw, 'utf-8')
}

async function doImport() {
  if (!importText.value.trim()) {
    ElMessage.warning('请粘贴或选择要导入的内容')
    return
  }
  importing.value = true
  try {
    importResult.value = await request.post('/api/admin/users/import', { text: importText.value })
    ElMessage.success(`导入完成：成功 ${importResult.value.successCount} 个账号`)
    load(1)
  } finally {
    importing.value = false
  }
}

function roleText(r: string) {
  return ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' } as any)[r] || r
}
function roleClass(r: string) {
  return (
    ({ STUDENT: 'chip--blue', TEACHER: 'chip--green', ADMIN: 'chip--violet' } as any)[r] || ''
  )
}

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

function setRole(role: string) {
  query.role = role
  load(1)
}

async function load(page?: number) {
  if (page) query.current = page
  loading.value = true
  try {
    const data: any = await request.get('/api/admin/users', { params: query })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function setStatus(row: any, status: number) {
  const label = status === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${label}账号「${row.username}」？`, `${label}账号`, {
    type: 'warning'
  })
  await request.post(`/api/admin/users/${row.id}/status`, null, { params: { status } })
  ElMessage.success(`已${label}`)
  load()
}

async function reset(row: any) {
  await ElMessageBox.confirm(
    `将「${row.username}」的密码重置为 123456？请提醒该用户登录后修改。`,
    '重置密码',
    { type: 'warning' }
  )
  await request.post(`/api/admin/users/${row.id}/reset`)
  ElMessage.success('密码已重置为 123456')
}

async function changeRole(row: any, role: string) {
  await ElMessageBox.confirm(
    `把「${row.username}」的角色改为${roleText(role)}？权限会立即生效。`,
    '修改角色',
    { type: 'warning' }
  )
  await request.post(`/api/admin/users/${row.id}/role`, null, { params: { role } })
  ElMessage.success('角色已更新')
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.grow {
  flex: 1;
}
.user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}
.user b {
  display: block;
  font-size: 13px;
  font-weight: 620;
}
.user span {
  font-size: 11px;
  color: var(--ink-400);
}
.status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  color: #1f7a4f;
  font-weight: 550;
}
.status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--green);
}
.status.off {
  color: var(--ink-400);
}
.status.off i {
  background: var(--ink-300);
}

/* ---------- 批量导入 ---------- */
.imp-spec {
  background: var(--surface-soft);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 14px 16px;
  margin-bottom: 14px;
}
.imp-spec b {
  font-size: 13px;
  font-weight: 680;
}
.imp-spec ul {
  margin: 8px 0 10px;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.imp-spec li {
  font-size: 12px;
  color: var(--ink-500);
  line-height: 1.7;
}
.imp-spec code {
  background: var(--surface-sunken);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 11.5px;
  color: var(--brand-700);
}
.imp-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.imp-result {
  margin-top: 14px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 12px 14px;
  background: var(--surface-soft);
}
.ir-head {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.ir-list {
  max-height: 160px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ir-row {
  font-size: 12px;
  color: #b4453b;
  line-height: 1.7;
}
</style>
