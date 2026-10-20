<template>
  <div v-loading="loading">
    <div class="page-card">
      <el-row :gutter="24">
        <el-col :span="16">
          <div class="title">{{ course.title }}</div>
          <div class="muted" style="margin-bottom: 12px">
            {{ course.categoryName }} · 授课教师：{{ course.teacherName }} ·
            {{ course.chapterCount }} 章 · {{ course.studentCount }} 人在学
          </div>
          <p class="desc">{{ course.description }}</p>
        </el-col>
        <el-col :span="8">
          <div class="side">
            <el-progress v-if="course.enrolled" type="dashboard" :percentage="course.progress || 0" />
            <template v-if="!course.enrolled">
              <el-button type="primary" size="large" style="width: 100%" @click="enroll">
                立即选课
              </el-button>
              <p class="muted" style="text-align: center">选课后即可学习章节并参加测验</p>
            </template>
            <template v-else>
              <el-button type="primary" size="large" style="width: 100%"
                         @click="router.push(`/learn/${course.id}`)">
                继续学习
              </el-button>
              <el-button text type="danger" style="width: 100%; margin: 8px 0 0" @click="cancel">
                退课
              </el-button>
            </template>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="page-card">
      <div class="page-title">课程目录</div>
      <el-empty v-if="chapters.length === 0" description="该课程还没有章节" />
      <div v-for="(ch, i) in chapters" :key="ch.id" class="chapter">
        <div class="ch-left">
          <span class="idx">{{ i + 1 }}</span>
          <span>{{ ch.title }}</span>
        </div>
        <el-tag v-if="finishedIds.includes(ch.id)" type="success" size="small">已学完</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const course = ref<any>({})
const chapters = ref<any[]>([])
const finishedIds = ref<number[]>([])

async function load() {
  loading.value = true
  try {
    const data: any = await request.get(`/api/public/courses/${route.params.id}`)
    course.value = data.course
    chapters.value = data.chapters
    finishedIds.value = data.finishedChapterIds || []
  } finally {
    loading.value = false
  }
}

async function enroll() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }
  await request.post(`/api/courses/${course.value.id}/enroll`)
  ElMessage.success('选课成功')
  load()
}

async function cancel() {
  await ElMessageBox.confirm('确认退课？学习进度将会保留。', '提示', { type: 'warning' })
  await request.delete(`/api/courses/${course.value.id}/enroll`)
  ElMessage.success('已退课')
  load()
}

onMounted(load)
</script>

<style scoped>
.title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
}
.desc {
  color: #606266;
  line-height: 1.8;
}
.side {
  border-left: 1px solid #ebeef5;
  padding-left: 24px;
  text-align: center;
}
.chapter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 8px;
  border-bottom: 1px solid #f2f6fc;
}
.ch-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.idx {
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
}
</style>
