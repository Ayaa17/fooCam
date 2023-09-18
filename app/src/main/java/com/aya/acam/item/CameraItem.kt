package com.aya.acam.item

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.video.*
import androidx.camera.view.CameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.camera.view.video.ExperimentalVideo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.databinding.BindingAdapter
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.aya.acam.CameraManager
import com.aya.acam.utils.MediaUtils
import com.theeasiestway.yuv.YuvUtils
import jp.co.cyberagent.android.gpuimage.*
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer

abstract class CameraItem {

    abstract val type: Int
    var previewView: PreviewView? = null
    protected var lifecycleOwner: LifecycleOwner? = null
    protected var cameraController: CameraController? = null
    protected val lensFacingState: ObservableField<Int> =
        ObservableField(CameraManager.LENS_FACING_BACK)

    protected var cameraState: LiveData<CameraState>? = null
    protected var photoTapToFocusState: LiveData<Int>? = null

    val evMin = ObservableField(0)
    val evMax = ObservableField(0)
    var seekBarValue = ObservableField<Int>(0)

    fun getTypeString(): String {
        return when (type) {
            TAG_PHOTO -> PHOTO
            TAG_VIDEO -> VIDEO
            TAG_FILTER -> FILTER
            else -> UNKNOWN
        }
    }

    open fun starCamera(
        lifecycleOwner: LifecycleOwner,
        lensFacingMode: Int = lensFacingState.get()!!
    ) {
        evMin.set(0)
        evMax.set(0)
        Timber.d("starCamera : lensFacingMode: $lensFacingMode")
    }

    fun startListenCameraState(cameraManager: CameraManager?, index: Int) {
        cameraState = cameraManager?.getCameraState(
            index
        )?.also { it ->
            it.observe(lifecycleOwner!!) {
                Timber.d(
                    "starCamera CameraState index: $index /type: ${it.type}"
                )
                if (it.type == CameraState.Type.OPEN) {
                    setEvRange()
                }
            }
        }
    }

    abstract fun shoot()
    abstract fun release()
    abstract fun switchFacing()

    fun setEvRange() {
        Timber.d("setEV")
        cameraController?.cameraInfo?.exposureState?.let {
            Timber.d("setEV exposureCompensationRange ${it.exposureCompensationRange}")
            Timber.d("setEV exposureCompensationIndex ${it.exposureCompensationIndex}")
            Timber.d("setEV exposureCompensationStep ${it.exposureCompensationStep}")
            Timber.d("setEV isExposureCompensationSupported ${it.isExposureCompensationSupported}")
            evMin.set(it.exposureCompensationRange.lower)
            evMax.set(it.exposureCompensationRange.upper)
        }
    }

    fun setExposureCompensation(index: Int) {
        cameraController?.cameraControl?.setExposureCompensationIndex(index);
    }

//    open fun focus(x: Float, y: Float) {
//        fixme:already implemented in previewView
//        previewView?.let {
//            Timber.d("focus $x,$y")
//            val factory = it.meteringPointFactory
//            val point = factory.createPoint(x, y)
//            val action = FocusMeteringAction.Builder(point).disableAutoCancel().build()
//            cameraManager?.startFocus(action)
//        }
//    }

//    open fun setZoom(z: Float) {
//        fixme:already implemented in previewView
//        cameraManager?.zoom(z)
//    }

    companion object {
        const val TAG_PHOTO = 1
        const val TAG_VIDEO = 2
        const val TAG_FILTER = 3
        const val PHOTO = "photo"
        const val VIDEO = "video"
        const val FILTER = "filter"
        const val UNKNOWN = "unknown"
    }
}

class PhotoState(private val application: Application, private val cameraManager: CameraManager?) :
    CameraItem() {

    override val type: Int = TAG_PHOTO

    var photoFlashState: ObservableField<Int> = ObservableField(CameraManager.FLASH_AUTO)
    override fun starCamera(lifecycleOwner: LifecycleOwner, lensFacingMode: Int) {
        super.starCamera(lifecycleOwner, lensFacingMode)

        previewView?.also { it ->

            seekBarValue.addOnPropertyChangedCallback(object :
                Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    Timber.d("seekBarValue seekBarValue: $sender / $propertyId / ${seekBarValue.get()}")
                    setExposureCompensation(seekBarValue.get()!!)
                }
            })

            cameraManager?.startCameraWithController(it, lifecycleOwner, lensFacingMode)
            this.lifecycleOwner = lifecycleOwner
            this.lensFacingState.set(lensFacingMode)
            this.cameraController = it.controller?.apply {
                this.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                this@apply.imageCaptureFlashMode =
                    photoFlashState.get() ?: CameraManager.FLASH_AUTO

                startListenCameraState(
                    cameraManager!!,
                    cameraManager.getCameraIndex(lensFacingMode)
                )

                //fixme: for debug
                this@PhotoState.photoTapToFocusState = this.tapToFocusState.also {
                    it.observe(lifecycleOwner) {
                        Timber.d("tapToFocusState: $it")
                    }
                }
            }
        }
    }

    override fun shoot() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Camera/")
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            application.contentResolver,
            MediaUtils.imageUriSd,
            contentValues
        ).build()
        cameraController?.takePicture(outputOptions, ContextCompat.getMainExecutor(application),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    //Todo: remove toast
                    Timber.d("Photo saved: ${outputFileResults.savedUri}")
                    Toast.makeText(
                        application,
                        "Photo saved: ${outputFileResults.savedUri}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    //Todo: remove toast
                    Timber.e("Photo capture failed: ${exception.message}")
                    Toast.makeText(
                        application,
                        "Photo capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    override fun release() {
    }

    fun switchNextFlashMode() {
        val next = photoFlashState.get()?.plus(1)?.rem(3) ?: -1
        switchFlashMode(next)
    }

    fun switchFlashMode(mode: Int) {
        try {
            cameraController?.imageCaptureFlashMode = mode
            photoFlashState.set(mode)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }
    }

    override fun switchFacing() {

        val currentFacing = lensFacingState.get()
        val nextState =
            when (currentFacing) {
                CameraManager.LENS_FACING_BACK -> CameraManager.LENS_FACING_FRONT
                CameraManager.LENS_FACING_FRONT -> CameraManager.LENS_FACING_BACK
                else -> CameraManager.LENS_FACING_UNKNOWN
            }

        this.lifecycleOwner?.let {
            starCamera(it, nextState)
        }
    }
}

class RecordState(
    private val application: Application,
    private val cameraManager: CameraManager?
) :
    CameraItem() {

    override val type: Int = TAG_VIDEO

    private var currentRecording: Recording? = null
    private var audioEnabled = false
    private var recordingState: VideoRecordEvent? = null
    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(application) }

    var torch: ObservableField<Boolean> = ObservableField(false)

    @ExperimentalVideo
    override fun starCamera(lifecycleOwner: LifecycleOwner, lensFacingMode: Int) {
        super.starCamera(lifecycleOwner, lensFacingMode)
        previewView?.also { it ->

            seekBarValue.addOnPropertyChangedCallback(object :
                Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    Timber.d("seekBarValue seekBarValue: $sender / $propertyId / ${seekBarValue.get()}")
                    setExposureCompensation(seekBarValue.get()!!)
                }
            })

            cameraManager?.startCameraWithController(it, lifecycleOwner, lensFacingMode)
            this.lifecycleOwner = lifecycleOwner
            lensFacingState.set(lensFacingMode)

            this.cameraController = it.controller?.apply {
                this.setEnabledUseCases(CameraController.VIDEO_CAPTURE)

                this.torchState.observe(lifecycleOwner) {
                    torch.set(it == 1)
                }

                startListenCameraState(
                    cameraManager!!,
                    cameraManager.getCameraIndex(lensFacingMode)
                )

                //fixme: for debug
                this@RecordState.photoTapToFocusState = this.tapToFocusState.also {
                    it.observe(lifecycleOwner) {
                        Timber.d("tapToFocusState: $it")
                    }
                }
            }
        }
    }

    @ExperimentalVideo
    override fun shoot() {
        Timber.d("videoCaptureClick")
        cameraController?.run {
            if (this.isRecording && currentRecording != null) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }

    @SuppressLint("MissingPermission")
    @ExperimentalVideo
    private fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val name = "${System.currentTimeMillis()}" + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DCIM}/Camera/"
                )
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            application.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        currentRecording = cameraController?.startRecording(
            mediaStoreOutput,
            AudioConfig.create(audioEnabled),
            mainThreadExecutor,
            captureListener
        )

    }

    private fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
    }

    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateUI(event)
//          Todo:
//        if (event is VideoRecordEvent.Finalize) {
        // display the captured video
//            viewModelScope.launch {
//                navController.navigate(
//                    CaptureFragmentDirections.actionCaptureToVideoViewer(
//                        event.outputResults.outputUri
//                    )
//                )
//            }
//        }
    }

    /**
     * UpdateUI according to CameraX VideoRecordEvent type:
     *   - user starts capture.
     *   - this app disables all UI selections.
     *   - this app enables capture run-time UI (pause/resume/stop).
     *   - user controls recording with run-time UI, eventually tap "stop" to end.
     *   - this app informs CameraX recording to stop with recording.stop() (or recording.close()).
     *   - CameraX notify this app that the recording is indeed stopped, with the Finalize event.
     *   - this app starts VideoViewer fragment to view the captured result.
     */
    private fun updateUI(event: VideoRecordEvent) {
//        val state = if (event is VideoRecordEvent.Status) recordingState
//        else event.getNameString()
//        when (event) {
//            is VideoRecordEvent.Status -> {
//                // placeholder: we update the UI with new status after this when() block,
//                // nothing needs to do here.
//            }
//            is VideoRecordEvent.Start -> {
//                showUI(UiState.RECORDING, event.getNameString())
//            }
//            is VideoRecordEvent.Finalize -> {
//                showUI(UiState.FINALIZED, event.getNameString())
//            }
//            is VideoRecordEvent.Pause -> {
//                captureViewBinding.captureButton.setImageResource(R.drawable.ic_resume)
//            }
//            is VideoRecordEvent.Resume -> {
//                captureViewBinding.captureButton.setImageResource(R.drawable.ic_pause)
//            }
//        }
        Timber.d("updateUI: ${event.recordingStats}")
    }

    @ExperimentalVideo
    override fun switchFacing() {

        val currentFacing = lensFacingState.get()
        val nextState =
            when (currentFacing) {
                CameraManager.LENS_FACING_BACK -> CameraManager.LENS_FACING_FRONT
                CameraManager.LENS_FACING_FRONT -> CameraManager.LENS_FACING_BACK
                else -> CameraManager.LENS_FACING_UNKNOWN

            }

        this.lifecycleOwner?.let {
            starCamera(it, nextState)
        }
    }

    fun switchTorch() {
        torch.get()?.let {
            if (it) {
                closeTorch()
            } else {
                openTorch()
            }
        }

    }

    private fun openTorch() {
        cameraManager?.switchTorch(true)
    }

    private fun closeTorch() {
        cameraManager?.switchTorch(false)
    }

    override fun release() {
        currentRecording?.run {
            this.stop()
            currentRecording = null
        }
    }
}

class FilterState(
    private val application: Application,
    private val cameraManager: CameraManager?
) :
    CameraItem() {
    override val type: Int = TAG_FILTER

    var photoFlashState: ObservableField<Int> = ObservableField(CameraManager.FLASH_AUTO)
    val gpuImageFilter = GPUImageGrayscaleFilter()
    val gpuImage = GPUImage(application)
    val yuvUtils = YuvUtils()
    var bitmap: Bitmap? = null
    var imageResult = ObservableField<Bitmap>()

    init {
        gpuImage.setFilter(gpuImageFilter)
    }

    override fun starCamera(lifecycleOwner: LifecycleOwner, lensFacingMode: Int) {
        super.starCamera(lifecycleOwner, lensFacingMode)

        //Todo: using custom LifeCycleOwner to not showing previewView
        previewView?.also { it ->

            seekBarValue.addOnPropertyChangedCallback(object :
                Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    Timber.d("seekBarValue seekBarValue: $sender / $propertyId / ${seekBarValue.get()}")
                    setExposureCompensation(seekBarValue.get()!!)
                }
            })

            cameraManager?.startCameraWithController(it, lifecycleOwner, lensFacingMode)

            this.lifecycleOwner = lifecycleOwner
            this.lensFacingState.set(lensFacingMode)
            this.cameraController = it.controller?.apply {

                this.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                this.imageAnalysisBackpressureStrategy

                //Fixme: change resolution here
//                val outputSize = CameraController.OutputSize(Size(1280, 720))
//                this.imageAnalysisTargetSize = outputSize
//                Timber.d("imageCaptureTargetSize: ${this.imageAnalysisTargetSize} ")

                this.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(this@FilterState.application)) {

                    @ExperimentalGetImage
                    var yuvFrame = yuvUtils.convertToI420(it.image!!)

                    //对图像进行旋转（由于回调的相机数据是横着的因此需要旋转90度）
//                    yuvFrame = yuvUtils.rotate(yuvFrame, 90)
                    //根据图像大小创建Bitmap
                    bitmap = Bitmap.createBitmap(
                        yuvFrame.width,
                        yuvFrame.height,
                        Bitmap.Config.ARGB_8888
                    )
                    //将图像转为Argb格式的并填充到Bitmap上
                    val argbData = yuvUtils.yuv420ToArgb(yuvFrame)
                    // 假设ArgbFrame包含了ARGB图像数据
                    // 将字节数组解码为Bitmap
                    bitmap!!.copyPixelsFromBuffer(ByteBuffer.wrap(argbData.asArray())) // for displaying argb

                    //利用GpuImage给图像添加滤镜
                    if (bitmap != null) {
                        bitmap = gpuImage?.getBitmapWithFilterApplied(bitmap)
                        imageResult.set(bitmap)
                    } else {
                        Timber.e("ImageAnalysisAnalyzer bitmap is null")
                    }
                    it.close()
                }

                Timber.d("this.isImageAnalysisEnabled :${this.isImageAnalysisEnabled}")

                this@apply.imageCaptureFlashMode =
                    photoFlashState.get() ?: CameraManager.FLASH_AUTO

                startListenCameraState(
                    cameraManager!!,
                    cameraManager.getCameraIndex(lensFacingMode)
                )

                //fixme: for debug
                this@FilterState.photoTapToFocusState = this.tapToFocusState.also {
                    it.observe(lifecycleOwner) {
                        Timber.d("tapToFocusState: $it")
                    }
                }
            }
        }
    }

    override fun shoot() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Camera/")
        }

        val uri = this.application.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        try {
            val outputStream = this.application.contentResolver.openOutputStream(uri!!)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream?.close()

            // 图片保存成功，uri 变量包含了保存图片的 URI
            // Todo: remove toast
            Toast.makeText(
                application,
                "Photo saved: ${uri}",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: IOException) {
            e.printStackTrace()
            // 图片保存失败
            //Todo: remove toast
            Timber.e("Photo capture failed: ${e.message}")
            Toast.makeText(
                application,
                "Photo capture failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun release() {
    }

    fun switchNextFlashMode() {
        val next = photoFlashState.get()?.plus(1)?.rem(3) ?: -1
        switchFlashMode(next)
    }

    fun switchFlashMode(mode: Int) {
        try {
            cameraController?.imageCaptureFlashMode = mode
            photoFlashState.set(mode)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }
    }

    override fun switchFacing() {

        val currentFacing = lensFacingState.get()
        val nextState =
            when (currentFacing) {
                CameraManager.LENS_FACING_BACK -> CameraManager.LENS_FACING_FRONT
                CameraManager.LENS_FACING_FRONT -> CameraManager.LENS_FACING_BACK
                else -> CameraManager.LENS_FACING_UNKNOWN
            }

        this.lifecycleOwner?.let {
            starCamera(it, nextState)
        }
    }

    fun switchFilter(id: Long) {
        val filter: GPUImageFilter =
            when (id.toInt()) {
                0 -> GPUImageGrayscaleFilter()
                1 -> GPUImageBulgeDistortionFilter()
                2 -> GPUImageEmbossFilter()
                3 -> GPUImageSharpenFilter()
                4 -> GPUImageColorInvertFilter()
                5 -> GPUImageSmoothToonFilter()
                6 -> GPUImageDissolveBlendFilter()
                7 -> GPUImageBoxBlurFilter()
                else -> GPUImageGrayscaleFilter()
            }
        setFilter(filter)
    }

    private fun setFilter(filter: GPUImageFilter) {
        gpuImage.setFilter(filter)
    }
}

@BindingAdapter("imagetBitmap")
fun bindImageViewBitmap(imageView: ImageView, bitmap: Bitmap?) {
    Timber.d("bindImageViewBitmap imagetBitmap: $bitmap")
    bitmap?.also { imageView.setImageBitmap(it) }
}