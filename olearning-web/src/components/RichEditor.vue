<template>
  <div
    class="rich"
    :class="{ 'is-readonly': readonly, 'is-focus': focused }"
    :style="{ '--rich-min': minHeight + 'px' }"
  >
    <div v-if="!readonly" class="rich-bar">
      <button
        v-for="btn in buttons"
        :key="btn.cmd + (btn.value || '')"
        type="button"
        class="rb"
        :title="btn.title"
        :class="{ active: activeCmd === btn.cmd + (btn.value || '') }"
        @mousedown.prevent="exec(btn)"
      >
        <span v-if="btn.text" class="rb-text" :style="btn.style">{{ btn.text }}</span>
        <el-icon v-else><component :is="btn.icon" /></el-icon>
      </button>
      <span class="rb-sep" />
      <button type="button" class="rb" title="插入图片（也可直接粘贴截图）" @mousedown.prevent="pickImage">
        <el-icon><Picture /></el-icon>
      </button>
      <span class="rb-grow" />
      <span class="rb-count">{{ count }}/{{ maxLength }}</span>
    </div>

    <div
      ref="box"
      class="rich-body"
      :class="{ empty: !hasContent }"
      :contenteditable="!readonly"
      :data-placeholder="placeholder"
      @input="onInput"
      @paste="onPaste"
      @focus="focused = true"
      @blur="onBlur"
      @keyup="syncActive"
      @mouseup="syncActive"
      @drop="onDrop"
    />

    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      class="hidden-file"
      @change="onFilePicked"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 轻量富文本编辑器：不引第三方编辑器，避免打包体积和样式冲突。
 * - 工具条覆盖加粗/斜体/下划线/标题/列表/引用/代码/图片
 * - 支持直接粘贴截图：图片会先上传到 /api/files/upload，再插入 <img>
 * - 粘贴外部 HTML 时只保留纯文本，避免把别的网站样式带进来
 * - readonly 时退化为纯展示（用于「已提交」的作业、答卷回看）
 */
import { computed, onMounted, ref, watch } from 'vue'
import {
  Bottom,
  Brush,
  Cellphone,
  Grid,
  List,
  Minus,
  Picture,
  RefreshLeft
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
    readonly?: boolean
    minHeight?: number
    maxLength?: number
  }>(),
  {
    placeholder: '在这里输入内容…',
    readonly: false,
    minHeight: 120,
    maxLength: 20000
  }
)

const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const box = ref<HTMLElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const focused = ref(false)
const activeCmd = ref('')
const count = ref(0)

const hasContent = computed(() => {
  const html = props.modelValue || ''
  return html.replace(/<[^>]+>/g, '').trim().length > 0 || html.includes('<img')
})

const buttons = [
  { cmd: 'bold', text: 'B', style: 'font-weight:800', title: '加粗 (Ctrl+B)' },
  { cmd: 'italic', text: 'I', style: 'font-style:italic;font-family:Georgia', title: '斜体 (Ctrl+I)' },
  { cmd: 'underline', text: 'U', style: 'text-decoration:underline', title: '下划线' },
  { cmd: 'strikeThrough', icon: Minus, title: '删除线' },
  { cmd: 'formatBlock', value: 'h2', text: 'H2', style: 'font-weight:800', title: '标题' },
  { cmd: 'formatBlock', value: 'h3', text: 'H3', style: 'font-weight:700', title: '小标题' },
  { cmd: 'insertUnorderedList', icon: List, title: '无序列表' },
  { cmd: 'insertOrderedList', icon: Grid, title: '有序列表' },
  { cmd: 'formatBlock', value: 'blockquote', icon: Bottom, title: '引用' },
  { cmd: 'formatBlock', value: 'pre', icon: Cellphone, title: '代码块' },
  { cmd: 'removeFormat', icon: Brush, title: '清除格式' },
  { cmd: 'undo', icon: RefreshLeft, title: '撤销' }
]

function exec(btn: any) {
  if (props.readonly) return
  box.value?.focus()
  document.execCommand(btn.cmd, false, btn.value)
  onInput()
  syncActive()
}

function syncActive() {
  try {
    const cmd = (document as any).queryCommandState
    activeCmd.value = cmd && document.queryCommandState('bold') ? 'bold' : ''
  } catch {
    activeCmd.value = ''
  }
}

function onInput() {
  if (!box.value) return
  let html = box.value.innerHTML
  // contenteditable 空内容时会留下 <br>，统一清掉，便于 placeholder 显示
  if (html === '<br>' || html === '<div><br></div>') html = ''
  const text = html.replace(/<[^>]+>/g, '')
  count.value = text.length
  emit('update:modelValue', html)
}

function onBlur() {
  focused.value = false
}

let lastRange: Range | null = null

function saveRange() {
  const sel = window.getSelection()
  if (sel && sel.rangeCount && box.value?.contains(sel.anchorNode)) {
    lastRange = sel.getRangeAt(0).cloneRange()
  }
}

function restoreRange() {
  if (!lastRange) return
  const sel = window.getSelection()
  sel?.removeAllRanges()
  sel?.addRange(lastRange)
}

/** 在光标处插入一段 HTML */
function insertHtml(html: string) {
  box.value?.focus()
  restoreRange()
  document.execCommand('insertHTML', false, html)
  onInput()
}

async function uploadImage(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  const data: any = await request.post('/api/files/upload', fd)
  return data?.url as string
}

function onPaste(e: ClipboardEvent) {
  if (props.readonly) return
  const items = e.clipboardData?.items
  if (!items) return

  // 1) 截图 / 复制的图片文件
  for (const item of Array.from(items)) {
    if (item.kind === 'file' && item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (!file) continue
      e.preventDefault()
      saveRange()
      const loading = ElMessage({ message: '图片上传中…', duration: 0 })
      uploadImage(file)
        .then((url) => insertHtml(`<img src="${url}" alt="粘贴的图片" />`))
        .catch(() => ElMessage.error('图片上传失败'))
        .finally(() => loading.close())
      return
    }
  }

  // 2) 外部 HTML：只取纯文本，避免带入站外样式
  const html = e.clipboardData.getData('text/html')
  if (html) {
    e.preventDefault()
    saveRange()
    const text = new DOMParser().parseFromString(html, 'text/html').body.innerText
    insertHtml(escapeHtml(text).replace(/\n/g, '<br>'))
  }
}

function onDrop(e: DragEvent) {
  if (props.readonly) return
  const file = e.dataTransfer?.files?.[0]
  if (file && file.type.startsWith('image/')) {
    e.preventDefault()
    const loading = ElMessage({ message: '图片上传中…', duration: 0 })
    uploadImage(file)
      .then((url) => {
        box.value?.focus()
        document.execCommand('insertHTML', false, `<img src="${url}" alt="拖入的图片" />`)
        onInput()
      })
      .finally(() => loading.close())
  }
}

function pickImage() {
  fileInput.value?.click()
}

async function onFilePicked(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  box.value?.focus()
  saveRange()
  const loading = ElMessage({ message: '图片上传中…', duration: 0 })
  try {
    const url = await uploadImage(file)
    insertHtml(`<img src="${url}" alt="${file.name}" />`)
  } catch {
    ElMessage.error('图片上传失败')
  } finally {
    loading.close()
  }
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

/** 外部改值（比如回填草稿）时同步到 DOM */
watch(
  () => props.modelValue,
  (v) => {
    if (!box.value) return
    if (box.value.innerHTML !== (v || '')) {
      box.value.innerHTML = v || ''
      count.value = (v || '').replace(/<[^>]+>/g, '').length
    }
  }
)

onMounted(() => {
  if (box.value) {
    box.value.innerHTML = props.modelValue || ''
    count.value = (props.modelValue || '').replace(/<[^>]+>/g, '').length
  }
})

defineExpose({ focus: () => box.value?.focus() })
</script>

<style scoped>
.rich {
  border: 1px solid var(--ink-200);
  border-radius: var(--r-sm);
  background: var(--surface);
  transition: border-color 0.18s, box-shadow 0.18s;
  overflow: hidden;
}
.rich.is-focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-50);
}
.rich-bar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 6px;
  border-bottom: 1px solid var(--ink-100);
  background: var(--surface-soft);
  flex-wrap: wrap;
}
.rb {
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--ink-500);
  display: grid;
  place-items: center;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.14s;
}
.rb:hover {
  background: var(--surface);
  color: var(--ink-900);
  box-shadow: var(--sh-xs);
}
.rb.active {
  background: var(--brand-50);
  color: var(--brand);
}
.rb-text {
  font-size: 12.5px;
  line-height: 1;
}
.rb-sep {
  width: 1px;
  height: 16px;
  background: var(--ink-200);
  margin: 0 4px;
}
.rb-grow {
  flex: 1;
}
.rb-count {
  font-size: 11px;
  color: var(--ink-300);
  padding-right: 4px;
}
.rich-body {
  padding: 10px 12px;
  min-height: var(--rich-min, 120px);
  max-height: 460px;
  overflow: auto;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ink-700);
  outline: none;
  word-break: break-word;
}
.rich-body.empty::before {
  content: attr(data-placeholder);
  color: var(--ink-300);
  pointer-events: none;
}
.rich-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 6px 0;
  display: block;
  border: 1px solid var(--ink-100);
}
.rich-body :deep(h2) {
  font-size: 17px;
  font-weight: 720;
  margin: 10px 0 6px;
  color: var(--ink-900);
}
.rich-body :deep(h3) {
  font-size: 15px;
  font-weight: 680;
  margin: 8px 0 4px;
  color: var(--ink-900);
}
.rich-body :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 12px;
  border-left: 3px solid var(--brand-200);
  background: var(--brand-50);
  color: var(--ink-600);
  border-radius: 0 8px 8px 0;
}
.rich-body :deep(pre) {
  margin: 8px 0;
  padding: 10px 12px;
  background: var(--surface-sunken);
  border-radius: 8px;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12.5px;
  white-space: pre-wrap;
}
.rich-body :deep(ul),
.rich-body :deep(ol) {
  padding-left: 22px;
  margin: 6px 0;
}
.rich.is-readonly {
  border-color: var(--ink-100);
  background: var(--surface-soft);
}
.rich.is-readonly .rich-body {
  max-height: none;
}
.hidden-file {
  display: none;
}
</style>
