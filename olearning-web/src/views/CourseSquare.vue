<template>
  <div class="square fade-up">
    <!-- ==================== 工具栏 ==================== -->
    <section class="toolbar ol-card">
      <div class="tb-top">
        <div class="tb-search">
          <el-icon><Search /></el-icon>
          <input
            v-model="keyword"
            placeholder="输入课程名、教师名或关键词，回车搜索"
            @keyup.enter="search"
          />
          <button v-if="keyword" class="clear" @click="resetKeyword">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <button class="tb-btn" :class="{ active: sort === 'hot' }" @click="setSort('hot')">
          <el-icon><TrendCharts /></el-icon> 最热
        </button>
        <button class="tb-btn" :class="{ active: sort === 'new' }" @click="setSort('new')">
          <el-icon><Clock /></el-icon> 最新
        </button>
      </div>

      <div class="tb-chips">
        <span
          class="chip chip-btn"
          :class="{ 'is-active': !categoryId }"
          @click="setCategory(null)"
        >
          全部课程
        </span>
        <span
          v-for="c in categories"
          :key="c.id"
          class="chip chip-btn"
          :class="{ 'is-active': categoryId === c.id }"
          @click="setCategory(c.id)"
        >
          {{ c.name }}
        </span>
      </div>
    </section>

    <div class="result-bar">
      <span class="ol-muted">
        共 <b>{{ total }}</b> 门课程
        <template v-if="keyword">· 关键词「{{ keyword }}」</template>
      </span>
      <span class="ol-muted">按 {{ sort === 'hot' ? '学习人数' : '发布时间' }} 排序</span>
    </div>

    <!-- ==================== 课程网格 ==================== -->
    <div v-loading="loading" class="grid">
      <article
        v-for="c in courses"
        :key="c.id"
        class="card"
        @click="router.push(`/courses/${c.id}`)"
      >
        <div class="cover-wrap" :style="coverStyle(c)">
          <!-- 渐变兜底时才叠加课程名/简介；真实封面图不再叠文字，避免和图内文字混在一起 -->
          <div v-if="!c.cover" class="cover-mask">
            <b>{{ c.title }}</b>
            <p>{{ c.description }}</p>
          </div>
          <span v-if="c.categoryName" class="cat">{{ c.categoryName }}</span>
          <span v-if="c.enrolled" class="enrolled">
            <el-icon><CircleCheckFilled /></el-icon> 已选
          </span>
        </div>

        <div class="c-body">
          <h3>{{ c.title }}</h3>
          <div class="meta">
            <span><el-icon><User /></el-icon>{{ c.teacherName }}</span>
            <span><el-icon><Notebook /></el-icon>{{ c.chapterCount || 0 }} 章</span>
            <span><el-icon><View /></el-icon>{{ c.viewCount || 0 }}</span>
          </div>

          <div v-if="c.enrolled" class="progress-row">
            <div class="bar"><i :style="{ width: (c.progress || 0) + '%' }" /></div>
            <span>{{ c.progress || 0 }}%</span>
          </div>
          <div v-else class="students">
            <div class="avatars">
              <i v-for="n in 3" :key="n" :style="{ background: coverOf(c.id + n) }" />
            </div>
            <span>{{ c.studentCount || 0 }} 人在学</span>
          </div>
        </div>
      </article>

      <div v-if="!loading && !courses.length" class="ol-empty" style="grid-column: 1 / -1">
        <span class="icon">🔍</span>
        没有匹配的课程，换个关键词试试
      </div>
    </div>

    <div v-if="total > size" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="current"
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { coverOf, coverStyle } from '@/utils/cover'

const route = useRoute()
const router = useRouter()

const categories = ref<any[]>([])
const courses = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const size = 9
const loading = ref(false)
const keyword = ref((route.query.keyword as string) || '')
const categoryId = ref<number | null>(null)
const sort = ref<'hot' | 'new'>('hot')

async function load() {
  loading.value = true
  try {
    const page: any = await request.get('/api/public/courses', {
      params: {
        keyword: keyword.value || undefined,
        categoryId: categoryId.value || undefined,
        sort: sort.value,
        current: current.value,
        size
      }
    })
    courses.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  current.value = 1
  load()
}

function resetKeyword() {
  keyword.value = ''
  search()
}

function setSort(s: 'hot' | 'new') {
  sort.value = s
  current.value = 1
  load()
}

function setCategory(id: number | null) {
  categoryId.value = id
  current.value = 1
  load()
}

function onPage(p: number) {
  current.value = p
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 顶栏搜索框跳过来时带 keyword，要跟着重新查
watch(
  () => route.query.keyword,
  (k) => {
    keyword.value = (k as string) || ''
    current.value = 1
    load()
  }
)

onMounted(async () => {
  const flat: any = await request.get('/api/public/categories/flat')
  categories.value = (flat || []).filter((c: any) => c.parentId === 0)
  load()
})
</script>

<style scoped>
.square {
  max-width: 1360px;
}

/* ---------- 工具栏 ---------- */
.toolbar {
  padding: 18px 20px 16px;
  margin-bottom: 16px;
}
.tb-top {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tb-search {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 9px;
  height: 44px;
  padding: 0 16px;
  background: var(--surface-sunken);
  border: 1px solid transparent;
  border-radius: var(--r-pill);
  transition: all 0.2s;
}
.tb-search:focus-within {
  background: var(--surface);
  border-color: var(--brand-300);
  box-shadow: 0 0 0 3px var(--brand-50);
}
.tb-search .el-icon {
  color: var(--ink-300);
  font-size: 16px;
}
.tb-search input {
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 14px;
  font-family: var(--font);
  color: var(--ink-900);
  min-width: 0;
}
.tb-search input::placeholder {
  color: var(--ink-300);
}
.clear {
  border: 0;
  background: transparent;
  color: var(--ink-300);
  cursor: pointer;
  display: grid;
  place-items: center;
  font-size: 14px;
}
.clear:hover {
  color: var(--ink-500);
}

.tb-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 44px;
  padding: 0 16px;
  border-radius: var(--r-pill);
  border: 1px solid var(--ink-100);
  background: var(--surface);
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 550;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.18s;
  white-space: nowrap;
}
.tb-btn:hover {
  border-color: var(--brand-200);
  color: var(--brand);
}
.tb-btn.active {
  background: var(--ink-900);
  border-color: var(--ink-900);
  color: #fff;
}

.tb-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.tb-chips .chip {
  height: 30px;
  padding: 0 14px;
  font-size: 13px;
}

.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 12px;
}
.result-bar b {
  color: var(--ink-900);
  font-weight: 700;
}

/* ---------- 网格 ---------- */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 18px;
  min-height: 200px;
}
.card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.24s cubic-bezier(0.22, 1, 0.36, 1);
  display: flex;
  flex-direction: column;
}
.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--sh-md);
  border-color: var(--brand-200);
}

.cover-wrap {
  height: 132px;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 14px;
  overflow: hidden;
}
.cover-mask {
  color: #fff;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.2);
  z-index: 1;
}
.cover-mask b {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.2px;
  display: block;
  margin-bottom: 3px;
}
.cover-mask p {
  margin: 0;
  font-size: 11.5px;
  opacity: 0.88;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.cat {
  position: absolute;
  top: 12px;
  left: 12px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.9);
  color: var(--ink-700);
}
.enrolled {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: var(--r-pill);
  background: rgba(47, 163, 107, 0.92);
  color: #fff;
}

.c-body {
  padding: 14px 16px 15px;
  display: flex;
  flex-direction: column;
  gap: 9px;
  flex: 1;
}
.c-body h3 {
  margin: 0;
  font-size: 14.5px;
  font-weight: 650;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: var(--ink-400);
}
.meta span {
  display: flex;
  align-items: center;
  gap: 3px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: auto;
}
.progress-row .bar {
  flex: 1;
}
.progress-row span {
  font-size: 11.5px;
  font-weight: 650;
  color: var(--brand-700);
}

.students {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  font-size: 11.5px;
  color: var(--ink-400);
}
.avatars {
  display: flex;
}
.avatars i {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #fff;
  margin-left: -5px;
}
.avatars i:first-child {
  margin-left: 0;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 26px 0 8px;
}
</style>
