<template>
  <div class="admin">
    <!-- ============ 深色侧栏 ============ -->
    <aside class="aside" :class="{ narrow: collapsed }">
      <div class="a-brand" @click="router.push('/')">
        <span class="a-mark"><el-icon><Reading /></el-icon></span>
        <div v-show="!collapsed" class="a-brand-text">
          <b>知学在线</b>
          <span>{{ isAdmin ? '管理后台' : '教师工作台' }}</span>
        </div>
      </div>

      <div v-show="!collapsed" class="a-label">功能菜单</div>
      <nav class="a-nav">
        <template v-if="isAdmin">
          <router-link
            v-for="m in adminMenus"
            :key="m.path"
            :to="m.path"
            class="a-item"
            :class="{ active: isActive(m.path) }"
          >
            <el-icon><component :is="m.icon" /></el-icon>
            <span v-show="!collapsed">{{ m.label }}</span>
            <i v-if="m.badge" class="a-badge">{{ m.badge }}</i>
          </router-link>
        </template>
        <template v-else>
          <router-link
            v-for="m in teacherMenus"
            :key="m.path"
            :to="m.path"
            class="a-item"
            :class="{ active: isActive(m.path) }"
          >
            <el-icon><component :is="m.icon" /></el-icon>
            <span v-show="!collapsed">{{ m.label }}</span>
          </router-link>
        </template>
      </nav>

      <div class="a-foot">
        <router-link to="/" class="a-item a-item--ghost">
          <el-icon><House /></el-icon>
          <span v-show="!collapsed">返回学习前台</span>
        </router-link>
        <button class="a-collapse" @click="collapsed = !collapsed">
          <el-icon><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
        </button>
      </div>
    </aside>

    <!-- ============ 右侧 ============ -->
    <div class="a-body">
      <header class="a-top">
        <div class="a-crumb">
          <span class="root">{{ isAdmin ? '管理后台' : '教师工作台' }}</span>
          <el-icon class="sep"><ArrowRight /></el-icon>
          <b>{{ pageTitle }}</b>
        </div>
        <div class="a-tools">
          <span class="role-chip">{{ isAdmin ? '管理员' : '教师' }}</span>
          <el-dropdown trigger="click">
            <div class="a-user">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">{{ initial }}</el-avatar>
              <span class="a-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/')">返回学习前台</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="a-main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const pendingAudit = ref(0)

const isAdmin = computed(() => userStore.role === 'ADMIN')
const pageTitle = computed(
  () => (route.meta.title as string) || (isAdmin.value ? '管理后台' : '教师工作台')
)
const initial = computed(() =>
  (userStore.userInfo?.realName || userStore.userInfo?.username || 'U').slice(0, 1)
)

const teacherMenus = [
  { path: '/teacher/dashboard', label: '学情看板', icon: 'DataBoard' },
  { path: '/teacher/courses', label: '我的课程', icon: 'Notebook' }
]

const adminMenus = computed(() => [
  { path: '/admin/dashboard', label: '平台统计', icon: 'PieChart' },
  { path: '/admin/users', label: '用户管理', icon: 'UserFilled' },
  {
    path: '/admin/audit',
    label: '课程审核',
    icon: 'CircleCheck',
    badge: pendingAudit.value || ''
  },
  { path: '/admin/categories', label: '分类管理', icon: 'Files' },
  { path: '/admin/notices', label: '公告管理', icon: 'Bell' }
])

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

function logout() {
  userStore.logout()
  router.push('/login')
}

onMounted(async () => {
  if (!isAdmin.value) return
  try {
    const page: any = await request.get('/api/admin/courses', { params: { status: 1, size: 1 } })
    pendingAudit.value = page.total || 0
  } catch {
    pendingAudit.value = 0
  }
})
</script>

<style scoped>
.admin {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

/* ---------------- 深色侧栏 ---------------- */
.aside {
  width: 226px;
  flex: none;
  background: #241f1b;
  display: flex;
  flex-direction: column;
  padding: 18px 12px 12px;
  position: sticky;
  top: 0;
  height: 100vh;
  transition: width 0.24s cubic-bezier(0.4, 0, 0.2, 1);
  background-image: radial-gradient(
    circle at 0% 0%,
    rgba(255, 107, 53, 0.18),
    transparent 42%
  );
}
.aside.narrow {
  width: 78px;
}

.a-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 6px 22px;
  cursor: pointer;
}
.a-mark {
  width: 36px;
  height: 36px;
  flex: none;
  border-radius: 11px;
  background: linear-gradient(135deg, var(--brand-400), var(--brand-600));
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 18px;
}
.a-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.a-brand-text b {
  color: #fdf7f1;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.2px;
}
.a-brand-text span {
  color: rgba(253, 247, 241, 0.45);
  font-size: 11px;
}

.a-label {
  font-size: 10.5px;
  font-weight: 650;
  letter-spacing: 1px;
  color: rgba(253, 247, 241, 0.3);
  padding: 0 12px 8px;
}

.a-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.a-item {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 42px;
  padding: 0 12px;
  border-radius: 11px;
  color: rgba(253, 247, 241, 0.62);
  font-size: 13.5px;
  font-weight: 550;
  transition: all 0.18s ease;
  white-space: nowrap;
}
.a-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.a-item.active {
  background: linear-gradient(100deg, var(--brand), var(--brand-600));
  color: #fff;
  box-shadow: 0 8px 18px rgba(255, 107, 53, 0.32);
}
.a-item .el-icon {
  font-size: 17px;
  flex: none;
}
.a-badge {
  margin-left: auto;
  background: #fff;
  color: var(--brand-700);
  font-size: 10.5px;
  font-style: normal;
  font-weight: 750;
  height: 17px;
  min-width: 17px;
  padding: 0 5px;
  border-radius: var(--r-pill);
  display: grid;
  place-items: center;
}

.a-foot {
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 6px;
}
.a-item--ghost {
  flex: 1;
  height: 38px;
  font-size: 12.5px;
  color: rgba(253, 247, 241, 0.45);
}
.a-collapse {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 9px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: transparent;
  color: rgba(253, 247, 241, 0.5);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all 0.18s;
}
.a-collapse:hover {
  color: #fff;
  border-color: rgba(255, 107, 53, 0.6);
}

/* ---------------- 主体 ---------------- */
.a-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.a-top {
  height: 68px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: var(--surface);
  border-bottom: 1px solid var(--ink-100);
  position: sticky;
  top: 0;
  z-index: 10;
}
.a-crumb {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13.5px;
  color: var(--ink-400);
}
.a-crumb b {
  color: var(--ink-900);
  font-weight: 650;
}
.a-crumb .sep {
  font-size: 11px;
  color: var(--ink-300);
}
.a-tools {
  display: flex;
  align-items: center;
  gap: 14px;
}
.role-chip {
  font-size: 11.5px;
  font-weight: 650;
  padding: 4px 10px;
  border-radius: var(--r-pill);
  background: var(--brand-50);
  color: var(--brand-700);
}
.a-user {
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  outline: none;
}
.a-name {
  font-size: 13.5px;
  font-weight: 600;
}
.caret {
  color: var(--ink-300);
  font-size: 12px;
}

.a-main {
  flex: 1;
  padding: 24px 28px 40px;
  min-width: 0;
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.page-leave-to {
  opacity: 0;
}
</style>
