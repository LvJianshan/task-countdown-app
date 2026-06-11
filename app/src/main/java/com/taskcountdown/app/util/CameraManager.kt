package com.taskcountdown.app.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 相机管理器
 * 在后台使用 CameraX 自动拍照，不显示预览画面
 * 完全独立于倒计时逻辑，拍照不会阻塞计时器
 * 使用前置摄像头，照片保存到系统相册
 */
class CameraManager(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isInitialized = false

    /**
     * 初始化 CameraX（前置摄像头）
     * 只绑定 ImageCapture（无预览），后台静默拍照
     */
    fun initialize(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture!!
                )
                isInitialized = true
            } catch (e: Exception) {
                isInitialized = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 拍照并保存到系统相册
     * @return 照片文件（内部存储，用于App内展示），失败返回 null
     */
    suspend fun takePhoto(): File? = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext null

        suspendCancellableCoroutine { continuation ->
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val fileName = "IMG_$timestamp.jpg"

            // 1. 先保存到内部存储（App内展示用）
            val internalDir = File(context.filesDir, "photos")
            internalDir.mkdirs()
            val internalFile = File(internalDir, fileName)

            val outputOptions = ImageCapture.OutputFileOptions.Builder(internalFile).build()

            imageCapture?.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        // 2. 同步到系统相册（让用户在相册中可见）
                        saveToGallery(internalFile, fileName)
                        continuation.resume(internalFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(null)
                    }
                }
            )
        }
    }

    /**
     * 将照片写入系统相册（MediaStore）
     * 用户可以在相册App中看到拍的照片
     */
    private fun saveToGallery(file: File, fileName: String) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                // API 29+ 使用相对路径，低版本不需要
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TaskCountdown")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // 标记完成（仅 API 29+ 需要）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            // 保存到相册失败不影响 App 内展示
            e.printStackTrace()
        }
    }

    /**
     * 释放相机资源
     */
    fun release() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
        isInitialized = false
    }
}
