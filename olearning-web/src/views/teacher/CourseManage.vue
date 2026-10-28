<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>我的课程</h1>
        <p>新建课程后需提交审核，管理员通过后才会出现在课程广场</p>
      </div>
      <div class="ap-actions">
        <el-button type="primary" @click="openEdit(null)">
          <el-icon><Plus /></el-icon> 新建课程
        </el-button>
      </div>
    </div>

    <!-- 状态筛选 -->
    <section class="ap-toolbar">
      <span
        v-for="(f, i) in filters"
        :key="i"
        class="chip chip-btn"
        :class="{ 'is-active': status === f.key }"
        @click="status = f.key"
      >
        {{ f.label }}
        <i v-if="countOf(f.key) !== null" class="cnt">{{ countOf(f.key) }}</i>
      </span>
      <span class="grow" />
      <span class="ol-muted">共 {{ filtered.length }} 门</span>
    </section>

    <!-- 课程列表 -->
    <div class="list">
      <article v-for="c in filtered" :key="c.id" class="row">
        <div
          class="cover"
          :style="coverStyle(c)"
        >
          <span v-if="!c.cover">{{ c.title }}</span>
        </div>

        <div class="body">
          <div class="r-top">
            <h3>{{ c.title }}</h3>
            <span class="chip" :class="statusClass(c.status)">{{ statusText(c.status) }}</span>
            <span v-if="c.pendingApplyCount" class="chip chip--violet">
              {{ c.pendingApplyCount }} 人选课待审核
            </span>
          </div>
          <p class="desc">{{ c.description || '暂无课程简介' }}</p>
          <div class="meta">
            <span><el-icon><Files /></el-icon>{{ c.categoryName || '未分类' }}</span>
            <span><el-icon><Notebook /></el-icon>{{ c.chapterCount || 0 }} 章</span>
            <span><el-icon><User /></el-icon>{{ c.studentCount || 0 }} 人</span>
            <span><el-icon><View /></el-icon>{{ c.viewCount || 0 }}</span>
          </div>
          <div v-if="c.status === 4 && c.rejectReason" class="reject">
            <el-icon><WarningFilled /></el-icon>驳回理由：{{ c.rejectReason }}
          </div>
        </div>

        <div class="ops">
          <el-button size="small" @click="go(c, 'chapters')">
            <el-icon><FolderOpened /></el-icon> 章节课件
          </el-button>
          <el-button size="small" @click="go(c, 'assignments')">
            <el-icon><Document /></el-icon> 作业
          </el-button>
          <el-button size="small" @click="go(c, 'questions')">
            <el-icon><Collection /></el-icon> 题库
          </el-button>
          <el-button size="small" @click="go(c, 'papers')">
            <el-icon><EditPen /></el-icon> 试卷
          </el-button>
          <el-button size="small" @click="go(c, 'students')">
            <el-icon><UserFilled /></el-icon> 学生
          </el-button>

          <el-dropdown trigger="click">
            <el-button size="small" type="primary" plain>
              更多<el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openEdit(c)">
                  <el-icon><Edit /></el-icon> 编辑信息
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="c.status !== 1 && c.status !== 2"
                  @click="submitAudit(c)"
                >
                  <el-icon><Promotion /></el-icon> 提交审核
                </el-dropdown-item>
                <el-dropdown-item v-if="c.status === 2" @click="offline(c)">
                  <el-icon><Download /></el-icon> 下架课程
                </el-dropdown-item>
                <el-dropdown-item divided @click="remove(c)">
                  <span style="color: #c0392b">删除课程</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </article>

      <div v-if="!filtered.length" class="ol-empty ol-card">
        <span class="icon">📘</span>
        这个状态下没有课程
      </div>
    </div>

    <!-- 新建 / 编辑 -->
    <el-dialog append-to-body v-model="dialog" :title="form.id ? '编辑课程' : '新建课程'" width="580px">
      <el-form label-position="top">
        <el-form-item label="课程名称">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="例如：计算机网络" />
        </el-form-item>
        <el-form-item label="课程分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程封面（可选）">
          <div class="cover-pick">
            <div v-if="form.cover" class="cp-thumb">
              <img :src="form.cover" alt="封面预览" />
              <button type="button" class="cp-del" title="移除封面" @click="form.cover = ''">
                <el-icon><Close /></el-icon>
              </button>
            </div>
            <div v-else class="cp-thumb cp-empty" :style="{ background: coverOf(form.id || 0) }">
              渐变色封面
            </div>
            <div class="cp-side">
              <FileUploader
                accept="image/*"
                label="选择图片"
                :max-size="5"
                size="small"
                @uploaded="(d) => (form.cover = d.url)"
              />
              <span class="cp-tip">建议 16:9 横图，留空则使用按课程 id 配的渐变色</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="写清课程内容、配套资料与适合人群"
          />
        </el-form-item>

        <div class="settings">
          <div class="set-head">
            <b>课程设置</b>
            <span>决定学生选课、看课件与导出的权限</span>
          </div>

          <div class="set-row">
            <div class="set-text">
              <b>选课需要审核</b>
              <span>开启后学生选课需你在「学生与成绩」里审批通过</span>
            </div>
            <el-switch v-model="form.enrollAudit" :active-value="1" :inactive-value="0" />
          </div>

          <div class="set-row">
            <div class="set-text">
              <b>允许下载课件资料</b>
              <span>关闭后学生只能在线查看 PDF / PPT / Word，不提供下载</span>
            </div>
            <el-switch v-model="form.resourceDownloadable" :active-value="1" :inactive-value="0" />
          </div>

          <div class="set-row set-row--column">
            <div class="set-text">
              <b>作业与试卷导出范围</b>
              <span>控制哪些人能把作业、试卷导出为 docx 文件</span>
            </div>
            <el-radio-group v-model="form.exportScope" size="small">
              <el-radio-button value="ENROLLED">仅已选课学生</el-radio-button>
              <el-radio-button value="ALL">平台用户</el-radio-button>
              <el-radio-button value="NONE">不允许导出</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { coverOf, coverStyle } from '@/utils/cover'
import FileUploader from '@/components/FileUploader.vue'

const router = useRouter()

const list = ref<any[]>([])
const categories = ref<any[]>([])
const dialog = ref(false)
const status = ref<number | null>(null)
const form = ref<any>({
  id: null,
  title: '',
  categoryId: null,
  cover: '',
  description: '',
  enrollAudit: 0,
  resourceDownloadable: 1,
  exportScope: 'ENROLLED'
})

const filters = [
  { key: null, label: '全部' },
  { key: 2, label: '已发布' },
  { key: 0, label: '草稿' },
  { key: 1, label: '待审核' },
  { key: 3, label: '已下架' },
  { key: 4, label: '已驳回' }
]

const filtered = computed(() =>
  status.value === null ? list.value : list.value.filter((c) => c.status === status.value)
)

function countOf(key: number | null) {
  if (key === null) return null
  return list.value.filter((c) => c.status === key).length || null
}

function statusText(s: number) {
  return ['草稿', '待审核', '已发布', '已下架', '审核驳回'][s] || '未知'
}
function statusClass(s: number) {
  return (
    ['', 'chip--amber', 'chip--green', '', 'chip--brand'][s] || ''
  )
}

async function load() {
  list.value = ((await request.get('/api/teacher/courses')) as any) || []
}

function openEdit(row: any) {
  form.value = row
    ? {
        id: row.id,
        title: row.title,
        categoryId: row.categoryId,
        cover: row.cover || '',
        description: row.description || '',
        enrollAudit: row.enrollAudit ?? 0,
        resourceDownloadable: row.resourceDownloadable ?? 1,
        exportScope: row.exportScope || 'ENROLLED'
      }
    : {
        id: null,
        title: '',
        categoryId: categories.value[0]?.id || null,
        cover: '',
        description: '',
        enrollAudit: 0,
        resourceDownloadable: 1,
        exportScope: 'ENROLLED'
      }
  dialog.value = true
}

async function save() {
  if (!form.value.title.trim()) {
    ElMessage.warning('课程名称不能为空')
    return
  }
  await request.post('/api/teacher/courses', form.value)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function submitAudit(row: any) {
  await request.post(`/api/teacher/courses/${row.id}/submit`)
  ElMessage.success('已提交审核，等待管理员处理')
  load()
}

async function offline(row: any) {
  await ElMessageBox.confirm(`下架后《${row.title}》将从课程广场移除，已选课学生仍可继续学习。`, '下架课程', {
    type: 'warning',
    confirmButtonText: '确认下架'
  })
  await request.post(`/api/teacher/courses/${row.id}/offline`)
  ElMessage.success('已下架')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除《${row.title}》？章节、题库与作业会一起删除。`, '删除课程', {
    type: 'warning',
    confirmButtonText: '删除',
    confirmButtonClass: 'el-button--danger'
  })
  await request.delete(`/api/teacher/courses/${row.id}`)
  ElMessage.success('已删除')
  load()
}

function go(row: any, tab: string) {
  router.push(`/teacher/courses/${row.id}/${tab}`)
}

onMounted(async () => {
  categories.value = ((await request.get('/api/teacher/categories')) as any) || []
  load()
})
</script>

<style scoped>
.cnt {
  font-style: normal;
  font-size: 10.5px;
  font-weight: 700;
  margin-left: 4px;
  opacity: 0.7;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.row {
  display: flex;
  gap: 18px;
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 16px;
  transition: all 0.2s;
}
.row:hover {
  border-color: var(--brand-200);
  box-shadow: var(--sh-sm);
}
.cover {
  width: 148px;
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
  gap: 7px;
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.desc {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-400);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
.reject {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: #c0392b;
  background: #fdecec;
  padding: 5px 10px;
  border-radius: 8px;
  align-self: flex-start;
}
.ops {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: none;
  align-items: stretch;
}
.ops :deep(.el-button) {
  margin-left: 0;
  justify-content: flex-start;
  width: 118px;
}
.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* 封面上传 */
.cover-pick {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  width: 100%;
}
.cp-thumb {
  width: 132px;
  height: 74px;
  flex: none;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  border: 1px solid var(--ink-100);
  display: grid;
  place-items: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  letter-spacing: 0.5px;
}
.cp-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cp-del {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 11px;
}
.cp-side {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.cp-tip {
  font-size: 11.5px;
  color: var(--ink-300);
  line-height: 1.5;
}

/* 课程设置区 */
.settings {
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  background: var(--surface-soft);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.set-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.set-head b {
  font-size: 13.5px;
  font-weight: 680;
}
.set-head span {
  font-size: 11.5px;
  color: var(--ink-400);
}
.set-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.set-row--column {
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.set-text b {
  display: block;
  font-size: 13px;
  font-weight: 600;
}
.set-text span {
  font-size: 11.5px;
  color: var(--ink-400);
}

@media (max-width: 1180px) {
  .row {
    flex-wrap: wrap;
  }
  .cover {
    width: 100%;
  }
  .ops {
    flex-direction: row;
    flex-wrap: wrap;
  }
  .ops :deep(.el-button) {
    width: auto;
  }
}
</style>
