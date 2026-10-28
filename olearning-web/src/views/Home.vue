<template>
  <div class="home fade-up">
    <!-- ==================== 欢迎卡 ==================== -->
    <section class="hero">
      <div class="hero-info">
        <span class="chip chip--brand">{{ logged ? '学习进行中' : '欢迎来到知学在线' }}</span>
        <h1>{{ logged ? `${greeting}，${name}` : '把一门课，真正学完' }}</h1>
        <p v-if="logged">
          {{ ov.courseCount ? `你有 ${ov.courseCount} 门在学课程，还有 ${remaining} 个章节等待完成` : '还没有选课，先去课程广场挑一门吧' }}
        </p>
        <p v-else>四门专业必修课已上线，配套课件、随堂测验与作业批改，全部可以在这里完成。</p>

        <div class="hero-kpis">
          <div class="kpi">
            <b>{{ ov.courseCount || 0 }}</b>
            <span>在学课程</span>
          </div>
          <div class="kpi">
            <b>{{ ov.finishedChapters || 0 }}<i>/{{ ov.totalChapters || 0 }}</i></b>
            <span>已完成章节</span>
          </div>
          <div class="kpi">
            <b>{{ ov.streak || 0 }}<i>天</i></b>
            <span>连续学习</span>
          </div>
          <div class="kpi">
            <b>{{ ov.todoAssignments || 0 }}</b>
            <span>待交作业</span>
          </div>
        </div>
      </div>

      <div class="hero-ring">
        <svg viewBox="0 0 140 140" class="ring">
          <circle cx="70" cy="70" r="56" class="ring-track" />
          <circle
            cx="70"
            cy="70"
            r="56"
            class="ring-bar"
            :stroke-dasharray="ringLen"
            :stroke-dashoffset="ringOffset"
          />
        </svg>
        <div class="ring-text">
          <b>{{ ov.completionRate || 0 }}%</b>
          <span>总完成率</span>
        </div>
      </div>
    </section>

    <div class="layout">
      <!-- ==================== 左栏 ==================== -->
      <div class="col-main">
        <!-- 继续学习 -->
        <section v-if="logged" class="ol-card block">
          <div class="ol-card-title">
            <h2 class="ol-h2">继续学习</h2>
            <router-link to="/my/courses" class="more">全部课程 →</router-link>
          </div>
          <div v-if="learning.length" class="continue-list">
            <div
              v-for="c in learning.slice(0, 3)"
              :key="c.id"
              class="continue-item"
              @click="go(`/learn/${c.id}`)"
            >
              <div class="thumb" :style="coverStyle(c)">
                <template v-if="!c.cover">{{ c.title.slice(0, 2) }}</template>
              </div>
              <div class="ci-body">
                <div class="ci-top">
                  <span class="chip">{{ c.categoryName || '课程' }}</span>
                  <span class="ol-muted">{{ c.progress || 0 }}%</span>
                </div>
                <b class="ci-title">{{ c.title }}</b>
                <div class="bar"><i :style="{ width: (c.progress || 0) + '%' }" /></div>
                <div class="ci-foot">
                  <span class="ol-muted">{{ c.teacherName }} · {{ c.chapterCount || 0 }} 章节</span>
                  <span class="go-btn">继续</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="ol-empty">
            <span class="icon">📚</span>
            还没有在学课程，<router-link to="/courses" class="link">去选一门</router-link>
          </div>
        </section>

        <!-- 课程推荐 -->
        <section class="ol-card block">
          <div class="ol-card-title">
            <h2 class="ol-h2">{{ logged ? '推荐课程' : '热门课程' }}</h2>
            <router-link to="/courses" class="more">课程广场 →</router-link>
          </div>
          <div class="course-grid">
            <article
              v-for="c in hotCourses"
              :key="c.id"
              class="course-card"
              @click="go(`/courses/${c.id}`)"
            >
              <div class="cc-cover" :style="coverStyle(c)">
                <span v-if="!c.cover">{{ c.title }}</span>
                <i v-if="c.categoryName" class="cc-tag">{{ c.categoryName }}</i>
              </div>
              <div class="cc-body">
                <b class="cc-title">{{ c.title }}</b>
                <p class="cc-desc">{{ c.description }}</p>
                <div class="cc-foot">
                  <span class="ol-muted">{{ c.teacherName }}</span>
                  <span class="ol-muted">{{ c.studentCount || 0 }} 人在学</span>
                </div>
              </div>
            </article>
            <div v-if="!hotCourses.length" class="ol-empty" style="grid-column: 1 / -1">
              暂无已发布课程
            </div>
          </div>
        </section>
      </div>

      <!-- ==================== 右栏 ==================== -->
      <aside class="col-side">
        <!-- 本周活跃 -->
        <section v-if="logged" class="ol-card block">
          <div class="ol-card-title">
            <h2 class="ol-h2">本周学习</h2>
            <span class="chip chip--green">{{ ov.weekTotal || 0 }} 章完成</span>
          </div>
          <div class="week-chart">
            <div v-for="d in ov.week || []" :key="d.date" class="week-col">
              <div class="week-bar-wrap">
                <span class="week-val" :class="{ zero: !d.count }">{{ d.count || '' }}</span>
                <div
                  class="week-bar"
                  :class="{ today: d.isToday, empty: !d.count }"
                  :style="{ height: barHeight(d.count) }"
                />
              </div>
              <span class="week-label" :class="{ today: d.isToday }">{{ d.label }}</span>
            </div>
          </div>
          <div class="streak-tip">
            <el-icon><Sunny /></el-icon>
            已连续学习 <b>{{ ov.streak || 0 }}</b> 天，累计学习 <b>{{ ov.studyDays || 0 }}</b> 天
          </div>
        </section>

        <!-- 待办 -->
        <section v-if="logged" class="ol-card block">
          <div class="ol-card-title">
            <h2 class="ol-h2">学习提醒</h2>
          </div>
          <div class="todo-list">
            <div class="todo-row" @click="go('/my/courses')">
              <span class="todo-ico todo-ico--brand"><el-icon><Reading /></el-icon></span>
              <div>
                <b>{{ remaining }} 个章节待完成</b>
                <p>按每天 2 章的节奏，{{ etaText }} 可学完</p>
              </div>
            </div>
            <div class="todo-row" @click="go('/grades')">
              <span class="todo-ico todo-ico--amber"><el-icon><EditPen /></el-icon></span>
              <div>
                <b>{{ ov.todoAssignments || 0 }} 份作业待提交</b>
                <p>逾期会影响平时成绩</p>
              </div>
            </div>
            <div class="todo-row" @click="go('/favorites')">
              <span class="todo-ico todo-ico--violet"><el-icon><Notebook /></el-icon></span>
              <div>
                <b>{{ ov.noteCount || 0 }} 条笔记 · {{ ov.favoriteCount || 0 }} 门收藏</b>
                <p>随手记下的要点，复习时最好用</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 公告 -->
        <section class="ol-card block">
          <div class="ol-card-title">
            <h2 class="ol-h2">平台公告</h2>
          </div>
          <div class="notice-list">
            <div v-for="n in notices.slice(0, 4)" :key="n.id" class="notice-item">
              <div class="notice-head">
                <span v-if="n.topFlag === 1" class="chip chip--brand">置顶</span>
                <b>{{ n.title }}</b>
              </div>
              <p>{{ n.content }}</p>
              <span class="ol-muted">{{ fmt(n.createTime) }}</span>
            </div>
            <div v-if="!notices.length" class="ol-empty" style="padding: 20px 0">暂无公告</div>
          </div>
        </section>

        <!-- 成绩趋势 -->
        <section v-if="logged && (ov.scoreTrend || []).length" class="ol-card block">
          <div class="ol-card-title">
            <h2 class="ol-h2">成绩趋势</h2>
            <router-link to="/grades" class="more">明细 →</router-link>
          </div>
          <svg class="trend" viewBox="0 0 260 90" preserveAspectRatio="none">
            <polyline :points="trendPoints" class="trend-line" />
            <circle
              v-for="(p, i) in trendCoords"
              :key="i"
              :cx="p.x"
              :cy="p.y"
              r="3.2"
              class="trend-dot"
            />
          </svg>
          <div class="trend-labels">
            <span v-for="(p, i) in ov.scoreTrend" :key="i">{{ p.time }}</span>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { coverOf, coverStyle } from '@/utils/cover'

const router = useRouter()
const userStore = useUserStore()

const hotCourses = ref<any[]>([])
const notices = ref<any[]>([])
const learning = ref<any[]>([])
const ov = ref<any>({})

const logged = computed(() => userStore.isLogin)
const name = computed(
  () => userStore.userInfo?.realName || userStore.userInfo?.username || '同学'
)
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const remaining = computed(() =>
  Math.max(0, (ov.value.totalChapters || 0) - (ov.value.finishedChapters || 0))
)
const etaText = computed(() => {
  const days = Math.ceil(remaining.value / 2)
  if (!days) return '继续保持'
  return `约 ${days} 天`
})

// ---------------- 环形进度 ----------------
const RING_R = 56
const ringLen = 2 * Math.PI * RING_R
const ringOffset = computed(() => {
  const rate = Math.min(100, Math.max(0, ov.value.completionRate || 0))
  return ringLen * (1 - rate / 100)
})

// ---------------- 封面配色：按课程 id 取一组稳定的暖色渐变 ----------------

// ---------------- 周柱状图高度 ----------------
function barHeight(count: number) {
  const max = Math.max(1, ...((ov.value.week || []) as any[]).map((d) => d.count || 0))
  const h = ((count || 0) / max) * 100
  return `${Math.max(count ? 12 : 4, h)}%`
}

// ---------------- 成绩趋势折线 ----------------
const trendCoords = computed(() => {
  const list = (ov.value.scoreTrend || []) as any[]
  if (!list.length) return []
  const n = list.length
  return list.map((p, i) => ({
    x: n === 1 ? 130 : 8 + (i * 244) / (n - 1),
    y: 82 - ((p.score || 0) / 100) * 68
  }))
})
const trendPoints = computed(() => trendCoords.value.map((p) => `${p.x},${p.y}`).join(' '))

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

function go(path: string) {
  router.push(path)
}

onMounted(async () => {
  const page: any = await request.get('/api/public/courses', { params: { sort: 'hot', size: 8 } })
  hotCourses.value = page.records || []
  notices.value = ((await request.get('/api/public/notices')) as any) || []

  if (logged.value) {
    try {
      learning.value = ((await request.get('/api/my/courses')) as any) || []
      // 只把还有进度的课程放进「继续学习」，已完成的沉到我的课程里
      learning.value = learning.value
        .filter((c: any) => (c.progress || 0) < 100)
        .sort((a: any, b: any) => (b.progress || 0) - (a.progress || 0))
      ov.value = (await request.get('/api/my/overview')) as any
    } catch {
      /* 统计接口失败不影响首页主体 */
    }
  }
})
</script>

<style scoped>
.home {
  max-width: 1360px;
}

/* ---------- 欢迎卡 ---------- */
.hero {
  display: flex;
  align-items: center;
  gap: 24px;
  background: linear-gradient(108deg, #fff 0%, #fffaf5 46%, #ffeede 100%);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-xl);
  padding: 28px 30px;
  margin-bottom: 18px;
  box-shadow: var(--sh-xs);
  position: relative;
  overflow: hidden;
}
.hero::after {
  content: '';
  position: absolute;
  right: -60px;
  top: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 107, 53, 0.16), transparent 68%);
  pointer-events: none;
}
.hero-info {
  flex: 1;
  min-width: 0;
}
.hero-info h1 {
  font-size: 27px;
  font-weight: 750;
  letter-spacing: -0.6px;
  margin: 12px 0 6px;
}
.hero-info > p {
  margin: 0;
  color: var(--ink-500);
  font-size: 13.5px;
}

.hero-kpis {
  display: flex;
  gap: 34px;
  margin-top: 22px;
}
.kpi {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.kpi b {
  font-size: 21px;
  font-weight: 750;
  letter-spacing: -0.5px;
  color: var(--ink-900);
}
.kpi b i {
  font-size: 12px;
  font-style: normal;
  font-weight: 550;
  color: var(--ink-400);
  margin-left: 2px;
}
.kpi span {
  font-size: 12px;
  color: var(--ink-400);
}

.hero-ring {
  position: relative;
  width: 150px;
  height: 150px;
  flex: none;
}
.ring {
  width: 150px;
  height: 150px;
  transform: rotate(-90deg);
}
.ring-track {
  fill: none;
  stroke: #f3e7da;
  stroke-width: 11;
}
.ring-bar {
  fill: none;
  stroke: var(--brand);
  stroke-width: 11;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.9s cubic-bezier(0.22, 1, 0.36, 1);
}
.ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
}
.ring-text b {
  font-size: 27px;
  font-weight: 780;
  letter-spacing: -1px;
}
.ring-text span {
  font-size: 11.5px;
  color: var(--ink-400);
}

/* ---------- 两栏 ---------- */
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 336px;
  gap: 18px;
  align-items: start;
}
.col-main,
.col-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}
.block {
  padding: 22px;
}
.more {
  font-size: 12.5px;
  color: var(--ink-400);
  transition: color 0.18s;
}
.more:hover {
  color: var(--brand);
}
.link {
  color: var(--brand);
  font-weight: 600;
}

/* ---------- 继续学习 ---------- */
.continue-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.continue-item {
  display: flex;
  gap: 14px;
  padding: 12px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  cursor: pointer;
  transition: all 0.2s ease;
}
.continue-item:hover {
  border-color: var(--brand-200);
  background: var(--surface-soft);
  transform: translateY(-1px);
  box-shadow: var(--sh-sm);
}
.thumb {
  width: 96px;
  height: 74px;
  flex: none;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.12);
}
.ci-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.ci-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ci-title {
  font-size: 14.5px;
  font-weight: 650;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ci-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}
.go-btn {
  font-size: 12.5px;
  font-weight: 650;
  color: var(--brand);
  background: var(--brand-50);
  padding: 4px 12px;
  border-radius: var(--r-pill);
}

/* ---------- 课程网格 ---------- */
.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(212px, 1fr));
  gap: 16px;
}
.course-card {
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  overflow: hidden;
  cursor: pointer;
  background: var(--surface);
  transition: all 0.22s ease;
  display: flex;
  flex-direction: column;
}
.course-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--sh-md);
  border-color: var(--brand-200);
}
.cc-cover {
  height: 104px;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 12px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.3px;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.18);
  line-height: 1.35;
}
.cc-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  font-style: normal;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.86);
  color: var(--ink-700);
  text-shadow: none;
}
.cc-body {
  padding: 12px 13px 14px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1;
}
.cc-title {
  font-size: 13.5px;
  font-weight: 650;
  line-height: 1.4;
}
.cc-desc {
  margin: 0;
  font-size: 12px;
  color: var(--ink-400);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 37px;
}
.cc-foot {
  display: flex;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 4px;
}

/* ---------- 周学习 ---------- */
.week-chart {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  height: 108px;
  align-items: flex-end;
  margin: 4px 0 12px;
}
.week-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  height: 100%;
}
.week-bar-wrap {
  flex: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  gap: 3px;
}
.week-val {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--brand-700);
}
.week-val.zero {
  visibility: hidden;
}
.week-bar {
  width: 100%;
  max-width: 22px;
  border-radius: 7px;
  background: linear-gradient(180deg, var(--brand-300), var(--brand-200));
  transition: height 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}
.week-bar.today {
  background: linear-gradient(180deg, var(--brand), var(--brand-600));
  box-shadow: 0 4px 10px rgba(255, 107, 53, 0.3);
}
.week-bar.empty {
  background: var(--surface-sunken);
}
.week-label {
  font-size: 11px;
  color: var(--ink-400);
}
.week-label.today {
  color: var(--brand);
  font-weight: 700;
}
.streak-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-500);
  background: var(--amber-soft);
  padding: 9px 12px;
  border-radius: 10px;
}
.streak-tip b {
  color: #a86a08;
}
.streak-tip .el-icon {
  color: var(--amber);
}

/* ---------- 待办 ---------- */
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.todo-row {
  display: flex;
  gap: 11px;
  padding: 10px;
  border-radius: var(--r-md);
  cursor: pointer;
  transition: background 0.18s;
}
.todo-row:hover {
  background: var(--surface-sunken);
}
.todo-ico {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 16px;
}
.todo-ico--brand {
  background: var(--brand-50);
  color: var(--brand);
}
.todo-ico--amber {
  background: var(--amber-soft);
  color: var(--amber);
}
.todo-ico--violet {
  background: var(--violet-soft);
  color: var(--violet);
}
.todo-row b {
  font-size: 13px;
  font-weight: 620;
  display: block;
  margin-bottom: 2px;
}
.todo-row p {
  margin: 0;
  font-size: 11.5px;
  color: var(--ink-400);
}

/* ---------- 公告 ---------- */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.notice-item {
  padding-left: 11px;
  border-left: 2px solid var(--brand-100);
}
.notice-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.notice-head b {
  font-size: 13px;
  font-weight: 620;
}
.notice-item p {
  margin: 0 0 4px;
  font-size: 12px;
  color: var(--ink-500);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ---------- 成绩趋势 ---------- */
.trend {
  width: 100%;
  height: 90px;
  display: block;
}
.trend-line {
  fill: none;
  stroke: var(--violet);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.trend-dot {
  fill: #fff;
  stroke: var(--violet);
  stroke-width: 2;
}
.trend-labels {
  display: flex;
  justify-content: space-between;
  font-size: 10.5px;
  color: var(--ink-300);
  margin-top: 4px;
}

@media (max-width: 1180px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 720px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-kpis {
    gap: 20px;
    flex-wrap: wrap;
  }
}
</style>
