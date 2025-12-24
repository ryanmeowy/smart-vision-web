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
          <div class="image-card" @mouseenter="handleMouseEnter(item)" @mouseleave="handleMouseLeave">
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
              <!-- 相似图片搜索提示 -->
              <div 
                v-if="hoveredItemId === item.id" 
                class="similar-search-tip" 
                :class="{ 'show': hoveredItemId === item.id }"
                @click="searchSimilarImages(item)"
              >
                查找相似图片
              </div>
              <div class="card-actions">
                <button class="ai-action-btn" @click.stop="openAiGen(item)" title="AI 生成文案">
                  <el-icon><MagicStick /></el-icon>
                </button>
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
    <GenCopyDialog ref="genDialogRef" />

  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {Loading, MagicStick, UploadFilled} from '@element-plus/icons-vue'
import axios from 'axios'
import {ElMessage} from 'element-plus'
// 引入我们的组件
import BatchUploadDialog from './components/BatchUploadDialog.vue'
import GenCopyDialog from './components/GenCopyDialog.vue'

const genDialogRef = ref(null)

// --- 状态 ---
const queryText = ref('')
const searching = ref(false)
const results = ref([])
const uploadDialogRef = ref(null)
const hoveredItemId = ref(null)
const searchingSimilar = ref(false)

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

// --- 动作 3: 鼠标悬停事件 ---
const handleMouseEnter = (item) => {
  hoveredItemId.value = item.id
}

// --- 动作 4: 鼠标离开事件 ---
const handleMouseLeave = () => {
  hoveredItemId.value = null
}

// --- 动作 5: 查找相似图片 ---
const searchSimilarImages = async (item) => {
  if (searchingSimilar.value) return
  
  searchingSimilar.value = true
  try {
    // 调用后端相似图片搜索接口
    const res = await axios.get('/api/v1/vision/similar', {
      params: { 
        imageId: item.id,
        limit: 20 
      }
    })
    
    // 更新结果
    results.value = res.data.data || []
    
    if (results.value.length === 0) {
      ElMessage.info('未找到相似图片')
    } else {
      ElMessage.success(`找到了 ${results.value.length} 张相似图片`)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('查找相似图片失败')
  } finally {
    searchingSimilar.value = false
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

const openAiGen = (item) => {
  // 假设 item.id 就是 Object Key，或者你的 DTO 里有 key 字段
  // 如果没有，你得在后端 SearchResultDTO 里把 key (url的path部分) 透传回来
  // 现在的 item.url 是签名过的长链接，提取 key 有点麻烦，建议后端 DTO 加一个 `objectKey` 字段

  // 临时方案：假设后端还没改，我们假装 item.filename 是 key (实际需要后端配合)
  const key = item.filename; // ⚠️ 这里需要后端配合返回真实的 OSS Key

  // 准备标签数据
  const tags = []
  if (item.ocrText) tags.push(item.ocrText)
  if (item.filename) tags.push(item.filename)

  genDialogRef.value.open(item.url, key, tags)
}
</script>

<style>
/* 全局样式重置 */
body { 
  margin: 0; 
  background-color: #1a1a1a; /* 更深的背景色 */
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', Arial, sans-serif; 
  color: #e8eaed;
}

.app-container { 
  max-width: 1400px; 
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
  color: #ffffff; /* 更亮的文字颜色 */
  margin-bottom: 20px; 
  letter-spacing: 1px;
}
.tag { 
  font-size: 14px; 
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4px 12px; 
  border-radius: 20px; 
  vertical-align: middle; 
  font-weight: 500;
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
  border-radius: 30px;
  background-color: #2d2d2d;
  border: 1px solid #444;
  padding: 2px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.search-input {
  flex: 1;
  background-color: transparent;
  color: #e8eaed;
  border: none;
  outline: none;
  padding: 15px 20px;
  border-radius: 30px 0 0 30px;
  font-size: 15px;
}

.search-input::placeholder {
  color: #aaa;
}

.search-button {
  border-radius: 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 15px;
}

.magic-wand {
  font-size: 18px;
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
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.search-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 自定义上传按钮样式 */
.custom-upload-btn {
  position: relative;
  padding: 0 20px;
  background: linear-gradient(135deg, #fbd793 0%, #f5576c 100%);
  color: white;
  font-size: 15px;
  font-weight: 500;
  border-radius: 30px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(245, 87, 108, 0.3);
  z-index: 1;
}

.custom-upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(245, 87, 108, 0.4);
}

/* 瀑布流样式 (核心) */
.waterfall {
  /* 分列：大屏4列，中屏3列... */
  column-count: 4; 
  column-gap: 24px;
}
@media (max-width: 1400px) { .waterfall { column-count: 3; } }
@media (max-width: 1024px) { .waterfall { column-count: 2; } }
@media (max-width: 768px) { .waterfall { column-count: 1; column-gap: 20px; } }

.waterfall-item {
  /* 防止卡片被拆分到两列 */
  break-inside: avoid;
  margin-bottom: 24px;
}

/* 新增图片卡片样式 */
.image-card {
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  background: #2d2d2d;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  position: relative;
}

.image-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

/* 卡片内部样式 */
.image-wrapper { 
  position: relative; 
  width: 100%; 
  min-height: 100px;
  overflow: hidden;
}
.card-img { 
  width: 100%; 
  display: block; 
  transition: transform 0.5s ease;
}
.image-card:hover .card-img {
  transform: scale(1.05);
}

.score-tag {
  position: absolute; 
  top: 16px;
  right: 16px;
  background: linear-gradient(135deg, rgb(147, 196, 251) 0%, rgb(192, 87, 245) 100%);
  color: #fff;
  font-size: 12px; 
  padding: 2px 8px;
  border-radius: 16px;
  font-weight: 500;
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.similar-search-tip {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  opacity: 0;
  visibility: hidden;
  transform: translateX(-50%) translateY(10px);
}

.similar-search-tip.show {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0);
}

.similar-search-tip:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateX(-50%) scale(1.05);
}

.card-info { 
  padding: 20px; 
  background: #2d2d2d; 
}

.ocr-text {
  font-size: 13px; 
  color: #cbd5e1;
  margin: 0 0 10px 0;
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.filename { 
  font-size: 14px;
  color: #f1f5f9;
  margin: 0;
  white-space: nowrap; 
  overflow: hidden; 
  text-overflow: ellipsis; 
  font-weight: 500;
  line-height: 1.4;
}

.empty-state { 
  margin-top: 100px; 
}

/* 自定义标签样式 */
.custom-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  font-size: 11px;
  border-radius: 12px;
  line-height: 1;
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
  color: white;
  flex-shrink: 0;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(99, 102, 241, 0.2);
}

.custom-tag.warning {
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
  color: white;
}

.image-wrapper { position: relative; }

.card-actions {
  position: absolute;
  top: 10px;
  left: 10px;
  opacity: 0;
  transition: opacity 0.2s;
}
.image-card:hover .card-actions { opacity: 1; }

.ai-action-btn {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: rgba(0,0,0,0.6);
  border: 1px solid rgba(255,255,255,0.3);
  color: #fff;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.ai-action-btn:hover { background: #8a5cf6; border-color: #8a5cf6; }

/* Empty状态样式 */
:deep(.el-empty) {
  background-color: #1a1a1a;
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
  
  .app-container {
    padding: 15px;
  }
  
  .header {
    margin-bottom: 30px;
  }
}
</style>