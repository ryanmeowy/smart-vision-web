# 🌌 SmartVision Web - 企业级多模态 AI 检索前端

> **SmartVision** 是一个基于 **Vue 3 + Vite + Element Plus** 构建的现代化 AI 搜索前端应用。它实现了“以文搜图”的核心交互，并包含一套完整的高并发图片批量入库、OSS 直传与错误重试机制。

![License](https://img.shields.io/badge/License-MIT-green)
![Vue](https://img.shields.io/badge/Vue.js-3.5.24-green.svg)
![Vite](https://img.shields.io/badge/Vite-7.2.4-purple.svg)
![ElementPlus](https://img.shields.io/badge/Element%20Plus-2.12.0-409eff.svg)

## ✨ 核心功能 (Features)

*   **🔍 语义检索 (Semantic Search)**：支持自然语言描述（如“雨后的森林”）搜索图片，通过瀑布流（Masonry Layout）优雅展示结果。
*   **🚀 极速批量上传 (Batch Ingestion)**：
    *   支持 **拖拽上传** 与 **多文件选择**。
    *   **客户端直传 OSS**：直接对接阿里云 OSS，节省后端带宽。
    *   **智能重试机制**：针对网络波动导致的上传失败，支持单文件维度的“一键重试”。
*   **🔄 异步任务管理**：
    *   上传后自动提交后端 AI 处理任务。
    *   实现 **分段式进度条**（上传进度 + 后端处理进度）。
    *   基于 **轮询 (Polling)** 的任务状态更新与结果汇总。
*   **📱 响应式设计**：适配不同屏幕尺寸的瀑布流布局。

## 🛠 技术栈 (Tech Stack)

| 模块 | 技术选型 |
| :--- | :--- |
| **框架** | Vue 3 (Composition API) |
| **构建工具** | Vite | 
| **UI 组件库** | Element Plus | 
| **网络请求** | Axios | 
| **云存储 SDK** | ali-oss 
| **样式** | CSS3 Flex / Grid | 

## 📐 架构设计 (Architecture)

### 1. 客户端直传架构 (Client-Side Direct Upload)
为了解决海量图片上传占用后端服务器带宽的问题，本项目采用了 **STS (Security Token Service)** 模式。
*   前端请求后端获取临时凭证 (STS Token)。
*   前端直接将文件 `PUT` 到阿里云 OSS。
*   上传成功后，仅将 Object Key 发送给后端进行 AI 分析。

### 2. 健壮的上传状态机
在 `BatchUploadDialog` 组件中，维护了一个微型状态机来管理复杂的上传流程：
*   `IDLE` (空闲/选择文件)
*   `UPLOADING` (OSS 网络传输中)
*   `PROCESSING` (后端 AI 向量化中 - 轮询)
*   `FINISHED` (完成/结果汇总)

### 3. 错误边界与重试
针对批量上传中可能出现的“部分成功”场景：
*   **隔离异常**：单张图片的失败不会中断整个队列。
*   **断点重试**：用户可点击“重试失败项”，程序会自动刷新 Token 并仅重新上传失败的文件。

## 🚀 快速开始 (Getting Started)

### 前置要求
*   Node.js >= 16.0
*   SmartVision 后端服务已启动 (默认运行在 8080 端口)

### 1. 安装依赖
```bash
npm install
```

### 2. 配置代理
检查 vite.config.js，确保代理指向你的后端地址:
```JavaStricp
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080', // 你的 Spring Boot 后端地址
      changeOrigin: true
    }
  }
}
```

### 3. 启动开发服务器
```bash
npm run dev
```
访问终端显示的地址 (通常是 http://localhost:5173)。

📂 目录结构
```text
src/
├── assets/             # 静态资源
├── components/
│   └── BatchUploadDialog.vue  # [核心] 批量上传与任务轮询组件
├── App.vue             # 主页面 (搜索栏 + 瀑布流)
├── main.js             # 入口文件
└── style.css           # 全局样式
```
