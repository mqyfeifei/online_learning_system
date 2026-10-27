<template>
  <div class="admin-page">
    <div class="ap-head">
      <div>
        <h1>课程分类</h1>
        <p>维护课程广场的分类体系，一级分类下可挂子分类，排序值越小越靠前。</p>
      </div>
      <div class="ap-actions">
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增分类</el-button>
      </div>
    </div>

    <div class="ap-stats">
      <div class="ap-stat">
        <div class="ico ico--brand"><el-icon><Folder /></el-icon></div>
        <div><b>{{ list.length }}</b><span>分类总数</span></div>
      </div>
      <div class="ap-stat">
        <div class="ico ico--violet"><el-icon><Grid /></el-icon></div>
        <div><b>{{ roots.length }}</b><span>一级分类</span></div>
      </div>
      <div class="ap-stat">
        <div class="ico ico--green"><el-icon><Files /></el-icon></div>
        <div><b>{{ list.length - roots.length }}</b><span>子分类</span></div>
      </div>
      <div class="ap-stat">
        <div class="ico ico--amber"><el-icon><Sort /></el-icon></div>
        <div><b>{{ minSortGroup }}</b><span>默认排序值</span></div>
      </div>
    </div>

    <div class="cat-grid" v-loading="loading">
      <div v-for="r in roots" :key="r.id" class="cat-card fade-up">
        <div class="cat-top">
          <span class="cat-mark">{{ r.name.slice(0, 1) }}</span>
          <div class="cat-title">
            <b>{{ r.name }}</b>
            <span>ID {{ r.id }} · 排序 {{ r.sort }}</span>
          </div>
          <div class="cat-ops">
            <el-tooltip content="编辑" placement="top">
              <el-button circle text :icon="EditPen" @click="openEdit(r)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button circle text type="danger" :icon="Delete" @click="remove(r)" />
            </el-tooltip>
          </div>
        </div>

        <div class="cat-children">
          <span v-if="!childrenOf(r.id).length" class="ol-muted" style="font-size: 13px">暂无子分类</span>
          <span
            v-for="c in childrenOf(r.id)"
            :key="c.id"
            class="cat-child"
          >
            {{ c.name }}
            <em>排序 {{ c.sort }}</em>
            <i class="el-icon-edit" @click.stop="openEdit(c)"><el-icon><EditPen /></el-icon></i>
            <i class="danger" @click.stop="remove(c)"><el-icon><Delete /></el-icon></i>
          </span>
        </div>

        <div class="cat-foot">
          <el-button link type="primary" :icon="Plus" @click="addChild(r)">添加子分类</el-button>
        </div>
      </div>

      <button class="cat-add" @click="openEdit(null)">
        <el-icon><Plus /></el-icon>
        <span>新增一级分类</span>
      </button>
    </div>

    <div v-if="!roots.length && !loading" class="ol-empty">
      <div class="icon"><el-icon><Folder /></el-icon></div>
      <p>还没有任何分类，先建一个一级分类吧</p>
    </div>

    <el-dialog append-to-body v-model="dialog" :title="form.id ? '编辑分类' : '新增分类'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="分类名称">
          <el-input v-model="form.name" placeholder="如：计算机专业" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option label="无（一级分类）" :value="0" />
            <el-option v-for="c in roots" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="ol-muted" style="margin-left: 10px; font-size: 12px">越小越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, EditPen, Files, Folder, Grid, Plus, Sort } from '@element-plus/icons-vue'
import request from '@/utils/request'

const list = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive<any>({ id: null, name: '', parentId: 0, sort: 0 })

/** 一级分类：parentId 为 0 或缺失（老数据兼容） */
const roots = computed(() =>
  list.value.filter((c) => !c.parentId).sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0) || a.id - b.id)
)

const minSortGroup = computed(() => {
  if (!list.value.length) return 0
  return Math.min(...list.value.map((c) => c.sort ?? 0))
})

function childrenOf(id: number) {
  return list.value
    .filter((c) => c.parentId === id)
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0) || a.id - b.id)
}

async function load() {
  loading.value = true
  try {
    list.value = ((await request.get('/api/admin/categories')) as any[]) || []
  } finally {
    loading.value = false
  }
}

function openEdit(row: any) {
  Object.assign(form, row ? { id: row.id, name: row.name, parentId: row.parentId ?? 0, sort: row.sort ?? 0 } : { id: null, name: '', parentId: 0, sort: 0 })
  dialog.value = true
}

function addChild(parent: any) {
  Object.assign(form, { id: null, name: '', parentId: parent.id, sort: 0 })
  dialog.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('分类名称不能为空')
    return
  }
  // 不能把自己设成自己的上级
  if (form.id && String(form.parentId) === String(form.id)) {
    ElMessage.warning('上级分类不能是自己')
    return
  }
  await request.post('/api/admin/categories', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row: any) {
  const kids = childrenOf(row.id)
  const extra = kids.length ? `，其下 ${kids.length} 个子分类也会一并失效` : ''
  await ElMessageBox.confirm(`确认删除分类「${row.name}」${extra}？`, '删除确认', { type: 'warning' })
  await request.delete('/api/admin/categories/' + row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.cat-card {
  background: var(--surface);
  border: 1px solid var(--ink-100);
  border-radius: var(--r-lg);
  padding: 16px 18px;
  box-shadow: var(--sh-sm);
  transition: box-shadow 0.18s, transform 0.18s;
}
.cat-card:hover {
  box-shadow: var(--sh-md);
  transform: translateY(-2px);
}

.cat-top {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cat-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 17px;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-600, #f2542d));
  flex: none;
}
.cat-title {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cat-title b {
  font-size: 15px;
  color: var(--ink-900);
}
.cat-title span {
  font-size: 12px;
  color: var(--ink-400);
}
.cat-ops {
  display: flex;
  gap: 2px;
  flex: none;
}

.cat-children {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px dashed var(--ink-100);
}
.cat-child {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: var(--r-pill);
  background: var(--brand-50, #fff3ec);
  color: var(--ink-700);
  font-size: 13px;
}
.cat-child em {
  font-style: normal;
  font-size: 11px;
  color: var(--ink-400);
}
.cat-child i {
  display: inline-flex;
  cursor: pointer;
  color: var(--ink-400);
  font-size: 12px;
}
.cat-child i:hover {
  color: var(--brand);
}
.cat-child i.danger:hover {
  color: var(--el-color-danger);
}

.cat-foot {
  margin-top: 12px;
}

.cat-add {
  min-height: 148px;
  border: 1px dashed var(--ink-200);
  border-radius: var(--r-lg);
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--ink-400);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.18s;
}
.cat-add:hover {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--brand-50, #fff3ec);
}
.cat-add .el-icon {
  font-size: 22px;
}

@media (max-width: 900px) {
  .cat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
