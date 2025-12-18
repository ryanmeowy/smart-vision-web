<template>
  <div class="app-container">
    <!-- 头部搜索区 -->
    <header class="header">
      <div class="logo">
        🌌 SmartVision <span class="tag">AI Search</span>
      </div>
      
      <div class="search-bar">
        <el-input 
          v-model="queryText" 
          placeholder="描述你想找的图片，例如：'雨后的森林' 或 '带文字的合同'" 
          size="large"
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch" :loading="searching">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
          </template>
        </el-input>
        
        <!-- 打开批量上传弹窗 -->
        <el-button type="primary" size="large" class="upload-btn" @click="openUpload">
          <el-icon><UploadFilled /></el-icon> 批量导入
        </el-button>
      </div>
    </header>

    <!-- 瀑布流内容区 -->
    <main class="content" v-loading="searching">
      <div v-if="results.length === 0 && !searching" class="empty-state">
        <el-empty description="输入文字开始搜索，或上传图片建立索引" />
      </div>

      <!-- 纯CSS瀑布流布局 -->
      <div class="waterfall" v-else>
        <div class="waterfall-item" v-for="item in results" :key="item.id">
          <el-card :body-style="{ padding: '0px' }" shadow="hover">
            <div class="image-wrapper">
              <!-- 图片点击预览 -->
              <el-image 
                :src="item.url" 
                loading="lazy"
                fit="cover"
                :preview-src-list="[item.url]"
                class="card-img"
              />
              <!-- 显示匹配分数 (Score) -->
              <div class="score-tag">
                匹配度: {{ (item.score * 100).toFixed(0) }}%
              </div>
            </div>
            
            <div class="card-info">
              <!-- 显示OCR文字摘要(如果有) -->
              <p class="ocr-text" v-if="item.ocrText">
                <el-tag size="small" type="warning">含文字</el-tag>
                {{ item.ocrText }}
              </p>
              <!-- 文件名 -->
              <p class="filename">{{ item.filename || '未命名图片' }}</p>
            </div>
          </el-card>
        </div>
      </div>
    </main>

    <!-- 引入上传组件 -->
    <BatchUploadDialog ref="uploadDialogRef" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { UploadFilled, Search } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
// 引入我们的组件
import BatchUploadDialog from './components/BatchUploadDialog.vue'

// --- 状态 ---
const queryText = ref('')
const searching = ref(false)
const results = ref([])
const uploadDialogRef = ref(null)

// --- 动作 1: 打开上传弹窗 ---
const openUpload = () => {
  uploadDialogRef.value.open()
}

// --- 动作 2: 执行搜索 ---
const handleSearch = async () => {
  if (!queryText.value.trim()) return
  
  searching.value = true
  try {
    // 调用后端搜索接口
    const res = await axios.get('/api/v1/vision/search', {
      params: { 
        text: queryText.value,
        limit: 20 
      }
    })
    
    // 结果赋值
    results.value = res.data.data || []
    
    if (results.value.length === 0) {
      ElMessage.info('未找到相关图片')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('搜索请求失败，请检查后端是否启动')
  } finally {
    searching.value = false
  }
}
</script>

<style>
/* 全局样式重置 */
body { margin: 0; background-color: #f5f7fa; font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', Arial, sans-serif; }

.app-container { max-width: 1200px; margin: 0 auto; padding: 20px; }

/* 头部样式 */
.header { text-align: center; margin-bottom: 40px; padding-top: 20px; }
.logo { font-size: 28px; font-weight: bold; color: #303133; margin-bottom: 20px; }
.tag { font-size: 14px; background: #ecf5ff; color: #409eff; padding: 2px 8px; border-radius: 4px; vertical-align: middle; }

.search-bar { display: flex; justify-content: center; gap: 15px; max-width: 800px; margin: 0 auto; }
.search-input { flex: 1; }

/* 瀑布流样式 (核心) */
.waterfall {
  /* 分列：大屏4列，中屏3列... */
  column-count: 4; 
  column-gap: 20px;
}
@media (max-width: 1200px) { .waterfall { column-count: 3; } }
@media (max-width: 768px) { .waterfall { column-count: 2; } }

.waterfall-item {
  /* 防止卡片被拆分到两列 */
  break-inside: avoid;
  margin-bottom: 20px;
}

/* 卡片内部样式 */
.image-wrapper { position: relative; width: 100%; min-height: 100px; background: #eee; }
.card-img { width: 100%; display: block; }
.score-tag {
  position: absolute; top: 8px; right: 8px;
  background: rgba(0,0,0,0.6); color: #fff;
  font-size: 12px; padding: 2px 6px; border-radius: 4px;
}
.card-info { padding: 10px; background: #fff; }
.ocr-text {
  font-size: 12px; color: #666; margin: 0 0 5px 0;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.filename { font-size: 13px; color: #333; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.empty-state { margin-top: 100px; }
</style>