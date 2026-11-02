<template>
  <div class="section-card">
    <div class="page-header">
      <h2>操作日志</h2>
      <el-select v-model="targetType" placeholder="全部类型" clearable style="width: 180px;" @change="handleFilter">
        <el-option label="用户" value="user" />
        <el-option label="城市" value="city" />
        <el-option label="帖子" value="post" />
        <el-option label="行程" value="itinerary" />
        <el-option label="AI 配置" value="ai_config" />
      </el-select>
    </div>

    <el-table :data="logs" stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="operatorId" label="操作人ID" width="100" />
      <el-table-column label="角色" width="110">
        <template #default="{ row }">{{ roleLabel(row.operatorRole) }}</template>
      </el-table-column>
      <el-table-column label="动作" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="actionTag(row.action)">{{ actionLabel(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetType" label="对象类型" width="100" />
      <el-table-column prop="targetId" label="对象ID" width="90" />
      <el-table-column prop="reason" label="备注" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>

    <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listLogs } from '@/api/admin'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const targetType = ref('')

const actionMap = {
  create: '新增', update: '更新', delete: '删除', status: '改状态',
  batch_hide: '批量隐藏', batch_delete: '批量删除'
}
function actionLabel(a) { return actionMap[a] || a }
function roleLabel(r) {
  if (r === 'admin') return '管理员'
  return '普通用户'
}
function actionTag(a) {
  if (a.includes('delete')) return 'danger'
  if (a === 'update' || a === 'status' || a === 'batch_hide') return 'warning'
  return 'success'
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (targetType.value) params.targetType = targetType.value
    const data = await listLogs(params)
    logs.value = data.records || data.list || data || []
    total.value = data.total || logs.value.length
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleFilter() {
  page.value = 1
  load()
}

onMounted(load)
</script>
