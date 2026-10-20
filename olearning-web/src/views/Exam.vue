<template>
  <div v-loading="loading">
    <!-- 交卷后展示成绩与错题解析 -->
    <div v-if="result" class="page-card">
      <el-result :icon="result.passed ? 'success' : 'warning'"
                 :title="result.passed ? '恭喜，测验通过' : '本次未达及格线'"
                 :sub-title="resultTip" />
      <div style="text-align: center; margin-bottom: 20px">
        <el-button type="primary" @click="router.push('/grades')">查看我的成绩</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>

      <div class="page-title">答题详情</div>
      <div v-for="(d, i) in result.details" :key="d.questionId" class="detail">
        <div class="q-title">
          <el-tag size="small" :type="d.correct ? 'success' : 'danger'">
            {{ d.correct ? '正确' : '错误' }}
          </el-tag>
          {{ i + 1 }}. {{ d.title }}
        </div>
        <div class="muted">你的答案：{{ d.yourAnswer || '未作答' }} ｜ 正确答案：{{ d.rightAnswer }}</div>
        <div v-if="d.analysis" class="analysis">解析：{{ d.analysis }}</div>
      </div>
    </div>

    <!-- 答题中 -->
    <template v-else>
      <div class="page-card head">
        <div>
          <div class="page-title" style="margin: 0">{{ paper.title }}</div>
          <div class="muted">共 {{ questions.length }} 题 · 总分 {{ paper.totalScore }} · 及格 {{ paper.passScore }}</div>
        </div>
        <div class="timer">
          <el-icon><Timer /></el-icon>
          <span>{{ leftText }}</span>
        </div>
      </div>

      <div v-for="(q, i) in questions" :key="q.id" class="page-card">
        <div class="q-title">
          <el-tag size="small" type="info">{{ typeName(q.type) }}</el-tag>
          {{ i + 1 }}. {{ q.title }}（{{ q.score }} 分）
        </div>

        <el-radio-group v-if="q.type === 1" v-model="single[q.id]">
          <el-radio v-for="o in q.options" :key="o.key" :value="o.key" class="opt">
            {{ o.key }}. {{ o.text }}
          </el-radio>
        </el-radio-group>

        <el-checkbox-group v-else-if="q.type === 2" v-model="multi[q.id]">
          <el-checkbox v-for="o in q.options" :key="o.key" :value="o.key" class="opt">
            {{ o.key }}. {{ o.text }}
          </el-checkbox>
        </el-checkbox-group>

        <el-radio-group v-else v-model="judge[q.id]">
          <el-radio value="T" class="opt">正确</el-radio>
          <el-radio value="F" class="opt">错误</el-radio>
        </el-radio-group>
      </div>

      <div style="text-align: center; margin: 20px 0">
        <el-button type="primary" size="large" :loading="submitting" @click="submit">交 卷</el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const paper = ref<any>({})
const questions = ref<any[]>([])
const result = ref<any>(null)

// 按题型分别收集答案：单选/判断存字符串，多选存数组
const single = reactive<Record<string, string>>({})
const multi = reactive<Record<string, string[]>>({})
const judge = reactive<Record<string, string>>({})

const seconds = ref(0)
let timer: number | undefined

const leftText = computed(() => {
  const m = Math.floor(seconds.value / 60)
  const s = seconds.value % 60
  return '剩余 ' + m + ':' + String(s).padStart(2, '0')
})

const resultTip = computed(() =>
  '得分 ' + result.value.score + ' / ' + result.value.totalScore + '，及格线 ' + result.value.passScore
)

function typeName(t: number) {
  return t === 1 ? '单选题' : t === 2 ? '多选题' : '判断题'
}

async function load() {
  loading.value = true
  try {
    const data: any = await request.get('/api/papers/' + route.params.paperId + '/start')
    paper.value = data.paper
    questions.value = data.questions
    questions.value.forEach((q) => {
      if (q.type === 2) multi[q.id] = []
    })
    seconds.value = (paper.value.duration || 30) * 60
    timer = window.setInterval(() => {
      seconds.value--
      if (seconds.value <= 0) {
        window.clearInterval(timer)
        ElMessage.warning('时间到，自动交卷')
        submit()
      }
    }, 1000)
  } finally {
    loading.value = false
  }
}

async function submit() {
  await ElMessageBox.confirm('确认交卷？交卷后不可修改。', '提示', { type: 'warning' })
  submitting.value = true
  try {
    const answers: Record<string, string> = {}
    questions.value.forEach((q) => {
      const key = String(q.id)
      if (q.type === 1) answers[key] = single[key] || ''
      else if (q.type === 2) answers[key] = (multi[key] || []).slice().sort().join(',')
      else answers[key] = judge[key] || ''
    })
    result.value = await request.post('/api/exams/submit', {
      paperId: paper.value.id,
      answers
    })
    if (timer) window.clearInterval(timer)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.timer {
  color: #e6a23c;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}
.q-title {
  font-weight: 600;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.opt {
  display: block;
  margin: 8px 0;
}
.detail {
  padding: 12px 0;
  border-bottom: 1px solid #f2f6fc;
}
.analysis {
  margin-top: 6px;
  font-size: 13px;
  color: #529b2e;
  background: #f0f9eb;
  padding: 8px;
  border-radius: 4px;
}
</style>
