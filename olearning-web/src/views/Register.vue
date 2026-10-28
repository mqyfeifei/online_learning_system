<template>
  <div class="auth">
    <!-- 左侧品牌区 -->
    <aside class="brand-side">
      <div class="bs-top">
        <div class="logo">
          <span class="mark"><el-icon><Reading /></el-icon></span>
          <b>知学在线</b>
        </div>
        <p class="slogan">从第一节课，<br />到最后一门课。</p>
        <p class="desc">
          注册后即可选课、下载课件、参加随堂测验并提交作业。学习进度会自动记录，随时接着上次的地方继续。
        </p>
      </div>

      <div class="steps">
        <div class="step">
          <span class="no">01</span>
          <div>
            <b>注册账号</b>
            <p>学生账号即时可用，教师账号需管理员启用</p>
          </div>
        </div>
        <div class="step">
          <span class="no">02</span>
          <div>
            <b>选一门课</b>
            <p>课程广场里按分类挑，随时可以退课</p>
          </div>
        </div>
        <div class="step">
          <span class="no">03</span>
          <div>
            <b>学完并测验</b>
            <p>章节打卡、随堂测验、作业批阅全部留痕</p>
          </div>
        </div>
      </div>
          <p class="bs-foot">Spring Boot 3 + Vue 3</p>
    </aside>

    <!-- 右侧表单 -->
    <main class="form-side">
      <div class="form-box">
        <h1>创建账号</h1>
        <p class="sub">几步就能开始学习</p>

        <div class="role-pick">
          <button
            v-for="r in roles"
            :key="r.value"
            class="role"
            :class="{ on: form.role === r.value }"
            @click="form.role = r.value"
          >
            <el-icon><component :is="r.icon" /></el-icon>
            <b>{{ r.label }}</b>
            <span>{{ r.tip }}</span>
          </button>
        </div>

        <el-form :model="form" label-position="top">
          <el-form-item :label="form.role === 'TEACHER' ? '职工号（登录用）' : '学号（登录用）'">
            <el-input
              v-model="form.userNo"
              placeholder="4-32 位字母、数字、- 或 _，全平台唯一"
              :prefix-icon="Postcard"
            />
          </el-form-item>
          <el-form-item label="用户名（昵称，可随时改）">
            <el-input v-model="form.username" placeholder="3-20 位，字母或数字" :prefix-icon="User" />
          </el-form-item>
          <div class="two">
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="form.confirm" type="password" show-password placeholder="再输一次" />
            </el-form-item>
          </div>
          <div class="two">
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" placeholder="用于成绩显示" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="选填" maxlength="11" />
            </el-form-item>
          </div>

          <el-alert
            v-if="form.role === 'TEACHER'"
            type="warning"
            :closable="false"
            show-icon
            title="教师账号注册后处于待启用状态，需要管理员在后台开启后才能登录。"
            style="margin-bottom: 14px; border-radius: 10px"
          />

          <el-button
            type="primary"
            size="large"
            class="submit"
            :loading="loading"
            @click="submit"
          >
            注 册
          </el-button>
        </el-form>

        <div class="extra">
          <span class="ol-muted">已经有账号？</span>
          <router-link to="/login" class="link">去登录</router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Postcard } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({
  userNo: '',
  username: '',
  password: '',
  confirm: '',
  realName: '',
  phone: '',
  role: 'STUDENT'
})

const roles = [
  { value: 'STUDENT', label: '我是学生', tip: '选课学习、测验作业', icon: 'Reading' },
  { value: 'TEACHER', label: '我是教师', tip: '建课、出题、批阅', icon: 'Management' }
]

async function submit() {
  const noLabel = form.role === 'TEACHER' ? '职工号' : '学号'
  if (!form.userNo || !form.username || !form.password) {
    ElMessage.warning(`请填写${noLabel}、用户名和密码`)
    return
  }
  if (!/^[A-Za-z0-9_-]{4,32}$/.test(form.userNo)) {
    ElMessage.warning(`${noLabel}为 4-32 位字母、数字、- 或 _`)
    return
  }
  if (form.username.length < 3) {
    ElMessage.warning('用户名至少 3 位')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (form.password !== form.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (form.phone && !/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }

  loading.value = true
  try {
    await request.post('/api/auth/register', {
      userNo: form.userNo.trim(),
      username: form.username,
      password: form.password,
      realName: form.realName || form.username,
      phone: form.phone,
      role: form.role
    })

    if (form.role === 'TEACHER') {
      ElMessage.success('注册成功，请等待管理员启用账号')
      router.push('/login')
      return
    }

    // 学生账号即时可用，注册完直接登录，少一步操作
    // 登录接口认的是学号（userNo），不是用户名
    await userStore.login(form.userNo.trim(), form.password)
    ElMessage.success('注册成功，欢迎加入')
    router.replace('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1.05fr;
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
.bs-top,
.steps,
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
  font-size: 32px;
  font-weight: 780;
  letter-spacing: -1.2px;
  line-height: 1.32;
  margin: 0 0 14px;
}
.desc {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.85;
  color: rgba(253, 247, 241, 0.62);
  max-width: 400px;
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 30px 0;
}
.step {
  display: flex;
  gap: 14px;
  padding: 11px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}
.step:last-child {
  border-bottom: 0;
}
.no {
  font-size: 12px;
  font-weight: 750;
  color: var(--brand-400);
  letter-spacing: 0.5px;
  padding-top: 2px;
}
.step b {
  font-size: 13.5px;
  font-weight: 650;
  display: block;
  margin-bottom: 3px;
}
.step p {
  margin: 0;
  font-size: 11.5px;
  color: rgba(253, 247, 241, 0.48);
  line-height: 1.6;
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
  max-width: 420px;
}
.form-box h1 {
  font-size: 26px;
  font-weight: 760;
  letter-spacing: -0.7px;
  margin: 0 0 6px;
}
.sub {
  margin: 0 0 20px;
  font-size: 13px;
  color: var(--ink-400);
}

.role-pick {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 20px;
}
.role {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 12px 14px;
  border: 1.5px solid var(--ink-100);
  border-radius: var(--r-md);
  background: var(--surface);
  cursor: pointer;
  font-family: var(--font);
  transition: all 0.18s;
  text-align: left;
}
.role .el-icon {
  font-size: 17px;
  color: var(--ink-300);
  margin-bottom: 3px;
}
.role b {
  font-size: 13px;
  font-weight: 650;
  color: var(--ink-900);
}
.role span {
  font-size: 11px;
  color: var(--ink-400);
}
.role:hover {
  border-color: var(--brand-200);
}
.role.on {
  border-color: var(--brand);
  background: var(--brand-50);
}
.role.on .el-icon {
  color: var(--brand);
}

.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.submit {
  width: 100%;
  height: 46px;
  margin-top: 8px;
  font-size: 15px;
  font-weight: 650;
  letter-spacing: 2px;
}
.extra {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 16px;
  font-size: 12.5px;
}
.link {
  color: var(--brand);
  font-weight: 650;
}
.link:hover {
  text-decoration: underline;
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
