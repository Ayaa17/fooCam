package com.aya.acam.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.aya.acam.MediaItem
import timber.log.Timber

object MediaUtils {

    val videoUriSd: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val videoUriHd: Uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI //外部存取空間
    val imageUriSd: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val imageUriHd: Uri = MediaStore.Images.Media.INTERNAL_CONTENT_URI //外部存取空間


    fun getPhotoData(context: Context): MutableList<MediaItem> {
        val resolver: ContentResolver = context.getContentResolver()
        return getPhotoData(resolver)
    }

    @SuppressLint("Range")
    fun getPhotoData(resolver: ContentResolver): MutableList<MediaItem> {
        val photoItemList = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.RELATIVE_PATH

        )
        val cursor: Cursor? = resolver.query(
            imageUriSd, projection,
            null, null, MediaStore.Images.Media.DATE_ADDED + " DESC"
        )
        cursor?.let {
            val idColumn: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {

                val id: Long = it.getLong(idColumn)
                val name: String =
                    it.getString(it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
                val imgPath: String =
                    it.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))
                val size: Int = it.getInt(it.getColumnIndex(MediaStore.Images.Media.SIZE))
                val dateAdded: String =
                    it.getString(it.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
                val image_uri: Uri = Uri.parse(imageUriSd.toString() + "/" + id.toString())
                val tempPhotoItem = MediaItem(
                    id, name, imgPath, size, dateAdded, image_uri,
                    MediaItem.TYPE_PHOTO
                )
                Timber.d("Media.RELATIVE_PATH: ${it.getString(it.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH))}")
                photoItemList.add(tempPhotoItem)
            }
            it.close()
        }
        return photoItemList
    }

    fun getVideoData(context: Context): MutableList<MediaItem> {
        val resolver: ContentResolver = context.getContentResolver()
        return getVideoData(resolver)
    }

    @SuppressLint("Range")
    fun getVideoData(resolver: ContentResolver): MutableList<MediaItem> {

        val videoItemList = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
        )
        val cursor: Cursor? = resolver.query(
            videoUriSd, projection,
            null, null, MediaStore.Video.Media.DATE_ADDED + " DESC"
        )
        cursor?.let {
            val idColumn: Int = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            while (it.moveToNext()) {

                val id: Long = it.getLong(idColumn)
                val name: String =
                    it.getString(it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME))
                val imgPath: String =
                    it.getString(it.getColumnIndex(MediaStore.Video.Media.DATA))
                val size: Int = it.getInt(it.getColumnIndex(MediaStore.Video.Media.SIZE))
                val dateAdded: String =
                    it.getString(it.getColumnIndex(MediaStore.Video.Media.DATE_ADDED))
                val image_uri: Uri = Uri.parse(videoUriSd.toString() + "/" + id.toString())
                val tempPhotoItem = MediaItem(
                    id, name, imgPath, size, dateAdded, image_uri,
                    MediaItem.TYPE_VIDEO
                )
                videoItemList.add(tempPhotoItem)
            }
            it.close()
        }
        return videoItemList
    }

    fun getBitmap(context: Context, mediaItem: MediaItem, _size: Int = -1): Bitmap? {
        if (_size == 0) {
            Timber.e("getBitmap size == 0")
            return null
        }
        var thumbnail: Bitmap? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            thumbnail = context.contentResolver.loadThumbnail(
                mediaItem.uri,
                Size(_size, _size),
                null
            )
            thumbnail = rigidBitmap(thumbnail, _size)

        } else {
            thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                context.contentResolver,
                mediaItem.id,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null
            )
            thumbnail = rigidBitmap(thumbnail, _size)
        }
        return thumbnail
    }

    private fun rigidBitmap(bitmap: Bitmap, newSize: Int): Bitmap {
        if (newSize == -1) {
            return bitmap
        }
        val width = bitmap.width
        val height = bitmap.height
        val cropWidth = if (width >= height) height else width
        val newScale = newSize.toFloat() / cropWidth.toFloat() / 3
        val matrix = Matrix()
        matrix.postScale(newScale, newScale)
        return Bitmap.createBitmap(
            bitmap, (bitmap.width - cropWidth) / 2,
            (bitmap.height - cropWidth) / 2, cropWidth, cropWidth, matrix, true
        )
    }
}