package com.aya.acam

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException


class GalleryAdapter(
    private var context: Context,
    private val gridLayoutManager: GridLayoutManager
) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {
    private var photoItemList: List<MediaItem> = ArrayList<MediaItem>()
    private val resolver: ContentResolver = context.contentResolver

    fun setData(newData: List<MediaItem>) {
        photoItemList = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val viewHolder: ViewHolder
        return when (viewType) {
            TITLE_TYPE -> {
                viewHolder = ViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.cell_title, parent, false)
                )
                viewHolder
            }
            else -> {
                viewHolder = ViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.cell_item, parent, false)
                )
                viewHolder.itemView.setOnClickListener {
                    val position = viewHolder.adapterPosition
                    val currentPhotoItem: MediaItem = photoItemList[position]
                    //Todo: click event
                }
                viewHolder
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        when (viewType) {
            TITLE_TYPE -> {
                val textView = holder.itemView.findViewById<TextView>(R.id.textView_cell)
                textView.text = photoItemList[position].dataAdded
            }
            ITEM_TYPE -> {
                val currentPhotoItem: MediaItem = photoItemList[position]
                var thumbnail: Bitmap
                val gridlayoutWidth = gridLayoutManager.width
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        thumbnail = resolver.loadThumbnail(
                            currentPhotoItem.uri,
                            Size(gridlayoutWidth, gridlayoutWidth),
                            null
                        )
                        thumbnail = rigidBitmap(thumbnail, gridlayoutWidth)
                        holder.mImageView.setImageBitmap(thumbnail)

                    } else {
                        thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                            resolver,
                            currentPhotoItem.id,
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            null
                        )
                        thumbnail = rigidBitmap(thumbnail, gridlayoutWidth)
                        holder.mImageView.setImageBitmap(thumbnail)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        when (photoItemList[position].type) {
            MediaItem.TYPE_UNDEFINED -> return TITLE_TYPE
            else -> return ITEM_TYPE
        }
        return -1
    }

    override fun getItemCount(): Int {
        return photoItemList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mImageView: ImageView by lazy { itemView.findViewById(R.id.imageView_cell) }

    }

    private fun rigidBitmap(bitmap: Bitmap, newSize: Int): Bitmap {
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

    companion object {
        const val TITLE_TYPE = 0
        const val ITEM_TYPE = 1
    }
}