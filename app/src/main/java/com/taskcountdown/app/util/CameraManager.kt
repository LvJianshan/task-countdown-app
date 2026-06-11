package com.taskcountdown.app.util

import android.content.Context
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
 */
class CameraManager(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isInitialized = false

    /**
     * 初始化 CameraX
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
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageCapture!!
                )
                isInitialized = true
            } catch (e: Exception) {
                // 相机初始化失败（如无摄像头），后续拍照静默跳过
                isInitialized = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 拍照并保存到内部存储
     * @return 照片文件，失败则返回 null
     */
    suspend fun takePhoto(): File? = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext null

        suspendCancellableCoroutine { continuation ->
            val photoDir = File(context.filesDir, "photos")
            photoDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val photoFile = File(photoDir, "IMG_$timestamp.jpg")

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture?.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        continuation.resume(photoFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(null)
                    }
                }
            )
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
