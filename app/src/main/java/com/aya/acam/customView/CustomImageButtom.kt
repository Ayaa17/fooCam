package com.aya.acam.customView

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.databinding.BindingAdapter
import com.aya.acam.CameraManager
import com.aya.acam.R
import timber.log.Timber


class CustomImageButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = androidx.appcompat.R.attr.imageButtonStyle
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    var type = 0 //default

    init {

        // 在这里设置初始样式、资源等
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.CustomImageButton)
            setFlashType(typedArray.getInt(R.styleable.CustomImageButton_flash_type, 1))
            typedArray.recycle()
        }
    }

    fun setFlashType(type: Int) {
        this.type = type;
        refreshDrawableState();
    }



    override fun onCreateDrawableState(extraSpace: Int): IntArray {

        // 添加自定义状态值到 drawableState
        if (type == CameraManager.FLASH_AUTO) {
            val drawableState = super.onCreateDrawableState(extraSpace + 1)
            mergeDrawableStates(drawableState, intArrayOf(R.attr.state_flash_auto))
            return drawableState
        } else if (type == CameraManager.FLASH_ON) {
            val drawableState = super.onCreateDrawableState(extraSpace + 1)
            mergeDrawableStates(drawableState, intArrayOf(R.attr.state_flash_on))
            return drawableState
        } else if (type == CameraManager.FLASH_OFF) {
            val drawableState = super.onCreateDrawableState(extraSpace + 1)
            mergeDrawableStates(drawableState, intArrayOf(R.attr.state_flash_off))
            return drawableState
        }

        return super.onCreateDrawableState(extraSpace + 1)
    }

}

@BindingAdapter("flash_type")
fun bindFlashType(customImageButton: CustomImageButton, flash_type: Int) {
    Timber.d("bindFlashType: $flash_type")
    customImageButton.setFlashType(flash_type)
}






