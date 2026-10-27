<template>
  <el-upload
    class="file-uploader"
    :http-request="doUpload"
    :show-file-list="false"
    :accept="accept"
    :before-upload="beforeUpload"
  >
    <el-button :size="size" :type="type" :plain="plain" :loading="uploading">
      <el-icon><Upload /></el-icon>
      <span>{{ uploading ? '上传中…' : label }}</span>
    </el-button>
  </el-upload>
</template>

<script setup lang="ts">
/**
 * 统一的上传入口：点按钮就能选文件，选完自动传到 /api/files/upload。
 * 平台上所有需要上传的地方都走这个组件，保证交互和提示一致。
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = withDefaults(
  defineProps<{
    /** 允许的扩展名，如 .pdf,.docx */
    accept?: string
    label?: string
    /** 单文件大小上限，单位 MB */
    maxSize?: number
    size?: 'large' | 'default' | 'small'
    type?: 'primary' | 'success' | 'warning' | 'info' | 'danger' | 'default'
    plain?: boolean
  }>(),
  {
    accept: '',
    label: '选择文件上传',
    maxSize: 100,
    size: 'default',
    type: 'primary',
    plain: true
  }
)

const emit = defineEmits<{
  (e: 'uploaded', data: { url: string; name: string; size: number; type: string }): void
}>()

const uploading = ref(false)

function beforeUpload(file: File) {
  if (props.maxSize && file.size > props.maxSize * 1024 * 1024) {
    ElMessage.warning(`文件不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

async function doUpload(options: any) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const data: any = await request.post('/api/files/upload', fd)
    emit('uploaded', data)
    ElMessage.success('上传成功')
  } catch (e) {
    // request 拦截器已经提示过错误
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.file-uploader {
  display: inline-block;
}
</style>
