<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>平台统计</h1>
        <p>用户规模、课程分布与学习活跃度总览</p>
      </div>
      <div class="ap-actions">
        <el-button @click="router.push('/admin/audit')">
          <el-icon><CircleCheck /></el-icon> 待审课程
        </el-button>
        <el-button type="primary" @click="load">
          <el-icon><Refresh /></el-icon> 刷新数据
        </el-button>
      </div>
    </div>

    <!-- 指标 -->
    <section class="ap-stats">
      <div class="ap-stat">
        <span class="ico ico--brand"><el-icon><UserFilled /></el-icon></span>
        <b>{{ stats.userCount || 0 }}<i>人</i></b>
        <span>注册用户</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--violet"><el-icon><Notebook /></el-icon></span>
        <b>{{ stats.courseCount || 0 }}<i>门</i></b>
        <span>已发布课程</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--green"><el-icon><Reading /></el-icon></span>
        <b>{{ stats.enrollCount || 0 }}<i>人次</i></b>
        <span>选课总数</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--amber"><el-icon><CircleCheckFilled /></el-icon></span>
        <b>{{ stats.studyCount || 0 }}<i>次</i></b>
        <span>章节学习打卡</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--blue"><el-icon><EditPen /></el-icon></span>
        <b>{{ stats.examCount || 0 }}<i>份</i></b>
        <span>测验答卷</span>
      </div>
    </section>

    <div class="charts">
      <section class="ap-card">
        <div class="ap-card-head">
          <h2>课程分类分布</h2>
          <span class="ol-muted">按已发布课程统计</span>
        </div>
        <div ref="pieRef" class="chart-box" />
      </section>

      <section class="ap-card">
        <div class="ap-card-head">
          <h2>分类占比明细</h2>
        </div>
        <div class="detail-list">
          <div v-for="(item, i) in categoryRows" :key="i" class="d-row">
            <span class="d-dot" :style="{ background: item.color }" />
            <b>{{ item.name }}</b>
            <div class="bar">
              <i :style="{ width: item.pct + '%', background: item.color }" />
            </div>
            <span class="d-num">{{ item.value }} 门</span>
            <span class="d-pct">{{ item.pct }}%</span>
          </div>
          <div v-if="!categoryRows.length" class="ol-empty" style="padding: 30px 0">
            暂无课程数据
          </div>
        </div>
      </section>
    </div>

    <!-- 运营概况 -->
    <section class="ap-card">
      <div class="ap-card-head">
        <h2>运营概况</h2>
      </div>
      <div class="overview">
        <div class="ov-item">
          <span>平均每门课程选课</span>
          <b>{{ avgEnroll }} 人次</b>
        </div>
        <div class="ov-item">
          <span>平均每门课程打卡</span>
          <b>{{ avgStudy }} 次</b>
        </div>
        <div class="ov-item">
          <span>测验参与度</span>
          <b>{{ examRate }}%</b>
        </div>
        <div class="ov-item">
          <span>数据口径</span>
          <span class="ol-muted">选课按 status=1 统计，打卡按 finished=1 统计</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { CHART_COLORS, donutOption, mountChart } from '@/utils/chart'

const router = useRouter()
const stats = ref<any>({})
const pieRef = ref<HTMLElement>()

const categoryRows = computed(() => {
  const map = (stats.value.courseByCategory || {}) as Record<string, number>
  const keys = Object.keys(map)
  const total = keys.reduce((s, k) => s + Number(map[k] || 0), 0) || 1
  return keys
    .map((k, i) => ({
      name: k,
      value: Number(map[k] || 0),
      pct: Math.round((Number(map[k] || 0) / total) * 100),
      color: CHART_COLORS[i % CHART_COLORS.length]
    }))
    .sort((a, b) => b.value - a.value)
})

const avgEnroll = computed(() =>
  stats.value.courseCount
    ? (Number(stats.value.enrollCount || 0) / Number(stats.value.courseCount)).toFixed(1)
    : '0'
)
const avgStudy = computed(() =>
  stats.value.courseCount
    ? (Number(stats.value.studyCount || 0) / Number(stats.value.courseCount)).toFixed(1)
    : '0'
)
const examRate = computed(() => {
  const enroll = Number(stats.value.enrollCount || 0)
  if (!enroll) return 0
  return Math.min(100, Math.round((Number(stats.value.examCount || 0) / enroll) * 100))
})

async function load() {
  stats.value = await request.get('/api/admin/stats')
  if (!pieRef.value) return
  const data = categoryRows.value.map((r) => ({
    name: r.name,
    value: r.value,
    itemStyle: { color: r.color }
  }))
  if (data.length) {
    mountChart(pieRef.value, donutOption(data, '门'))
  } else {
    mountChart(pieRef.value, {
      title: {
        text: '暂无课程',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#b3aaa0', fontSize: 13, fontWeight: 400 }
      }
    })
  }
}

onMounted(load)
</script>

<style scoped>
.charts {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}
.chart-box {
  height: 300px;
  padding: 8px;
}

.detail-list {
  padding: 8px 20px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.d-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.d-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex: none;
}
.d-row b {
  width: 110px;
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: none;
}
.d-row .bar {
  flex: 1;
}
.d-num {
  width: 46px;
  text-align: right;
  font-size: 12px;
  font-weight: 650;
  color: var(--ink-700);
  flex: none;
}
.d-pct {
  width: 36px;
  text-align: right;
  font-size: 11.5px;
  color: var(--ink-400);
  flex: none;
}

.overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  padding: 18px 20px;
}
.ov-item {
  padding: 14px 16px;
  background: var(--surface-sunken);
  border-radius: var(--r-md);
}
.ov-item span {
  font-size: 11.5px;
  color: var(--ink-400);
  display: block;
  margin-bottom: 5px;
}
.ov-item b {
  font-size: 18px;
  font-weight: 750;
  letter-spacing: -0.4px;
}

@media (max-width: 1100px) {
  .charts {
    grid-template-columns: 1fr;
  }
}
</style>
