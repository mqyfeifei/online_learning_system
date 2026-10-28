<template>
  <div class="auth">
    <!-- 左侧品牌区 -->
    <aside class="brand-side">
      <div class="bs-top">
        <div class="logo">
          <span class="mark"><el-icon><Reading /></el-icon></span>
          <b>知学在线</b>
        </div>
        <p class="slogan">把一门课，真正学完。</p>
        <p class="desc">
          课程章节、课件下载、随堂测验、作业批改与学情统计，一个平台走完整个学习闭环。
        </p>
      </div>

      <div class="bs-cards">
        <div class="bs-card">
          <span class="bc-ico"><el-icon><Notebook /></el-icon></span>
          <div>
            <b>4 门专业必修课</b>
            <p>计算机网络 / 数据结构 / 操作系统 / 数据库</p>
          </div>
        </div>
        <div class="bs-card">
          <span class="bc-ico"><el-icon><FolderOpened /></el-icon></span>
          <div>
            <b>28 份真实课件</b>
            <p>随课程章节挂载，随时下载复习</p>
          </div>
        </div>
        <div class="bs-card">
          <span class="bc-ico"><el-icon><TrendCharts /></el-icon></span>
          <div>
            <b>学习进度可视化</b>
            <p>完成率、周活跃、成绩趋势一目了然</p>
          </div>
        </div>
      </div>

      <p class="bs-foot">Spring Boot 3 + Vue 3</p>
    </aside>

    <!-- 右侧表单 -->
    <main class="form-side">
      <div class="form-box">
        <h1>欢迎回来</h1>
        <p class="sub">登录后继续你的学习进度</p>

        <el-form :model="form" label-position="top" @keyup.enter="submit">
          <el-form-item label="学号 / 职工号">
            <el-input
              v-model="form.account"
              size="large"
              placeholder="学生填学号，教师填职工号"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              size="large"
              placeholder="请输入密码"
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit"
            :loading="loading"
            @click="submit"
          >
            登 录
          </el-button>
        </el-form>

        <div class="extra">
          <span class="ol-muted">还没有账号？</span>
          <router-link to="/register" class="link">立即注册</router-link>
        </div>

        <div class="demo">
          <div class="demo-head">
            <el-icon><InfoFilled /></el-icon> 演示账号（密码均为 123456）
          </div>
          <div class="demo-grid">
            <button
              v-for="d in demos"
              :key="d.account"
              class="demo-btn"
              @click="fill(d.account)"
            >
              <b>{{ d.label }}</b>
              <span>{{ d.account }}</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({ account: '', password: '' })

const demos = [
  { label: '学生', account: '20210001' },
  { label: '教师', account: 'T000001' },
  { label: '管理员', account: 'A000001' }
]

function fill(account: string) {
  form.account = account
  form.password = '123456'
}

async function submit() {
  if (!form.account || !form.password) {
    ElMessage.warning('请输入学号/职工号和密码')
    return
  }
  loading.value = true
  try {
    const data: any = await userStore.login(form.account, form.password)
    ElMessage.success(`欢迎回来，${data.realName || data.username}`)
    // 登录后按角色落到该去的地方，其次回到拦截前的页面
    const redirect = route.query.redirect as string
    if (redirect) {
      router.replace(redirect)
    } else if (data.role === 'TEACHER') {
      router.replace('/teacher')
    } else if (data.role === 'ADMIN') {
      router.replace('/admin')
    } else {
      router.replace('/')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  background: var(--bg);
}

/* ---------- 品牌区 ---------- */
.brand-side {
  background: linear-gradient(150deg, #2b2622 0%, #3a2f26 46%, #5a3620 100%);
  color: #fdf7f1;
  padding: 48px 52px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}
.brand-side::before {
  content: '';
  position: absolute;
  right: -140px;
  top: -120px;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 107, 53, 0.42), transparent 66%);
}
.brand-side::after {
  content: '';
  position: absolute;
  left: -100px;
  bottom: -120px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(246, 185, 59, 0.22), transparent 68%);
}
.bs-top,
.bs-cards,
.bs-foot {
  position: relative;
  z-index: 1;
}
.logo {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 34px;
}
.mark {
  width: 40px;
  height: 40px;
  border-radius: 13px;
  background: linear-gradient(135deg, var(--brand-400), var(--brand-600));
  display: grid;
  place-items: center;
  font-size: 20px;
  box-shadow: 0 10px 24px rgba(255, 107, 53, 0.4);
}
.logo b {
  font-size: 18px;
  font-weight: 750;
  letter-spacing: -0.3px;
}
.slogan {
  font-size: 34px;
  font-weight: 780;
  letter-spacing: -1.2px;
  line-height: 1.28;
  margin: 0 0 12px;
}
.desc {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.85;
  color: rgba(253, 247, 241, 0.62);
  max-width: 400px;
}

.bs-cards {
  display: flex;
  flex-direction: column;
  gap: 11px;
  margin: 36px 0;
}
.bs-card {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 13px 16px;
  border-radius: var(--r-md);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.09);
  backdrop-filter: blur(6px);
}
.bc-ico {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: rgba(255, 107, 53, 0.2);
  color: #ff9a62;
  font-size: 16px;
}
.bs-card b {
  font-size: 13.5px;
  font-weight: 650;
  display: block;
  margin-bottom: 2px;
}
.bs-card p {
  margin: 0;
  font-size: 11.5px;
  color: rgba(253, 247, 241, 0.5);
}
.bs-foot {
  font-size: 11.5px;
  color: rgba(253, 247, 241, 0.34);
  margin: 0;
}

/* ---------- 表单区 ---------- */
.form-side {
  display: grid;
  place-items: center;
  padding: 40px 24px;
}
.form-box {
  width: 100%;
  max-width: 372px;
}
.form-box h1 {
  font-size: 27px;
  font-weight: 760;
  letter-spacing: -0.7px;
  margin: 0 0 6px;
}
.sub {
  margin: 0 0 28px;
  font-size: 13px;
  color: var(--ink-400);
}
.submit {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  font-size: 15px;
  font-weight: 650;
  letter-spacing: 2px;
}
.extra {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 18px;
  font-size: 12.5px;
}
.link {
  color: var(--brand);
  font-weight: 650;
}
.link:hover {
  text-decoration: underline;
}

.demo {
  margin-top: 30px;
  padding-top: 18px;
  border-top: 1px dashed var(--ink-100);
}
.demo-head {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: var(--ink-400);
  margin-bottom: 11px;
}
.demo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.demo-btn {
  border: 1px solid var(--ink-100);
  background: var(--surface);
  border-radius: var(--r-sm);
  padding: 9px 6px;
  cursor: pointer;
  font-family: var(--font);
  transition: all 0.18s;
}
.demo-btn:hover {
  border-color: var(--brand-300);
  background: var(--brand-50);
}
.demo-btn b {
  display: block;
  font-size: 12.5px;
  font-weight: 650;
  color: var(--ink-900);
  margin-bottom: 1px;
}
.demo-btn span {
  font-size: 10.5px;
  color: var(--ink-400);
}

@media (max-width: 900px) {
  .auth {
    grid-template-columns: 1fr;
  }
  .brand-side {
    display: none;
  }
}
</style>
