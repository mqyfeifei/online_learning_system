<template>
  <div class="fav fade-up">
    <!-- 顶部切换 -->
    <section class="head-card">
      <div class="seg">
        <button class="seg-btn" :class="{ active: tab === 'fav' }" @click="tab = 'fav'">
          <el-icon><Star /></el-icon> 收藏课程
          <i>{{ favorites.length }}</i>
        </button>
        <button class="seg-btn" :class="{ active: tab === 'note' }" @click="tab = 'note'">
          <el-icon><Notebook /></el-icon> 学习笔记
          <i>{{ notes.length }}</i>
        </button>
      </div>
      <el-button v-if="tab === 'note'" type="primary" @click="openEditor()">
        <el-icon><Plus /></el-icon> 新建笔记
      </el-button>
    </section>

    <!-- ==================== 收藏 ==================== -->
    <div v-if="tab === 'fav'" v-loading="loadingFav">
      <div v-if="favorites.length" class="fav-grid">
        <article
          v-for="c in favorites"
          :key="c.id"
          class="fav-card"
          @click="router.push(`/courses/${c.id}`)"
        >
          <div class="fc-cover" :style="{ background: coverOf(c.id) }">
            <span>{{ c.title }}</span>
            <button class="unfav" title="取消收藏" @click.stop="removeFav(c)">
              <el-icon><StarFilled /></el-icon>
            </button>
          </div>
          <div class="fc-body">
            <b>{{ c.title }}</b>
            <p>{{ c.description }}</p>
            <div class="fc-meta">
              <span>{{ c.teacherName }}</span>
              <span>{{ c.chapterCount || 0 }} 章</span>
              <span v-if="c.enrolled" class="chip chip--green">已选</span>
            </div>
            <div v-if="c.enrolled" class="bar">
              <i :style="{ width: (c.progress || 0) + '%' }" />
            </div>
          </div>
        </article>
      </div>
      <div v-else class="ol-empty ol-card">
        <span class="icon">⭐</span>
        还没有收藏课程，在课程详情页点一下「收藏课程」
      </div>
    </div>

    <!-- ==================== 笔记 ==================== -->
    <div v-else v-loading="loadingNote">
      <div v-if="notes.length" class="note-grid">
        <article v-for="n in notes" :key="n.id" class="note-card">
          <div class="nc-top">
            <span class="chip chip--brand">{{ n.courseTitle }}</span>
            <span v-if="n.chapterTitle" class="nc-ch">{{ n.chapterTitle }}</span>
            <span v-if="n.refType" class="chip chip--violet">{{ refName(n.refType) }}</span>
          </div>
          <h3>{{ n.title }}</h3>
          <div class="nc-body" v-html="n.content" />
          <div class="nc-foot">
            <span class="ol-muted">{{ fmt(n.updateTime) }}</span>
            <div class="nc-ops">
              <button @click="openEditor(n)">编辑</button>
              <button class="danger" @click="removeNote(n)">删除</button>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="ol-empty ol-card">
        <span class="icon">📝</span>
        还没有笔记，学习页右侧可以随手记录
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog append-to-body v-model="editor" :title="form.id ? '编辑笔记' : '新建笔记'" width="560px">
      <el-form label-position="top">
        <el-form-item label="所属课程">
          <el-select v-model="form.courseId" placeholder="选择课程" style="width: 100%">
            <el-option v-for="c in myCourses" :key="c.id" :label="c.title" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题（可留空，自动取正文前 20 字）">
          <el-input v-model="form.title" placeholder="例如：二叉树遍历手写技巧" />
        </el-form-item>
        <el-form-item label="内容">
          <RichEditor
            v-model="form.content"
            :min-height="180"
            placeholder="写下要点、易错点或自己的理解；截图可以直接 Ctrl+V 粘贴进来"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editor = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { coverOf } from '@/utils/cover'
import RichEditor from '@/components/RichEditor.vue'

const router = useRouter()

const tab = ref<'fav' | 'note'>('fav')
const favorites = ref<any[]>([])
const notes = ref<any[]>([])
const myCourses = ref<any[]>([])
const loadingFav = ref(false)
const loadingNote = ref(false)

const editor = ref(false)
const form = ref<any>({
  id: null,
  courseId: null,
  refType: null,
  refId: null,
  title: '',
  content: ''
})

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

/** 富文本判空：去掉标签和图片后再看有没有内容 */
function plain(html: string) {
  return (html || '')
    .replace(/<img[^>]*>/gi, ' [图片] ')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
}

function refName(type: string) {
  return ({ ASSIGNMENT: '作业笔记', EXAM: '测验笔记' } as Record<string, string>)[type] || '关联笔记'
}

async function loadFav() {
  loadingFav.value = true
  try {
    favorites.value = ((await request.get('/api/my/favorites')) as any) || []
  } finally {
    loadingFav.value = false
  }
}

async function loadNotes() {
  loadingNote.value = true
  try {
    notes.value = ((await request.get('/api/my/notes')) as any) || []
  } finally {
    loadingNote.value = false
  }
}

async function removeFav(c: any) {
  await request.post(`/api/courses/${c.id}/favorite`)
  ElMessage.success('已取消收藏')
  loadFav()
}

function openEditor(n?: any) {
  if (n) {
    form.value = {
      id: n.id,
      courseId: n.courseId,
      refType: n.refType || null,
      refId: n.refId || null,
      title: n.title,
      content: n.content
    }
  } else {
    form.value = {
      id: null,
      courseId: myCourses.value[0]?.id || null,
      refType: null,
      refId: null,
      title: '',
      content: ''
    }
  }
  editor.value = true
}

async function save() {
  if (!form.value.courseId) {
    ElMessage.warning('请选择所属课程')
    return
  }
  if (!plain(form.value.content)) {
    ElMessage.warning('笔记内容不能为空')
    return
  }
  await request.post('/api/my/notes', {
    id: form.value.id,
    courseId: form.value.courseId,
    chapterId: null,
    refType: form.value.refType || null,
    refId: form.value.refId || null,
    title: form.value.title,
    content: form.value.content
  })
  ElMessage.success('已保存')
  editor.value = false
  loadNotes()
}

async function removeNote(n: any) {
  await request.delete(`/api/my/notes/${n.id}`)
  ElMessage.success('已删除')
  loadNotes()
}

onMounted(async () => {
  loadFav()
  loadNotes()
  myCourses.value = ((await request.get('/api/my/courses')) as any) || []
})
</script>

<style scoped>
.fav {
  max-width: 1240px;
}

.head-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 12px 16px;
  margin-bottom: 16px;
}
.seg {
  display: flex;
  gap: 6px;
  background: var(--surface-sunken);
  padding: 4px;
  border-radius: var(--r-pill);
}
.seg-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border: 0;
  border-radius: var(--r-pill);
  background: transparent;
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.18s;
}
.seg-btn:hover {
  color: var(--ink-900);
}
.seg-btn.active {
  background: var(--surface);
  color: var(--ink-900);
  box-shadow: var(--sh-xs);
}
.seg-btn i {
  font-style: normal;
  font-size: 11px;
  font-weight: 700;
  background: var(--brand-50);
  color: var(--brand-700);
  padding: 1px 6px;
  border-radius: var(--r-pill);
}
.seg-btn.active i {
  background: var(--brand);
  color: #fff;
}

/* ---------- 收藏网格 ---------- */
.fav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.fav-card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.22s;
}
.fav-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--sh-md);
  border-color: var(--brand-200);
}
.fc-cover {
  height: 118px;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 14px;
  color: #fff;
  font-size: 15.5px;
  font-weight: 700;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.18);
}
.unfav {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: var(--amber);
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 15px;
  transition: transform 0.16s;
}
.unfav:hover {
  transform: scale(1.1);
}
.fc-body {
  padding: 13px 15px 15px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.fc-body b {
  font-size: 14px;
  font-weight: 650;
}
.fc-body p {
  margin: 0;
  font-size: 12px;
  color: var(--ink-400);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}
.fc-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11.5px;
  color: var(--ink-400);
}

/* ---------- 笔记 ---------- */
.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.note-card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 17px 18px;
  display: flex;
  flex-direction: column;
  gap: 9px;
  transition: all 0.2s;
}
.note-card:hover {
  border-color: var(--brand-200);
  box-shadow: var(--sh-sm);
}
.nc-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nc-ch {
  font-size: 11px;
  color: var(--ink-400);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.note-card h3 {
  margin: 0;
  font-size: 14.5px;
  font-weight: 680;
  line-height: 1.4;
}
.nc-body {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--ink-500);
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.nc-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 4px 0;
  border: 1px solid var(--ink-100);
}
.nc-body :deep(h2),
.nc-body :deep(h3) {
  font-size: 13px;
  margin: 4px 0;
}
.nc-body :deep(blockquote) {
  margin: 4px 0;
  padding: 4px 10px;
  border-left: 3px solid var(--brand-200);
  background: var(--brand-50);
}
.nc-body :deep(ul),
.nc-body :deep(ol) {
  padding-left: 18px;
  margin: 4px 0;
}
.nc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--ink-100);
}
.nc-ops {
  display: flex;
  gap: 12px;
}
.nc-ops button {
  border: 0;
  background: transparent;
  color: var(--ink-400);
  font-size: 12px;
  font-weight: 550;
  font-family: var(--font);
  cursor: pointer;
  padding: 0;
}
.nc-ops button:hover {
  color: var(--brand);
}
.nc-ops .danger:hover {
  color: #c0392b;
}
</style>
