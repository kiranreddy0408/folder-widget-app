package com.kiran.appfolder

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.text.TextPaint
import android.text.TextUtils

object Render {
    val FONTS = listOf("Default", "Medium", "Serif", "Mono", "Cursive", "Casual", "Condensed bold")
    val SHAPES = listOf("Circle", "Squircle", "Rounded", "Square", "Original")

    fun tf(i: Int): Typeface = when (i) {
        1 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        2 -> Typeface.SERIF
        3 -> Typeface.MONOSPACE
        4 -> Typeface.create("cursive", Typeface.NORMAL)
        5 -> Typeface.create("casual", Typeface.NORMAL)
        6 -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
        else -> Typeface.DEFAULT
    }

    fun dpf(c: Context, v: Float) = v * c.resources.displayMetrics.density

    fun sizeDp(m: AppWidgetManager, id: Int): Pair<Int, Int> {
        val o = m.getAppWidgetOptions(id)
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        return (if (w > 0) w else 300) to (if (h > 0) h else 300)
    }

    fun cols(s: FS, wDp: Int) = if (s.cols > 0) s.cols else ((wDp - 16) / (s.icon + 14)).coerceIn(2, 7)

    private fun round(b: Bitmap, r: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        Canvas(b).drawRoundRect(RectF(0f, 0f, b.width.toFloat(), b.height.toFloat()), r, r, p)
    }

    fun bg(c: Context, s: FS, id: Int, w: Int, h: Int): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        when (s.bgType) {
            0 -> cv.drawColor(s.c1)
            1 -> {
                val a = Math.toRadians(s.angle.toDouble())
                val dx = (Math.cos(a) * w / 2).toFloat()
                val dy = (Math.sin(a) * h / 2).toFloat()
                p.shader = LinearGradient(w / 2f - dx, h / 2f - dy, w / 2f + dx, h / 2f + dy, s.c1, s.c2, Shader.TileMode.CLAMP)
                cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
            }
            else -> {
                val f = Store.imgFile(c, id)
                val im = if (s.hasImg && f.exists()) BitmapFactory.decodeFile(f.path) else null
                if (im != null) {
                    val sc = maxOf(w / im.width.toFloat(), h / im.height.toFloat())
                    val m = Matrix()
                    m.postScale(sc, sc)
                    m.postTranslate((w - im.width * sc) / 2, (h - im.height * sc) / 2)
                    cv.drawBitmap(im, m, p)
                    cv.drawColor(Color.argb(s.dim * 255 / 100, 0, 0, 0))
                } else cv.drawColor(s.c1)
            }
        }
        round(b, dpf(c, s.radius.toFloat()))
        return b
    }

    fun head(c: Context, s: FS, w: Int): Bitmap {
        val h = dpf(c, 36f).toInt()
        val b = Bitmap.createBitmap(w.coerceAtLeast(1), h, Bitmap.Config.ARGB_8888)
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.tcolor; typeface = tf(s.font); textSize = dpf(c, (s.label + 6).toFloat())
            setShadowLayer(3f, 0f, 1f, 0x99000000.toInt()); textAlign = Paint.Align.CENTER
        }
        Canvas(b).drawText(s.title, w / 2f, h / 2f - (pt.ascent() + pt.descent()) / 2, pt)
        return b
    }

    fun tile(c: Context, s: FS, pkg: String, colW: Int): Bitmap? {
        return try {
            val pm = c.packageManager
            val name = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            val d = pm.getApplicationIcon(pkg)
            val ic = minOf(dpf(c, s.icon.toFloat()).toInt(), colW - dpf(c, 4f).toInt()).coerceAtLeast(16)
            val ib = Bitmap.createBitmap(ic, ic, Bitmap.Config.ARGB_8888)
            val icv = Canvas(ib)
            if (d is AdaptiveIconDrawable) {
                val o = (ic * 0.25f).toInt()
                d.background?.apply { setBounds(-o, -o, ic + o, ic + o); draw(icv) }
                d.foreground?.apply { setBounds(-o, -o, ic + o, ic + o); draw(icv) }
            } else { d.setBounds(0, 0, ic, ic); d.draw(icv) }
            if (s.shape < 4) round(ib, floatArrayOf(.5f, .32f, .18f, 0f)[s.shape] * ic)
            val gap = dpf(c, 4f)
            val pt = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = s.tcolor; typeface = tf(s.font); textSize = dpf(c, s.label.toFloat())
                setShadowLayer(3f, 0f, 1f, 0x99000000.toInt()); textAlign = Paint.Align.CENTER
            }
            val fm = pt.fontMetrics
            val lh = if (s.labels) fm.descent - fm.ascent else 0f
            val out = Bitmap.createBitmap(colW, (ic + gap + lh + dpf(c, 8f)).toInt(), Bitmap.Config.ARGB_8888)
            val cv = Canvas(out)
            cv.drawBitmap(ib, (colW - ic) / 2f, 0f, null)
            if (s.labels) {
                val t = TextUtils.ellipsize(name, pt, colW - dpf(c, 6f), TextUtils.TruncateAt.END).toString()
                cv.drawText(t, colW / 2f, ic + gap - fm.ascent, pt)
            }
            out
        } catch (e: Exception) { null }
    }

    fun compose(c: Context, s: FS, id: Int, w: Int, h: Int): Bitmap {
        val out = bg(c, s, id, w, h)
        val cv = Canvas(out)
        val pad = dpf(c, 8f).toInt()
        val n = cols(s, (w / c.resources.displayMetrics.density).toInt())
        var y = pad.toFloat()
        if (s.showTitle) { cv.drawBitmap(head(c, s, w - 2 * pad), pad.toFloat(), y, null); y += dpf(c, 36f) }
        val colW = (w - 2 * pad) / n
        s.pkgs.mapNotNull { tile(c, s, it, colW) }.forEachIndexed { i, t ->
            cv.drawBitmap(t, (pad + (i % n) * colW).toFloat(), y + (i / n) * t.height, null)
        }
        return out
    }
}
