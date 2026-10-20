<template>
  <div>
    <div class="page-card">
      <div class="page-title">我的课程</div>
      <el-row :gutter="16">
        <el-col v-for="c in list" :key="c.id" :span="8" style="margin-bottom: 16px">
          <div class="item">
            <div class="cover">{{ c.title.slice(0, 8) }}</div>
            <div class="title">{{ c.title }}</div>
            <div class="muted">{{ c.teacherName }} · {{ c.chapterCount }} 章</div>
            <el-progress :percentage="c.progress || 0" style="margin: 10px 0" />
            <el-button type="primary" size="small" @click="router.push(`/learn/${c.id}`)">
              {{ (c.progress || 0) === 0 ? '开始学习' : '继续学习' }}
            </el-button>
          </div>
        </el-col>
      </el-row>
      <el-empty v-if="list.length === 0" description="还没有选课，去课程广场看看吧">
        <el-button type="primary" @click="router.push('/courses')">去选课</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const list = ref<any[]>([])

onMounted(async () => {
  list.value = (await request.get('/api/my/courses')) as any
})
</script>

<style scoped>
.item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}
.title {
  font-weight: 600;
  margin: 10px 0 4px;
}
</style>
