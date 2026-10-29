<template>
  <div class="grades fade-up">
    <!-- 当前筛选范围：选标签后整页数据同步 -->
    <div v-if="filterCourseId" class="scope-bar">
      <span class="scope-tag">
        <el-icon><Filter /></el-icon>当前范围：{{ filterCourseTitle }}
      </span>
      <span class="scope-hint">统计卡、趋势、等级分布与下方明细均已按这门课重新统计</span>
      <el-button text type="primary" size="small" @click="clearFilter">查看全部课程</el-button>
    </div>

    <!-- ==================== 统计卡 ==================== -->
    <section class="stats">
      <div class="stat">
        <div class="st-head">
          <span class="st-ico st-ico--brand"><el-icon><Trophy /></el-icon></span>
          <span class="st-label">平均分</span>
        </div>
        <b class="st-num">{{ scopedStats.average }}<i>分</i></b>
        <div class="bar"><i :style="{ width: Math.min(100, scopedStats.average || 0) + '%' }" /></div>
      </div>

      <div class="stat">
        <div class="st-head">
          <span class="st-ico st-ico--violet"><el-icon><EditPen /></el-icon></span>
          <span class="st-label">测验次数</span>
        </div>
        <b class="st-num">{{ scopedStats.examCount }}<i>次</i></b>
        <p class="st-sub">
          及格率 {{ scopedStats.passRate == null ? '—' : scopedStats.passRate + '%' }}
        </p>
      </div>

      <div class="stat">
        <div class="st-head">
          <span class="st-ico st-ico--green"><el-icon><Document /></el-icon></span>
          <span class="st-label">成绩记录</span>
        </div>
        <b class="st-num">{{ scopedStats.recordCount }}<i>条</i></b>
        <p class="st-sub">
          作业 {{ scopedStats.homeworkCount }} · 测验 {{ scopedStats.examCount }}
        </p>
      </div>

      <div class="stat">
        <div class="st-head">
          <span class="st-ico st-ico--amber"><el-icon><Medal /></el-icon></span>
          <span class="st-label">最高分</span>
        </div>
        <b class="st-num">{{ scopedStats.max }}<i>分</i></b>
        <p class="st-sub">{{ scopedStats.max >= 90 ? '状态不错，继续保持' : '还有提升空间' }}</p>
      </div>
    </section>

    <div class="grid">
      <!-- ==================== 趋势 ==================== -->
      <section class="ol-card">
        <div class="ol-card-title">
          <h2 class="ol-h2">成绩趋势</h2>
          <span class="ol-muted">{{ scopeLabel }} · 按提交时间排序</span>
        </div>
        <div v-if="trend.length" class="chart">
          <svg :viewBox="`0 0 ${chartW} ${chartH}`" preserveAspectRatio="none" class="chart-svg">
            <!-- 网格线 -->
            <line
              v-for="(g, i) in [0, 25, 50, 75, 100]"
              :key="i"
              :x1="0"
              :x2="chartW"
              :y1="yOf(g)"
              :y2="yOf(g)"
              class="grid-line"
            />
            <polyline :points="areaPoints" class="area" />
            <polyline :points="linePoints" class="line" />
            <circle
              v-for="(p, i) in trendCoords"
              :key="i"
              :cx="p.x"
              :cy="p.y"
              r="3.6"
              class="dot"
            />
          </svg>
          <div class="axis-y">
            <span>100</span><span>75</span><span>50</span><span>25</span><span>0</span>
          </div>
        </div>
        <div v-else class="ol-empty" style="padding: 34px 0">
          {{ filterCourseId ? '这门课还没有成绩数据' : '还没有成绩数据' }}
        </div>

        <div v-if="trend.length" class="legend">
          <span v-for="(p, i) in trend" :key="i" class="legend-item">
            <i :class="p.type === '作业' ? 'lc-amber' : 'lc-violet'" />
            {{ p.type }} {{ p.score }} · {{ p.time }}
          </span>
        </div>
      </section>

      <!-- ==================== 等级分布 ==================== -->
      <section class="ol-card">
        <div class="ol-card-title">
          <h2 class="ol-h2">等级分布</h2>
          <span class="ol-muted">{{ scopeLabel }} · {{ scopedStats.recordCount }} 条成绩</span>
        </div>
        <div class="dist">
          <div v-for="d in distribution" :key="d.label" class="dist-row">
            <span class="dist-label">{{ d.label }}</span>
            <div class="dist-bar">
              <i :style="{ width: d.pct + '%', background: d.color }" />
            </div>
            <span class="dist-count">{{ d.count }}</span>
          </div>
        </div>
      </section>
    </div>

    <!-- ==================== 测验记录 ==================== -->
    <section class="ol-card">
      <div class="ol-card-title">
        <h2 class="ol-h2">测验记录</h2>
        <span class="ol-muted">{{ scopeLabel }} · 共 {{ filteredRecords.length }} 次</span>
      </div>

      <div class="course-bar">
        <button class="c-chip" :class="{ on: !filterCourseId }" @click="clearFilter">
          全部课程<i>{{ allItems.length }}</i>
        </button>
        <button
          v-for="c in recordCourses"
          :key="c.courseId"
          class="c-chip"
          :class="{ on: filterCourseId === c.courseId }"
          @click="pickCourse(c.courseId)"
        >
          {{ c.courseTitle }}<i>{{ c.count }}</i>
        </button>
        <span v-if="!recordCourses.length" class="ol-muted">还没有按课程归类的成绩记录</span>
      </div>
      <p v-if="recordCourses.length" class="filter-hint">
        点击课程标签可筛选整页数据，标签上的数字是该课程的成绩条数（作业 + 测验）
      </p>

      <div v-if="filteredRecords.length" class="rec-list">
        <div v-for="r in filteredRecords" :key="r.record.id" class="rec-row">
          <div class="rec-score" :class="{ pass: r.record.passed === 1, stale: r.current === false }">
            <b>{{ r.record.score }}</b>
            <span>/ {{ r.totalScore || 100 }}</span>
          </div>
          <div class="rec-body">
            <div class="rec-top">
              <span class="course-tag">
                <el-icon><Collection /></el-icon>{{ r.courseTitle || '未关联课程' }}
              </span>
              <b>{{ r.paperTitle }}</b>
            </div>
            <p>
              交卷时间：{{ fmt(r.record.submitTime) }}
              <template v-if="r.chapterTitle"> · {{ r.chapterTitle }}</template>
              <template v-if="r.current === false"> · 试卷已重新发布，需重新作答</template>
            </p>
          </div>
          <span v-if="r.current === false" class="chip chip--violet">历史作答</span>
          <span v-else class="chip" :class="r.record.passed === 1 ? 'chip--green' : 'chip--brand'">
            {{ r.record.passed === 1 ? '及格' : '未及格' }}
          </span>
          <el-button text type="primary" @click="viewDetail(r.record.id)">查看解析</el-button>
        </div>
      </div>
      <div v-else class="ol-empty">
        <span class="icon">📝</span>
        <template v-if="filterCourseId">
          {{ filterCourseTitle }} 暂无测验记录{{ filteredItems.length ? '，该课程的成绩见下方明细' : '' }}
        </template>
        <template v-else>还没有测验记录，去课程里做一套随堂测验吧</template>
      </div>
    </section>

    <!-- ==================== 成绩明细 ==================== -->
    <section class="ol-card">
      <div class="ol-card-title">
        <h2 class="ol-h2">作业与测验明细</h2>
        <span class="ol-muted">{{ scopeLabel }} · 共 {{ filteredItems.length }} 条</span>
      </div>
      <el-table v-if="filteredItems.length" :data="filteredItems" stripe style="width: 100%">
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <span class="chip" :class="row.type === '作业' ? 'chip--amber' : 'chip--violet'">
              {{ row.type }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="课程 / 名称" min-width="220">
          <template #default="{ row }">
            <b class="tbl-title">{{ row.title }}</b>
            <span class="ol-muted"> · {{ row.courseTitle || '未关联课程' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分数" width="150">
          <template #default="{ row }">
            <b class="tbl-score" :class="{ low: row.score < 60 }">{{ row.score }}</b>
            <span class="ol-muted"> / 100</span>
          </template>
        </el-table-column>
        <el-table-column label="评级" width="120">
          <template #default="{ row }">{{ gradeOf(row.score) }}</template>
        </el-table-column>
        <el-table-column label="时间">
          <template #default="{ row }">{{ fmt(row.time) }}</template>
        </el-table-column>
      </el-table>
      <div v-else class="ol-empty">
        {{ filterCourseId ? '这门课还没有成绩明细' : '暂无成绩明细' }}
      </div>
    </section>

    <!-- ==================== 按课程汇总 ==================== -->
    <section class="ol-card" v-if="courseStats.length">
      <div class="ol-card-title">
        <h2 class="ol-h2">各课程成绩</h2>
        <span class="ol-muted">点击卡片可切换筛选，与上方数据联动</span>
      </div>
      <div class="cs-grid">
        <div
          v-for="g in courseStats"
          :key="g.courseId"
          class="cs-card"
          :class="{ on: filterCourseId === g.courseId }"
          @click="pickCourse(g.courseId)"
        >
          <div class="cs-head">
            <b>{{ g.courseTitle }}</b>
            <span class="chip">{{ g.count }} 次</span>
          </div>
          <div class="cs-num">
            <b>{{ g.average }}</b>
            <span>平均分</span>
          </div>
          <div class="bar"><i :style="{ width: Math.min(100, g.average || 0) + '%' }" /></div>
          <p class="ol-muted">最高 {{ g.max }} 分</p>
        </div>
      </div>
    </section>

    <!-- 答卷详情 -->
    <el-dialog append-to-body v-model="dialog" title="答卷详情" width="760px" top="6vh">
      <div v-if="detail">
        <div class="dlg-head">
          <div class="dlg-score" :class="{ pass: detail.passed }">
            <b>{{ detail.score }}</b>
            <span>/ {{ detail.totalScore }}</span>
          </div>
          <div>
            <b class="dlg-title">{{ detail.paperTitle || '测验答卷' }}</b>
            <p class="ol-muted">
              共 {{ detail.details?.length || 0 }} 题，答对
              {{ (detail.details || []).filter((d: any) => d.correct).length }} 题
            </p>
          </div>
        </div>

        <div class="q-list">
          <div v-for="(d, i) in detail.details" :key="d.questionId" class="q-item">
            <div class="q-head">
              <span class="q-no" :class="{ ok: d.correct }">{{ i + 1 }}</span>
              <b>{{ d.title }}</b>
              <span v-if="d.needManual" class="chip chip--violet">
                {{ d.score == null ? '待教师批阅' : `已批阅 ${d.score} 分` }}
              </span>
              <span v-else class="chip" :class="d.correct ? 'chip--green' : 'chip--brand'">
                {{ d.correct ? '正确' : '错误' }}
              </span>
            </div>
            <!-- 选择题：列出选项，正确答案标绿、选错的标红 -->
            <div v-if="isChoice(d)" class="opt-list">
              <div
                v-for="o in optionsOf(d)"
                :key="o.key"
                class="opt"
                :class="{
                  'opt--right': isRightKey(d, o.key),
                  'opt--wrong': isWrongPick(d, o.key)
                }"
              >
                <i class="opt-key">{{ o.key }}</i>
                <span class="opt-text">{{ o.text }}</span>
                <em v-if="isRightKey(d, o.key)" class="opt-tag">正确答案</em>
                <em v-else-if="isWrongPick(d, o.key)" class="opt-tag opt-tag--bad">你的选择</em>
              </div>
            </div>
            <div class="q-ans">
              <template v-if="d.type === 5">
                你的作答：<b>{{ plainText(d.yourAnswer) || '未作答' }}</b>
                <template v-if="d.referenceAnswer">
                  <span class="sep">|</span>
                  参考答案：<b class="ans-ok">{{ d.referenceAnswer }}</b>
                </template>
              </template>
              <template v-else>
                你的答案：<b :class="{ 'ans-bad': !d.correct }">{{ answerLabel(d, d.yourAnswer) || '未作答' }}</b>
                <template v-if="d.rightAnswer">
                  <span class="sep">|</span>
                  正确答案：<b class="ans-ok">{{ answerLabel(d, d.rightAnswer) }}</b>
                </template>
              </template>
            </div>
            <div v-if="d.comment" class="q-analysis">
              <el-icon><ChatLineSquare /></el-icon>教师评语：{{ d.comment }}
            </div>
            <div v-if="d.analysis" class="q-analysis">
              <el-icon><InfoFilled /></el-icon>{{ d.analysis }}
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'
import { answerLabel, isChoice, isRightKey, isWrongPick, optionsOf } from '@/utils/quiz'

const summary = ref<any>({})
const records = ref<any[]>([])
const dialog = ref(false)
const detail = ref<any>(null)
/** 课程筛选：null 表示全部课程；切换后整页数据同步刷新 */
const filterCourseId = ref<number | null>(null)

const chartW = 560
const chartH = 150

/** 全量成绩明细（作业 + 测验），按提交时间倒序 */
const allItems = computed(() => {
  const list = ((summary.value.items || []) as any[]).filter((i: any) => i && i.score != null)
  return [...list].sort((a: any, b: any) => (a.time < b.time ? 1 : -1))
})

/** 课程维度的成绩汇总（后端已按课程算好平均分与最高分） */
const courseStats = computed(() => (summary.value.courseStats || []) as any[])

/** 课程标签：明细与测验记录里出现过的课程取并集，只有作业的课程也能筛出来 */
const recordCourses = computed(() => {
  const itemCounts = new Map<number, number>()
  const recordCounts = new Map<number, number>()
  const titles = new Map<number, string>()
  allItems.value.forEach((i: any) => {
    if (i.courseId == null) return
    itemCounts.set(i.courseId, (itemCounts.get(i.courseId) || 0) + 1)
    if (!titles.has(i.courseId)) titles.set(i.courseId, i.courseTitle)
  })
  records.value.forEach((r: any) => {
    if (r.courseId == null) return
    recordCounts.set(r.courseId, (recordCounts.get(r.courseId) || 0) + 1)
    if (!titles.has(r.courseId)) titles.set(r.courseId, r.courseTitle)
  })
  const ids = new Set<number>([...itemCounts.keys(), ...recordCounts.keys()])
  return [...ids]
    .map((id) => ({
      courseId: id,
      courseTitle: titles.get(id) || '未关联课程',
      count: itemCounts.get(id) || recordCounts.get(id) || 0
    }))
    .sort((a, b) => b.count - a.count)
})

/** 当前筛选的课程名 */
const filterCourseTitle = computed(() => {
  if (!filterCourseId.value) return ''
  const hit =
    courseStats.value.find((g: any) => g.courseId === filterCourseId.value) ||
    recordCourses.value.find((c: any) => c.courseId === filterCourseId.value)
  return hit?.courseTitle || '当前课程'
})

/** 各处文案统一用的范围描述 */
const scopeLabel = computed(() => (filterCourseId.value ? filterCourseTitle.value : '全部课程'))

/** 当前筛选范围内的成绩明细（表格 + 趋势 + 等级分布都用它） */
const filteredItems = computed(() =>
  filterCourseId.value
    ? allItems.value.filter((i: any) => i.courseId === filterCourseId.value)
    : allItems.value
)

/** 当前筛选范围内的测验记录 */
const filteredRecords = computed(() =>
  filterCourseId.value
    ? records.value.filter((r: any) => r.courseId === filterCourseId.value)
    : records.value
)

/** 统计卡：全部由筛选后的数据推导，切标签即同步变化 */
const scopedStats = computed(() => {
  const scores = filteredItems.value.map((i: any) => Number(i.score) || 0)
  const total = scores.reduce((a, b) => a + b, 0)
  const homework = filteredItems.value.filter((i: any) => i.type === '作业').length
  const passed = filteredRecords.value.filter((r: any) => r.record?.passed === 1).length
  return {
    average: scores.length ? Math.round((total / scores.length) * 10) / 10 : 0,
    max: scores.length ? Math.max(...scores) : 0,
    recordCount: scores.length,
    homeworkCount: homework,
    examCount: filteredRecords.value.length,
    passRate: filteredRecords.value.length
      ? Math.round((passed / filteredRecords.value.length) * 100)
      : null
  }
})

/** 折线图取当前筛选下时间正序的最后 10 条 */
const trend = computed(() => {
  const list = [...filteredItems.value].sort((a: any, b: any) => (a.time < b.time ? -1 : 1))
  return list.slice(-10)
})

const trendCoords = computed(() => {
  const n = trend.value.length
  if (!n) return []
  return trend.value.map((p, i) => ({
    x: n === 1 ? chartW / 2 : 10 + (i * (chartW - 20)) / (n - 1),
    y: yOf(p.score || 0)
  }))
})
const linePoints = computed(() => trendCoords.value.map((p) => `${p.x},${p.y}`).join(' '))
const areaPoints = computed(() => {
  if (!trendCoords.value.length) return ''
  const first = trendCoords.value[0]
  const last = trendCoords.value[trendCoords.value.length - 1]
  return `${first.x},${chartH} ${linePoints.value} ${last.x},${chartH}`
})

function yOf(score: number) {
  // 留出上下 12px 边距，0→底部，100→顶部
  return chartH - 12 - ((score / 100) * (chartH - 24))
}

function gradeOf(score: number) {
  if (score >= 90) return '优秀'
  if (score >= 80) return '良好'
  if (score >= 70) return '中等'
  if (score >= 60) return '及格'
  return '不及格'
}

/** 等级分布同样跟随筛选范围 */
const distribution = computed(() => {
  const all = filteredItems.value.map((i: any) => i.score || 0)
  const buckets = [
    { label: '优秀 90+', min: 90, color: '#2fa36b' },
    { label: '良好 80-89', min: 80, color: '#7c6cf5' },
    { label: '中等 70-79', min: 70, color: '#3b82f6' },
    { label: '及格 60-69', min: 60, color: '#f0a52c' },
    { label: '不及格 <60', min: 0, color: '#e5533d' }
  ]
  const total = all.length || 1
  return buckets.map((b) => {
    const count = all.filter((s: number) => {
      if (b.min === 90) return s >= 90
      if (b.min === 0) return s < 60
      return s >= b.min && s < b.min + 10
    }).length
    return { ...b, count, pct: all.length ? Math.round((count / total) * 100) : 0 }
  })
})

/** 点同一个标签再点一次就回到全部课程 */
function pickCourse(id: number) {
  filterCourseId.value = filterCourseId.value === id ? null : id
}

function clearFilter() {
  filterCourseId.value = null
}

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

/** 问答题作答是富文本，详情弹窗里只显示纯文本 */
function plainText(html?: string) {
  if (!html) return ''
  return html
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

async function viewDetail(id: number) {
  detail.value = await request.get('/api/records/' + id)
  dialog.value = true
}

onMounted(async () => {
  summary.value = await request.get('/api/my/grades')
  records.value = ((await request.get('/api/my/records')) as any) || []
})
</script>

<style scoped>
.grades {
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- 筛选范围条 ---------- */
.scope-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 14px;
  border: 1px solid var(--brand-200);
  border-radius: var(--r-md);
  background: var(--brand-50);
}
.scope-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 650;
  color: var(--brand-700);
}
.scope-hint {
  flex: 1;
  min-width: 200px;
  font-size: 11.5px;
  color: var(--ink-500);
}
.filter-hint {
  margin: -4px 0 12px;
  font-size: 11.5px;
  color: var(--ink-400);
}

/* ---------- 统计卡 ---------- */
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.stat {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 16px 18px;
}
.st-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.st-ico {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 15px;
}
.st-ico--brand {
  background: var(--brand-50);
  color: var(--brand);
}
.st-ico--violet {
  background: var(--violet-soft);
  color: var(--violet);
}
.st-ico--green {
  background: var(--green-soft);
  color: var(--green);
}
.st-ico--amber {
  background: var(--amber-soft);
  color: var(--amber);
}
.st-label {
  font-size: 12.5px;
  color: var(--ink-500);
  font-weight: 550;
}
.st-num {
  font-size: 26px;
  font-weight: 780;
  letter-spacing: -0.8px;
  display: block;
  line-height: 1.15;
  margin-bottom: 8px;
}
.st-num i {
  font-size: 12px;
  font-style: normal;
  font-weight: 550;
  color: var(--ink-400);
  margin-left: 3px;
}
.st-sub {
  margin: 0;
  font-size: 11.5px;
  color: var(--ink-400);
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 16px;
}

/* ---------- 折线图 ---------- */
.chart {
  position: relative;
  padding-left: 30px;
}
.chart-svg {
  width: 100%;
  height: 150px;
  display: block;
  overflow: visible;
}
.grid-line {
  stroke: var(--ink-100);
  stroke-width: 1;
  stroke-dasharray: 3 4;
}
.line {
  fill: none;
  stroke: var(--brand);
  stroke-width: 2.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.area {
  fill: rgba(255, 107, 53, 0.1);
  stroke: none;
}
.dot {
  fill: #fff;
  stroke: var(--brand);
  stroke-width: 2.2;
}
.axis-y {
  position: absolute;
  left: 0;
  top: 0;
  height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  font-size: 10px;
  color: var(--ink-300);
}
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--ink-100);
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--ink-400);
}
.legend-item i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.lc-amber {
  background: var(--amber);
}
.lc-violet {
  background: var(--violet);
}

/* ---------- 等级分布 ---------- */
.dist {
  display: flex;
  flex-direction: column;
  gap: 13px;
  padding-top: 2px;
}
.dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.dist-label {
  width: 76px;
  font-size: 12px;
  color: var(--ink-500);
  flex: none;
}
.dist-bar {
  flex: 1;
  height: 8px;
  border-radius: var(--r-pill);
  background: var(--surface-sunken);
  overflow: hidden;
}
.dist-bar i {
  display: block;
  height: 100%;
  border-radius: var(--r-pill);
  transition: width 0.5s ease;
}
.dist-count {
  width: 24px;
  text-align: right;
  font-size: 12px;
  font-weight: 650;
  color: var(--ink-700);
  flex: none;
}

/* ---------- 测验记录 ---------- */
.rec-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rec-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  transition: all 0.18s;
}
.rec-row:hover {
  border-color: var(--brand-200);
  background: var(--surface-soft);
}
.rec-score {
  width: 58px;
  height: 52px;
  flex: none;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--brand-50);
  color: var(--brand-700);
}
.rec-score.pass {
  background: var(--green-soft);
  color: #1f7a4f;
}
/* 试卷重新发布后作废的历史作答，用灰底区分 */
.rec-score.stale {
  background: var(--surface-sunken);
  color: var(--ink-400);
}
.rec-top {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  margin-bottom: 2px;
}
.rec-top b {
  margin: 0;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.course-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex: none;
  max-width: 170px;
  padding: 2px 9px;
  border-radius: var(--r-pill);
  background: var(--brand-50);
  color: var(--brand-700);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 课程筛选条 ---------- */
.course-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.c-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  border: 1px solid var(--ink-100);
  background: var(--surface);
  color: var(--ink-500);
  font-size: 12.5px;
  font-weight: 550;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.16s;
}
.c-chip:hover {
  border-color: var(--brand-200);
  color: var(--brand);
}
.c-chip i {
  font-style: normal;
  font-size: 10.5px;
  padding: 0 5px;
  border-radius: var(--r-pill);
  background: var(--surface-sunken);
  color: var(--ink-400);
}
.c-chip.on {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}
.c-chip.on i {
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
}

/* ---------- 各课程成绩 ---------- */
.cs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.cs-card {
  padding: 14px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  cursor: pointer;
  transition: all 0.18s;
}
.cs-card:hover {
  border-color: var(--brand-200);
  background: var(--surface-soft);
}
.cs-card.on {
  border-color: var(--brand);
  background: var(--brand-50);
}
.cs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.cs-head b {
  font-size: 13px;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cs-num {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin-bottom: 6px;
}
.cs-num b {
  font-size: 22px;
  font-weight: 760;
  color: var(--brand);
  letter-spacing: -0.5px;
}
.cs-num span {
  font-size: 11px;
  color: var(--ink-400);
}
.cs-card .bar {
  height: 6px;
  border-radius: 4px;
  background: var(--surface-sunken);
  overflow: hidden;
  margin-bottom: 6px;
}
.cs-card .bar i {
  display: block;
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--brand-300), var(--brand));
}
.cs-card p {
  margin: 0;
  font-size: 11.5px;
}

.tbl-title {
  font-weight: 620;
}
.rec-score b {
  font-size: 19px;
  font-weight: 780;
  line-height: 1.1;
}
.rec-score span {
  font-size: 10px;
  opacity: 0.75;
}
.rec-body {
  flex: 1;
  min-width: 0;
}
.rec-body b {
  font-size: 13.5px;
  font-weight: 620;
  display: block;
  margin-bottom: 2px;
}
.rec-body p {
  margin: 0;
  font-size: 11.5px;
  color: var(--ink-400);
}

.tbl-score {
  font-size: 15px;
  font-weight: 720;
}
.tbl-score.low {
  color: #c0392b;
}

/* ---------- 弹窗 ---------- */
.dlg-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  background: var(--surface-sunken);
  border-radius: var(--r-md);
  margin-bottom: 16px;
}
.dlg-score {
  width: 68px;
  height: 62px;
  flex: none;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--brand-50);
  color: var(--brand-700);
}
.dlg-score.pass {
  background: var(--green-soft);
  color: #1f7a4f;
}
.dlg-score b {
  font-size: 23px;
  font-weight: 780;
  line-height: 1.1;
}
.dlg-score span {
  font-size: 10.5px;
  opacity: 0.75;
}
.dlg-title {
  font-size: 15px;
  font-weight: 680;
}

.q-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 54vh;
  overflow-y: auto;
  padding-right: 4px;
}
.q-item {
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  padding: 13px 15px;
}
.q-head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 8px;
}
.q-head b {
  flex: 1;
  font-size: 13.5px;
  font-weight: 620;
  line-height: 1.5;
}
.q-no {
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  background: var(--brand-50);
  color: var(--brand-700);
}
.q-no.ok {
  background: var(--green-soft);
  color: var(--green);
}
.q-ans {
  font-size: 12.5px;
  color: var(--ink-500);
}
/* ---------- 答卷详情的选项列表 ---------- */
.opt-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 10px 0 4px;
}
.opt {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 7px 11px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-sm);
  font-size: 12.5px;
  line-height: 1.55;
  color: var(--ink-700);
  background: var(--surface);
}
.opt-key {
  font-style: normal;
  font-weight: 700;
  flex: none;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 11px;
  background: var(--surface-sunken);
  color: var(--ink-500);
}
.opt-text {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}
.opt-tag {
  font-style: normal;
  flex: none;
  font-size: 10.5px;
  font-weight: 650;
  padding: 1px 7px;
  border-radius: var(--r-pill);
  background: var(--green-soft);
  color: var(--green);
}
.opt-tag--bad {
  background: #fdecea;
  color: #c0392b;
}
.opt--right {
  border-color: var(--green);
  background: var(--green-soft);
}
.opt--right .opt-key {
  background: var(--green);
  color: #fff;
}
.opt--wrong {
  border-color: #e7a5a0;
  background: #fdf3f2;
}
.opt--wrong .opt-key {
  background: #c0392b;
  color: #fff;
}
.q-ans b {
  font-weight: 650;
  color: var(--ink-900);
}
.ans-bad {
  color: #c0392b !important;
}
.ans-ok {
  color: var(--green) !important;
}
.sep {
  margin: 0 8px;
  color: var(--ink-200);
}
.q-analysis {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 9px;
  font-size: 12px;
  line-height: 1.7;
  color: #1f7a4f;
  background: var(--green-soft);
  padding: 9px 11px;
  border-radius: var(--r-sm);
}
.q-analysis .el-icon {
  margin-top: 2px;
  flex: none;
}

@media (max-width: 1080px) {
  .stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
