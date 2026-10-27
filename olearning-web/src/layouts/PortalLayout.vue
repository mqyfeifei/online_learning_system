<template>
  <!-- 学习页等沉浸式页面：只留一条极简顶栏 -->
  <div v-if="immersive" class="immersive">
    <header class="immersive-bar">
      <div class="brand" @click="go('/')">
        <span class="brand-mark"><el-icon><Reading /></el-icon></span>
        <span class="brand-name">知学在线</span>
      </div>
      <el-button text @click="go('/my/courses')">
        <el-icon><Back /></el-icon> 返回我的课程
      </el-button>
    </header>
    <main class="immersive-main">
      <router-view />
    </main>
  </div>

  <div v-else class="shell" :class="{ 'is-collapsed': collapsed }">
    <!-- ================= 左侧导航 ================= -->
    <aside class="side">
      <div class="brand" @click="go('/')">
        <span class="brand-mark"><el-icon><Reading /></el-icon></span>
        <span v-show="!collapsed" class="brand-name">知学在线</span>
      </div>

      <nav class="nav">
        <template v-for="group in menus" :key="group.label">
          <div v-show="!collapsed" class="nav-label">{{ group.label }}</div>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
            :title="collapsed ? item.label : ''"
          >
            <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
            <span v-show="!collapsed" class="nav-text">{{ item.label }}</span>
            <span v-if="!collapsed && item.badge" class="nav-badge">{{ item.badge }}</span>
          </router-link>
        </template>
      </nav>

      <div class="side-foot">
        <template v-if="userStore.isLogin">
          <div class="mini-user" @click="go('/profile')">
            <el-avatar :size="34" :src="userStore.userInfo?.avatar">
              {{ initial }}
            </el-avatar>
            <div v-show="!collapsed" class="mini-info">
              <b>{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</b>
              <span>{{ roleText }}</span>
            </div>
          </div>
        </template>
        <template v-else>
          <el-button v-show="!collapsed" type="primary" class="side-cta" @click="go('/login')">
            登录 / 注册
          </el-button>
        </template>
        <button class="collapse-btn" @click="collapsed = !collapsed">
          <el-icon><component :is="collapsed ? 'DArrowRight' : 'DArrowLeft'" /></el-icon>
        </button>
      </div>
    </aside>

    <!-- ================= 右侧主体 ================= -->
    <div class="body">
      <header class="topbar">
        <div class="topbar-title">
          <h1>{{ pageTitle }}</h1>
          <p v-if="pageDesc">{{ pageDesc }}</p>
        </div>

        <div class="topbar-tools">
          <div class="search">
            <el-icon class="search-icon"><Search /></el-icon>
            <input
              v-model="keyword"
              placeholder="搜索课程、教师或关键词"
              @keyup.enter="doSearch"
            />
          </div>

          <el-popover placement="bottom-end" :width="330" trigger="click">
            <template #reference>
              <button class="icon-btn" title="平台公告">
                <el-icon><Bell /></el-icon>
                <i v-if="notices.length" class="dot" />
              </button>
            </template>
            <div class="notice-pop">
              <div class="notice-pop-head">
                <b>平台公告</b>
                <span class="ol-muted">{{ notices.length }} 条</span>
              </div>
              <div v-for="n in notices.slice(0, 5)" :key="n.id" class="notice-row">
                <span v-if="n.topFlag === 1" class="chip chip--brand">置顶</span>
                <div class="notice-body">
                  <b>{{ n.title }}</b>
                  <p>{{ n.content }}</p>
                </div>
              </div>
              <div v-if="!notices.length" class="ol-empty" style="padding: 20px 0">暂无公告</div>
            </div>
          </el-popover>

          <el-dropdown v-if="userStore.isLogin" trigger="click">
            <div class="avatar-btn">
              <el-avatar :size="34" :src="userStore.userInfo?.avatar">{{ initial }}</el-avatar>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="go('/profile')">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item @click="go('/favorites')">
                  <el-icon><Star /></el-icon> 收藏与笔记
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.role === 'TEACHER'" @click="go('/teacher')">
                  <el-icon><Management /></el-icon> 教师工作台
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.role === 'ADMIN'" @click="go('/admin')">
                  <el-icon><Setting /></el-icon> 管理后台
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <template v-else>
            <el-button text @click="go('/login')">登录</el-button>
            <el-button type="primary" @click="go('/register')">免费注册</el-button>
          </template>
        </div>
      </header>

      <main class="main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <footer class="foot">
        知学在线 · 计算机软件综合开发实训项目 · Spring Boot 3 + Vue 3
      </footer>
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
const keyword = ref('')
const notices = ref<any[]>([])

const immersive = computed(() => route.meta.immersive === true)

/** badge 只在个别菜单项上出现（比如待办数），标成可选，否则 TS 会报属性不存在 */
type NavItem = { path: string; label: string; icon: string; badge?: string }
type NavGroup = { label: string; items: NavItem[] }

const menus = computed<NavGroup[]>(() => [
  {
    label: '学习',
    items: [
      { path: '/', label: '首页', icon: 'HomeFilled' },
      { path: '/courses', label: '课程广场', icon: 'Compass' },
      { path: '/my/courses', label: '我的课程', icon: 'Reading' }
    ]
  },
  {
    label: '互动',
    items: [
      { path: '/discussions', label: '讨论区', icon: 'ChatDotRound' },
      { path: '/grades', label: '成绩与测验', icon: 'DataAnalysis' }
    ]
  },
  {
    label: '我的',
    items: [
      { path: '/favorites', label: '收藏与笔记', icon: 'Star' },
      { path: '/profile', label: '个人中心', icon: 'User' }
    ]
  }
])

const pageTitle = computed(() => (route.meta.title as string) || '首页')
const pageDesc = computed(() => (route.meta.desc as string) || '')
const initial = computed(() =>
  (userStore.userInfo?.realName || userStore.userInfo?.username || 'U').slice(0, 1)
)
const roleText = computed(
  () =>
    ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' } as Record<string, string>)[
      userStore.role
    ] || '访客'
)

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(path + '/')
}

function go(path: string) {
  router.push(path)
}

function doSearch() {
  router.push({ path: '/courses', query: keyword.value ? { keyword: keyword.value } : {} })
}

function logout() {
  userStore.logout()
  router.push('/login')
}

onMounted(async () => {
  try {
    notices.value = ((await request.get('/api/public/notices')) as any) || []
  } catch {
    notices.value = []
  }
})
</script>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

/* ---------------- 侧边栏 ---------------- */
.side {
  width: 236px;
  flex: none;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-right: 1px solid var(--ink-100);
  padding: 18px 14px 14px;
  position: sticky;
  top: 0;
  height: 100vh;
  transition: width 0.24s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 20;
}
.is-collapsed .side {
  width: 82px;
  padding: 18px 12px 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px 20px;
  cursor: pointer;
  user-select: none;
}
.brand-mark {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--brand-400), var(--brand-600));
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 20px;
  box-shadow: var(--sh-brand);
}
.brand-name {
  font-size: 17px;
  font-weight: 750;
  letter-spacing: -0.3px;
  white-space: nowrap;
}

.nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-y: auto;
  margin: 0 -4px;
  padding: 0 4px;
}
.nav-label {
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.8px;
  color: var(--ink-300);
  padding: 14px 12px 6px;
  text-transform: uppercase;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 42px;
  padding: 0 12px;
  border-radius: 12px;
  color: var(--ink-500);
  font-size: 14px;
  font-weight: 550;
  transition: all 0.18s ease;
  position: relative;
  white-space: nowrap;
}
.nav-item:hover {
  background: var(--surface-sunken);
  color: var(--ink-900);
}
.nav-item.active {
  background: var(--brand-50);
  color: var(--brand-700);
  font-weight: 650;
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: -4px;
  top: 11px;
  bottom: 11px;
  width: 3px;
  border-radius: var(--r-pill);
  background: var(--brand);
}
.nav-icon {
  font-size: 17px;
  flex: none;
}
.nav-badge {
  margin-left: auto;
  background: var(--brand);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  height: 18px;
  min-width: 18px;
  padding: 0 5px;
  border-radius: var(--r-pill);
  display: grid;
  place-items: center;
}

.side-foot {
  padding-top: 12px;
  border-top: 1px solid var(--ink-100);
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
}
.mini-user {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  min-width: 0;
  padding: 4px;
  border-radius: 12px;
  transition: background 0.18s;
}
.mini-user:hover {
  background: var(--surface-sunken);
}
.mini-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.mini-info b {
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 96px;
}
.mini-info span {
  font-size: 11px;
  color: var(--ink-400);
}
.side-cta {
  flex: 1;
}
.collapse-btn {
  width: 30px;
  height: 30px;
  flex: none;
  border: 1px solid var(--ink-100);
  background: var(--surface);
  border-radius: 9px;
  color: var(--ink-400);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all 0.18s;
}
.collapse-btn:hover {
  color: var(--brand);
  border-color: var(--brand-200);
}

/* ---------------- 右侧 ---------------- */
.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 78px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 0 32px;
  background: rgba(253, 247, 241, 0.86);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--ink-100);
  position: sticky;
  top: 0;
  z-index: 10;
}
.topbar-title h1 {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.4px;
}
.topbar-title p {
  margin: 2px 0 0;
  font-size: 12.5px;
  color: var(--ink-400);
}
.topbar-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  width: 300px;
  padding: 0 14px;
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-pill);
  transition: all 0.2s;
}
.search:focus-within {
  border-color: var(--brand-300);
  box-shadow: 0 0 0 3px var(--brand-50);
}
.search-icon {
  color: var(--ink-300);
  font-size: 15px;
}
.search input {
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 13.5px;
  font-family: var(--font);
  color: var(--ink-900);
  min-width: 0;
}
.search input::placeholder {
  color: var(--ink-300);
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--r-pill);
  border: 1px solid var(--ink-100);
  background: var(--surface);
  color: var(--ink-500);
  cursor: pointer;
  display: grid;
  place-items: center;
  font-size: 16px;
  position: relative;
  transition: all 0.18s;
}
.icon-btn:hover {
  color: var(--brand);
  border-color: var(--brand-200);
}
.dot {
  position: absolute;
  top: 9px;
  right: 10px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--brand);
  border: 2px solid var(--surface);
}

.avatar-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  outline: none;
}
.caret {
  color: var(--ink-300);
  font-size: 12px;
}

.main {
  flex: 1;
  padding: 26px 32px 40px;
  min-width: 0;
}

.foot {
  text-align: center;
  color: var(--ink-300);
  font-size: 12px;
  padding: 18px;
}

/* 公告气泡 */
.notice-pop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--ink-100);
  margin-bottom: 8px;
}
.notice-row {
  display: flex;
  gap: 8px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--ink-100);
}
.notice-row:last-child {
  border-bottom: 0;
}
.notice-body b {
  font-size: 13px;
  display: block;
  margin-bottom: 3px;
}
.notice-body p {
  margin: 0;
  font-size: 12px;
  color: var(--ink-400);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 路由切换动画 */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-to {
  opacity: 0;
}

/* ---------------- 沉浸式 ---------------- */
.immersive {
  min-height: 100vh;
  background: var(--bg);
  display: flex;
  flex-direction: column;
}
.immersive-bar {
  height: 64px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--surface);
  border-bottom: 1px solid var(--ink-100);
}
.immersive-bar .brand {
  padding: 0;
}
.immersive-main {
  flex: 1;
  min-height: 0;
}

@media (max-width: 1080px) {
  .search {
    width: 180px;
  }
  .main {
    padding: 20px 18px 32px;
  }
  .topbar {
    padding: 0 18px;
  }
}
</style>
