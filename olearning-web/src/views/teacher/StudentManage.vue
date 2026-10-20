<template>
  <div class="page-card">
    <el-page-header content="学生名单与进度" @back="router.push('/teacher/courses')" style="margin-bottom: 16px" />

    <el-table :data="list" stripe>
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="username" label="账号" width="140" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column label="学习进度" min-width="200">
        <template #default="scope">
          <el-progress :percentage="scope.row.progress" />
        </template>
      </el-table-column>
      <el-table-column prop="enrollTime" label="选课时间" width="200" />
    </el-table>
    <el-empty v-if="list.length === 0" description="还没有学生选课" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const list = ref<any[]>([])

onMounted(async () => {
  list.value = (await request.get('/api/teacher/courses/' + route.params.courseId + '/students')) as any
})
</script>
