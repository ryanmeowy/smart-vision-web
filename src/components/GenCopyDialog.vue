<template>
  <el-dialog
      v-model="visible"
      title="✨ AI 灵感生成"
      width="800px"
      class="ai-dialog"
      destroy-on-close
  >
    <div class="container">
      <!-- 左侧：图片预览 -->
      <div class="image-box">
        <el-image :src="previewUrl" fit="contain" class="preview-img" />
      </div>

      <!-- 右侧：生成控制区 -->
      <div class="content-box">
        <!-- 风格选择器 -->
        <div class="style-selector">
          <label>文案风格：</label>
          <el-radio-group v-model="selectedStyle" :disabled="isGenerating">
            <el-radio-button label="xiaohongshu">📕 小红书</el-radio-button>
            <el-radio-button label="ecommerce">🛒 电商带货</el-radio-button>
            <el-radio-button label="moment">🌿 朋友圈</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 生成按钮 -->
        <div class="actions">
          <el-button
              type="primary"
              :loading="isGenerating"
              @click="startGenerate"
              class="gen-btn"
          >
            <el-icon><MagicStick /></el-icon>
            {{ isGenerating ? 'AI 正在思考...' : '开始生成' }}
          </el-button>
        </div>

        <!-- 文本展示区 (打字机效果) -->
        <div class="text-output" v-loading="isThinking">
          <div v-if="!generatedText && !isGenerating" class="placeholder">
            选择风格，点击生成，让 AI 为您撰写文案...
          </div>
          <!-- pre-wrap 保留换行符 -->
          <div class="real-text" v-else>{{ generatedText }}<span v-if="isGenerating" class="cursor">|</span></div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const visible = ref(false)
const isGenerating = ref(false)
const isThinking = ref(false) // 连接建立前的等待状态
const generatedText = ref('')
const selectedStyle = ref('xiaohongshu')
const currentImgKey = ref('')
const previewUrl = ref('')

// 对外暴露的方法
const open = (imgUrl, objectKey) => {
  previewUrl.value = imgUrl
  currentImgKey.value = objectKey // 注意：这里需要传入 Key，因为后端需要重新签个有时效的 URL
  generatedText.value = ''
  visible.value = true
}

defineExpose({ open })

// 核心：SSE 调用逻辑
const startGenerate = async () => {
  if (!currentImgKey.value) return ElMessage.error('图片参数丢失')

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
    ElMessage.error('生成失败，请重试')
  } finally {
    isGenerating.value = false
    isThinking.value = false
  }
}
</script>

<style scoped>
.container { display: flex; height: 500px; gap: 20px; }
.image-box { flex: 1; background: #000; display: flex; align-items: center; justify-content: center; border-radius: 8px; overflow: hidden; }
.preview-img { max-width: 100%; max-height: 100%; }
.content-box { flex: 1; display: flex; flex-direction: column; gap: 15px; }

.style-selector label { font-size: 14px; color: #ccc; margin-right: 10px; }
.gen-btn { width: 100%; background: linear-gradient(90deg, #6366f1, #a855f7); border: none; font-weight: bold; }
.gen-btn:hover { opacity: 0.9; }

.text-output {
  flex: 1;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 15px;
  overflow-y: auto;
  color: #e6edf3;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap; /* 关键：保留 AI 的换行 */
}
.placeholder { color: #8b949e; text-align: center; margin-top: 80px; }
.cursor { animation: blink 1s infinite; display: inline-block; margin-left: 2px; }
@keyframes blink { 50% { opacity: 0; } }

/* 深度定制 Dialog 样式 */
:deep(.ai-dialog) { background-color: #0d1117; border: 1px solid #30363d; }
:deep(.el-dialog__title) { color: #e6edf3; }
:deep(.el-radio-button__inner) { background: #0d1117; border-color: #30363d; color: #8b949e; }
:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #238636; border-color: #238636; box-shadow: none; color: white;
}
</style>