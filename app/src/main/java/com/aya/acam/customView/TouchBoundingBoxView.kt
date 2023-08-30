package com.aya.acam.customView

import android.content.Context
import android.graphics.*
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import timber.log.Timber

class TouchBoundingBoxView(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val boundingBox = Rect()
    private val paint = Paint()
    private var shouldClearCanvas = false
    private val timer: CountDownTimer

    init {
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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (shouldClearCanvas) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            shouldClearCanvas = false
        } else {
            canvas.drawRect(boundingBox, paint)
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
            timerReset()
            invalidate() // Request redraw
            //Todo: add ui feature here
        }
        return false
    }

    private fun clearCanvas() {
        shouldClearCanvas = true
        invalidate() // Request redraw again to restore previous state
    }

    private fun timerReset() {
        timer.cancel()
        timer.start()
    }

}