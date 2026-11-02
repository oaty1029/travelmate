<template>
  <div class="section-card">
    <h2>个人中心</h2>

    <el-card v-loading="loading" style="margin-top: 16px;">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="昵称">{{ profile?.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ profile?.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ profile?.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="isAdminRole(profile?.role) ? 'danger' : 'info'">
            {{ roleLabel(profile?.role) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="profile?.status === 1 ? 'success' : 'danger'">
            {{ profile?.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatTime(profile?.createdAt) }}</el-descriptions-item>
      </el-descriptions>

      <el-button type="primary" style="margin-top: 16px;" @click="openEdit">编辑资料</el-button>
    </el-card>

    <!-- 资料编辑 -->
    <el-dialog v-model="editVisible" title="编辑资料" width="440px" :close-on-click-modal="false">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="2-30 个字符" maxlength="30" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="选填，留空则清除" maxlength="11" />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            :show-file-list="false"
            accept="image/jpeg,image/png"
            :http-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
          >
            <div class="avatar-uploader" :class="{ 'is-uploading': avatarUploading }">
              <el-avatar :size="64" :src="avatarPreview">
                {{ profile?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="avatar-uploader-tip">点击更换头像</div>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码 -->
    <el-card style="margin-top: 16px;">
      <h3>修改密码</h3>
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" style="max-width: 440px; margin-top: 16px;">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-64 位" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="changing" @click="handleChangePassword">确认修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile, updateProfile, changePassword, uploadAvatar } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const profile = ref(null)

const editVisible = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const avatarPreview = ref('')
const editFormRef = ref(null)
const editForm = ref({ nickname: '', phone: '' })
const editRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 30, message: '昵称长度须在 2-30 个字符之间', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ]
}

const changing = ref(false)
const pwdFormRef = ref(null)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度须在 6-64 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function load() {
  loading.value = true
  try {
    profile.value = await getProfile()
  } catch (e) { /* interceptor handles */ }
  finally { loading.value = false }
}

function isAdminRole(r) { return r === 'admin' }
function roleLabel(r) {
  if (r === 'admin') return '管理员'
  return '普通用户'
}

function openEdit() {
  editForm.value = {
    nickname: profile.value?.nickname || '',
    phone: profile.value?.phone || ''
  }
  avatarPreview.value = profile.value?.avatarUrl || ''
  editVisible.value = true
}

function beforeAvatarUpload(file) {
  const okType = ['image/jpeg', 'image/png'].includes(file.type)
  if (!okType) {
    ElMessage.error('仅支持 jpg/png 格式图片')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function handleAvatarUpload({ file }) {
  avatarUploading.value = true
  try {
    const data = await uploadAvatar(file)
    profile.value = data
    avatarPreview.value = data.avatarUrl || ''
    userStore.setUserInfo({
      id: data.id, nickname: data.nickname, email: data.email,
      phone: data.phone, role: data.role, status: data.status, avatarUrl: data.avatarUrl
    })
    ElMessage.success('头像已更新')
  } catch (e) { /* interceptor handles */ }
  finally { avatarUploading.value = false }
}

async function handleSave() {
  try {
    await editFormRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const data = await updateProfile(editForm.value)
    profile.value = data
    userStore.setUserInfo({
      id: data.id, nickname: data.nickname, email: data.email,
      phone: data.phone, role: data.role, status: data.status, avatarUrl: data.avatarUrl
    })
    ElMessage.success('资料已更新')
    editVisible.value = false
  } catch (e) { /* interceptor handles */ }
  finally { saving.value = false }
}

async function handleChangePassword() {
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  changing.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    await ElMessageBox.alert('密码修改成功，请重新登录', '提示', { type: 'success' })
    userStore.logout()
    router.push('/login')
  } catch (e) { /* interceptor handles */ }
  finally { changing.value = false }
}

function formatTime(t) {
  return t ? t.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(load)
</script>

<style scoped>
.avatar-uploader {
  position: relative;
  width: 64px;
  cursor: pointer;
  text-align: center;
}
.avatar-uploader-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.avatar-uploader:hover .avatar-uploader-tip {
  color: #409eff;
}
.avatar-uploader.is-uploading {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
