<!-- src/components/BatchUploadDialog.vue -->
<template>
  <el-dialog
    v-model="visible"
    title="批量导入图片"
    width="700px"
    :close-on-click-modal="false"
    :before-close="handleBeforeClose"
    @closed="resetState"
  >
    <!-- ==================== 阶段 1: 文件选择与 OSS 上传 ==================== -->
    <div v-if="phase === 'UPLOAD_PHASE'">
      <!-- 上传控件 (仅在没开始上传时显示，或者允许追加) -->
      <div class="upload-area" v-if="!isUploading">
        <el-upload
          action="#"
          multiple
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileSelect"
          accept=".jpg,.jpeg,.png"
          drag
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处，或 <em>点击选择</em> (支持多选)
          </div>
        </el-upload>
      </div>

      <!-- 文件列表状态展示 (核心交互区域) -->
      <div v-if="customFileList.length > 0" class="file-list-container">
        <div class="list-header">
          <span>待处理文件: {{ customFileList.length }}</span>
          <el-button link type="primary" @click="customFileList = []" :disabled="isUploading">清空</el-button>
        </div>
        
        <el-scrollbar height="300px">
          <div v-for="(item, index) in customFileList" :key="index" class="file-item">
            <div class="file-info">
              <span class="file-name" :title="item.file.name">{{ item.file.name }}</span>
              <!-- 状态标签 -->
              <el-tag v-if="item.status === 'ready'" size="small" type="info">待上传</el-tag>
              <el-tag v-else-if="item.status === 'uploading'" size="small">上传中 {{ item.percent }}%</el-tag>
              <el-tag v-else-if="item.status === 'success'" size="small" type="success">OSS上传成功</el-tag>
              <el-tag v-else-if="item.status === 'error'" size="small" type="danger">失败</el-tag>
            </div>
            <!-- 进度条 -->
            <el-progress 
              :percentage="item.percent" 
              :status="item.status === 'error' ? 'exception' : (item.status === 'success' ? 'success' : '')" 
              :show-text="false"
              :stroke-width="2"
            />
          </div>
        </el-scrollbar>
      </div>
    </div>

    <!-- ==================== 阶段 2: 后端 AI 处理 (轮询中) ==================== -->
    <div v-else-if="phase === 'PROCESS_PHASE'" class="processing-box">
      <el-progress type="dashboard" :percentage="backendProgress" />
      <div class="status-text">
        <h3>AI 正在分析语义与入库...</h3>
        <p>已处理: {{ processedCount }} / {{ totalCount }}</p>
        <p class="sub-tip">后端异步处理中，请勿关闭窗口</p>
      </div>
    </div>

    <!-- ==================== 阶段 3: 最终结果汇总 ==================== -->
    <div v-else-if="phase === 'Result_PHASE'" class="result-box">
      <el-result
        icon="success"
        title="处理完成"
        :sub-title="`成功入库 ${finalResult.successCount} 张，失败 ${finalResult.failureCount} 张`"
      >
        <template #extra>
          <el-button type="primary" @click="closeDialog">关闭</el-button>
        </template>
      </el-result>
      
      <!-- 如果有后端处理失败的，展示在这里 -->
      <div v-if="finalResult.failureCount > 0" class="error-log">
        <p>处理失败详情 (AI/数据库错误):</p>
        <ul>
          <li v-for="err in finalResult.failures" :key="err.objectKey">
            {{ err.originalName }}: {{ err.errorMessage }}
          </li>
        </ul>
      </div>
    </div>

    <!-- ==================== 底部按钮区 ==================== -->
    <template #footer>
      <div v-if="phase === 'UPLOAD_PHASE'" class="dialog-footer">
        <span class="summary-text" v-if="stats.total > 0">
          成功: {{ stats.success }} | 失败: {{ stats.error }}
        </span>

        <!-- 场景 A: 还没开始，或者全部成功 -->
        <el-button 
          v-if="stats.error === 0 && stats.success < stats.total" 
          type="primary" 
          @click="startOssUpload" 
          :loading="isUploading"
          :disabled="stats.total === 0"
        >
          {{ isUploading ? '正在上传OSS...' : '开始上传' }}
        </el-button>

        <!-- 场景 B: 有失败的，显示重试按钮 -->
        <el-button 
          v-if="stats.error > 0" 
          type="danger" 
          @click="retryFailedUploads" 
          :loading="isUploading"
        >
          重试失败项 ({{ stats.error }})
        </el-button>

        <!-- 场景 C: 有成功的，允许提交给后端 -->
        <el-button 
          v-if="stats.success > 0 && !isUploading" 
          type="success" 
          @click="submitToBackend"
        >
          提交处理 ({{ stats.success }}张)
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, reactive, onUnmounted } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import OSS from 'ali-oss' // 务必安装: npm install ali-oss

// --- 核心状态 ---
const visible = ref(false)
// 阶段: UPLOAD_PHASE (直传OSS) -> PROCESS_PHASE (后端AI处理) -> Result_PHASE (结果)
const phase = ref('UPLOAD_PHASE') 
const isUploading = ref(false) // 是否正在直传OSS中

// 自定义文件列表，存储每个文件的详细状态
// Item结构: { file: File, status: 'ready'|'uploading'|'success'|'error', percent: 0, objectKey: '' }
const customFileList = ref([])

// 后端轮询相关
const taskId = ref('')
const backendProgress = ref(0)
const processedCount = ref(0)
const totalCount = ref(0)
const finalResult = ref({})
let pollTimer = null

// --- 计算属性: 统计当前上传状态 ---
const stats = computed(() => {
  let success = 0, error = 0, total = customFileList.value.length
  customFileList.value.forEach(item => {
    if (item.status === 'success') success++
    if (item.status === 'error') error++
  })
  return { total, success, error }
})

// --- 对外暴露方法 ---
const open = () => {
  resetState()
  visible.value = true
}
defineExpose({ open })

// --- 逻辑 1: 文件选择 ---
const handleFileSelect = (uploadFile) => {
  // Element Plus 的 onChange 会多次触发，这里做简单的去重或追加
  // 我们只关心 raw (原生File对象)
  const rawFile = uploadFile.raw
  // 包装成我们的状态对象
  customFileList.value.push({
    file: rawFile,
    status: 'ready',
    percent: 0,
    objectKey: '' // 上传成功后回填
  })
}

// --- 逻辑 2: 获取 STS Token (关键安全步骤) ---
let ossClient = null
const initOssClient = async () => {
  try {
    // 调用后端获取临时凭证
    const res = await axios.get('/api/v1/oss/sts')
    const { accessKeyId, accessKeySecret, securityToken, region, bucket } = res.data.data
    
    // 初始化阿里 OSS SDK
    ossClient = new OSS({
      region: region || 'oss-cn-shanghai',
      accessKeyId,
      accessKeySecret,
      stsToken: securityToken,
      bucket: bucket,
      secure: true // 使用 HTTPS
    })
    return true
  } catch (e) {
    console.error('STS获取失败', e)
    ElMessage.error('无法获取上传凭证，请检查后端服务')
    return false
  }
}

// --- 逻辑 3: 执行 OSS 上传 (支持重试) ---
const startOssUpload = async () => {
  if (customFileList.value.length === 0) return
  
  // 1. 初始化/刷新 Token
  const ready = await initOssClient()
  if (!ready) return

  isUploading.value = true

  // 2. 遍历列表，只上传状态为 'ready' 或 'error' 的文件
  const uploadPromises = customFileList.value.map(async (item) => {
    if (item.status === 'success') return // 已经成功的跳过

    item.status = 'uploading'
    item.percent = 0

    try {
      // 构造存储路径: images/2024/05/timestamp_filename
      const filename = `${Date.now()}_${item.file.name}`
      const storeAs = `images/${new Date().toISOString().split('T')[0]}/${filename}`

      // 执行直传
      const result = await ossClient.put(storeAs, item.file, {
        // 进度回调
        progress: (p) => {
          item.percent = Math.floor(p * 100)
        }
      })

      // 成功: 记录 objectKey (发给后端只要这个 Key，不要完整 URL)
      item.status = 'success'
      item.objectKey = result.name 
      item.percent = 100
    } catch (e) {
      console.error('单文件上传失败', e)
      item.status = 'error'
      item.percent = 0
    }
  })

  // 等待所有上传完成 (无论成功失败)
  await Promise.all(uploadPromises)
  isUploading.value = false
  
  if (stats.value.error > 0) {
    ElMessage.warning(`有 ${stats.value.error} 个文件上传失败，请点击重试`)
  } else {
    ElMessage.success('所有文件上传OSS成功，请提交处理')
  }
}

// --- 逻辑 4: 重试失败项 (复用上传逻辑) ---
const retryFailedUploads = () => {
  // startOssUpload 内部逻辑会自动挑选非 success 的文件进行重传
  // 这里再次调用即可
  startOssUpload()
}

// --- 逻辑 5: 提交给后端并开始轮询 ---
const submitToBackend = async () => {
  // 1. 提取所有成功的 Key
  const successKeys = customFileList.value
    .filter(item => item.status === 'success')
    .map(item => item.objectKey)

  if (successKeys.length === 0) return

  // 切换界面状态
  phase.value = 'PROCESS_PHASE'
  
  try {
    // 2. 调用后端异步接口，提交任务
    // 接口: POST /api/v1/vision/batch-process-async
    // 参数: ["images/xx.jpg", "images/yy.jpg"]
    const res = await axios.post('/api/v1/vision/batch-process-async', successKeys)
    
    // 3. 拿到 TaskID，开始轮询
    taskId.value = res.data.data
    startPolling()
    
  } catch (e) {
    console.error(e)
    ElMessage.error('提交后端任务失败')
    phase.value = 'UPLOAD_PHASE' // 回退
  }
}

// --- 逻辑 6: 轮询状态 ---
const startPolling = () => {
  pollTimer = setInterval(async () => {
    try {
      const res = await axios.get(`/api/v1/vision/task/${taskId.value}`)
      const task = res.data.data // AsyncBatchTask 对象
      
      // 更新进度显示
      totalCount.value = task.total
      processedCount.value = task.processed
      if (task.total > 0) {
        backendProgress.value = Math.floor((task.processed / task.total) * 100)
      }

      // 判断终态
      if (task.status === 'COMPLETED' || task.status === 'FAILED') {
        clearInterval(pollTimer)
        finalResult.value = task.result || {}
        // 稍微延时展示结果
        setTimeout(() => { phase.value = 'Result_PHASE' }, 500)
      }
    } catch (e) {
      console.warn('轮询异常', e)
    }
  }, 1000) // 1秒一次
}

// --- 辅助逻辑: 关闭与重置 ---
const handleBeforeClose = (done) => {
  if (phase.value === 'PROCESS_PHASE') {
    ElMessageBox.confirm('后台正在处理数据，关闭窗口不会停止任务，但您将无法查看进度。确定关闭吗？')
      .then(() => done())
      .catch(() => {})
  } else {
    done()
  }
}

const resetState = () => {
  if (pollTimer) clearInterval(pollTimer)
  visible.value = false
  phase.value = 'UPLOAD_PHASE'
  customFileList.value = []
  backendProgress.value = 0
  taskId.value = ''
}

const closeDialog = () => {
  visible.value = false
  // 触发父组件刷新列表
  // emit('refresh')
}

// 组件卸载时兜底清理定时器
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.upload-area {
  margin-bottom: 20px;
}
.file-list-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}
.file-item {
  margin-bottom: 12px;
}
.file-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 13px;
}
.file-name {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.processing-box {
  text-align: center;
  padding: 40px 0;
}
.status-text {
  margin-top: 20px;
}
.sub-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
.result-box {
  padding: 20px;
}
.error-log {
  background: #fef0f0;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #f56c6c;
  max-height: 150px;
  overflow-y: auto;
}
.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.summary-text {
  font-size: 13px;
  color: #606266;
}
</style>