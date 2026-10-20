<template>
  <div>
    <div class="banner">
      <div class="banner-text">
        <h1>随时随地，开始学习</h1>
        <p>课程学习 · 在线测验 · 作业提交 · 学情分析</p>
        <el-button type="primary" size="large" @click="router.push('/courses')">浏览课程广场</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <div class="page-card">
          <div class="page-title">热门课程</div>
          <el-row :gutter="16">
            <el-col v-for="c in hotCourses" :key="c.id" :span="8">
              <div class="course-item" @click="router.push(`/courses/${c.id}`)">
                <div class="cover">{{ c.title }}</div>
                <div class="course-title">{{ c.title }}</div>
                <div class="muted">{{ c.teacherName }} · {{ c.chapterCount }} 章</div>
                <div class="muted">{{ c.studentCount }} 人在学 · {{ c.viewCount }} 次浏览</div>
              </div>
            </el-col>
          </el-row>
          <el-empty v-if="hotCourses.length === 0" description="暂无已发布课程" />
        </div>
      </el-col>

      <el-col :span="8">
        <div class="page-card">
          <div class="page-title">平台公告</div>
          <el-timeline>
            <el-timeline-item v-for="n in notices" :key="n.id"
                              :timestamp="fmt(n.createTime)" placement="top">
              <div class="notice-title">
                <el-tag v-if="n.topFlag === 1" type="danger" size="small">置顶</el-tag>
                {{ n.title }}
              </div>
              <div class="muted">{{ n.content }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="notices.length === 0" description="暂无公告" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const hotCourses = ref<any[]>([])
const notices = ref<any[]>([])

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

onMounted(async () => {
  const page: any = await request.get('/api/public/courses', { params: { sort: 'hot', size: 6 } })
  hotCourses.value = page.records
  notices.value = (await request.get('/api/public/notices')) as any
})
</script>

<style scoped>
.banner {
  height: 220px;
  border-radius: 12px;
  background: linear-gradient(120deg, #409eff 0%, #79bbff 60%, #b3d8ff 100%);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 48px;
  margin-bottom: 16px;
}
.banner-text h1 {
  margin: 0 0 8px;
  font-size: 28px;
}
.banner-text p {
  margin: 0 0 20px;
  opacity: 0.9;
}
.course-item {
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: 0.2s;
}
.course-item:hover {
  background: #f5f7fa;
}
.course-title {
  font-weight: 600;
  margin: 8px 0 4px;
  font-size: 14px;
}
.notice-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}
</style>
