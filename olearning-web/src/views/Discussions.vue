<template>
  <div class="disc fade-up">
    <!-- 工具栏 -->
    <section class="toolbar">
      <div class="tb-search">
        <el-icon><Search /></el-icon>
        <input v-model="keyword" placeholder="搜索帖子标题" @keyup.enter="load(1)" />
      </div>
      <span class="ol-muted">{{ total }} 个话题</span>
      <el-button type="primary" @click="openPost">
        <el-icon><EditPen /></el-icon> 发个帖子
      </el-button>
    </section>

    <!-- 课程标签筛选 -->
    <section class="tags-bar">
      <button class="tag-chip" :class="{ on: !filterCourseId }" @click="setCourse(null)">
        全部课程
      </button>
      <button
        v-for="c in courseTags"
        :key="c.courseId"
        class="tag-chip"
        :class="{ on: filterCourseId === c.courseId }"
        @click="setCourse(c.courseId)"
      >
        {{ c.courseTitle }}<i>{{ c.count }}</i>
      </button>
      <span v-if="!courseTags.length" class="ol-muted">还没有带课程标签的帖子</span>
    </section>

    <div class="layout">
      <!-- ==================== 帖子列表 ==================== -->
      <main v-loading="loading" class="list">
        <article
          v-for="item in posts"
          :key="item.post.id"
          class="post"
          @click="openDetail(item.post.id)"
        >
          <div class="avatar" :style="{ background: avatarColor(item.post.id) }">
            {{ (item.authorName || '匿').slice(0, 1) }}
          </div>
          <div class="p-body">
            <div class="p-top">
              <span class="course-tag">
                <el-icon><Collection /></el-icon>{{ item.courseTitle || '未关联课程' }}
              </span>
              <h3>{{ item.post.title }}</h3>
            </div>
            <p class="excerpt">{{ item.post.content }}</p>
            <div class="p-meta">
              <span>{{ item.authorName }}</span>
              <span class="dot">·</span>
              <span>{{ fmt(item.post.createTime) }}</span>
              <span class="stat"><el-icon><ChatDotRound /></el-icon>{{ item.replyCount || 0 }}</span>
              <span class="stat" :class="{ helped: item.helped }">
                <el-icon><Pointer /></el-icon>{{ item.post.helpfulCount || 0 }} 有帮助
              </span>
            </div>
          </div>
          <el-icon class="arrow"><ArrowRight /></el-icon>
        </article>

        <div v-if="!loading && !posts.length" class="ol-empty ol-card">
          <span class="icon">💬</span>
          {{ filterCourseId ? '这门课还没有帖子，来发第一个' : '还没有帖子，第一个来提问吧' }}
        </div>

        <div v-if="total > size" class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="size"
            :current-page="current"
            @current-change="load"
          />
        </div>
      </main>

      <!-- ==================== 侧栏 ==================== -->
      <aside class="side">
        <section class="ol-card">
          <div class="ol-card-title">
            <h2 class="ol-h2">讨论指引</h2>
          </div>
          <ul class="guide">
            <li><b>先搜后问</b>：搜索关键词，很多问题已经有人问过</li>
            <li><b>写清上下文</b>：贴上报错信息、已经试过的方法</li>
            <li><b>一个帖子一个问题</b>：方便后来的人检索</li>
          </ul>
        </section>

        <section class="ol-card">
          <div class="ol-card-title">
            <h2 class="ol-h2">热门话题</h2>
          </div>
          <div class="hot-list">
            <div
              v-for="(item, i) in hot"
              :key="item.post.id"
              class="hot-row"
              @click="openDetail(item.post.id)"
            >
              <span class="hot-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
              <div class="hot-body">
                <b>{{ item.post.title }}</b>
                <span class="ol-muted">{{ item.courseTitle || '未关联课程' }}</span>
              </div>
            </div>
            <div v-if="!hot.length" class="ol-empty" style="padding: 16px 0">暂无数据</div>
          </div>
        </section>
      </aside>
    </div>

    <!-- ==================== 发帖 ==================== -->
    <el-dialog append-to-body v-model="postDialog" title="发个帖子" width="560px">
      <el-form label-position="top">
        <el-form-item label="所属课程">
          <el-select v-model="form.courseId" placeholder="选择课程" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.title" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="一句话说清问题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="1000"
            show-word-limit
            placeholder="描述你的问题：背景、现象、已经试过什么"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="postDialog = false">取消</el-button>
        <el-button type="primary" :loading="posting" @click="submitPost">发布</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 帖子详情 ==================== -->
    <el-drawer v-model="detailDrawer" append-to-body size="620px" :with-header="false">
      <div v-if="detail" class="detail">
        <div class="d-head">
          <div class="avatar" :style="{ background: avatarColor(detail.post.id) }">
            {{ (detail.authorName || '匿').slice(0, 1) }}
          </div>
          <div class="d-meta">
            <b>{{ detail.authorName }}</b>
            <span>{{ fmt(detail.post.createTime) }}</span>
          </div>
          <el-button
            v-if="canDelete(detail.post)"
            text
            type="danger"
            size="small"
            @click="removePost(detail.post.id)"
          >
            删除
          </el-button>
        </div>

        <div class="d-course">
          <el-icon><Collection /></el-icon>
          {{ detail.courseTitle || '未关联课程' }}
        </div>

        <h2 class="d-title">{{ detail.post.title }}</h2>
        <p class="d-content">{{ detail.post.content }}</p>

        <div class="d-actions">
          <button
            class="like"
            :class="{ on: detail.helped }"
            :disabled="detail.helped"
            @click="markHelpful"
          >
            <el-icon><Pointer /></el-icon>
            {{ detail.helped ? '已标记有帮助' : '有帮助' }} {{ detail.post.helpfulCount || 0 }}
          </button>
          <span v-if="detail.helped" class="ol-muted">每个帖子只能标记一次</span>
        </div>

        <div class="replies">
          <h3>全部回复 <i>{{ detail.replies?.length || 0 }}</i></h3>
          <div v-for="r in detail.replies" :key="r.reply.id" class="reply">
            <div class="r-avatar" :style="{ background: avatarColor(r.reply.id + 3) }">
              {{ (r.authorName || '匿').slice(0, 1) }}
            </div>
            <div class="r-body">
              <div class="r-top">
                <b>{{ r.authorName }}</b>
                <span>{{ fmt(r.reply.createTime) }}</span>
              </div>
              <p>{{ r.reply.content }}</p>
            </div>
          </div>
          <div v-if="!detail.replies?.length" class="ol-empty" style="padding: 20px 0">
            还没有人回复，来说点什么
          </div>
        </div>

        <div class="reply-box">
          <textarea v-model="replyText" rows="3" placeholder="写下你的回复…" class="reply-input" />
          <el-button type="primary" :disabled="!replyText.trim()" @click="sendReply">
            回复
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { avatarColor } from '@/utils/cover'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const posts = ref<any[]>([])
const hot = ref<any[]>([])
const courses = ref<any[]>([])
/** 课程标签：只列有帖子的课程，点一下就按课程筛选 */
const courseTags = ref<any[]>([])
const filterCourseId = ref<number | null>(null)
const total = ref(0)
const current = ref(1)
const size = 8
const keyword = ref('')
const loading = ref(false)

const postDialog = ref(false)
const posting = ref(false)
const form = ref<any>({ courseId: null, title: '', content: '' })

const detailDrawer = ref(false)
const detail = ref<any>(null)
const replyText = ref('')

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

async function load(page = 1) {
  loading.value = true
  current.value = page
  try {
    const data: any = await request.get('/api/public/discussions', {
      params: {
        keyword: keyword.value || undefined,
        courseId: filterCourseId.value || undefined,
        current: page,
        size
      }
    })
    posts.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function setCourse(id: number | null) {
  filterCourseId.value = filterCourseId.value === id ? null : id
  load(1)
}

async function loadTags() {
  courseTags.value = ((await request.get('/api/public/discussions/courses')) as any) || []
}

async function loadHot() {
  const data: any = await request.get('/api/public/discussions', {
    params: { current: 1, size: 30 }
  })
  const list = (data.records || []) as any[]
  hot.value = [...list]
    .sort(
      (a, b) =>
        (b.post?.helpfulCount || 0) * 2 + (b.replyCount || 0) -
        ((a.post?.helpfulCount || 0) * 2 + (a.replyCount || 0))
    )
    .slice(0, 5)
}

function needLogin() {
  ElMessage.warning('请先登录')
  router.push({ path: '/login', query: { redirect: '/discussions' } })
}

function openPost() {
  if (!userStore.isLogin) return needLogin()
  form.value = { courseId: courses.value[0]?.id || null, title: '', content: '' }
  postDialog.value = true
}

async function submitPost() {
  if (!form.value.courseId) {
    ElMessage.warning('请选择所属课程')
    return
  }
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请填写内容')
    return
  }
  posting.value = true
  try {
    await request.post('/api/discussions', form.value)
    ElMessage.success('已发布')
    postDialog.value = false
    load(1)
    loadHot()
  } finally {
    posting.value = false
  }
}

async function openDetail(id: number) {
  detail.value = await request.get(`/api/public/discussions/${id}`)
  detailDrawer.value = true
  replyText.value = ''
}

function canDelete(post: any) {
  if (!userStore.isLogin) return false
  return userStore.role === 'ADMIN' || post.userId === userStore.userInfo?.id
}

async function removePost(id: number) {
  await ElMessageBox.confirm('确认删除这条帖子？', '提示', { type: 'warning' })
  await request.delete(`/api/discussions/${id}`)
  ElMessage.success('已删除')
  detailDrawer.value = false
  load(current.value)
}

/**
 * 标记「有帮助」：后端按 (帖子, 用户) 去重，一个人只能成功一次。
 * 第二次点会返回 added=false，这里把按钮置灰而不是继续加数。
 */
async function markHelpful() {
  if (!userStore.isLogin) return needLogin()
  const post = detail.value?.post
  if (!post || detail.value.helped) return
  try {
    const res: any = await request.post(`/api/discussions/${post.id}/helpful`)
    if (res?.added) {
      detail.value.helped = true
      post.helpfulCount = (post.helpfulCount || 0) + 1
      ElMessage.success('已标记为有帮助')
    } else {
      detail.value.helped = true
      ElMessage.info(res?.message || '你已经点过有帮助了')
    }
    load(current.value)
    loadHot()
  } catch {
    /* request 拦截器已提示 */
  }
}

async function sendReply() {
  if (!userStore.isLogin) return needLogin()
  const content = replyText.value.trim()
  if (!content) return
  await request.post(`/api/discussions/${detail.value.post.id}/reply`, { content })
  ElMessage.success('回复成功')
  replyText.value = ''
  detail.value = await request.get(`/api/public/discussions/${detail.value.post.id}`)
  load(current.value)
  loadHot()
}

onMounted(async () => {
  load(1)
  loadHot()
  loadTags()
  const page: any = await request.get('/api/public/courses', { params: { size: 100 } })
  courses.value = page.records || []
  // 从课程详情页「去讨论区提问」跳过来时，直接定位到该课程的标签
  const from = Number(route.query.courseId)
  if (from) {
    filterCourseId.value = from
    load(1)
  }
})
</script>

<style scoped>
.disc {
  max-width: 1200px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}
.tb-search {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 9px;
  height: 44px;
  padding: 0 16px;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-pill);
  transition: all 0.2s;
}
.tb-search:focus-within {
  border-color: var(--brand-300);
  box-shadow: 0 0 0 3px var(--brand-50);
}
.tb-search .el-icon {
  color: var(--ink-300);
}
.tb-search input {
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 13.5px;
  font-family: var(--font);
  min-width: 0;
}
.tb-search input::placeholder {
  color: var(--ink-300);
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

/* ---------- 课程标签 ---------- */
.tags-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  border: 1px solid var(--ink-100);
  background: var(--surface);
  color: var(--ink-500);
  font-size: 12.5px;
  font-weight: 550;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.16s;
}
.tag-chip:hover {
  border-color: var(--brand-200);
  color: var(--brand);
}
.tag-chip i {
  font-style: normal;
  font-size: 10.5px;
  padding: 0 5px;
  border-radius: var(--r-pill);
  background: var(--surface-sunken);
  color: var(--ink-400);
}
.tag-chip.on {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}
.tag-chip.on i {
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
}

/* ---------- 列表 ---------- */
.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}
.post {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 16px 18px;
  cursor: pointer;
  transition: all 0.2s;
}
.post:hover {
  border-color: var(--brand-200);
  box-shadow: var(--sh-sm);
  transform: translateY(-1px);
}
.avatar {
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}
.p-body {
  flex: 1;
  min-width: 0;
}
.p-top {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 4px;
  min-width: 0;
}
.p-top h3 {
  margin: 0;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.course-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex: none;
  max-width: 190px;
  padding: 2px 9px;
  border-radius: var(--r-pill);
  background: var(--brand-50);
  color: var(--brand-700);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.p-body h3 {
  margin: 0 0 5px;
  font-size: 14.5px;
  font-weight: 650;
  line-height: 1.45;
}
.excerpt {
  margin: 0 0 9px;
  font-size: 12.5px;
  color: var(--ink-400);
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.p-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: var(--ink-400);
}
.dot {
  color: var(--ink-200);
}
.stat {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-left: 6px;
}
.stat.helped {
  color: var(--brand);
  font-weight: 600;
}
.arrow {
  color: var(--ink-300);
  font-size: 13px;
  align-self: center;
  flex: none;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 18px 0 4px;
}

/* ---------- 侧栏 ---------- */
.side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.guide {
  margin: 0;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.guide li {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-500);
}
.guide b {
  color: var(--ink-900);
  font-weight: 650;
}
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.hot-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 4px;
  border-radius: 9px;
  cursor: pointer;
  transition: background 0.16s;
}
.hot-row:hover {
  background: var(--surface-sunken);
}
.hot-no {
  width: 18px;
  height: 18px;
  flex: none;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 10.5px;
  font-weight: 700;
  background: var(--surface-sunken);
  color: var(--ink-400);
}
.hot-no.top {
  background: var(--brand);
  color: #fff;
}
.hot-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.hot-body b {
  font-size: 12.5px;
  font-weight: 550;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hot-body span {
  font-size: 10.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 详情抽屉 ---------- */
.detail {
  padding: 4px 6px 20px;
}
.d-head {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 16px;
}
.d-meta {
  flex: 1;
}
.d-meta b {
  font-size: 13.5px;
  font-weight: 650;
  display: block;
}
.d-meta span {
  font-size: 11.5px;
  color: var(--ink-400);
}
.d-course {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: var(--brand-50);
  color: var(--brand-700);
  font-size: 11.5px;
  font-weight: 600;
}
.d-title {
  font-size: 20px;
  font-weight: 730;
  letter-spacing: -0.4px;
  margin: 0 0 12px;
  line-height: 1.4;
}
.d-content {
  margin: 0 0 16px;
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--ink-700);
  white-space: pre-wrap;
}
.d-actions {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--ink-100);
}
.like {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-pill);
  background: var(--surface);
  color: var(--ink-500);
  font-size: 12.5px;
  font-weight: 550;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.18s;
}
.like:hover {
  border-color: var(--brand-300);
  color: var(--brand);
}
.like.on {
  background: var(--brand-50);
  border-color: var(--brand-200);
  color: var(--brand-700);
}
.like:disabled {
  cursor: default;
}
.d-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.replies {
  padding-top: 16px;
}
.replies h3 {
  font-size: 13.5px;
  font-weight: 680;
  margin: 0 0 14px;
}
.replies h3 i {
  font-style: normal;
  font-size: 11px;
  color: var(--ink-400);
  margin-left: 4px;
}
.reply {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.r-avatar {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 9px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}
.r-body {
  flex: 1;
  background: var(--surface-sunken);
  border-radius: var(--r-md);
  padding: 10px 13px;
}
.r-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 5px;
}
.r-top b {
  font-size: 12.5px;
  font-weight: 650;
}
.r-top span {
  font-size: 11px;
  color: var(--ink-300);
}
.r-body p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--ink-700);
  white-space: pre-wrap;
}

.reply-box {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--ink-100);
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.reply-input {
  flex: 1;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-sm);
  padding: 10px 12px;
  font-family: var(--font);
  font-size: 12.5px;
  line-height: 1.7;
  resize: vertical;
  outline: none;
  transition: border-color 0.18s;
}
.reply-input:focus {
  border-color: var(--brand-300);
  box-shadow: 0 0 0 3px var(--brand-50);
}

@media (max-width: 1080px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
