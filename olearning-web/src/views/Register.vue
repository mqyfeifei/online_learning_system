<template>
  <div class="login-page">
    <div class="box">
      <h2>注册账号</h2>
      <p class="muted">教师账号注册后需管理员审核开通</p>

      <el-form :model="form" label-position="top">
        <el-form-item label="用户名（登录用）">
          <el-input v-model="form.username" placeholder="3-20 位" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirm" type="password" show-password />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="注册身份">
          <el-radio-group v-model="form.role">
            <el-radio value="STUDENT">学生</el-radio>
            <el-radio value="TEACHER">教师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          注 册
        </el-button>
      </el-form>

      <div class="tips">
        已有账号？
        <el-link type="primary" @click="router.push('/login')">去登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirm: '',
  realName: '',
  phone: '',
  role: 'STUDENT'
})

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('用户名和密码不能为空')
    return
  }
  if (form.password !== form.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await request.post('/api/auth/register', {
      username: form.username,
      password: form.password,
      realName: form.realName,
      phone: form.phone,
      role: form.role
    })
    ElMessage.success(form.role === 'TEACHER' ? '注册成功，请等待管理员审核' : '注册成功，请登录')
    router.push('/login')
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
  padding: 40px 0;
}
.box {
  width: 420px;
  background: #fff;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(64, 158, 255, 0.12);
}
h2 {
  margin: 0 0 4px;
  font-size: 22px;
}
.muted {
  margin: 0 0 20px;
  color: #909399;
  font-size: 13px;
}
.tips {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: #606266;
}
</style>
