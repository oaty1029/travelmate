<template>
  <div class="section-card">
    <div class="page-header">
      <h2>用户管理</h2>
      <div style="display: flex; gap: 8px; align-items: center;">
        <el-input v-model="nickname" placeholder="按昵称搜索" clearable style="width: 220px;" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <el-table :data="users" stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="邮箱">
        <template #default="{ row }">{{ maskEmail(row.email) }}</template>
      </el-table-column>
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
      </el-table-column>
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'admin'" type="danger">管理员</el-tag>
          <el-tag v-else type="info">普通用户</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.role === 'user'">
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggle(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
          <span v-else class="text-muted">受保护账号</span>
        </template>
      </el-table-column>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, updateUserStatus, deleteUser } from '@/api/admin'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const nickname = ref('')

function maskPhone(p) {
  if (!p) return '-'
  return String(p).replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}
function maskEmail(e) {
  if (!e) return '-'
  return String(e).replace(/^(.).*(@.+)$/, '$1***$2')
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (nickname.value.trim()) params.nickname = nickname.value.trim()
    const data = await listUsers(params)
    users.value = data.records || data.list || data || []
    total.value = data.total || users.value.length
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleSearch() {
  page.value = 1
  load()
}

async function handleToggle(row) {
  const next = row.status === 1 ? 0 : 1
  try {
    await updateUserStatus(row.id, next)
    ElMessage.success('操作成功')
    await load()
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户 "${row.nickname || row.email}" 吗？`, '警告', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) { /* ignore */ }
}

onMounted(load)
</script>
