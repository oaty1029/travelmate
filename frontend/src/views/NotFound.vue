<template>
  <div class="section-card flex-col items-center" style="min-height: 60vh; justify-content: center;">
    <el-icon :size="120" color="#c0c4cc"><Warning /></el-icon>
    <h1 style="font-size: 28px; margin: 16px 0;">404</h1>
    <p class="text-muted">抱歉，您访问的页面不存在</p>
    <el-button type="primary" style="margin-top: 16px;" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<script setup></script>
