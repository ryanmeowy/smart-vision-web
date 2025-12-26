<template>
  <div v-if="visible" class="custom-dialog-overlay" @click="handleOverlayClick">
    <div class="custom-dialog" @click.stop>
      <!-- 对话框头部 -->
      <div class="dialog-header">
        <h3 class="dialog-title">✨ AI 灵感生成</h3>
        <button class="dialog-close" @click="close">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M12 4L4 12M4 4l8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <!-- 对话框内容 -->
      <div class="dialog-body">
        <div class="container">
          <!-- 左侧：图片预览 -->
          <div class="image-box">
            <img :src="previewUrl" fit="contain" class="preview-img" />
          </div>

          <!-- 右侧：生成控制区 -->
          <div class="content-box">
            <!-- 风格选择器 -->
            <div class="style-selector">
              <label>文案风格：</label>
              <div class="style-options">
                <button
                  v-for="style in styleOptions"
                  :key="style.value"
                  class="style-btn"
                  :class="{ active: selectedStyle === style.value }"
                  @click="selectedStyle = style.value"
                  :disabled="isGenerating"
                >
                  {{ style.emoji }} {{ style.label }}
                </button>
              </div>
            </div>

            <!-- 生成按钮 -->
            <div class="actions">
              <button
                class="gen-btn"
                :disabled="isGenerating"
                @click="startGenerate"
              >
                <svg v-if="!isGenerating" class="magic-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M8 1v3m0 8v3m7-7h-3M4 8H1m11.5-4.5l-2.1 2.1M4.6 13.4l-2.1 2.1m11-2.1l-2.1-2.1M4.6 2.6L2.5 0.5M8 11a3 3 0 1 1 0-6 3 3 0 0 1 0 6z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else class="loading-icon" width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" stroke-dasharray="31.416" stroke-dashoffset="7.854"/>
                </svg>
                {{ isGenerating ? 'AI 正在思考...' : '开始生成' }}
              </button>
            </div>

            <!-- 文本展示区 (打字机效果) -->
            <div class="text-output">
              <div v-if="!generatedText && !isGenerating" class="placeholder">
                ✨ 选择风格，点击生成，让 AI 为您撰写文案...
              </div>
              <!-- pre-wrap 保留换行符 -->
              <div class="real-text" v-else>{{ generatedText }}<span v-if="isGenerating" class="cursor">|</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref} from 'vue'

const visible = ref(false)
const isGenerating = ref(false)
const isThinking = ref(false) // 连接建立前的等待状态
const generatedText = ref('')
const selectedStyle = ref('xiaohongshu')
const currentImgKey = ref('')
const previewUrl = ref('')

// 风格选项配置
const styleOptions = [
  { value: 'xiaohongshu', label: '小红书', emoji: '📕' },
  { value: 'ecommerce', label: '电商带货', emoji: '🛒' },
  { value: 'moment', label: '朋友圈', emoji: '🌿' }
]

// 对外暴露的方法
const open = (imgUrl, objectKey) => {
  previewUrl.value = imgUrl
  currentImgKey.value = objectKey // 注意：这里需要传入 Key，因为后端需要重新签个有时效的 URL
  generatedText.value = ''
  visible.value = true
}

const close = () => {
  visible.value = false
}

const handleOverlayClick = () => {
  close()
}

defineExpose({ open })

// 核心：SSE 调用逻辑
const startGenerate = async () => {
  if (!currentImgKey.value) {
    alert('图片参数丢失')
    return
  }

  isGenerating.value = true
  isThinking.value = true
  generatedText.value = ''

  try {
    // 使用 fetch API 处理流式响应 (比 EventSource 更灵活，支持 POST，虽然这里是 GET)
    const response = await fetch(
        `/api/v1/gen/stream?key=${encodeURIComponent(currentImgKey.value)}&style=${selectedStyle.value}`
    )

    if (!response.ok) throw new Error('网络请求失败')

    isThinking.value = false // 开始传输了

    // 获取 Reader
    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      // 解析 SSE 格式 (data: xxx)
      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n')

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const content = line.slice(5) // 去掉 'data:' 前缀
          generatedText.value += content
        }
      }

      // 自动滚动到底部
      const textBox = document.querySelector('.text-output')
      if(textBox) textBox.scrollTop = textBox.scrollHeight
    }

  } catch (e) {
    console.error(e)
    alert('生成失败，请重试')
  } finally {
    isGenerating.value = false
    isThinking.value = false
  }
}
</script>

<style scoped>
/* 遮罩层样式 */
.custom-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

/* 对话框主体 */
.custom-dialog {
  width: 800px;
  max-width: 90vw;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 20px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.05);
  overflow: hidden;
  animation: slideIn 0.3s ease;
}

/* 对话框头部 */
.dialog-header {
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  padding: 24px 24px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dialog-title {
  color: #f1f5f9;
  font-weight: 600;
  font-size: 18px;
  letter-spacing: 0.5px;
  margin: 0;
}

.dialog-close {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-close:hover {
  color: #f1f5f9;
  background: rgba(148, 163, 184, 0.1);
  transform: rotate(90deg);
}

/* 对话框内容 */
.dialog-body {
  padding: 24px;
}

.container {
  display: flex;
  height: 500px;
  gap: 24px;
}

.image-box {
  flex: 1;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
}

.image-box::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, transparent 30%, rgba(99, 102, 241, 0.05) 50%, transparent 70%);
  pointer-events: none;
}

.preview-img {
  max-width: 100%;
  max-height: 100%;
  border-radius: 12px;
  transition: transform 0.3s ease;
  object-fit: contain;
}

.preview-img:hover {
  transform: scale(1.02);
}

.content-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.style-selector {
  background: rgba(15, 23, 42, 0.6);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.style-selector label {
  font-size: 14px;
  color: #cbd5e1;
  font-weight: 500;
  display: block;
  margin-bottom: 12px;
}

.style-options {
  display: flex;
  gap: 8px;
}

.style-btn {
  flex: 1;
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.3);
  color: #94a3b8;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.style-btn:hover {
  border-color: rgba(99, 102, 241, 0.5);
  color: #e2e8f0;
  transform: translateY(-1px);
}

.style-btn.active {
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  color: white;
  transform: translateY(-1px);
}

.actions {
  display: flex;
}

.gen-btn {
  width: 100%;
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
  border: none;
  font-weight: 600;
  border-radius: 12px;
  padding: 14px;
  font-size: 16px;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.gen-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.gen-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.4);
}

.gen-btn:hover:not(:disabled)::before {
  left: 100%;
}

.gen-btn:active:not(:disabled) {
  transform: translateY(0);
}

.gen-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.magic-icon {
  animation: sparkle 2s ease-in-out infinite;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

.text-output {
  flex: 1;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  padding: 20px;
  overflow-y: auto;
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  position: relative;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.1);
}

.text-output::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.3), transparent);
}

.placeholder {
  color: #64748b;
  text-align: center;
  margin-top: 80px;
  font-style: italic;
  position: relative;
}

.placeholder::before {
  content: '✨';
  display: block;
  font-size: 24px;
  margin-bottom: 12px;
  opacity: 0.6;
}

.real-text {
  animation: fadeIn 0.5s ease;
}

.cursor {
  animation: blink 1s infinite;
  display: inline-block;
  margin-left: 2px;
  color: #6366f1;
}

/* 动画定义 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes blink {
  50% { opacity: 0; }
}

@keyframes sparkle {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.1); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .custom-dialog {
    width: 95vw;
    margin: 20px;
  }

  .container {
    flex-direction: column;
    height: auto;
  }

  .image-box {
    height: 200px;
  }

  .style-options {
    flex-direction: column;
  }
}
</style>