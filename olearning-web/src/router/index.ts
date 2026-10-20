import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/PortalLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/Home.vue') },
      { path: 'courses', name: 'courses', component: () => import('@/views/CourseSquare.vue') },
      { path: 'courses/:id', name: 'courseDetail', component: () => import('@/views/CourseDetail.vue') },
      { path: 'discussions', name: 'discussions', component: () => import('@/views/Discussions.vue') },
      { path: 'my/courses', name: 'myCourses', component: () => import('@/views/MyCourses.vue'), meta: { auth: true } },
      { path: 'learn/:courseId', name: 'learn', component: () => import('@/views/Learn.vue'), meta: { auth: true } },
      { path: 'exam/:paperId', name: 'exam', component: () => import('@/views/Exam.vue'), meta: { auth: true } },
      { path: 'grades', name: 'grades', component: () => import('@/views/Grades.vue'), meta: { auth: true } },
      { path: 'profile', name: 'profile', component: () => import('@/views/Profile.vue'), meta: { auth: true } }
    ]
  },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('@/views/Register.vue') },
  {
    path: '/teacher',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { auth: true, role: 'TEACHER' },
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      { path: 'dashboard', component: () => import('@/views/teacher/Dashboard.vue') },
      { path: 'courses', component: () => import('@/views/teacher/CourseManage.vue') },
      { path: 'courses/:courseId/chapters', component: () => import('@/views/teacher/ChapterManage.vue') },
      { path: 'courses/:courseId/questions', component: () => import('@/views/teacher/QuestionManage.vue') },
      { path: 'courses/:courseId/papers', component: () => import('@/views/teacher/PaperManage.vue') },
      { path: 'courses/:courseId/assignments', component: () => import('@/views/teacher/AssignmentManage.vue') },
      { path: 'courses/:courseId/students', component: () => import('@/views/teacher/StudentManage.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { auth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'audit', component: () => import('@/views/admin/CourseAudit.vue') },
      { path: 'categories', component: () => import('@/views/admin/CategoryManage.vue') },
      { path: 'notices', component: () => import('@/views/admin/NoticeManage.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳登录页，角色不符跳回首页
router.beforeEach((to) => {
  const userStore = useUserStore()
  const needAuth = to.matched.some((r) => r.meta.auth)
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
