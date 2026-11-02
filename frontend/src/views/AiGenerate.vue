<template>
  <div class="section-card">
    <div class="page-header">
      <h2>AI 智能生成行程</h2>
      <span class="text-muted">填写出行信息，AI 为你规划完整行程</span>
    </div>

    <!-- 输入表单 -->
    <el-form :model="form" label-width="110px" style="max-width: 860px;">
      <el-divider content-position="left">基本信息（必填）</el-divider>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="出发地" required>
            <el-input v-model="form.departure" placeholder="如：上海" maxlength="20" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="目的地" required>
            <el-select
              v-model="form.destinations"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入并回车添加城市，可多选"
              style="width: 100%;"
            >
              <el-option v-for="d in form.destinations" :key="d" :label="d" :value="d" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="8">
          <el-form-item label="出发日期" required>
            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%;" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="游玩天数" required>
            <el-input-number v-model="form.days" :min="1" :max="15" style="width: 100%;" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="出行人数" required>
            <el-input-number v-model="form.people" :min="1" :max="30" style="width: 100%;" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="8">
          <el-form-item label="人均预算" required>
            <el-input-number v-model="form.budgetPerPerson" :min="0" :step="500" style="width: 100%;" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="预算档次" required>
            <el-select v-model="form.budgetLevel" style="width: 100%;">
              <el-option v-for="o in levels" :key="o" :label="o" :value="o" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="酒店偏好" required>
            <el-select v-model="form.hotelLevel" style="width: 100%;">
              <el-option v-for="o in levels" :key="o" :label="o" :value="o" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">偏好信息（选填）</el-divider>

      <el-form-item label="同行人">
        <el-checkbox-group v-model="form.companions">
          <el-checkbox v-for="o in companionOptions" :key="o" :value="o">{{ o }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="兴趣偏好">
        <el-checkbox-group v-model="form.interests">
          <el-checkbox v-for="o in interestOptions" :key="o" :value="o">{{ o }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="8">
          <el-form-item label="出行方式">
            <el-select v-model="form.transport" clearable placeholder="不限" style="width: 100%;">
              <el-option v-for="o in transportOptions" :key="o" :label="o" :value="o" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="行程节奏">
            <el-select v-model="form.pace" clearable placeholder="不限" style="width: 100%;">
              <el-option v-for="o in paceOptions" :key="o" :label="o" :value="o" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="餐饮偏好">
            <el-input v-model="form.diet" placeholder="如：清淡、辣" maxlength="50" />
          </el-form-item>
        </el-col>
      </el-row>

      <div style="margin-top: 8px;">
        <el-button type="primary" size="large" :loading="generating" @click="handleGenerate">
          <el-icon><Promotion /></el-icon>{{ result ? '重新生成' : '生成行程' }}
        </el-button>
      </div>
    </el-form>

    <!-- 生成结果 -->
    <div v-if="result" v-loading="generating" style="margin-top: 24px;">
      <el-divider content-position="left">生成结果</el-divider>
      <el-form-item label="行程标题" label-width="110px" style="max-width: 620px;">
        <el-input v-model="saveTitle" maxlength="100" placeholder="可修改行程标题" />
      </el-form-item>

      <el-descriptions :column="3" border style="margin-bottom: 16px;">
        <el-descriptions-item label="总天数">{{ result.summary?.totalDays }} 天</el-descriptions-item>
        <el-descriptions-item label="总预算">¥{{ result.summary?.totalBudget }}</el-descriptions-item>
        <el-descriptions-item label="亮点">{{ result.summary?.headline || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="day-nav">
        <strong>每日行程</strong>
        <el-pagination
          small
          background
          layout="prev, pager, next"
          :total="(result.days || []).length"
          :page-size="1"
          :current-page="currentDay"
          @current-change="v => currentDay = v"
        />
      </div>
      <el-card v-if="currentDayData" shadow="never" class="day-card">
        <template #header>
          <strong>第 {{ currentDayData.day }} 天</strong>
          <span class="text-muted" style="margin-left: 12px;">
            {{ currentDayData.date }} · {{ currentDayData.city }}<template v-if="currentDayData.theme"> · {{ currentDayData.theme }}</template>
          </span>
        </template>
        <div v-for="(item, i) in currentDayData.items" :key="i" class="item-row">
          <el-tag size="small" :type="itemType(item.type).tag" effect="plain" class="item-tag">
            {{ itemType(item.type).label }}
          </el-tag>
          <span class="item-time">{{ item.time }}</span>
          <span class="item-name">{{ item.name }}</span>
          <span v-if="item.price != null" class="item-price">¥{{ item.price }}</span>
          <div v-if="item.note" class="text-muted item-note">{{ item.note }}</div>
          <a v-if="item.link" :href="item.link" target="_blank" rel="noopener" class="item-link">第三方链接</a>
        </div>
      </el-card>

      <h4 style="margin: 20px 0 10px;">费用明细</h4>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="交通">¥{{ result.costBreakdown?.transport }}</el-descriptions-item>
        <el-descriptions-item label="住宿">¥{{ result.costBreakdown?.accommodation }}</el-descriptions-item>
        <el-descriptions-item label="餐饮">¥{{ result.costBreakdown?.meals }}</el-descriptions-item>
        <el-descriptions-item label="门票">¥{{ result.costBreakdown?.tickets }}</el-descriptions-item>
        <el-descriptions-item label="其他">¥{{ result.costBreakdown?.other }}</el-descriptions-item>
        <el-descriptions-item label="合计">¥{{ result.costBreakdown?.total }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="以上价格为 AI 估算，仅供参考，实际费用可能与估算略有出入（可略高或略低）。"
        style="margin-top: 12px;"
      />

      <template v-if="result.tips?.length">
        <h4 style="margin: 20px 0 10px;">小贴士</h4>
        <ul class="tips-list">
          <li v-for="(t, i) in result.tips" :key="i">{{ t }}</li>
        </ul>
      </template>

      <div style="margin-top: 20px; display: flex; gap: 12px;">
        <el-button type="primary" size="large" :loading="saving" @click="handleSave">
          <el-icon><Check /></el-icon>保存到我的行程
        </el-button>
        <el-button size="large" :loading="generating" @click="handleGenerate">
          <el-icon><Refresh /></el-icon>重新生成
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateItinerary, saveItinerary } from '@/api/itinerary'
import { resolveKeyword } from '@/api/destination'

const router = useRouter()

const levels = ['经济', '舒适', '豪华']
const companionOptions = ['成人', '儿童', '老人']
const interestOptions = ['自然风光', '历史人文', '美食', '亲子', '购物', '休闲']
const transportOptions = ['自驾', '高铁', '飞机', '公共交通']
const paceOptions = ['轻松', '紧凑', '均衡']

const form = ref({
  departure: '',
  destinations: [],
  startDate: '',
  days: 3,
  people: 2,
  budgetPerPerson: 2000,
  budgetLevel: '舒适',
  hotelLevel: '舒适',
  companions: [],
  interests: [],
  transport: '',
  diet: '',
  pace: ''
})

const result = ref(null)
const saveTitle = ref('')
const currentDay = ref(1)
const generating = ref(false)
const saving = ref(false)

const currentDayData = computed(() => result.value?.days?.[currentDay.value - 1] || null)

function itemType(t) {
  const map = {
    attraction: { label: '景点', tag: 'primary' },
    meal: { label: '餐饮', tag: 'success' },
    hotel: { label: '住宿', tag: 'warning' },
    transport: { label: '交通', tag: 'info' }
  }
  return map[t] || { label: '其他', tag: 'info' }
}

function buildInput() {
  return {
    departure: form.value.departure.trim(),
    destinations: [...form.value.destinations],
    startDate: form.value.startDate,
    days: form.value.days,
    people: form.value.people,
    budgetPerPerson: form.value.budgetPerPerson,
    budgetLevel: form.value.budgetLevel,
    hotelLevel: form.value.hotelLevel,
    companions: form.value.companions.length ? [...form.value.companions] : null,
    interests: form.value.interests.length ? [...form.value.interests] : null,
    transport: form.value.transport || null,
    diet: form.value.diet?.trim() || null,
    pace: form.value.pace || null
  }
}

async function checkDepartureDestination() {
  const dep = form.value.departure.trim()
  const dests = [...form.value.destinations]
  if (!dep || !dests.length) return true

  let depRes = null
  const destResList = []
  try {
    depRes = await resolveKeyword(dep)
    for (const d of dests) destResList.push(await resolveKeyword(d))
  } catch (e) {
    return true // 解析接口异常时不阻断生成
  }

  const notes = []
  const substitutes = []
  const unknownNames = []

  if (depRes?.type === 'province') {
    const cities = (depRes.cities || []).map(c => c.name)
    notes.push(cities.length
      ? `出发地「${dep}」是省份，建议改为具体城市：${cities.join('、')}`
      : `出发地「${dep}」是省份，请改为具体城市`)
  } else if (depRes?.type === 'unknown') {
    unknownNames.push(`出发地「${dep}」`)
  }

  for (const r of destResList) {
    if (r.type === 'province') {
      substitutes.push({ from: r.keyword, cities: (r.cities || []).map(c => c.name) })
    } else if (r.type === 'unknown') {
      unknownNames.push(`目的地「${r.keyword}」`)
    }
  }

  // 出发地是城市、且位于某目的地省份内 → 逻辑提示
  if (depRes?.type === 'city') {
    const depProvinceName = depRes.cities?.[0]?.provinceName
    for (const r of destResList) {
      if (r.type !== 'province') continue
      const pname = r.province?.name || r.keyword
      if (depProvinceName && (depProvinceName === pname || pname.indexOf(depProvinceName) === 0 || depProvinceName.indexOf(pname) === 0)) {
        notes.push(`出发地「${dep}」位于目的地「${r.keyword}」省内，通常为省内短途出游，请确认行程范围`)
        break
      }
    }
  }

  if (substitutes.length) {
    const msgs = substitutes.map(s => s.cities.length
      ? `目的地「${s.from}」是省份，将改为其下城市：${s.cities.join('、')}`
      : `目的地「${s.from}」是省份，但信息库中暂无其下城市，将保留原名`)
    try {
      await ElMessageBox.confirm(msgs.join('\n'), '目的地校验', {
        type: 'warning',
        confirmButtonText: '按建议调整',
        cancelButtonText: '返回修改'
      })
    } catch (e) { return false }
    for (const s of substitutes) {
      const idx = form.value.destinations.indexOf(s.from)
      if (idx >= 0 && s.cities.length) {
        const merged = [...form.value.destinations]
        merged.splice(idx, 1, ...s.cities)
        form.value.destinations = [...new Set(merged)]
      }
    }
  }

  if (unknownNames.length) {
    try {
      await ElMessageBox.confirm(
        `${unknownNames.join('、')}未在信息库中找到，AI 将按原文本生成，是否继续？`,
        '地名校验',
        { type: 'warning', confirmButtonText: '仍要生成', cancelButtonText: '返回修改' }
      )
    } catch (e) { return false }
  }

  if (notes.length) {
    try {
      await ElMessageBox.confirm(notes.join('\n'), '出发地提示', {
        type: 'warning',
        confirmButtonText: '仍要生成',
        cancelButtonText: '返回修改'
      })
    } catch (e) { return false }
  }

  return true
}

async function handleGenerate() {
  if (!form.value.departure.trim()) { ElMessage.warning('请填写出发地'); return }
  if (!form.value.destinations.length) { ElMessage.warning('请至少填写一个目的地'); return }
  if (!form.value.startDate) { ElMessage.warning('请选择出发日期'); return }

  if (!(await checkDepartureDestination())) return

  const people = form.value.people
  const comps = form.value.companions || []
  const warnings = []
  if (comps.length > people) {
    warnings.push(`出行人数为 ${people} 人，但勾选了 ${comps.length} 类同行人（${comps.join('、')}），请核对`)
  }
  if (comps.includes('儿童') && !comps.includes('成人')) {
    warnings.push('仅勾选了「儿童」同行人，儿童出行建议有成人陪同')
  }
  if (warnings.length) {
    try {
      await ElMessageBox.confirm(warnings.join('\n'), '同行人提示', {
        type: 'warning',
        confirmButtonText: '仍要生成',
        cancelButtonText: '返回修改'
      })
    } catch (e) { return }
  }

  generating.value = true
  try {
    const data = await generateItinerary(buildInput())
    result.value = data
    saveTitle.value = data?.summary?.title || '我的行程'
    currentDay.value = 1
  } catch (e) {
    result.value = null
  } finally {
    generating.value = false
  }
}

async function handleSave() {
  if (!result.value) { ElMessage.warning('请先生成行程'); return }
  saving.value = true
  try {
    const it = await saveItinerary({
      title: saveTitle.value.trim() || '我的行程',
      input: buildInput(),
      result: result.value
    })
    ElMessage.success('行程已保存')
    router.push(`/itineraries/${it.id}`)
  } catch (e) { /* interceptor handles message */ } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.day-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 0 10px;
}
.day-card {
  border: 1px solid #ebeef5;
}
.item-row {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.item-row:last-child {
  border-bottom: none;
}
.item-tag {
  margin-right: 10px;
}
.item-time {
  color: #909399;
  font-size: 13px;
  margin-right: 10px;
}
.item-name {
  font-weight: 600;
}
.item-price {
  float: right;
  color: #f56c6c;
  font-weight: 600;
}
.item-note {
  font-size: 12px;
  margin-top: 4px;
  padding-left: 58px;
}
.item-link {
  font-size: 12px;
  margin-left: 10px;
}
.tips-list {
  padding-left: 20px;
  color: #606266;
  line-height: 1.8;
}
</style>
