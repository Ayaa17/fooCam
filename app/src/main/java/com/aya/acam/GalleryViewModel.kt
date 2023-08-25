package com.aya.acam

import android.app.Application
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aya.acam.utils.MediaUtils
import kotlinx.coroutines.launch
import timber.log.Timber
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    val mediaItemListLiveData = MutableLiveData<List<MediaItem>>()
    private var mediaItemList = mutableListOf<MediaItem>()
    private val contentResolver: ContentResolver = application.contentResolver
    private val photoObserver: ContentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // 新的相片被添加到相簿，更新ViewModel
            Timber.d("photoObserver selfChange:$selfChange, uri:$uri")
            viewModelScope.launch {
                updatePhotos()
            }
        }
    }

    private val videoObserver: ContentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // 新的影片被添加到相簿，更新ViewModel
            //Todo: update video
            Timber.d("videoObserver selfChange:$selfChange, uri:$uri")
        }
    }

    init {
        Timber.d("init")
        updatePhotos()
        registerContentObserver()
    }

    fun updatePhotos() {
        mediaItemList =
            MediaUtils.getPhotoData(contentResolver)
        mediaItemListLiveData.value = mediaItemList
    }

    fun updateVideos() {
        mediaItemList =
            MediaUtils.getVideoData(contentResolver)
        mediaItemListLiveData.value = mediaItemList
    }

    private fun addTitle(photoItemList: MutableList<MediaItem>) {
        val pattern = "yyyy年MM月dd日"
        val simpleDateFormat = SimpleDateFormat(pattern)
        var lastDataFormat = ""
        var i = 0
        while (i < photoItemList.size) {
            val currentPhotoItem: MediaItem = photoItemList[i]
            val date: Date = Timestamp(currentPhotoItem.dataAdded.toLong() * 1000)
            val dataFormat: String = simpleDateFormat.format(date)
            if (dataFormat != lastDataFormat) {
                lastDataFormat = dataFormat
                val titleItem =
                    currentPhotoItem.copy(dataAdded = dataFormat, type = MediaItem.TYPE_UNDEFINED)
                photoItemList.add(i, titleItem)
                i += 1
            }
            i++
        }
    }

    private fun registerContentObserver() {
        Timber.d("registerContentObserver")
        contentResolver.registerContentObserver(MediaUtils.imageUriSd, true, photoObserver);
        contentResolver.registerContentObserver(MediaUtils.videoUriSd, true, videoObserver);
    }

    private fun unregisterContentObserver() {
        Timber.d("unregisterContentObserver")
        contentResolver.unregisterContentObserver(photoObserver)
        contentResolver.unregisterContentObserver(videoObserver)
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
        unregisterContentObserver()
    }

}