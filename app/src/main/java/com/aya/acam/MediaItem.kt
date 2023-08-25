package com.aya.acam

import android.net.Uri

data class MediaItem(
    val id: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Int,
    val dataAdded: String,
    val uri: Uri,
    val type:Int = TYPE_UNDEFINED
) {
    companion object {
        val TYPE_PHOTO = 1
        val TYPE_VIDEO = 2
        val TYPE_UNDEFINED = 0
    }
}



