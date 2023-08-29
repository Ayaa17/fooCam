package com.aya.acam.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import timber.log.Timber

class TouchBoundingBoxView(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val boundingBox = Rect()
    private val paint = Paint()

    init {
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(boundingBox, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        val isSingleTouch = event.pointerCount == 1
        val isUpEvent = event.action == MotionEvent.ACTION_UP
        val notALongPress = (event.eventTime - event.downTime
                < ViewConfiguration.getLongPressTimeout())

        Timber.d("onTouchEvent : ${event.action} / ${isSingleTouch} / ${event.x} / ${event.y}")

        boundingBox.set(
            (event.x - 50).toInt(),  // Left
            (event.y - 50).toInt(),  // Top
            (event.x + 50).toInt(),  // Right
            (event.y + 50).toInt()   // Bottom
        )
        invalidate() // Request redraw
        //Todo: add ui feature here
        return false
    }

}