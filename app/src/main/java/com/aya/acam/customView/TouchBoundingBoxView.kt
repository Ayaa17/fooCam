package com.aya.acam.customView

import android.content.Context
import android.graphics.*
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.aya.acam.R
import timber.log.Timber

class TouchBoundingBoxView(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val boundingBox = Rect()
    private val paint = Paint()
    private var shouldClearCanvas = false
    private var shouldUpdateCanvas = false
    private val timer: CountDownTimer

    lateinit var seekBar: SeekBar
    var ev_min = 0
    var ev_max = 0
    var seekBarValue = 0

    init {

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.TouchBoundingBoxView)
            this.ev_min = typedArray.getInt(R.styleable.TouchBoundingBoxView_ev_max, 0)
            this.ev_max = typedArray.getInt(R.styleable.TouchBoundingBoxView_ev_max, 0)
            this.seekBarValue = typedArray.getInt(R.styleable.TouchBoundingBoxView_ev_max, 0)
            typedArray.recycle()
        }

        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f

        timer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                clearCanvas()
            }
        }
        initSeekBar()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (shouldClearCanvas) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            shouldClearCanvas = false
        } else if (shouldUpdateCanvas) {
            canvas.drawRect(boundingBox, paint)
            shouldUpdateCanvas = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        val isSingleTouch = event.pointerCount == 1
        val isUpEvent = event.action == MotionEvent.ACTION_UP
        val notALongPress = (event.eventTime - event.downTime
                < ViewConfiguration.getLongPressTimeout())

        Timber.d("onTouchEvent : ${event.action} / ${isSingleTouch} / ${event.x} / ${event.y}")
        if (isSingleTouch && isUpEvent && notALongPress) {
            boundingBox.set(
                (event.x - 50).toInt(),  // Left
                (event.y - 50).toInt(),  // Top
                (event.x + 50).toInt(),  // Right
                (event.y + 50).toInt()   // Bottom
            )
            seekBar.progress = 0
            timerReset()
            setVisibility(true)
            shouldUpdateCanvas = true
            invalidate() // Request redraw
            //Todo: add ui feature here
        }
        return false
    }

    private fun clearCanvas() {
        setVisibility(false)
        shouldClearCanvas = true
        invalidate() // Request redraw again to restore previous state
    }

    fun timerReset() {
        timer.cancel()
        timer.start()
    }

    fun setEvRange(min: Int?, max: Int?) {
        min?.also {
            this.ev_min = it
        }
        max?.also {
            this.ev_max = it
        }
        Timber.d("setEvRange : ${this.ev_min} / ${this.ev_max}")
        seekBar?.min = ev_min
        seekBar?.max = ev_max
    }

    private fun initSeekBar() {
        // 創建並添加底部的 SeekBar
        seekBar = SeekBar(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams.bottomMargin = 100
        layoutParams.leftMargin = 100
        layoutParams.rightMargin = 100
        layoutParams.gravity = android.view.Gravity.BOTTOM
        seekBar.layoutParams = layoutParams
        seekBar.visibility = INVISIBLE
        seekBar.progress = 0
        seekBar.max = 100
        seekBar.min = -100

        addView(seekBar)
    }

    private fun setSeekBarVisible(visible: Int) {
        seekBar.visibility = visible
    }

    private fun setVisibility(visible: Boolean) {
        Timber.d("setVisibility: $visible")
        when (visible) {
            true -> setSeekBarVisible(VISIBLE)
            false -> setSeekBarVisible(INVISIBLE)
        }
    }
}

@BindingAdapter("ev_min")
fun bindEv_min(touchBoundingBoxView: TouchBoundingBoxView, min: Int) {
    Timber.d("bindEv_min: $min")
    touchBoundingBoxView.setEvRange(min, null)
}

@BindingAdapter("ev_max")
fun bindEv_max(touchBoundingBoxView: TouchBoundingBoxView, max: Int) {
    Timber.d("bindEv_max: $max")
    touchBoundingBoxView.setEvRange(null, max)
}

@BindingAdapter("seekBar_value")
fun bindSeekBar_value(touchBoundingBoxView: TouchBoundingBoxView, value: Int) {
    Timber.d("bindSeekBar_value: $value")
    //Todo:
}

@InverseBindingAdapter(attribute = "seekBar_value")
fun getSeekBar_value(touchBoundingBoxView: TouchBoundingBoxView): Int? {
    Timber.d("getSeekBar_value:")
    return touchBoundingBoxView.seekBar.progress
}

@BindingAdapter("seekBar_valueAttrChanged")
fun setColorListener(
    touchBoundingBoxView: TouchBoundingBoxView,
    inverseBindingListener: InverseBindingListener?
) {
    if (inverseBindingListener != null) {
        // 设置颜色更改监听器
        touchBoundingBoxView.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Timber.d("seekBar onProgressChanged = $progress")
                inverseBindingListener.onChange()
                touchBoundingBoxView.timerReset()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Timber.d("seekBar onStartTrackingTouch")
                touchBoundingBoxView.timerReset()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Timber.d("seekBar onStopTrackingTouch")
                touchBoundingBoxView.timerReset()
            }
        })
    }
}