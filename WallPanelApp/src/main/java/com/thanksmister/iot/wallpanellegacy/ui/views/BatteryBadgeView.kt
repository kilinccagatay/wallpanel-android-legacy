package com.thanksmister.iot.wallpanellegacy.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/** Compact battery indicator drawn above the dashboard without changing its layout. */
class BatteryBadgeView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val body = RectF()
    private val inner = RectF()
    private val bolt = Path()

    private var percentage = 0
    private var charging = false

    init {
        bodyPaint.color = Color.argb(210, 46, 52, 64)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = density
        borderPaint.color = Color.argb(225, 236, 239, 244)

        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = 14f * density

        boltPaint.color = Color.WHITE
        boltPaint.style = Paint.Style.FILL
        isClickable = false
        contentDescription = "Battery 0"
    }

    fun setBatteryState(level: Int, isCharging: Boolean) {
        percentage = Math.max(0, Math.min(100, level))
        charging = isCharging
        contentDescription = if (charging) {
            "Battery $percentage, charging"
        } else {
            "Battery $percentage"
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val capWidth = 3f * density
        val capGap = 1f * density
        val bodyRight = width - capWidth - capGap
        val verticalInset = 2f * density
        val radius = 6f * density
        body.set(0f, verticalInset, bodyRight, height - verticalInset)

        canvas.drawRoundRect(body, radius, radius, bodyPaint)

        val fillInset = 2f * density
        val usableWidth = body.width() - (fillInset * 2f)
        val fillRight = body.left + fillInset + usableWidth * percentage / 100f
        inner.set(body.left + fillInset, body.top + fillInset,
                fillRight, body.bottom - fillInset)
        fillPaint.color = when {
            percentage <= 15 -> Color.rgb(191, 97, 106)
            percentage <= 35 -> Color.rgb(235, 203, 139)
            else -> Color.rgb(76, 190, 104)
        }
        if (inner.width() > 0f) {
            canvas.drawRoundRect(inner, radius - fillInset, radius - fillInset, fillPaint)
        }

        canvas.drawRoundRect(body, radius, radius, borderPaint)
        val capLeft = bodyRight + capGap
        val capTop = height * 0.34f
        val capBottom = height * 0.66f
        canvas.drawRoundRect(RectF(capLeft, capTop, width.toFloat(), capBottom),
                density, density, borderPaint)

        val textCenterX = if (charging) body.centerX() - 4f * density else body.centerX()
        val textBaseline = body.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(percentage.toString(), textCenterX, textBaseline, textPaint)

        if (charging) {
            val cx = body.right - 8f * density
            val cy = body.centerY()
            bolt.reset()
            bolt.moveTo(cx + 1f * density, cy - 8f * density)
            bolt.lineTo(cx - 5f * density, cy + 1f * density)
            bolt.lineTo(cx - 1f * density, cy + 1f * density)
            bolt.lineTo(cx - 3f * density, cy + 8f * density)
            bolt.lineTo(cx + 5f * density, cy - 2f * density)
            bolt.lineTo(cx + 1f * density, cy - 2f * density)
            bolt.close()
            canvas.drawPath(bolt, boltPaint)
        }
    }
}
