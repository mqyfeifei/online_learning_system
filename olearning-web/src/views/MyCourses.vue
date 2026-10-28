<template>
  <div class="my fade-up">
    <!-- 概览 -->
    <section class="summary">
      <div class="sum-item">
        <span class="sum-ico sum-ico--brand"><el-icon><Reading /></el-icon></span>
        <div>
          <b>{{ list.length }}</b>
          <span>在学课程</span>
        </div>
      </div>
      <div class="sum-item">
        <span class="sum-ico sum-ico--green"><el-icon><CircleCheckFilled /></el-icon></span>
        <div>
          <b>{{ doneCount }}</b>
          <span>已学完</span>
        </div>
      </div>
      <div class="sum-item">
        <span class="sum-ico sum-ico--violet"><el-icon><TrendCharts /></el-icon></span>
        <div>
          <b>{{ avgProgress }}%</b>
          <span>平均进度</span>
        </div>
      </div>
      <div class="sum-item">
        <span class="sum-ico sum-ico--amber"><el-icon><Clock /></el-icon></span>
        <div>
          <b>{{ learningCount }}</b>
          <span>进行中</span>
        </div>
      </div>
    </section>

    <!-- 筛选 -->
    <div class="filters">
      <span
        v-for="f in filters"
        :key="f.key"
        class="chip chip-btn"
        :class="{ 'is-active': active === f.key }"
        @click="active = f.key"
      >
        {{ f.label }}
      </span>
    </div>

    <!-- 课程列表 -->
    <div v-loading="loading" class="list">
      <article
        v-for="c in filtered"
        :key="c.id"
        class="row"
        @click="c.enrollStatus === 2 ? null : router.push(`/learn/${c.id}`)"
      >
        <div class="cover" :style="coverStyle(c)">
          <span v-if="!c.cover">{{ c.title.slice(0, 2) }}</span>
          <i v-if="(c.progress || 0) >= 100" class="badge-done">
            <el-icon><CircleCheckFilled /></el-icon> 已完成
          </i>
          <i v-else-if="c.enrollStatus === 2" class="badge-pending">待审核</i>
        </div>

        <div class="body">
          <div class="row-top">
            <h3>{{ c.title }}</h3>
            <span v-if="c.enrollStatus === 2" class="chip chip--violet">选课待教师审核</span>
            <span v-else class="pct" :class="{ done: (c.progress || 0) >= 100 }">
              {{ c.progress || 0 }}%
            </span>
          </div>
          <div class="meta">
            <span class="chip">{{ c.categoryName || '课程' }}</span>
            <span>{{ c.teacherName }}</span>
            <span>{{ c.chapterCount || 0 }} 章节</span>
            <span>{{ c.studentCount || 0 }} 人在学</span>
          </div>
          <div class="bar" :class="{ 'bar--green': (c.progress || 0) >= 100 }">
            <i :style="{ width: (c.progress || 0) + '%' }" />
          </div>
        </div>

        <div class="actions">
          <button class="act" @click.stop="router.push(`/courses/${c.id}`)">详情</button>
          <button
            v-if="c.enrollStatus === 2"
            class="act"
            disabled
          >
            等待审核
          </button>
          <button v-else class="act act--primary" @click.stop="router.push(`/learn/${c.id}`)">
            {{ (c.progress || 0) === 0 ? '开始学习' : (c.progress || 0) >= 100 ? '复习' : '继续学习' }}
          </button>
        </div>
      </article>

      <div v-if="!loading && !filtered.length" class="ol-empty">
        <span class="icon">📚</span>
        <template v-if="active === 'all'">
          还没有选课，<router-link to="/courses" class="link">去课程广场看看</router-link>
        </template>
        <template v-else>这个分类下暂时没有课程</template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { coverOf, coverStyle } from '@/utils/cover'

const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const active = ref<'all' | 'learning' | 'done'>('all')

const filters = [
  { key: 'all' as const, label: '全部课程' },
  { key: 'learning' as const, label: '进行中' },
  { key: 'done' as const, label: '已完成' }
]

const doneCount = computed(() => list.value.filter((c) => (c.progress || 0) >= 100).length)
const learningCount = computed(() => list.value.length - doneCount.value)
const avgProgress = computed(() =>
  list.value.length
    ? Math.round(list.value.reduce((s, c) => s + (c.progress || 0), 0) / list.value.length)
    : 0
)

const filtered = computed(() => {
  if (active.value === 'learning') return list.value.filter((c) => (c.progress || 0) < 100)
  if (active.value === 'done') return list.value.filter((c) => (c.progress || 0) >= 100)
  return list.value
})

onMounted(async () => {
  loading.value = true
  try {
    const data: any = await request.get('/api/my/courses')
    list.value = (data || []).sort((a: any, b: any) => (b.progress || 0) - (a.progress || 0))
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.my {
  max-width: 1180px;
}

.summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}
.sum-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 16px 18px;
}
.sum-ico {
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 18px;
}
.sum-ico--brand {
  background: var(--brand-50);
  color: var(--brand);
}
.sum-ico--green {
  background: var(--green-soft);
  color: var(--green);
}
.sum-ico--violet {
  background: var(--violet-soft);
  color: var(--violet);
}
.sum-ico--amber {
  background: var(--amber-soft);
  color: var(--amber);
}
.sum-item b {
  display: block;
  font-size: 21px;
  font-weight: 760;
  letter-spacing: -0.5px;
  line-height: 1.2;
}
.sum-item span {
  font-size: 12px;
  color: var(--ink-400);
}

.filters {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.filters .chip {
  height: 30px;
  padding: 0 14px;
  font-size: 13px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 160px;
}
.row {
  display: flex;
  align-items: center;
  gap: 18px;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.row:hover {
  border-color: var(--brand-200);
  box-shadow: var(--sh-sm);
  transform: translateY(-1px);
}
.cover {
  width: 126px;
  height: 82px;
  flex: none;
  border-radius: var(--r-md);
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 19px;
  font-weight: 720;
  letter-spacing: 2px;
  position: relative;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.16);
}
.badge-pending {
  position: absolute;
  bottom: 6px;
  right: 6px;
  font-style: normal;
  font-size: 10px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.94);
  color: #5a4bcf;
  padding: 2px 7px;
  border-radius: var(--r-pill);
}
.badge-done {
  position: absolute;
  bottom: 6px;
  right: 6px;
  font-style: normal;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.94);
  color: #1f7a4f;
  padding: 2px 7px;
  border-radius: var(--r-pill);
  text-shadow: none;
}
.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.row-top h3 {
  margin: 0;
  font-size: 15.5px;
  font-weight: 680;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pct {
  font-size: 15px;
  font-weight: 760;
  color: var(--brand);
  flex: none;
}
.pct.done {
  color: var(--green);
}
.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--ink-400);
}
.actions {
  display: flex;
  gap: 8px;
  flex: none;
}
.act {
  height: 36px;
  padding: 0 16px;
  border-radius: var(--r-pill);
  border: 1px solid var(--ink-100);
  background: var(--surface);
  color: var(--ink-600, var(--ink-500));
  font-size: 12.5px;
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.18s;
  white-space: nowrap;
}
.act:hover {
  border-color: var(--brand-200);
  color: var(--brand);
}
.act--primary {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.24);
}
.act--primary:hover {
  color: #fff;
  background: var(--brand-600);
  border-color: var(--brand-600);
}
.link {
  color: var(--brand);
  font-weight: 600;
}

@media (max-width: 980px) {
  .summary {
    grid-template-columns: repeat(2, 1fr);
  }
  .row {
    flex-wrap: wrap;
  }
  .cover {
    width: 100%;
    height: 96px;
  }
}
</style>
