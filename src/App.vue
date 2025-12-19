<template>
  <div class="app-container">
    <!-- 头部搜索区 -->
    <header class="header">
      <div class="logo">
        🌌 SmartVision <span class="tag">AI Search</span>
      </div>
      
      <div class="search-bar">
        <div class="search-container">
          <!-- 自定义搜索框 -->
          <div class="custom-search-input">
            <input 
              v-model="queryText" 
              placeholder="描述你想找的图片，例如：'雨后的森林' 或 '带文字的合同'" 
              class="search-input"
              @keyup.enter="handleSearch"
            />
            <button @click="handleSearch" :disabled="searching" class="search-button">
              <span class="magic-wand">✨</span>
              <el-icon v-if="searching" class="is-loading"><Loading /></el-icon>
              <span>AI Search</span>
            </button>
          </div>
          
          <!-- 自定义批量导入按钮 -->
          <button class="custom-upload-btn" @click="openUpload">
            <el-icon><UploadFilled /></el-icon>
            <span>批量导入</span>
          </button>
        </div>
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
          <div class="image-card">
            <div class="image-wrapper">
              <!-- 图片点击预览 -->
              <el-image 
                :src="item.url" 
                loading="lazy"
                fit="cover"
                :preview-src-list="[item.url]"
                :initial-index="results.indexOf(item)"
                preview-teleported
                class="card-img"
                hide-on-click-modal
              />
              <!-- 显示匹配分数 (Score) -->
              <div class="score-tag">
                匹配度: {{ (item.score * 100).toFixed(0) }}%
              </div>
            </div>
            
            <div class="card-info">
              <!-- 显示OCR文字摘要(如果有) -->
              <p class="ocr-text" v-if="item.ocrText">
                <span class="custom-tag warning">含文字</span>
                {{ item.ocrText }}
              </p>
              <!-- 文件名 -->
              <p class="filename">{{ item.filename || '未命名图片' }}</p>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 引入上传组件 -->
    <BatchUploadDialog ref="uploadDialogRef" />
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {Loading, UploadFilled} from '@element-plus/icons-vue'
import axios from 'axios'
import {ElMessage} from 'element-plus'
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

// 添加mock数据
const loadMockData = () => {
  results.value = [
    { 
      id: '1', 
      url: 'https://images.unsplash.com/photo-1501854140801-50d01698950b?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.95, 
      filename: 'mountain-landscape.jpg',
      ocrText: '自然风景照片'
    },
    { 
      id: '2', 
      url: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.87, 
      filename: 'forest-mist.jpg',
      ocrText: '清晨的森林'
    },
    { 
      id: '3', 
      url: 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.78, 
      filename: 'colorful-sky.jpg'
    },
    { 
      id: '4', 
      url: 'https://images.unsplash.com/photo-1505765050516-f72dcac9c60e?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.92, 
      filename: 'river-valley.jpg',
      ocrText: '河流与山谷'
    },
    { 
      id: '5', 
      url: 'https://images.unsplash.com/photo-1418065460487-3e41a6c84dc5?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.85, 
      filename: 'green-forest.jpg'
    },
    { 
      id: '6', 
      url: 'https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.76, 
      filename: 'rocky-ocean.jpg'
    },
    { 
      id: '7', 
      url: 'https://images.unsplash.com/photo-1476820865390-c52aeebb9891?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.88, 
      filename: 'colorful-clouds.jpg'
    },
    { 
      id: '8', 
      url: 'https://images.unsplash.com/photo-1439853949127-fa647821eba0?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.81, 
      filename: 'sunset-pier.jpg',
      ocrText: '夕阳下的码头'
    },
    { 
      id: '9', 
      url: 'https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.93, 
      filename: 'misty-forest.jpg'
    },
    { 
      id: '10', 
      url: 'https://images.unsplash.com/photo-1426604966848-d7adac402bff?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.79, 
      filename: 'mountain-lake.jpg'
    },
    { 
      id: '11', 
      url: 'https://images.unsplash.com/photo-1470240731273-7821a6eeb6bd?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.84, 
      filename: 'autumn-forest.jpg'
    },
    { 
      id: '12', 
      url: 'https://images.unsplash.com/photo-1511497584788-876760111969?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1200&q=80', 
      score: 0.91, 
      filename: 'beach-sand.jpg',
      ocrText: '沙滩度假'
    }
  ]
}

// 页面加载时显示mock数据
onMounted(() => {
  loadMockData()
})
</script>

<style>
/* 全局样式重置 */
body { 
  margin: 0; 
  background-color: #202124; /* Google暗夜模式背景色 */
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', Arial, sans-serif; 
  color: #e8eaed;
}

.app-container { 
  max-width: 1200px; 
  margin: 0 auto; 
  padding: 20px; 
}

/* 头部样式 */
.header { 
  text-align: center; 
  margin-bottom: 40px; 
  padding-top: 20px; 
}
.logo { 
  font-size: 28px; 
  font-weight: bold; 
  color: #e8eaed; /* Google暗夜模式文字颜色 */
  margin-bottom: 20px; 
}
.tag { 
  font-size: 14px; 
  background: #303134; /* Google暗夜模式元素背景色 */
  color: #9aa0a6; /* Google暗夜模式次要文字颜色 */
  padding: 2px 8px; 
  border-radius: 4px; 
  vertical-align: middle; 
}

.search-bar { 
  display: flex; 
  justify-content: center; 
  max-width: 800px; 
  margin: 0 auto; 
}

.search-container {
  display: flex;
  gap: 15px;
  width: 100%;
}

/* 自定义搜索框样式 */
.custom-search-input {
  flex: 1;
  display: flex;
  border-radius: 24px;
  background-color: #303134;
  border: 1px solid #5f6368;
  padding: 2px;
}

.search-input {
  flex: 1;
  background-color: #303134;
  color: #e8eaed;
  border: none;
  outline: none;
  padding: 12px 16px;
  border-radius: 24px 0 0 24px;
  font-size: 14px;
}

.search-input::placeholder {
  color: #9aa0a6;
}

.search-button {
  border-radius: 24px;
  background-color: #303134;
  color: #e8eaed;
  border: none;
  padding: 0 15px;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.magic-wand {
  font-size: 16px;
  animation: sparkle 2s ease-in-out infinite;
}

@keyframes sparkle {
  0%, 100% {
    transform: scale(1) rotate(0deg);
    opacity: 1;
  }
  25% {
    transform: scale(1.1) rotate(5deg);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.2) rotate(-5deg);
    opacity: 1;
  }
  75% {
    transform: scale(1.1) rotate(3deg);
    opacity: 0.9;
  }
}

.search-button:hover:not(:disabled) {
  border-color: transparent; /* 隐藏原始物理边框 */
  color: #fff;

  /* 双层背景实现渐变边框：
     第一层：内部背景色 (深灰)
     第二层：边框渐变色 (Google 四色彩虹)
  */
  background-image:
      linear-gradient(#303134, #303134),
      linear-gradient(135deg, #4285f4, #34a853, #fbbc05, #ea4335);

  /* 外部光晕 (蓝色系，增强科技感) */
  box-shadow: 0 0 12px rgba(66, 133, 244);
}

.search-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 自定义上传按钮样式 - 更深的颜色 */
.custom-upload-btn {
  position: relative;
  padding: 0 16px;
  /* 默认背景：深灰，与搜索框区分开 */
  background-color: #434549;
  color: #e8eaed;
  font-size: 15px;
  font-weight: 500;
  border-radius: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;

  /* 默认边框：细灰线 */
  border: 1px solid #5f6368;
  /* 关键：设置背景裁剪，为渐变做准备 */
  background-clip: padding-box, border-box;
  background-origin: border-box;
  transition: all 0.3s ease;
  z-index: 1;
}

.custom-upload-btn:hover {
  border-color: transparent; /* 隐藏原始物理边框 */
  color: #fff;

  /* 双层背景实现渐变边框：
     第一层：内部背景色 (深灰)
     第二层：边框渐变色 (Google 四色彩虹)
  */
  background-image:
      linear-gradient(#303134, #303134),
      linear-gradient(135deg, #4285f4, #34a853, #fbbc05, #ea4335);

  /* 外部光晕 (蓝色系，增强科技感) */
  box-shadow: 0 0 12px rgba(255, 255, 255, 0.6);
}

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

/* 新增图片卡片样式 */
.image-card {
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  background: #303134;
}

.image-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3);
}

/* 卡片内部样式 */
.image-wrapper { 
  position: relative; 
  width: 100%; 
  min-height: 100px;
}
.card-img { width: 100%; display: block; }
.score-tag {
  position: absolute; top: 8px; right: 8px;
  background: rgba(0,0,0,0.6); color: #fff;
  font-size: 12px; padding: 2px 6px; border-radius: 4px;
}
.card-info { padding: 10px; background: #303134; }
.ocr-text {
  font-size: 12px; 
  color: #9aa0a6; 
  margin: 0 0 5px 0;
  display: flex;
  align-items: center;
}

.filename { 
  font-size: 13px; 
  color: #e8eaed; 
  margin: 0; 
  white-space: nowrap; 
  overflow: hidden; 
  text-overflow: ellipsis; 
}

.empty-state { margin-top: 100px; }

/* 自定义标签样式 */
.custom-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 12px; /* 更圆的圆角 */
  line-height: 1;
  margin-right: 6px;
  background: transparent; /* 去掉背景色 */
  border: 1px solid #8ab4f8; /* 淡蓝色边框 */
  color: #8ab4f8; /* 淡蓝色文字 */
  flex-shrink: 0;
}

.custom-tag.warning {
  background: transparent;
  color: #8ab4f8;
  border: 1px solid #8ab4f8;
}

/* Empty状态样式 */
:deep(.el-empty) {
  background-color: #202124;
}
:deep(.el-empty__description) {
  color: #9aa0a6;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .search-container {
    flex-direction: column;
  }
  
  .custom-upload-btn {
    width: fit-content;
    align-self: center;
  }
}
</style>