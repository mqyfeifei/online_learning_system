<template>
  <div>
    <div class="page-card">
      <el-row justify="space-between" align="middle">
        <el-input v-model="keyword" placeholder="搜索帖子标题" clearable style="width: 280px"
                  @keyup.enter="load(1)" />
        <el-button type="primary" @click="openCreate">发 帖</el-button>
      </el-row>
    </div>

    <div class="page-card">
      <div v-for="item in list" :key="item.post.id" class="post" @click="openDetail(item.post.id)">
        <div class="post-title">{{ item.post.title }}</div>
        <div class="muted">
          {{ item.authorName }} · {{ fmt(item.post.createTime) }} ·
          {{ item.replyCount }} 条回复 · {{ item.post.likeCount }} 赞
        </div>
      </div>
      <el-empty v-if="list.length === 0" description="还没有帖子" />
      <el-pagination v-model:current-page="current" :page-size="10" :total="total"
                     layout="prev, pager, next, total" style="justify-content: center; margin-top: 12px"
                     @current-change="load()" />
    </div>

    <el-dialog v-model="createDialog" title="发布新帖" width="560px">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="所属课程 ID（可选）">
          <el-input v-model="form.courseId" placeholder="填课程ID，不填则为公共讨论" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="create">发布</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" title="帖子详情" size="560px">
      <div v-if="detail">
        <h3 style="margin-top: 0">{{ detail.post.title }}</h3>
        <div class="muted" style="margin-bottom: 12px">
          {{ detail.authorName }} · {{ fmt(detail.post.createTime) }}
        </div>
        <p class="content">{{ detail.post.content }}</p>
        <el-button size="small" @click="like">点赞（{{ detail.post.likeCount }}）</el-button>

        <el-divider>回复（{{ detail.replies.length }}）</el-divider>
        <div v-for="r in detail.replies" :key="r.reply.id" class="reply">
          <div class="reply-author">{{ r.authorName }}</div>
          <div>{{ r.reply.content }}</div>
        </div>

        <div style="margin-top: 16px">
          <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="写下你的回复" />
          <el-button type="primary" style="margin-top: 8px" @click="doReply">回复</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const userStore = useUserStore()
const list = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const keyword = ref('')

const createDialog = ref(false)
const form = reactive<any>({ title: '', content: '', courseId: '' })

const detailDrawer = ref(false)
const detail = ref<any>(null)
const replyContent = ref('')

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

async function load(page?: number) {
  if (page) current.value = page
  const data: any = await request.get('/api/public/discussions', {
    params: { keyword: keyword.value, current: current.value, size: 10 }
  })
  list.value = data.records
  total.value = data.total
}

function openCreate() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  createDialog.value = true
}

async function create() {
  if (!form.title) {
    ElMessage.warning('标题不能为空')
    return
  }
  await request.post('/api/discussions', {
    title: form.title,
    content: form.content,
    courseId: form.courseId ? Number(form.courseId) : null
  })
  ElMessage.success('发布成功')
  createDialog.value = false
  form.title = ''
  form.content = ''
  load(1)
}

async function openDetail(id: number) {
  detail.value = await request.get('/api/public/discussions/' + id)
  detailDrawer.value = true
}

async function like() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  await request.post('/api/discussions/' + detail.value.post.id + '/like')
  detail.value.post.likeCount += 1
  load()
}

async function doReply() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  if (!replyContent.value) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  await request.post('/api/discussions/' + detail.value.post.id + '/reply', {
    content: replyContent.value
  })
  ElMessage.success('回复成功')
  replyContent.value = ''
  openDetail(detail.value.post.id)
  load()
}

onMounted(() => load())
</script>

<style scoped>
.post {
  padding: 14px 8px;
  border-bottom: 1px solid #f2f6fc;
  cursor: pointer;
}
.post:hover {
  background: #f5f7fa;
}
.post-title {
  font-weight: 600;
  margin-bottom: 6px;
}
.content {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}
.reply {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.reply-author {
  font-size: 12px;
  color: #409eff;
  margin-bottom: 4px;
}
</style>
