<template>
  <div>
    <div class="page-card">
      <el-row :gutter="12" align="middle">
        <el-col :span="6">
          <el-input v-model="query.keyword" placeholder="搜索课程标题" clearable @keyup.enter="load(1)">
            <template #append>
              <el-button :icon="Search" @click="load(1)" />
            </template>
          </el-input>
        </el-col>
        <el-col :span="12">
          <el-radio-group v-model="query.categoryId" @change="load(1)">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</el-radio-button>
          </el-radio-group>
        </el-col>
        <el-col :span="6" style="text-align: right">
          <el-radio-group v-model="query.sort" @change="load(1)">
            <el-radio-button value="new">最新</el-radio-button>
            <el-radio-button value="hot">最热</el-radio-button>
          </el-radio-group>
        </el-col>
      </el-row>
    </div>

    <el-row :gutter="16">
      <el-col v-for="c in list" :key="c.id" :span="8" style="margin-bottom: 16px">
        <div class="page-card card" @click="router.push(`/courses/${c.id}`)">
          <div class="cover">{{ c.title.slice(0, 8) }}</div>
          <div class="title">{{ c.title }}</div>
          <div class="muted">{{ c.categoryName }} · {{ c.teacherName }}</div>
          <div class="desc">{{ c.description }}</div>
          <div class="foot">
            <span class="muted">{{ c.chapterCount }} 章 · {{ c.studentCount }} 人在学</span>
            <el-tag v-if="c.enrolled" size="small" type="success">已选</el-tag>
            <el-tag v-else size="small">{{ c.viewCount }} 浏览</el-tag>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="list.length === 0" description="没有找到课程" />
    <el-pagination v-model:current-page="query.current" :page-size="query.size" :total="total"
                   layout="prev, pager, next, total" style="justify-content: center"
                   @current-change="load()" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const list = ref<any[]>([])
const categories = ref<any[]>([])
const total = ref(0)
const query = reactive<any>({ keyword: '', categoryId: undefined, sort: 'new', current: 1, size: 9 })

async function load(page?: number) {
  if (page) query.current = page
  const data: any = await request.get('/api/public/courses', { params: query })
  list.value = data.records
  total.value = data.total
}

onMounted(async () => {
  categories.value = (await request.get('/api/public/categories')) as any
  load()
})
</script>

<style scoped>
.card {
  cursor: pointer;
  transition: 0.2s;
  height: 100%;
}
.card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.title {
  font-weight: 600;
  margin: 10px 0 4px;
}
.desc {
  font-size: 13px;
  color: #606266;
  height: 38px;
  overflow: hidden;
  margin: 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
