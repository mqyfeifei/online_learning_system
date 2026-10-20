<template>
  <div class="portal">
    <header class="header">
      <div class="inner">
        <div class="logo" @click="go('/')">
          <span class="book">📖</span> 在线学习平台
        </div>

        <el-menu mode="horizontal" :default-active="active" :ellipsis="false" router class="nav">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/courses">课程广场</el-menu-item>
          <el-menu-item index="/discussions">讨论区</el-menu-item>
          <el-menu-item v-if="userStore.isLogin" index="/my/courses">我的课程</el-menu-item>
          <el-menu-item v-if="userStore.isLogin" index="/grades">我的成绩</el-menu-item>
        </el-menu>

        <div class="right">
          <template v-if="userStore.isLogin">
            <el-dropdown>
              <span class="user">
                <el-avatar :size="28" :src="userStore.userInfo?.avatar">
                  {{ (userStore.userInfo?.realName || userStore.userInfo?.username || 'U').slice(0, 1) }}
                </el-avatar>
                {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="go('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.role === 'TEACHER'" @click="go('/teacher')">
                    教师后台
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.role === 'ADMIN'" @click="go('/admin')">
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="go('/login')">登录</el-button>
            <el-button type="primary" @click="go('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">在线学习平台 · 计算机软件综合开发实训</footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const active = computed(() => (route.path.startsWith('/courses') ? '/courses' : route.path))

function go(path: string) {
  router.push(path)
}

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.portal {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 100;
}
.inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 16px;
}
.logo {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
  cursor: pointer;
  white-space: nowrap;
}
.book {
  margin-right: 4px;
}
.nav {
  flex: 1;
  border-bottom: none;
}
.right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}
.main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 20px 16px 40px;
}
.footer {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 16px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}
</style>
