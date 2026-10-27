<template>
  <div class="admin-page fade-up">
    <div class="ap-head">
      <div>
        <h1>学情看板</h1>
        <p>名下课程的选课规模、完成情况与成绩分布</p>
      </div>
      <div class="ap-actions">
        <el-button @click="router.push('/teacher/courses')">
          <el-icon><Notebook /></el-icon> 管理课程
        </el-button>
        <el-button type="primary" @click="router.push('/teacher/courses')">
          <el-icon><Plus /></el-icon> 新建课程
        </el-button>
      </div>
    </div>

    <!-- 指标 -->
    <section class="ap-stats">
      <div class="ap-stat">
        <span class="ico ico--brand"><el-icon><Notebook /></el-icon></span>
        <b>{{ stats.courseCount || 0 }}<i>门</i></b>
        <span>我的课程</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--violet"><el-icon><UserFilled /></el-icon></span>
        <b>{{ stats.studentCount || 0 }}<i>人次</i></b>
        <span>累计选课</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--green"><el-icon><TrendCharts /></el-icon></span>
        <b>{{ avgFinish }}<i>%</i></b>
        <span>平均完成率</span>
      </div>
      <div class="ap-stat">
        <span class="ico ico--amber"><el-icon><Trophy /></el-icon></span>
        <b>{{ totalRecords }}<i>份</i></b>
        <span>测验答卷</span>
      </div>
    </section>

    <!-- 图表 -->
    <div class="charts">
      <section class="ap-card">
        <div class="ap-card-head">
          <h2>各课程选课人数</h2>
          <span class="ol-muted">单位：人次</span>
        </div>
        <div ref="barRef" class="chart-box" />
      </section>

      <section class="ap-card">
        <div class="ap-card-head">
          <h2>成绩分布</h2>
          <span class="ol-muted">单位：份</span>
        </div>
        <div ref="pieRef" class="chart-box" />
      </section>
    </div>

    <!-- 课程完成率 -->
    <section class="ap-card">
      <div class="ap-card-head">
        <h2>课程完成情况</h2>
        <span class="ol-muted">按选课人数排序</span>
      </div>
      <div class="finish-list">
        <div v-for="(c, i) in rows" :key="i" class="finish-row">
          <div class="fr-title">
            <b>{{ c.title }}</b>
            <span class="ol-muted">{{ c.enroll }} 人在学</span>
          </div>
          <div class="fr-bar">
            <div class="bar"><i :style="{ width: (c.rate || 0) + '%' }" /></div>
            <span>{{ c.rate || 0 }}%</span>
          </div>
        </div>
        <div v-if="!rows.length" class="ol-empty">名下还没有课程</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { barOption, donutOption, mountChart, SCORE_COLORS } from '@/utils/chart'

const router = useRouter()
const stats = ref<any>({})
const barRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()

const rows = computed(() => {
  const titles = (stats.value.courseTitles || []) as string[]
  const enrolls = (stats.value.enrollCounts || []) as number[]
  const rates = (stats.value.finishRates || []) as number[]
  return titles
    .map((t, i) => ({ title: t, enroll: enrolls[i] || 0, rate: rates[i] || 0 }))
    .sort((a, b) => b.enroll - a.enroll)
})

const avgFinish = computed(() => {
  const rates = (stats.value.finishRates || []) as number[]
  if (!rates.length) return 0
  return Math.round(rates.reduce((a, b) => a + b, 0) / rates.length)
})

const totalRecords = computed(() => {
  const buckets = (stats.value.scoreBuckets || {}) as Record<string, number>
  return Object.values(buckets).reduce((a, b) => a + Number(b || 0), 0)
})

onMounted(async () => {
  stats.value = await request.get('/api/teacher/stats')

  const titles = (stats.value.courseTitles || []) as string[]
  const enrolls = (stats.value.enrollCounts || []) as number[]

  if (barRef.value && titles.length) {
    mountChart(barRef.value, barOption(titles, enrolls, '#ff6b35', '选课人数'))
  } else if (barRef.value) {
    mountChart(barRef.value, {
      title: {
        text: '还没有课程数据',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#b3aaa0', fontSize: 13, fontWeight: 400 }
      }
    })
  }

  const buckets = (stats.value.scoreBuckets || {}) as Record<string, number>
  const pieData = Object.keys(buckets).map((k) => ({
    name: `${k} 分`,
    value: buckets[k],
    itemStyle: { color: SCORE_COLORS[k] || '#c48b5c' }
  }))
  if (pieRef.value) {
    if (pieData.some((d) => d.value > 0)) {
      mountChart(pieRef.value, donutOption(pieData, '份'))
    } else {
      mountChart(pieRef.value, {
        title: {
          text: '暂无答卷数据',
          left: 'center',
          top: 'middle',
          textStyle: { color: '#b3aaa0', fontSize: 13, fontWeight: 400 }
        }
      })
    }
  }
})
</script>

<style scoped>
.charts {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 16px;
}
.chart-box {
  height: 288px;
  padding: 8px 12px;
}

.finish-list {
  padding: 8px 20px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.finish-row {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 11px 0;
  border-bottom: 1px dashed var(--ink-100);
}
.finish-row:last-child {
  border-bottom: 0;
}
.fr-title {
  width: 240px;
  flex: none;
  min-width: 0;
}
.fr-title b {
  display: block;
  font-size: 13.5px;
  font-weight: 620;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}
.fr-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}
.fr-bar .bar {
  flex: 1;
}
.fr-bar span {
  width: 40px;
  text-align: right;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--brand-700);
  flex: none;
}

@media (max-width: 1100px) {
  .charts {
    grid-template-columns: 1fr;
  }
  .fr-title {
    width: 150px;
  }
}
</style>
