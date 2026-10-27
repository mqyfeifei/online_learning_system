import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/PortalLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', desc: '欢迎回来，今天也保持学习的节奏' }
      },
      {
        path: 'courses',
        name: 'courses',
        component: () => import('@/views/CourseSquare.vue'),
        meta: { title: '课程广场', desc: '挑一门课，按自己的节奏学下去' }
      },
      {
        path: 'courses/:id',
        name: 'courseDetail',
        component: () => import('@/views/CourseDetail.vue'),
        meta: { title: '课程详情', desc: '课程介绍、章节目录与配套资料' }
      },
      {
        path: 'discussions',
        name: 'discussions',
        component: () => import('@/views/Discussions.vue'),
        meta: { title: '讨论区', desc: '把卡住你的问题贴出来，一起解决' }
      },
      {
        path: 'my/courses',
        name: 'myCourses',
        component: () => import('@/views/MyCourses.vue'),
        meta: { title: '我的课程', desc: '继续未完成的进度', requiresAuth: true }
      },
      {
        path: 'learn/:courseId',
        name: 'learn',
        component: () => import('@/views/Learn.vue'),
        meta: { title: '学习中', immersive: true, requiresAuth: true }
      },
      {
        path: 'exam/:paperId',
        name: 'exam',
        component: () => import('@/views/Exam.vue'),
        meta: { title: '在线测验', immersive: true, requiresAuth: true }
      },
      {
        path: 'grades',
        name: 'grades',
        component: () => import('@/views/Grades.vue'),
        meta: { title: '成绩与测验', desc: '作业、测验与学习成果一屏掌握', requiresAuth: true }
      },
      {
        path: 'favorites',
        name: 'favorites',
        component: () => import('@/views/Favorites.vue'),
        meta: { title: '收藏与笔记', desc: '你标记过的课程和记下的要点', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', desc: '资料、密码与学习足迹', requiresAuth: true }
      }
    ]
  },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('@/views/Register.vue') },
  {
    path: '/teacher',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'TEACHER' },
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      {
        path: 'dashboard',
        component: () => import('@/views/teacher/Dashboard.vue'),
        meta: { title: '学情看板' }
      },
      {
        path: 'courses',
        component: () => import('@/views/teacher/CourseManage.vue'),
        meta: { title: '我的课程' }
      },
      {
        path: 'courses/:courseId/chapters',
        component: () => import('@/views/teacher/ChapterManage.vue'),
        meta: { title: '章节与课件' }
      },
      {
        path: 'courses/:courseId/questions',
        component: () => import('@/views/teacher/QuestionManage.vue'),
        meta: { title: '题库管理' }
      },
      {
        path: 'courses/:courseId/papers',
        component: () => import('@/views/teacher/PaperManage.vue'),
        meta: { title: '试卷管理' }
      },
      {
        path: 'courses/:courseId/assignments',
        component: () => import('@/views/teacher/AssignmentManage.vue'),
        meta: { title: '作业与批阅' }
      },
      {
        path: 'courses/:courseId/students',
        component: () => import('@/views/teacher/StudentManage.vue'),
        meta: { title: '学生与成绩' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '平台统计' }
      },
      {
        path: 'users',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'audit',
        component: () => import('@/views/admin/CourseAudit.vue'),
        meta: { title: '课程审核' }
      },
      {
        path: 'categories',
        component: () => import('@/views/admin/CategoryManage.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'notices',
        component: () => import('@/views/admin/NoticeManage.vue'),
        meta: { title: '公告管理' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 路由守卫：未登录跳登录页，角色不符跳回首页
router.beforeEach((to) => {
  const userStore = useUserStore()
  const needAuth = to.matched.some((r) => r.meta.auth || r.meta.requiresAuth)
  const needRole = to.matched.find((r) => r.meta.role)?.meta.role as string | undefined

  if (needAuth && !userStore.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (needRole && userStore.role !== needRole) {
    return { path: '/' }
  }
  return true
})

export default router
