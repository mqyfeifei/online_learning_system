<template>
  <div>
    <div class="page-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-statistic title="成绩记录数" :value="summary.count || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均分" :value="summary.average || 0" />
        </el-col>
      </el-row>
    </div>

    <div class="page-card">
      <div class="page-title">作业与测验成绩</div>
      <el-table :data="summary.items || []" stripe>
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="score" label="分数" width="100" />
        <el-table-column label="时间">
          <template #default="scope">{{ fmt(scope.row.time) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!summary.items || summary.items.length === 0" description="暂无成绩" />
    </div>

    <div class="page-card">
      <div class="page-title">测验记录</div>
      <el-table :data="records" stripe>
        <el-table-column prop="paperTitle" label="试卷" />
        <el-table-column label="得分" width="120">
          <template #default="scope">{{ scope.row.record.score }} / {{ scope.row.totalScore }}</template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.record.passed === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.record.passed === 1 ? '及格' : '不及格' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="交卷时间" width="180">
          <template #default="scope">{{ fmt(scope.row.record.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button link type="primary" @click="viewDetail(scope.row.record.id)">查看解析</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="records.length === 0" description="还没有测验记录" />
    </div>

    <el-dialog v-model="dialog" title="答卷详情" width="720px">
      <div v-if="detail">
        <el-alert :type="detail.passed ? 'success' : 'warning'" :closable="false"
                  :title="'得分 ' + detail.score + ' / ' + detail.totalScore"
                  style="margin-bottom: 16px" />
        <div v-for="(d, i) in detail.details" :key="d.questionId" class="detail">
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
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

const summary = ref<any>({})
const records = ref<any[]>([])
const dialog = ref(false)
const detail = ref<any>(null)

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : ''
}

async function viewDetail(id: number) {
  detail.value = await request.get('/api/records/' + id)
  dialog.value = true
}

onMounted(async () => {
  summary.value = await request.get('/api/my/grades')
  records.value = (await request.get('/api/my/records')) as any
})
</script>

<style scoped>
.detail {
  padding: 10px 0;
  border-bottom: 1px solid #f2f6fc;
}
.q-title {
  font-weight: 600;
  margin-bottom: 6px;
  display: flex;
  gap: 8px;
  align-items: center;
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
