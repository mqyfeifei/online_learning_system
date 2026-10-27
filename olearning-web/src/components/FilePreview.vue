<template>
  <el-dialog
    v-model="show"
    append-to-body
    destroy-on-close
    :show-close="true"
    width="min(1100px, 94vw)"
    top="4vh"
    class="pv-dialog"
  >
    <template #header>
      <div class="pv-head">
        <span class="pv-ico" :class="`t-${(data?.fileType || '').slice(0, 4)}`">
          {{ (data?.fileType || 'FILE').toUpperCase().slice(0, 4) }}
        </span>
        <div class="pv-title">
          <b>{{ data?.name || '课件预览' }}</b>
          <span>{{ typeLabel }} · {{ sizeText(data?.fileSize) }}</span>
        </div>
        <span class="grow" />
        <el-button
          v-if="data?.kind === 'pptx' && data?.slides?.length"
          size="small"
          type="primary"
          plain
          @click="startPlay"
        >
          <el-icon><VideoPlay /></el-icon> 放映
        </el-button>
        <el-button
          v-if="data?.downloadable"
          size="small"
          @click="download"
        >
          <el-icon><Download /></el-icon> 下载
        </el-button>
        <el-tag v-else size="small" type="info" effect="plain">仅在线查看</el-tag>
      </div>
    </template>

    <div v-loading="loading" class="pv-body">
      <template v-if="data">
        <!-- PDF：浏览器原生渲染 -->
        <iframe v-if="data.kind === 'pdf'" :src="data.rawUrl" class="pv-frame" title="PDF 预览" />

        <!-- 图片 -->
        <div v-else-if="data.kind === 'image'" class="pv-center">
          <img :src="data.rawUrl" :alt="data.name" class="pv-img" />
        </div>

        <!-- 视频 -->
        <video
          v-else-if="data.kind === 'video'"
          :src="data.rawUrl"
          class="pv-video"
          controls
          autoplay
        />

        <!-- 音频 -->
        <div v-else-if="data.kind === 'audio'" class="pv-center">
          <audio :src="data.rawUrl" controls />
        </div>

        <!-- 纯文本 -->
        <pre v-else-if="data.kind === 'text'" class="pv-text">{{ data.text }}</pre>

        <!-- Word：段落 / 表格 / 图片 -->
        <article v-else-if="data.kind === 'docx'" class="pv-doc">
          <template v-for="(b, i) in data.blocks" :key="i">
            <h4 v-if="b.type === 'paragraph' && b.heading">{{ b.text }}</h4>
            <p v-else-if="b.type === 'paragraph'">{{ b.text }}</p>
            <img v-else-if="b.type === 'image'" :src="b.src" class="pv-doc-img" />
            <table v-else-if="b.type === 'table'" class="pv-table">
              <tbody>
                <tr v-for="(row, ri) in b.rows" :key="ri">
                  <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
                </tr>
              </tbody>
            </table>
          </template>
          <p v-if="!data.blocks?.length" class="pv-empty">文档内容为空</p>
        </article>

        <!-- PPT：逐页浏览 -->
        <div v-else-if="data.kind === 'pptx'" class="pv-slides">
          <aside class="pv-thumbs">
            <button
              v-for="s in data.slides"
              :key="s.index"
              class="pv-thumb"
              :class="{ active: s.index === slideIndex }"
              @click="slideIndex = s.index"
            >
              {{ s.index }}
            </button>
          </aside>
          <section v-if="currentSlide" class="pv-slide">
            <h4>{{ currentSlide.title }}</h4>
            <p v-for="(line, i) in currentSlide.lines" :key="i">{{ line }}</p>
            <img
              v-for="(img, i) in currentSlide.images"
              :key="'img' + i"
              :src="img"
              class="pv-slide-img"
            />
            <div class="pv-slide-foot">
              <el-button size="small" :disabled="slideIndex <= 1" @click="slideIndex--">
                上一页
              </el-button>
              <span>{{ slideIndex }} / {{ data.slides.length }}</span>
              <el-button
                size="small"
                :disabled="slideIndex >= data.slides.length"
                @click="slideIndex++"
              >
                下一页
              </el-button>
            </div>
          </section>
        </div>

        <!-- Excel -->
        <div v-else-if="data.kind === 'xlsx'" class="pv-sheets">          <el-tabs v-model="sheetName">
            <el-tab-pane
              v-for="s in data.sheets"
              :key="s.name"
              :label="s.name"
              :name="s.name"
            >
              <div class="pv-sheet-scroll">
                <table class="pv-table">
                  <tbody>
                    <tr v-for="(row, ri) in s.rows" :key="ri">
                      <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 不支持的类型 -->
        <div v-else class="pv-center pv-unsupported">
          <span class="icon">📄</span>
          <p>{{ data.message || '该格式暂不支持在线预览' }}</p>
          <el-button v-if="data.downloadable" type="primary" @click="download">
            下载后查看
          </el-button>
        </div>
      </template>
    </div>

    <!-- 放映模式：整屏黑底，键盘 ← → 翻页，可自动播放 -->
    <Teleport to="body">
      <div v-if="playing && data?.slides?.length" class="pv-play" @click.self="stopPlay">
        <div class="play-stage">
          <div class="play-slide">
            <h2>{{ playSlide?.title }}</h2>
            <p v-for="(line, i) in playSlide?.lines" :key="i">{{ line }}</p>
            <div v-if="playSlide?.images?.length" class="play-imgs">
              <img v-for="(img, i) in playSlide.images" :key="i" :src="img" />
            </div>
          </div>
          <div class="play-bar">
            <button class="pb" @click="playIndex--" :disabled="playIndex <= 1">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <span class="pb-num">{{ playIndex }} / {{ data.slides.length }}</span>
            <button
              class="pb"
              @click="playIndex++"
              :disabled="playIndex >= data.slides.length"
            >
              <el-icon><ArrowRight /></el-icon>
            </button>
            <span class="pb-sep" />
            <button class="pb" :class="{ on: autoPlay }" @click="toggleAuto">
              <el-icon><VideoPlay /></el-icon> {{ autoPlay ? '暂停' : '自动播放' }}
            </button>
            <select v-model.number="autoSec" class="pb-sel">
              <option :value="3">3 秒/页</option>
              <option :value="5">5 秒/页</option>
              <option :value="8">8 秒/页</option>
            </select>
            <span class="pb-sep" />
            <button class="pb" @click="stopPlay">退出放映 Esc</button>
          </div>
        </div>
      </div>
    </Teleport>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import request from '@/utils/request'
import { openFile } from '@/utils/file'

const props = defineProps<{
  modelValue: boolean
  /** 课件资源 id */
  rid?: number | null
  /** 任意已上传文件的访问地址（作业附件、头像、正文图片等），与 rid 二选一 */
  url?: string | null
  /** 展示用的文件名，url 模式建议传，否则只能从地址里推断 */
  name?: string | null
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const show = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const loading = ref(false)
const data = ref<any>(null)
const slideIndex = ref(1)
const sheetName = ref('')
const playing = ref(false)
const playIndex = ref(1)
const autoPlay = ref(false)
const autoSec = ref(5)
let autoTimer: number | null = null

const playSlide = computed(() => {
  if (!data.value?.slides) return null
  return (
    data.value.slides.find((s: any) => s.index === playIndex.value) || data.value.slides[0]
  )
})

const currentSlide = computed(() => {
  if (!data.value?.slides) return null
  return data.value.slides.find((s: any) => s.index === slideIndex.value) || data.value.slides[0]
})

const typeLabel = computed(() => {
  const map: Record<string, string> = {
    pdf: 'PDF 文档',
    docx: 'Word 文档',
    pptx: 'PowerPoint 幻灯片',
    xlsx: 'Excel 表格',
    image: '图片',
    video: '视频',
    audio: '音频',
    text: '文本'
  }
  return map[data.value?.kind] || '文件'
})

watch(
  () => [props.modelValue, props.rid, props.url],
  async () => {
    if (!props.modelValue) {
      stopPlay()
      return
    }
    if (!props.rid && !props.url) return
    loading.value = true
    data.value = null
    slideIndex.value = 1
    try {
      const params = props.rid ? { rid: props.rid } : { url: props.url, name: props.name }
      data.value = await request.get('/api/public/files/preview', { params })
      if (data.value?.sheets?.length) {
        sheetName.value = data.value.sheets[0].name
      }
    } catch {
      data.value = { kind: 'unsupported', message: '预览失败，请稍后重试或下载查看', downloadable: true }
    } finally {
      loading.value = false
    }
  },
  { immediate: true }
)

function download() {
  if (props.rid) {
    openFile(`/api/public/files/download?rid=${props.rid}`)
  } else if (props.url) {
    openFile(
      `/api/public/files/download?url=${encodeURIComponent(props.url)}` +
        (props.name ? `&name=${encodeURIComponent(props.name)}` : '')
    )
  }
}

// ---------------- 放映 ----------------

function startPlay() {
  playIndex.value = slideIndex.value || 1
  playing.value = true
  document.addEventListener('keydown', onKey)
  if (autoPlay.value) startTimer()
}

function stopPlay() {
  playing.value = false
  autoPlay.value = false
  clearTimer()
  document.removeEventListener('keydown', onKey)
}

function onKey(e: KeyboardEvent) {
  if (!playing.value) return
  if (e.key === 'Escape') stopPlay()
  if (e.key === 'ArrowRight' || e.key === ' ') {
    e.preventDefault()
    next()
  }
  if (e.key === 'ArrowLeft') {
    e.preventDefault()
    prev()
  }
  syncSlide()
}

function next() {
  if (playIndex.value < (data.value?.slides?.length || 1)) playIndex.value++
  syncSlide()
}

function prev() {
  if (playIndex.value > 1) playIndex.value--
  syncSlide()
}

function syncSlide() {
  slideIndex.value = playIndex.value
}

function toggleAuto() {
  autoPlay.value = !autoPlay.value
  if (autoPlay.value) startTimer()
  else clearTimer()
}

function startTimer() {
  clearTimer()
  autoTimer = window.setInterval(() => {
    if (playIndex.value >= (data.value?.slides?.length || 1)) {
      stopPlay()
      return
    }
    next()
  }, autoSec.value * 1000)
}

function clearTimer() {
  if (autoTimer != null) {
    window.clearInterval(autoTimer)
    autoTimer = null
  }
}

watch(autoSec, () => {
  if (autoPlay.value) startTimer()
})

onBeforeUnmount(() => {
  clearTimer()
  document.removeEventListener('keydown', onKey)
})

function sizeText(bytes: number) {
  if (!bytes) return '未知大小'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.pv-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-right: 12px;
}
.pv-ico {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 9.5px;
  font-weight: 750;
  background: var(--surface-sunken);
  color: var(--ink-500);
}
.t-pptx,
.t-ppt {
  background: #fff0e8;
  color: #d2691e;
}
.t-pdf {
  background: #ffeaea;
  color: #c9302c;
}
.t-docx,
.t-doc {
  background: var(--blue-soft);
  color: #2b6cb0;
}
.t-xlsx,
.t-xls {
  background: var(--green-soft);
  color: #1f7a4f;
}
.pv-title {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
  min-width: 0;
}
.pv-title b {
  font-size: 14.5px;
  font-weight: 680;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 520px;
}
.pv-title span {
  font-size: 11.5px;
  color: var(--ink-400);
}
.grow {
  flex: 1;
}

.pv-body {
  height: min(76vh, 820px);
  background: var(--surface-sunken);
  border-radius: var(--r-md);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.pv-frame {
  width: 100%;
  height: 100%;
  border: 0;
  background: #fff;
}
.pv-center {
  flex: 1;
  display: grid;
  place-items: center;
  overflow: auto;
  padding: 16px;
}
.pv-img {
  max-width: 100%;
  max-height: 100%;
  border-radius: var(--r-sm);
  box-shadow: var(--sh-md);
}
.pv-video {
  width: 100%;
  height: 100%;
  background: #000;
}
.pv-text {
  flex: 1;
  overflow: auto;
  margin: 0;
  padding: 24px 28px;
  background: #fff;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.75;
  color: var(--ink-700);
  white-space: pre-wrap;
  word-break: break-word;
}

/* Word */
.pv-doc {
  flex: 1;
  overflow: auto;
  background: #fff;
  padding: 32px 44px;
  line-height: 1.9;
  color: var(--ink-700);
  font-size: 14px;
}
.pv-doc h4 {
  font-size: 16px;
  font-weight: 700;
  margin: 18px 0 8px;
  color: var(--ink-900);
}
.pv-doc p {
  margin: 0 0 8px;
  white-space: pre-wrap;
}
.pv-doc-img {
  max-width: 100%;
  margin: 10px 0;
  border-radius: 8px;
}

/* 表格通用 */
.pv-table {
  border-collapse: collapse;
  margin: 10px 0;
  font-size: 13px;
  width: 100%;
}
.pv-table td {
  border: 1px solid var(--ink-100);
  padding: 7px 10px;
  color: var(--ink-700);
  min-width: 60px;
}
.pv-table tr:first-child td {
  background: var(--surface-sunken);
  font-weight: 600;
}

/* PPT */
.pv-slides {
  flex: 1;
  display: flex;
  min-height: 0;
}
.pv-thumbs {
  width: 68px;
  flex: none;
  overflow-y: auto;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--surface);
  border-right: 1px solid var(--ink-100);
}
.pv-thumb {
  height: 40px;
  border-radius: 8px;
  border: 1px solid var(--ink-100);
  background: var(--surface-soft);
  color: var(--ink-400);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.pv-thumb.active {
  background: var(--brand-50);
  border-color: var(--brand);
  color: var(--brand-700);
}
.pv-slide {
  flex: 1;
  overflow: auto;
  background: #fff;
  padding: 36px 48px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pv-slide h4 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 730;
  color: var(--ink-900);
  line-height: 1.4;
}
.pv-slide p {
  margin: 0;
  font-size: 15px;
  line-height: 1.9;
  color: var(--ink-700);
  border-left: 3px solid var(--brand-100);
  padding-left: 12px;
}
.pv-slide-img {
  max-width: 100%;
  margin-top: 8px;
  border-radius: 10px;
  box-shadow: var(--sh-sm);
}
.pv-slide-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  font-size: 12.5px;
  color: var(--ink-400);
  padding-top: 16px;
}

/* Excel */
.pv-sheets {
  flex: 1;
  overflow: hidden;
  background: #fff;
  padding: 12px 18px;
  display: flex;
  flex-direction: column;
}
.pv-sheets :deep(.el-tabs) {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.pv-sheets :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}
.pv-sheets :deep(.el-tab-pane) {
  height: 100%;
}
.pv-sheet-scroll {
  height: 100%;
  overflow: auto;
}
.pv-sheet-scroll .pv-table td {
  white-space: nowrap;
}

.pv-empty {
  color: var(--ink-400);
  text-align: center;
  padding: 40px 0;
}
.pv-unsupported {
  gap: 10px;
  color: var(--ink-400);
  font-size: 13px;
}
.pv-unsupported .icon {
  font-size: 42px;
  opacity: 0.5;
}

/* ---------- 放映 ---------- */
.pv-play {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: #0d0d10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px 88px;
}
.play-stage {
  width: min(1180px, 100%);
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.play-slide {
  flex: 1;
  background: #fff;
  border-radius: 14px;
  padding: 46px 56px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
}
.play-slide h2 {
  margin: 0 0 6px;
  font-size: 30px;
  font-weight: 760;
  line-height: 1.35;
  color: #1c1c22;
}
.play-slide p {
  margin: 0;
  font-size: 19px;
  line-height: 1.95;
  color: #3a3a44;
  border-left: 4px solid var(--brand-200);
  padding-left: 14px;
}
.play-imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 8px;
}
.play-imgs img {
  max-width: 100%;
  max-height: 46vh;
  border-radius: 10px;
}
.play-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #d8d8de;
}
.pb {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  color: #e8e8ef;
  font-size: 12.5px;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.16s;
}
.pb:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.16);
}
.pb:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.pb.on {
  border-color: var(--brand);
  color: #ffb391;
}
.pb-num {
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  min-width: 64px;
  text-align: center;
}
.pb-sep {
  width: 1px;
  height: 18px;
  background: rgba(255, 255, 255, 0.16);
  margin: 0 4px;
}
.pb-sel {
  height: 32px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.06);
  color: #e8e8ef;
  font-size: 12.5px;
  padding: 0 6px;
  outline: none;
}
</style>
