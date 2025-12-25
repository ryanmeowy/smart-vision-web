<template>
  <el-dialog
      v-model="visible"
      title="批量导入图片"
      width="720px"
      :close-on-click-modal="false"
      :before-close="handleBeforeClose"
      @closed="resetState"
  >
    <!-- ==================== 界面状态 1: 文件选择与列表展示 ==================== -->
    <div v-if="phase === 'IDLE' || phase === 'UPLOADING'">

      <!-- 1.1 拖拽上传区 (只有非上传状态才显示，防止误操作) -->
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

      <!-- 1.2 文件列表 (核心展示区) -->
      <div v-if="customFileList.length > 0" class="file-list-box">
        <div class="list-header">
          <span>待处理清单 ({{ customFileList.length }})</span>
          <el-button
              v-if="!isUploading"
              link type="primary"
              @click="customFileList = []"
          >
            清空列表
          </el-button>
        </div>

        <el-scrollbar height="320px">
          <div v-for="(item, index) in customFileList" :key="index" class="file-item">
            <div class="file-row">
              <span class="file-name" :title="item.file.name">{{ item.file.name }}</span>

              <!-- 状态徽章 -->
              <div class="file-status">
                <el-tag v-if="item.status === 'ready'" type="info" size="small">准备就绪</el-tag>
                <el-tag v-else-if="item.status === 'uploading_oss'" size="small">OSS上传中...</el-tag>
                <el-tag v-else-if="item.status === 'oss_success'" type="success" size="small">OSS已完成</el-tag>
                <el-tag v-else-if="item.status === 'processing_backend'" type="warning" size="small">AI处理中...</el-tag>
                <el-tag v-else-if="item.status === 'done'" type="success" effect="dark" size="small">✅ 入库成功</el-tag>
                <el-tag v-else-if="item.status === 'error'" type="danger" size="small">失败</el-tag>
              </div>
            </div>

            <!-- 错误提示 -->
            <div v-if="item.status === 'error'" class="error-msg">
              {{ item.errorMsg }}
            </div>

            <!-- 进度条 (仅在上传OSS时显示) -->
            <el-progress
                v-if="item.status === 'uploading_oss'"
                :percentage="item.uploadPercent"
                :show-text="false"
                :stroke-width="2"
            />
          </div>
        </el-scrollbar>
      </div>
    </div>

    <!-- ==================== 界面状态 2: 后端处理进度 (大盘展示) ==================== -->
    <div v-else-if="phase === 'PROCESSING'" class="processing-panel">
      <el-progress type="dashboard" :percentage="overallProgress" />
      <div class="status-text">
        <h3>正在进行 AI 向量化与索引...</h3>
        <p>已处理: {{ processedCount }} / {{ totalSubmitCount }}</p>
        <p class="tip">为防止请求超时，系统正在分批提交，请勿关闭窗口</p>
      </div>
    </div>

    <!-- ==================== 界面状态 3: 最终结果 ==================== -->
    <div v-else-if="phase === 'FINISHED'" class="result-panel">
      <el-result
          :icon="finalStats.fail > 0 ? 'warning' : 'success'"
          :title="finalStats.fail > 0 ? '处理完成 (部分失败)' : '全部成功'"
          :sub-title="`成功: ${finalStats.success} | 失败: ${finalStats.fail}`"
      >
        <template #extra>
          <el-button @click="closeDialog">关闭</el-button>
          <el-button v-if="finalStats.fail > 0" type="primary" @click="retryFailures">
            重试失败项
          </el-button>
        </template>
      </el-result>
    </div>

    <!-- ==================== 底部按钮 ==================== -->
    <template #footer>
      <div v-if="phase === 'IDLE' || phase === 'UPLOADING'" class="footer-actions">
        <div class="summary">
          <span v-if="stats.error > 0" class="text-danger">失败: {{ stats.error }} 项</span>
        </div>

        <div class="buttons">
          <el-button @click="visible = false" :disabled="isUploading">取消</el-button>

          <!-- 核心按钮：一键开始 (包含上传+处理) -->
          <el-button
              type="primary"
              @click="startPipeline"
              :loading="isUploading"
              :disabled="customFileList.length === 0"
          >
            {{ stats.error > 0 ? '重试失败项' : '开始上传与处理' }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {computed, ref} from 'vue'
import {UploadFilled} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import axios from 'axios'
import OSS from 'ali-oss'
import { decrypt } from '../utils/crypto'

// --- 数据结构定义 ---
// phase: 'IDLE' (空闲) -> 'UPLOADING' (直传OSS中) -> 'PROCESSING' (后端分批处理中) -> 'FINISHED' (结束)
const phase = ref('IDLE')
const visible = ref(false)
const isUploading = ref(false) // 这是一个总的 Loading 状态
const accessToken = ref('') // 用户输入的访问口令

// 文件列表，每个 Item 包含完整状态机
// Item { file: File, status: string, uploadPercent: number, objectKey: string, errorMsg: string }
// status枚举: 'ready', 'uploading_oss', 'oss_success', 'processing_backend', 'done', 'error'
const customFileList = ref([])

// 进度统计相关
const processedCount = ref(0)
const totalSubmitCount = ref(0)
const overallProgress = computed(() => {
  if (totalSubmitCount.value === 0) return 0
  return Math.floor((processedCount.value / totalSubmitCount.value) * 100)
})

// 统计当前列表状态
const stats = computed(() => {
  let success = 0, error = 0, total = customFileList.value.length
  customFileList.value.forEach(i => {
    if (i.status === 'done') success++
    if (i.status === 'error') error++
  })
  return { success, error, total }
})

// 最终结果统计 (用于结果页)
const finalStats = ref({ success: 0, fail: 0 })

// --- 暴露给父组件的方法 ---
const open = () => {
  resetState()
  visible.value = true
}
defineExpose({ open })

// --- 1. 文件选择 ---
const handleFileSelect = (uploadFile) => {
  // 检查文件大小 (限制为10MB)
  const maxSize = 10 * 1024 * 1024 // 10MB in bytes
  if (uploadFile.raw.size > maxSize) {
    customFileList.value.push({
      file: uploadFile.raw,
      status: 'error',
      uploadPercent: 0,
      objectKey: '',
      errorMsg: '文件大小超过10MB限制'
    })
    return
  }
  
  customFileList.value.push({
    file: uploadFile.raw,
    status: 'ready', // 初始状态
    uploadPercent: 0,
    objectKey: '',   // 上传成功后填入
    errorMsg: ''
  })
}

// --- 2. 获取 STS Token (鉴权) ---
let ossClient = null
const initOssClient = async () => {
  try {
    const res = await axios.get('/api/v1/auth/sts', {
      headers: {
        'X-Access-Token': accessToken.value
      }
    })
    
    // 检查响应状态
    if (res.status !== 200) {
      ElMessage.error(`获取STS凭证失败，HTTP状态码: ${res.status}`)
      return false
    }
    
    const data = decrypt(res.data.data)

    // 检查返回的数据是否完整
    if (!data || !data.accessKeyId || !data.accessKeySecret || !data.securityToken) {
      ElMessage.error('STS凭证信息不完整')
      console.error('STS返回数据:', data)
      return false
    }
    
    ossClient = new OSS({
      region: 'oss-cn-shanghai', // 请确保和后端配置一致
      accessKeyId: data.accessKeyId,
      accessKeySecret: data.accessKeySecret,
      stsToken: data.securityToken,
      bucket: 'ryansimg', // 这里替换成你真实的 Bucket 名字
      secure: true
    })
    return true
  } catch (e) {
    console.error('获取STS凭证异常:', e)
    if (e.response) {
      // 服务器返回了错误响应
      ElMessage.error(`获取上传凭证失败: ${e.response.status} - ${e.response.statusText}`)
      if (e.response.status === 403) {
        ElMessage.error('访问被拒绝，请检查您的访问令牌是否正确以及是否有足够权限')
      }
    } else if (e.request) {
      // 请求发出但没有收到响应
      ElMessage.error('无法连接到服务器获取上传凭证，请检查网络连接')
    } else {
      // 其他错误
      ElMessage.error(`初始化上传客户端失败: ${e.message}`)
    }
    return false
  }
}

// --- 3. 主流程入口 (点击开始/重试) ---
const startPipeline = async () => {
  if (customFileList.value.length === 0) return

  // 提示用户输入访问口令
  const { value: token } = await ElMessageBox.prompt('口令联系管理员获取: ryanxys@gmail.com', '身份验证', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPlaceholder: '请输入访问口令',
    inputValidator: (value) => {
      if (!value || value.trim() === '') {
        return '口令不能为空'
      }
      return true
    }
  })

  accessToken.value = token

  // 初始化 OSS 客户端
  const ready = await initOssClient()
  if (!ready) return

  isUploading.value = true

  try {
    // 步骤 A: 过滤出需要上传 OSS 的文件 (状态是 ready 或 error 且没有 key)
    const filesToUpload = customFileList.value.filter(item =>
        item.status === 'ready' || (item.status === 'error' && !item.objectKey)
    )

    // 执行 OSS 直传
    if (filesToUpload.length > 0) {
      phase.value = 'UPLOADING' // 界面显示上传状态
      await uploadToOss(filesToUpload)
    }

    // 步骤 B: 过滤出需要提交后端的 (OSS 成功了，但还没入库成功的)
    const filesToProcess = customFileList.value.filter(item =>
        (item.status === 'oss_success') || (item.status === 'error' && item.objectKey)
    )

    if (filesToProcess.length > 0) {
      phase.value = 'PROCESSING' // 界面切换到大盘进度条
      await processInBackend(filesToProcess)
    }

    // 流程结束，展示结果
    phase.value = 'FINISHED'

  } catch (e) {
    console.error('Pipeline Error', e)
    ElMessage.error('流程异常中断')
  } finally {
    isUploading.value = false
  }
}

// --- 4. 具体的 OSS 上传逻辑 ---
const uploadToOss = async (items) => {
  // 并发上传，Promise.all
  const uploads = items.map(async (item) => {
    item.status = 'uploading_oss'
    item.errorMsg = ''

    try {
      // 构造文件名: images/2024/12/18/timestamp_filename
      const dateStr = new Date().toISOString().split('T')[0]
      const storeAs = `images/${dateStr}/${Date.now()}_${item.file.name}`

      const result = await ossClient.put(storeAs, item.file, {
        progress: (p) => { item.uploadPercent = Math.floor(p * 100) }
      })

      // 成功，保存 Key
      item.objectKey = result.name
      item.status = 'oss_success'
    } catch (e) {
      console.error('OSS上传失败:', e)
      item.status = 'error'
      
      // 提供更具体的错误信息
      if (e.code) {
        switch(e.code) {
          case 'AccessDenied':
            item.errorMsg = '访问被拒绝，请检查权限配置'
            break
          case 'InvalidAccessKeyId':
            item.errorMsg = 'AccessKeyId无效'
            break
          case 'RequestTimeTooSkewed':
            item.errorMsg = '客户端时间与服务器时间相差过大'
            break
          case 'SignatureDoesNotMatch':
            item.errorMsg = '签名错误'
            break
          default:
            item.errorMsg = `上传失败: ${e.code}`
        }
      } else {
        item.errorMsg = '网络传输失败或服务器拒绝访问'
      }
      
      console.error('详细错误信息:', {
        code: e.code,
        message: e.message,
        name: e.name,
        status: e.status
      })
    }
  })

  await Promise.all(uploads)
}

// --- 5. 具体的后端处理逻辑 (分批提交) ---
const processInBackend = async (items) => {
  totalSubmitCount.value = items.length
  processedCount.value = 0

  // 核心策略：切片 (Chunking)，每 5 个发一次请求，防止超时
  const CHUNK_SIZE = 5

  for (let i = 0; i < items.length; i += CHUNK_SIZE) {
    const chunk = items.slice(i, i + CHUNK_SIZE)

    // 先把这一批的状态改为处理中
    chunk.forEach(it => it.status = 'processing_backend')

    // 构造请求参数 DTO: [{ key: "...", fileName: "..." }]
    const payload = chunk.map(it => ({
      key: it.objectKey,
      fileName: it.file.name
    }))

    try {
      // 调用后端同步接口 (后端内部会重试)
      const res = await axios.post('/api/v1/image/batch-process', payload, {
        headers: {
          'X-Access-Token': accessToken.value
        }
      })
      const resultData = res.data.data // { successCount, failureCount, failures: [] }

      // 更新进度
      processedCount.value += chunk.length

      // 根据后端返回的失败列表，更新每个文件的状态
      // 失败列表里的 item 结构: { objectKey, errorMessage }
      const failedList = resultData.failures || []

      chunk.forEach(feItem => {
        // 在失败列表里找，找得到就是失败，找不到就是成功
        const failRecord = failedList.find(f => f.objectKey === feItem.objectKey)

        if (failRecord) {
          feItem.status = 'error'
          feItem.errorMsg = failRecord.errorMessage // 显示后端给的原因(如 AI超时)
        } else {
          feItem.status = 'done' // 全部成功
        }
      })

    } catch (e) {
      // 如果整个请求挂了 (500/网络断了)
      console.error('后端处理请求失败:', e)
      chunk.forEach(it => {
        it.status = 'error'
        if (e.response) {
          if (e.response.status === 403) {
            it.errorMsg = '服务器拒绝访问，请检查令牌权限'
          } else {
            it.errorMsg = `服务器错误: ${e.response.status}`
          }
        } else {
          it.errorMsg = '服务器连接失败'
        }
      })
      processedCount.value += chunk.length
    }
  }

  // 统计最终结果
  finalStats.value.success = customFileList.value.filter(i => i.status === 'done').length
  finalStats.value.fail = customFileList.value.filter(i => i.status === 'error').length
}

// --- 6. 重试逻辑 (Retry) ---
const retryFailures = () => {
  // 重置状态
  phase.value = 'IDLE' // 回到列表页

  // 这里的关键是：不清除 customFileList，只是把 error 的项让用户看到
  // 用户再次点击 "开始上传与处理" 时，startPipeline 会自动筛选状态为 error 的项
}

// --- 7. 辅助方法 ---
const resetState = () => {
  visible.value = false
  phase.value = 'IDLE'
  customFileList.value = []
  processedCount.value = 0
  totalSubmitCount.value = 0
  isUploading.value = false
  accessToken.value = ''
}

const closeDialog = () => {
  visible.value = false
  // 通知父组件刷新
  // emit('refresh')
}

const handleBeforeClose = (done) => {
  if (isUploading.value) {
    ElMessageBox.confirm('任务正在进行中，关闭将中断后续操作。确定关闭？')
        .then(() => done())
        .catch(() => {})
  } else {
    done()
  }
}
</script>

<style scoped>
.upload-area { margin-bottom: 15px; }
.file-list-box { border: 1px solid #e4e7ed; border-radius: 4px; padding: 10px; background: #fff; }
.list-header { display: flex; justify-content: space-between; margin-bottom: 10px; color: #606266; font-size: 14px; }

.file-item { padding: 8px 0; border-bottom: 1px dashed #f2f2f2; }
.file-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; }
.file-name { font-size: 13px; color: #333; max-width: 60%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.error-msg { font-size: 12px; color: #f56c6c; margin-bottom: 4px; }

.processing-panel { text-align: center; padding: 40px 0; }
.status-text { margin-top: 20px; }
.tip { font-size: 12px; color: #909399; margin-top: 5px; }

.result-panel { padding: 20px; }

.footer-actions { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.text-danger { color: #f56c6c; font-size: 13px; font-weight: bold; }
</style>