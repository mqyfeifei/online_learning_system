<template>
  <div class="page-card" v-loading="loading">
    <div class="page-title">个人中心</div>
    <el-row :gutter="32">
      <el-col :span="12">
        <el-form label-width="90px">
          <el-form-item label="用户名">
            <el-input v-model="profile.username" disabled />
          </el-form-item>
          <el-form-item label="角色">
            <el-tag>{{ roleName }}</el-tag>
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="profile.realName" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profile.phone" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveProfile">保存修改</el-button>
          </el-form-item>
        </el-form>
      </el-col>

      <el-col :span="12">
        <el-form label-width="90px">
          <el-form-item label="原密码">
            <el-input v-model="pwd.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwd.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" @click="changePwd">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const profile = ref<any>({})
const pwd = reactive({ oldPassword: '', newPassword: '' })

const roleName = computed(() => {
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[profile.value.role] || profile.value.role
})

async function load() {
  loading.value = true
  try {
    profile.value = await request.get('/api/user/profile')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  await request.put('/api/user/profile', {
    realName: profile.value.realName,
    phone: profile.value.phone,
    avatar: profile.value.avatar
  })
  ElMessage.success('已保存')
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  await request.put('/api/user/password', pwd)
  ElMessage.success('密码已修改，请重新登录')
  localStorage.clear()
  window.location.href = '/login'
}

onMounted(load)
</script>
