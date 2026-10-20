<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6">
        <div class="page-card"><el-statistic title="我的课程数" :value="stats.courseCount || 0" /></div>
      </el-col>
      <el-col :span="6">
        <div class="page-card"><el-statistic title="累计选课人次" :value="stats.studentCount || 0" /></div>
      </el-col>
      <el-col :span="6">
        <div class="page-card"><el-statistic title="试卷份数" :value="(stats.courseTitles || []).length" /></div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">各课程选课人数</div>
          <div ref="barRef" style="height: 300px"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">成绩分布</div>
          <div ref="pieRef" style="height: 300px"></div>
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
const barRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()

onMounted(async () => {
  stats.value = await request.get('/api/teacher/stats')

  const bar = echarts.init(barRef.value!)
  bar.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, bottom: 60, top: 20 },
    xAxis: {
      type: 'category',
      data: stats.value.courseTitles || [],
      axisLabel: { interval: 0, rotate: 20 }
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: stats.value.enrollCounts || [],
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      barWidth: 28
    }]
  })

  const buckets = stats.value.scoreBuckets || {}
  const pie = echarts.init(pieRef.value!)
  pie.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      data: Object.keys(buckets).map((k) => ({ name: k + ' 分', value: buckets[k] })),
      label: { formatter: '{b}: {c} 人' }
    }]
  })

  window.addEventListener('resize', () => {
    bar.resize()
    pie.resize()
  })
})
</script>
