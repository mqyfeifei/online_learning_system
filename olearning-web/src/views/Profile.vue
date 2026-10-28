<template>
  <div v-loading="loading" class="profile fade-up">
    <div class="layout">
      <!-- ==================== 左：资料卡 ==================== -->
      <div class="col-left">
        <section class="id-card">
          <div class="id-bg" />
          <div class="id-body">
            <el-avatar :size="76" :src="profile.avatar" class="id-avatar">
              {{ (profile.realName || profile.username || 'U').slice(0, 1) }}
            </el-avatar>
            <h2>{{ profile.realName || profile.username }}</h2>
            <div class="id-tags">
              <span class="chip chip--brand">{{ roleName }}</span>
              <span class="chip">{{ noLabel }} {{ profile.userNo || '—' }}</span>
            </div>
            <p class="id-phone">
              <el-icon><Iphone /></el-icon>{{ profile.phone || '未填写手机号' }}
            </p>
          </div>

          <div class="id-stats">
            <div>
              <b>{{ ov.courseCount || 0 }}</b>
              <span>在学课程</span>
            </div>
            <div>
              <b>{{ ov.finishedChapters || 0 }}</b>
              <span>完成章节</span>
            </div>
            <div>
              <b>{{ ov.noteCount || 0 }}</b>
              <span>笔记</span>
            </div>
          </div>
        </section>

        <!-- 本周活跃 -->
        <section class="ol-card">
          <div class="ol-card-title">
            <h2 class="ol-h2">本周活跃</h2>
            <span class="chip chip--green">连续 {{ ov.streak || 0 }} 天</span>
          </div>
          <div class="week">
            <div v-for="d in ov.week || []" :key="d.date" class="wk">
              <div class="wk-bar-wrap">
                <div
                  class="wk-bar"
                  :class="{ today: d.isToday, empty: !d.count }"
                  :style="{ height: barHeight(d.count) }"
                />
              </div>
              <span :class="{ today: d.isToday }">{{ d.label }}</span>
            </div>
          </div>
          <p class="ol-muted" style="margin: 10px 0 0">
            累计学习 {{ ov.studyDays || 0 }} 天，本周完成 {{ ov.weekTotal || 0 }} 个章节
          </p>
        </section>
      </div>

      <!-- ==================== 右：表单 ==================== -->
      <div class="col-right">
        <section class="ol-card">
          <div class="tabs">
            <button
              v-for="t in tabs"
              :key="t.key"
              class="tab"
              :class="{ active: tab === t.key }"
              @click="tab = t.key"
            >
              {{ t.label }}
            </button>
          </div>

          <!-- 资料 -->
          <el-form v-show="tab === 'info'" label-position="top" class="form">
            <el-form-item :label="noLabel + '（登录账号）'">
              <el-input :model-value="profile.userNo" disabled />
              <span class="tip">登录用的{{ noLabel }}由学校统一分配，如需修改请联系管理员</span>
            </el-form-item>
            <el-form-item label="用户名（昵称）">
              <el-input v-model="profile.username" placeholder="3-20 位，显示在讨论区等位置" />
              <span class="tip">用户名可以随时修改，和登录无关</span>
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input :model-value="profile.realName" disabled />
              <span class="tip">学籍姓名，只有管理员可以修改</span>
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profile.phone" placeholder="11 位手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="头像">
              <div class="avatar-pick">
                <el-avatar :size="56" :src="profile.avatar" class="ap-img">
                  {{ (profile.realName || profile.username || 'U').slice(0, 1) }}
                </el-avatar>
                <div class="ap-side">
                  <FileUploader
                    accept="image/*"
                    label="选择头像"
                    :max-size="2"
                    size="small"
                    @uploaded="onAvatar"
                  />
                  <button v-if="profile.avatar" type="button" class="ap-clear" @click="profile.avatar = ''">
                    移除头像
                  </button>
                  <span class="tip">支持 JPG / PNG，建议正方形，2MB 以内</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>

          <!-- 密码 -->
          <el-form v-show="tab === 'pwd'" label-position="top" class="form">
            <el-form-item label="当前密码">
              <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="pwd.confirm" type="password" show-password placeholder="再输一次" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="changePwd">修改密码</el-button>
              <span class="tip" style="margin-left: 10px">修改后需要重新登录</span>
            </el-form-item>
          </el-form>
        </section>

        <!-- 学习入口 -->
        <section class="ol-card">
          <div class="ol-card-title">
            <h2 class="ol-h2">快捷入口</h2>
          </div>
          <div class="shortcuts">
            <div class="sc" @click="router.push('/my/courses')">
              <span class="sc-ico sc-ico--brand"><el-icon><Reading /></el-icon></span>
              <b>我的课程</b>
              <p>继续未完成的学习</p>
            </div>
            <div class="sc" @click="router.push('/grades')">
              <span class="sc-ico sc-ico--violet"><el-icon><DataAnalysis /></el-icon></span>
              <b>成绩与测验</b>
              <p>查看分数与错题解析</p>
            </div>
            <div class="sc" @click="router.push('/favorites')">
              <span class="sc-ico sc-ico--amber"><el-icon><Star /></el-icon></span>
              <b>收藏与笔记</b>
              <p>复习时最先看这里</p>
            </div>
            <div v-if="role === 'TEACHER'" class="sc" @click="router.push('/teacher')">
              <span class="sc-ico sc-ico--green"><el-icon><Management /></el-icon></span>
              <b>教师工作台</b>
              <p>课程、作业与学情</p>
            </div>
            <div v-if="role === 'ADMIN'" class="sc" @click="router.push('/admin')">
              <span class="sc-ico sc-ico--green"><el-icon><Setting /></el-icon></span>
              <b>管理后台</b>
              <p>用户、审核与统计</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import FileUploader from '@/components/FileUploader.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const profile = ref<any>({})
const ov = ref<any>({})
const tab = ref<'info' | 'pwd'>('info')
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const tabs = [
  { key: 'info' as const, label: '个人资料' },
  { key: 'pwd' as const, label: '修改密码' }
]

const role = computed(() => profile.value.role || userStore.role)
const roleName = computed(
  () =>
    ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' } as Record<string, string>)[
      role.value
    ] || role.value
)

/** 学生叫学号，教师/管理员叫职工号 */
const noLabel = computed(() => (role.value === 'STUDENT' ? '学号' : '职工号'))

function barHeight(count: number) {
  const max = Math.max(1, ...((ov.value.week || []) as any[]).map((d) => d.count || 0))
  return `${Math.max(count ? 12 : 4, ((count || 0) / max) * 100)}%`
}

/** 头像上传完立即回显，点「保存修改」才落库 */
function onAvatar(data: { url: string }) {
  profile.value.avatar = data.url
}

async function saveProfile() {  if (profile.value.phone && !/^1\d{10}$/.test(profile.value.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  saving.value = true
  try {
    await request.put('/api/user/profile', {
      username: profile.value.username,
      phone: profile.value.phone,
      avatar: profile.value.avatar
    })
    ElMessage.success('资料已更新')
    // 顶栏和侧栏都读 store，改完要同步，否则还显示旧名字
    if (userStore.userInfo) {
      userStore.userInfo.username = profile.value.username
      userStore.userInfo.avatar = profile.value.avatar
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
    }
  } finally {
    saving.value = false
  }
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (pwd.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (pwd.newPassword !== pwd.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await request.put('/api/user/password', {
      oldPassword: pwd.oldPassword,
      newPassword: pwd.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    userStore.logout()
    setTimeout(() => (window.location.href = '/login'), 600)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    profile.value = await request.get('/api/user/profile')
    try {
      ov.value = await request.get('/api/my/overview')
    } catch {
      ov.value = {}
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.profile {
  max-width: 1120px;
}
.layout {
  display: grid;
  grid-template-columns: 336px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}
.col-left,
.col-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

/* ---------- 名片 ---------- */
.id-card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-xl);
  overflow: hidden;
}
.id-bg {
  height: 92px;
  background: linear-gradient(118deg, var(--brand-300), var(--brand) 62%, var(--brand-600));
  position: relative;
  /* 背景条只做装饰，不能盖住上移的头像（position 元素默认画在 static 元素之上） */
  z-index: 0;
}
.id-bg::after {
  content: '';
  position: absolute;
  right: -20px;
  top: -50px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
}
.id-body {
  padding: 0 22px 18px;
  margin-top: -38px;
  text-align: center;
  /* 上移后必须抬高层级，否则会被 .id-bg 的定位层压住半张头像 */
  position: relative;
  z-index: 1;
}
.id-avatar {
  border: 4px solid var(--surface);
  background: var(--ink-900);
  font-size: 28px;
  font-weight: 700;
  box-shadow: var(--sh-sm);
}

/* ---------- 头像上传 ---------- */
.avatar-pick {
  display: flex;
  align-items: center;
  gap: 14px;
}
.ap-img {
  flex: none;
  border: 2px solid var(--ink-100);
  background: var(--ink-900);
  font-size: 20px;
  font-weight: 700;
}
.ap-side {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}
.ap-clear {
  border: 0;
  background: transparent;
  color: var(--ink-400);
  font-size: 12px;
  font-family: var(--font);
  cursor: pointer;
  padding: 0;
}
.ap-clear:hover {
  color: #c0392b;
}
.id-body h2 {
  margin: 10px 0 8px;
  font-size: 19px;
  font-weight: 730;
  letter-spacing: -0.3px;
}
.id-tags {
  display: flex;
  gap: 6px;
  justify-content: center;
}
.id-phone {
  margin: 12px 0 0;
  font-size: 12.5px;
  color: var(--ink-400);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.id-stats {
  display: flex;
  border-top: 1px solid var(--ink-100);
}
.id-stats > div {
  flex: 1;
  padding: 14px 0;
  text-align: center;
  border-right: 1px solid var(--ink-100);
}
.id-stats > div:last-child {
  border-right: 0;
}
.id-stats b {
  display: block;
  font-size: 19px;
  font-weight: 760;
  letter-spacing: -0.5px;
}
.id-stats span {
  font-size: 11px;
  color: var(--ink-400);
}

/* ---------- 周活跃 ---------- */
.week {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  height: 84px;
  align-items: flex-end;
}
.wk {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  height: 100%;
}
.wk-bar-wrap {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.wk-bar {
  width: 100%;
  max-width: 20px;
  border-radius: 6px;
  background: linear-gradient(180deg, var(--brand-300), var(--brand-200));
  transition: height 0.5s ease;
}
.wk-bar.today {
  background: linear-gradient(180deg, var(--brand), var(--brand-600));
}
.wk-bar.empty {
  background: var(--surface-sunken);
}
.wk span {
  font-size: 10.5px;
  color: var(--ink-400);
}
.wk span.today {
  color: var(--brand);
  font-weight: 700;
}

/* ---------- 表单 ---------- */
.tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid var(--ink-100);
  margin-bottom: 18px;
}
.tab {
  border: 0;
  background: transparent;
  padding: 2px 0 12px;
  font-size: 14px;
  font-weight: 600;
  font-family: var(--font);
  color: var(--ink-400);
  cursor: pointer;
  position: relative;
}
.tab.active {
  color: var(--ink-900);
}
.tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 3px;
  border-radius: var(--r-pill);
  background: var(--brand);
}
.form {
  max-width: 420px;
}
.tip {
  font-size: 11.5px;
  color: var(--ink-300);
  margin-top: 3px;
}

/* ---------- 快捷入口 ---------- */
.shortcuts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
  gap: 10px;
}
.sc {
  padding: 14px;
  border: 1px solid var(--ink-100);
  border-radius: var(--r-md);
  cursor: pointer;
  transition: all 0.18s;
}
.sc:hover {
  border-color: var(--brand-200);
  background: var(--surface-soft);
  transform: translateY(-2px);
}
.sc-ico {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 16px;
  margin-bottom: 9px;
}
.sc-ico--brand {
  background: var(--brand-50);
  color: var(--brand);
}
.sc-ico--violet {
  background: var(--violet-soft);
  color: var(--violet);
}
.sc-ico--amber {
  background: var(--amber-soft);
  color: var(--amber);
}
.sc-ico--green {
  background: var(--green-soft);
  color: var(--green);
}
.sc b {
  font-size: 13px;
  font-weight: 650;
  display: block;
  margin-bottom: 2px;
}
.sc p {
  margin: 0;
  font-size: 11.5px;
  color: var(--ink-400);
}

@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
