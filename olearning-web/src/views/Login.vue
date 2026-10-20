<template>
  <div class="login-page">
    <div class="box">
      <h2>在线学习平台</h2>
      <p class="muted">计算机软件综合开发实训项目</p>

      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password size="large"
                    placeholder="请输入密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>

      <div class="tips">
        还没有账号？
        <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
      </div>

      <el-divider>演示账号（点击填充）</el-divider>
      <div class="demo">
        <el-tag @click="fill('admin')">admin / 123456</el-tag>
        <el-tag type="success" @click="fill('teacher1')">teacher1 / 123456</el-tag>
        <el-tag type="warning" @click="fill('student1')">student1 / 123456</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({ username: '', password: '' })

function fill(name: string) {
  form.username = name
  form.password = '123456'
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data: any = await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect as string
    if (redirect) {
      router.push(redirect)
    } else if (data.role === 'ADMIN') {
      router.push('/admin')
    } else if (data.role === 'TEACHER') {
      router.push('/teacher')
    } else {
      router.push('/')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f3ff 0%, #f5f7fa 60%);
}
.box {
  width: 400px;
  background: #fff;
  padding: 36px 32px;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(64, 158, 255, 0.12);
}
h2 {
  margin: 0 0 4px;
  font-size: 22px;
}
.muted {
  margin: 0 0 24px;
  color: #909399;
  font-size: 13px;
}
.tips {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: #606266;
}
.demo {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}
.demo .el-tag {
  cursor: pointer;
}
</style>
