<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6"><div class="page-card"><el-statistic title="用户总数" :value="stats.userCount || 0" /></div></el-col>
      <el-col :span="6"><div class="page-card"><el-statistic title="已发布课程" :value="stats.courseCount || 0" /></div></el-col>
      <el-col :span="6"><div class="page-card"><el-statistic title="选课记录" :value="stats.enrollCount || 0" /></div></el-col>
      <el-col :span="6"><div class="page-card"><el-statistic title="学习打卡数" :value="stats.studyCount || 0" /></div></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <div class="page-card">
          <div class="page-title">课程分类分布</div>
          <div ref="chartRef" style="height: 320px"></div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="page-card">
          <div class="page-title">平台概况</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="考试记录数">{{ stats.examCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="选课总人次">{{ stats.enrollCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="学习完成记录">{{ stats.studyCount || 0 }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const stats = ref<any>({})
const chartRef = ref<HTMLElement>()

onMounted(async () => {
  stats.value = await request.get('/api/admin/stats')
  const map = stats.value.courseByCategory || {}
  const chart = echarts.init(chartRef.value!)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '60%',
      // key 已经是分类名（后端 join 过 course_category），不用再加「分类」前缀
      data: Object.keys(map).map((k) => ({ name: k, value: map[k] })),
      label: { formatter: '{b}: {c} 门' }
    }]
  })
  window.addEventListener('resize', () => chart.resize())
})
</script>
