package com.aya.acam

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import com.aya.acam.databinding.CameraPhotoItemBinding
import com.aya.acam.databinding.CameraVideoItemBinding
import com.aya.acam.item.CameraItem
import timber.log.Timber

class CameraViewAdapter(private val viewModel: CameraViewModel) :
    RecyclerView.Adapter<CameraViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.d("onCreateViewHolder")
        when (viewType) {
            CameraItem.TAG_PHOTO -> {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = CameraPhotoItemBinding.inflate(layoutInflater, parent, false)
                binding.photoState = viewModel.photoState
//                binding.executePendingBindings() //fixme: need this?
                return ViewHolder(binding.root)
            }

            CameraItem.TAG_VIDEO ->{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = CameraVideoItemBinding.inflate(layoutInflater, parent, false)
                binding.recordState = viewModel.recordState
//                binding.executePendingBindings() //fixme: need this?
                return ViewHolder(binding.root)
            }

        }
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.camera_photo_item, parent, false)
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Timber.d("onBindViewHolder")

        // 在此处设置视图的内容，可以根据 position 来设置不同的内容
        holder.bindView()
        viewModel.views.get(position).previewView = holder.previewView
    }

    override fun getItemCount(): Int {
        return viewModel.views.size
    }

    override fun getItemViewType(position: Int): Int {
        // 返回对应位置的视图的布局资源 ID
        Timber.d("getItemViewType: position= $position / tag= ${viewModel.views[position]}")
        return viewModel.views[position].type
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var previewView: PreviewView? = null
        fun bindView() {
            // 在此处设置视图的内容
            previewView = view.findViewById(R.id.preview_view)

        }
    }
}