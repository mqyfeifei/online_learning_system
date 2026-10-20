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
    // minInterval: 1 —— 人数是整数，不加的话 ECharts 会画出 0.5 / 1.5 这种刻度
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: stats.value.enrollCounts || [],
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      barWidth: 28
    }]
  })

  const buckets = stats.value.scoreBuckets || {}
  // 分数段固定配色：高分段绿、低分段红，避免按数组下标取默认色盘导致每次刷新颜色都变
  const bucketColors: Record<string, string> = {
    '90-100': '#67c23a',
    '80-89': '#95d475',
    '70-79': '#e6a23c',
    '60-69': '#f3d19e',
    '0-59': '#f56c6c'
  }
  const pie = echarts.init(pieRef.value!)
  pie.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      data: Object.keys(buckets).map((k) => ({
        name: k + ' 分',
        value: buckets[k],
        itemStyle: { color: bucketColors[k] }
      })),
      label: { formatter: '{b}: {c} 人' }
    }]
  })

  window.addEventListener('resize', () => {
    bar.resize()
    pie.resize()
  })
})
</script>
