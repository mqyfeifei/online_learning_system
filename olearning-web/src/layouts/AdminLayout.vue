<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="brand" @click="router.push('/')">
        📖 在线学习平台
        <div class="sub">{{ isAdmin ? '管理后台' : '教师工作台' }}</div>
      </div>
      <el-menu :default-active="route.path" router class="menu">
        <template v-if="isAdmin">
          <el-menu-item index="/admin/dashboard"><el-icon><DataLine /></el-icon>平台统计</el-menu-item>
          <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
          <el-menu-item index="/admin/audit"><el-icon><Checked /></el-icon>课程审核</el-menu-item>
          <el-menu-item index="/admin/categories"><el-icon><Menu /></el-icon>分类管理</el-menu-item>
          <el-menu-item index="/admin/notices"><el-icon><Bell /></el-icon>公告管理</el-menu-item>
        </template>
        <template v-else>
          <el-menu-item index="/teacher/dashboard"><el-icon><DataLine /></el-icon>学情看板</el-menu-item>
          <el-menu-item index="/teacher/courses"><el-icon><Notebook /></el-icon>我的课程</el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="crumb">{{ pageTitle }}</div>
        <div class="right">
          <el-button text @click="router.push('/')">返回前台</el-button>
          <el-dropdown>
            <span class="user">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.role === 'ADMIN')
const pageTitle = computed(() => (route.meta.title as string) || (isAdmin.value ? '管理后台' : '教师工作台'))

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #1f2d3d;
  color: #fff;
}
.brand {
  height: 64px;
  line-height: 1.3;
  padding-top: 12px;
  text-align: center;
  font-weight: 700;
  font-size: 15px;
  color: #fff;
  cursor: pointer;
}
.sub {
  font-size: 12px;
  color: #a3b1c2;
  font-weight: 400;
}
.menu {
  background: transparent;
  border-right: none;
}
.menu :deep(.el-menu-item) {
  color: #c0c4cc;
}
.menu :deep(.el-menu-item.is-active),
.menu :deep(.el-menu-item:hover) {
  background: #16222e;
  color: #409eff;
}
.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.crumb {
  font-weight: 600;
}
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user {
  cursor: pointer;
  font-size: 14px;
}
.main {
  background: #f5f7fa;
}
</style>
