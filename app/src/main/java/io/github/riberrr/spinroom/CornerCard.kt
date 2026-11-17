package io.github.riberrr.spinroom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import coil3.load
import io.github.riberrr.spinroom.core.audio.service.PlaybackActivity
import io.github.riberrr.spinroom.core.design.SpinroomColors
import io.github.riberrr.spinroom.core.design.SpinroomThemeId

/**
 * The card in the top right corner shown ~3 s after a global key (SUI9, mockup 10C).
 * Drawn as an accessibility overlay: no extra permission, never takes focus or touches. If the system refuses the
 * overlay, a plain notification carries the same text.
 */
class CornerCard(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var view: View? = null
    private val hide = Runnable { dismiss() }
    private val dp = context.resources.displayMetrics.density

    private fun px(v: Int) = (v * dp).toInt()

    fun show(keyLabel: String, caption: String, title: String, imageUrl: String?, now: () -> String) {
        handler.post {
            dismiss()
            val card = build(keyLabel, caption, title, imageUrl, now)
            val lp = WindowManager.LayoutParams(
                px(430), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = px(40); y = px(32)
            }
            try {
                wm.addView(card.root, lp)
                view = card.root
                handler.postDelayed(hide, DURATION_MS)
                handler.postDelayed(card.refreshNow, REFRESH_MS)
            } catch (e: Exception) {
                notify(keyLabel, caption, title)
            }
        }
    }

    fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }

    private class Card(val root: View, val refreshNow: Runnable)

    private fun build(keyLabel: String, caption: String, title: String, imageUrl: String?, now: () -> String): Card {
        val bg = SpinroomThemeId.Grafit
        val primary = SpinroomColors.TextPrimary
        val secondary = SpinroomColors.TextSecondary

        val cover = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(bg.surfaceHigh, 8)
            clipToOutline = true
            if (imageUrl != null) load(imageUrl) else setImageResource(R.drawable.banner)
        }
        fun text(t: String, size: Float, color: Color, bold: Boolean = false) = TextView(context).apply {
            text = t; textSize = size; setTextColor(color.toArgb()); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
        val chip = text(keyLabel, 11f, primary, bold = true).apply {
            background = rounded(bg.surfaceHigh, 5); setPadding(px(8), px(2), px(8), px(2))
        }
        val captionView = text(caption, 12f, secondary).apply { setPadding(px(8), 0, 0, 0) }
        val head = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(chip); addView(captionView)
        }
        val titleView = text(title, 18f, primary, bold = true).apply { setPadding(0, px(4), 0, px(2)) }
        val nowView = text(now(), 13f, secondary).apply { setTextColor(secondary.toArgb()) }
        val dot = View(context).apply { background = rounded(SpinroomColors.Success, 4) }
        val nowRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(dot, LinearLayout.LayoutParams(px(8), px(8)).apply { marginEnd = px(8) })
            addView(nowView)
        }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(head); addView(titleView); addView(nowRow)
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(px(16), px(16), px(20), px(16))
            addView(cover, LinearLayout.LayoutParams(px(74), px(74)).apply { marginEnd = px(16) })
            addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        val root = FrameLayout(context).apply {
            background = rounded(bg.surface.copy(alpha = 0.94f), 16, SpinroomColors.GlassBorder)
            addView(row)
        }
        // The track usually arrives a moment after the switch: refresh the "now playing" line once.
        val refresh = Runnable { nowView.text = now() }
        return Card(root, refresh)
    }

    private fun rounded(fill: Color, radiusDp: Int, stroke: Color? = null) =
        GradientDrawable().apply {
            setColor(fill.toArgb()); cornerRadius = radiusDp * dp
            stroke?.let { setStroke(px(1), it.toArgb()) }
        }

    private fun notify(keyLabel: String, caption: String, title: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.global_card_channel), NotificationManager.IMPORTANCE_LOW))
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("$keyLabel · $title")
            .setContentText(caption)
            .setTimeoutAfter(DURATION_MS)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(NOTIFICATION_ID, n) }
    }

    companion object {
        const val DURATION_MS = 3_000L
        private const val REFRESH_MS = 1_500L
        private const val CHANNEL = "spinroom_global_keys"
        private const val NOTIFICATION_ID = 4090

        /** "Now playing" line from the playback service, or empty when unknown. */
        fun nowText(): String = PlaybackActivity.now.value?.let { n ->
            listOfNotNull(n.artist?.takeIf { it.isNotBlank() }, n.title?.takeIf { it.isNotBlank() }).joinToString(" — ")
        }.orEmpty()
    }
}
