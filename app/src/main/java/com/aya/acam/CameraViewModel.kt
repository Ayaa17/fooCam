package com.aya.acam

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.aya.acam.item.PhotoState
import com.aya.acam.item.RecordState
import timber.log.Timber
import com.aya.acam.item.CameraItem
import com.aya.acam.item.FilterState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CameraViewModel(private val application: Application) : AndroidViewModel(application) {

    private var cameraManager: CameraManager? = null

    val orientationObservable = ObservableField(0F)

    var photoState: PhotoState? = null
    var recordState: RecordState? = null
    var filterState: FilterState? = null

    val views: MutableList<CameraItem> = mutableListOf()

    fun initCamera() {
        cameraManager = CameraManager(this.getApplication())
        this.getApplication<Application>().externalMediaDirs

        photoState = PhotoState(application, cameraManager).apply {
            views.add(this)
        }
        recordState = RecordState(application, cameraManager).apply {
            views.add(this)
        }
        filterState = FilterState(application, cameraManager).apply {
            views.add(this)
        }
    }

    fun startCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        cameraManager?.startCamera(previewView, lifecycleOwner)
    }

    fun startCamera(position: Int, lifecycleOwner: LifecycleOwner) {
        Timber.d("startCamera position:$position")
        viewModelScope.launch {
            //Fixme: work around now
            delay(100L)
            views.get(position).starCamera(lifecycleOwner)
        }
    }


    fun shot(position: Int) {
        Timber.d("shot position:$position")
        views.get(position).shoot()
    }

    fun releaseCamera() {
        photoState = null
        recordState?.release()
        recordState = null
        views.clear()

        cameraManager?.releaseCamera()
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
    }

}