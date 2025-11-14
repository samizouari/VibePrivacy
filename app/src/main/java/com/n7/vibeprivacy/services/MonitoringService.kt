package com.n7.vibeprivacy.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.n7.vibeprivacy.R
import com.n7.vibeprivacy.core.audio.KeywordDetector
import com.n7.vibeprivacy.core.ml.FaceDetector
import com.n7.vibeprivacy.data.models.ThreatEvent
import com.n7.vibeprivacy.data.source.Repository
import com.n7.vibeprivacy.ui.overlay.ScreenMask
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : LifecycleService() {

    @Inject
    lateinit var faceDetector: FaceDetector
    @Inject
    lateinit var keywordDetector: KeywordDetector
    @Inject
    lateinit var screenMask: ScreenMask
    @Inject
    lateinit var repository: Repository

    private val CHANNEL_ID = "VibePrivacyMonitoringChannel"
    private val NOTIFICATION_ID = 1

    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var threatDetected: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        startCameraMonitoring()
        keywordDetector.startDetection { keyword ->
            Log.d("MonitoringService", "Keyword detected: $keyword")
            handleThreatDetection("Keyword: $keyword", "Screen masked")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCameraMonitoring()
        keywordDetector.stopDetection()
        screenMask.hideMask()
        faceDetector.close()
        cameraExecutor.shutdown()
    }

    private fun startCameraMonitoring() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        cameraProvider?.unbindAll()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, FaceDetectionAnalyzer { numberOfFaces ->
                    if (numberOfFaces > 1) {
                        handleThreatDetection("Multiple faces detected", "Screen masked")
                    } else if (numberOfFaces == 0 && threatDetected) {
                        // If no faces are detected after a threat, hide the mask
                        screenMask.hideMask()
                        threatDetected = false
                    }
                })
            }

        try {
            cameraProvider?.bindToLifecycle(
                this, // Use the service as LifecycleOwner
                cameraSelector,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Log.e("MonitoringService", "Use case binding failed", exc)
        }
    }

    private fun stopCameraMonitoring() {
        cameraProvider?.unbindAll()
    }

    private fun handleThreatDetection(threatType: String, actionTaken: String) {
        if (!threatDetected) {
            threatDetected = true
            screenMask.showMask()
            CoroutineScope(Dispatchers.IO).launch {
                repository.insertThreatEvent(
                    ThreatEvent(
                        timestamp = System.currentTimeMillis(),
                        threatType = threatType,
                        sensorData = "N/A", // Placeholder for actual sensor data
                        actionTaken = actionTaken
                    )
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "VibePrivacy Monitoring Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VibePrivacy Monitoring")
            .setContentText("Your privacy is being protected.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use an appropriate icon
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private inner class FaceDetectionAnalyzer(private val listener: (Int) -> Unit) : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image
            if (image != null) {
                val results = faceDetector.detectFaces(image, rotationDegrees)
                val numberOfFaces = results?.size ?: 0
                listener(numberOfFaces)
            }
            imageProxy.close()
        }
    }
}
