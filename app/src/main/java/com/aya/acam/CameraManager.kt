package com.aya.acam

import android.content.Context
import androidx.camera.core.*

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import timber.log.Timber

class CameraManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    val cameraCapabilities = mutableListOf<Quality>()

    init {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProvider = cameraProviderFuture.get()
    }

    fun startCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {

        releaseCamera()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            camera = cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }, ContextCompat.getMainExecutor(context))

    }

    fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        imageCapture: ImageCapture,
        cameraSelectorType: Int = LENS_FACING_BACK
    ) {
        releaseCamera()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val cameraSelector = when (cameraSelectorType) {
                LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                //Todo: unsupport LENS_FACING_EXTERNAL and LENS_FACING_UNKNOWN
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )?.also {
                getSupportedQualities(it)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun startCameraWithController(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        cameraSelectorType: Int = LENS_FACING_BACK
    ) {
        releaseCamera()
        // Set up the CameraController
        val cameraController = LifecycleCameraController(context)

        val _cameraSelector = when (cameraSelectorType) {
            LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            //Todo: unsupport LENS_FACING_EXTERNAL and LENS_FACING_UNKNOWN
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        Timber.d("startCameraWithController lensFacingMode: $cameraSelectorType")
        cameraController.cameraSelector = _cameraSelector
        cameraController.bindToLifecycle(lifecycleOwner)

        // Attach the CameraController to PreviewView
        previewView.controller = cameraController
    }

    fun startCamera(
        previewView: PreviewView, lifecycleOwner: LifecycleOwner,
        videoCapture: VideoCapture<Recorder>, cameraSelectorType: Int = LENS_FACING_BACK
    ) {

        releaseCamera()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val cameraSelector = when (cameraSelectorType) {
                LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                //Todo: unsupport LENS_FACING_EXTERNAL and LENS_FACING_UNKNOWN
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )?.also {
                getSupportedQualities(it)
            }
        }, ContextCompat.getMainExecutor(context))

    }

    private fun getSupportedQualities(camera: Camera) {

        QualitySelector
            .getSupportedQualities(camera.cameraInfo)
            .filter { quality ->
                listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
                    .contains(quality)
            }.also {
                cameraCapabilities.clear()
                cameraCapabilities.addAll(it)
            }
    }

    //Todo: need def other
    fun getCameraIndex(lensFacingMode: Int): Int {
        return when (lensFacingMode) {
            LENS_FACING_FRONT -> 1
            LENS_FACING_BACK -> 0
            else -> 0 //fixme:not handle this now
        }
    }

    fun getCameraState(index: Int): LiveData<CameraState>? {
        return cameraProvider?.availableCameraInfos?.get(index)?.cameraState
    }

    fun checkFlashUnit(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() ?: false
    }

    fun switchTorch(state: Boolean): Boolean {
        if (checkFlashUnit()) {
            camera?.cameraControl?.enableTorch(state)
            return true
        } else {
            Timber.e("there is no flash unit.")
        }
        return false
    }

    fun startFocus(action: FocusMeteringAction) {
        camera?.let { it.cameraControl.startFocusAndMetering(action) }
    }

    fun zoom(z: Float) {
        camera?.let { it.cameraControl.setLinearZoom(z) }
    }

    fun releaseCamera() {
        cameraCapabilities.clear()
        cameraProvider?.unbindAll()
        camera = null
    }

    companion object {
        const val FLASH_AUTO = 0
        const val FLASH_ON = 1
        const val FLASH_OFF = 2

        const val LENS_FACING_FRONT = 0
        const val LENS_FACING_BACK = 1
        const val LENS_FACING_EXTERNAL = 2
        const val LENS_FACING_UNKNOWN = -1
    }

}

