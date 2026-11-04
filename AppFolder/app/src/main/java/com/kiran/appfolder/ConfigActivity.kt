package com.kiran.appfolder

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.*

class ConfigActivity : Activity() {
    private var id = AppWidgetManager.INVALID_APPWIDGET_ID
    private var s = FS()
    private lateinit var root: LinearLayout
    private lateinit var preview: ImageView
    private lateinit var sel: LinearLayout
    private val h = Handler(Looper.getMainLooper())
    private val PAL = listOf(0xFF4F46E5, 0xFFEC4899, 0xFF0EA5E9, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444,
        0xFF111827, 0xFFFFFFFF, 0xFF8B5CF6, 0xFF14B8A6, 0xFF1E293B, 0xFFFDE68A).map { it.toInt() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setContentView(TextView(this).apply {
                text = "App Folder\n\nGo to your home screen, long-press an empty area, tap Widgets, find \"App Folder\" and drag it onto the screen. Then choose your apps and style. Resize it any time by long-pressing the widget. Tap its title or background to edit."
                textSize = 18f; setPadding(dp(24), dp(80), dp(24), dp(24))
            })
            return
        }
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        s = Store.load(this, id)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(40), dp(16), dp(32)) }
        setContentView(ScrollView(this).apply { addView(root) })
        build()
    }

    override fun onPause() {
        super.onPause()
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Store.save(this, id, s)
            FolderWidget.update(this, AppWidgetManager.getInstance(this), id)
        }
    }

    private fun refresh() {
        h.removeCallbacksAndMessages(null)
        h.postDelayed({ preview.setImageBitmap(Render.compose(this, s, id, dp(320), dp(300))) }, 120)
    }

    private fun lbl(t: String): TextView = TextView(this).apply {
        text = t; textSize = 13f; setTypeface(null, Typeface.BOLD); setPadding(0, dp(14), 0, dp(2))
        root.addView(this)
    }

    private fun slider(t: String, lo: Int, hi: Int, v: Int, f: (Int) -> Unit) {
        val tv = lbl("$t: $v")
        root.addView(SeekBar(this).apply {
            max = hi - lo; progress = v - lo
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) { val x = p + lo; tv.text = "$t: $x"; f(x); refresh() }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })
    }

    private fun spin(t: String, items: List<String>, cur: Int, f: (Int) -> Unit) {
        lbl(t)
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@ConfigActivity, android.R.layout.simple_spinner_dropdown_item, items)
            setSelection(cur)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                var first = true
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, i: Long) { if (first) { first = false; return }; f(pos); refresh() }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        })
    }

    private fun swatches(t: String, f: (Int) -> Unit) {
        lbl(t)
        val row = LinearLayout(this)
        PAL.forEach { col ->
            row.addView(View(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(col); setStroke(dp(1), 0x55000000) }
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }
                setOnClickListener { f(col); refresh() }
            })
        }
        root.addView(HorizontalScrollView(this).apply { addView(row) })
    }

    private fun build() {
        root.removeAllViews()
        preview = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER }
        root.addView(preview, LinearLayout.LayoutParams(-1, dp(300)))
        root.addView(Button(this).apply { text = "Add / remove apps"; setOnClickListener { pickApps() } })
        lbl("Selected apps (long-press a row and drag to reorder)")
        sel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(sel)
        buildSel()

        lbl("Title")
        root.addView(EditText(this).apply {
            setText(s.title); setSingleLine()
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(e: Editable?) { s = s.copy(title = e.toString()); refresh() }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })
        })
        root.addView(Switch(this).apply { text = "Show title"; isChecked = s.showTitle; setOnCheckedChangeListener { _, v -> s = s.copy(showTitle = v); refresh() } })
        root.addView(Switch(this).apply { text = "Show app names"; isChecked = s.labels; setOnCheckedChangeListener { _, v -> s = s.copy(labels = v); refresh() } })

        slider("Columns (0 = auto by widget width)", 0, 7, s.cols) { s = s.copy(cols = it) }
        slider("Icon size", 32, 84, s.icon) { s = s.copy(icon = it) }
        slider("Corner radius", 0, 60, s.radius) { s = s.copy(radius = it) }
        spin("Icon shape", Render.SHAPES, s.shape) { s = s.copy(shape = it) }

        spin("Background", listOf("Solid", "Gradient", "Image"), s.bgType) { s = s.copy(bgType = it) }
        swatches("Color 1") { s = s.copy(c1 = it) }
        swatches("Color 2 (gradient)") { s = s.copy(c2 = it) }
        slider("Gradient angle", 0, 360, s.angle) { s = s.copy(angle = it) }
        root.addView(Button(this).apply { text = "Choose background image"; setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), 7) } })
        slider("Image dim %", 0, 80, s.dim) { s = s.copy(dim = it) }

        spin("Font", Render.FONTS, s.font) { s = s.copy(font = it) }
        slider("Text size", 9, 20, s.label) { s = s.copy(label = it) }
        swatches("Text color") { s = s.copy(tcolor = it) }
        root.addView(Button(this).apply { text = "Done"; setOnClickListener { finish() } })
        refresh()
    }

    private fun buildSel() {
        sel.removeAllViews()
        if (s.pkgs.isEmpty()) sel.addView(TextView(this).apply { text = "No apps yet. Tap “Add / remove apps”." })
        s.pkgs.forEachIndexed { i, p ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(4), dp(4), dp(4)); setBackgroundColor(0x11000000)
            }
            val iv = ImageView(this)
            try { iv.setImageDrawable(packageManager.getApplicationIcon(p)) } catch (e: Exception) {}
            row.addView(iv, LinearLayout.LayoutParams(dp(36), dp(36)))
            val name: CharSequence = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(p, 0)) } catch (e: Exception) { p }
            row.addView(TextView(this).apply { text = name; setPadding(dp(12), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply {
                text = "✕"; textSize = 18f; setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnClickListener { s = s.copy(pkgs = s.pkgs - p); buildSel(); refresh() }
            })
            row.setOnLongClickListener { v ->
                v.startDragAndDrop(ClipData.newPlainText("i", i.toString()), View.DragShadowBuilder(v), null, 0); true
            }
            row.setOnDragListener { _, e ->
                if (e.action == DragEvent.ACTION_DROP) {
                    val from = e.clipData.getItemAt(0).text.toString().toInt()
                    h.post {
                        val l = s.pkgs.toMutableList()
                        val m = l.removeAt(from)
                        l.add(i, m)
                        s = s.copy(pkgs = l); buildSel(); refresh()
                    }
                }
                true
            }
            sel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(2), 0, dp(2)) })
        }
    }

    private fun pickApps() {
        val pm = packageManager
        val li = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }.sortedBy { it.second.lowercase() }
        val chk = BooleanArray(li.size) { li[it].first in s.pkgs }
        AlertDialog.Builder(this).setTitle("Choose apps")
            .setMultiChoiceItems(li.map { it.second }.toTypedArray(), chk) { _, i, v -> chk[i] = v }
            .setPositiveButton("OK") { _, _ ->
                val keep = s.pkgs.filter { p -> val ix = li.indexOfFirst { it.first == p }; ix < 0 || chk[ix] }
                val add = li.indices.filter { chk[it] && li[it].first !in keep }.map { li[it].first }
                s = s.copy(pkgs = keep + add); buildSel(); refresh()
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onActivityResult(rq: Int, rs: Int, d: Intent?) {
        super.onActivityResult(rq, rs, d)
        val u = d?.data
        if (rq == 7 && rs == RESULT_OK && u != null) {
            try {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
                var ss = 1
                while (maxOf(o.outWidth, o.outHeight) / ss > 1400) ss *= 2
                val bm = contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = ss }) }
                if (bm != null) {
                    Store.imgFile(this, id).outputStream().use { bm.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                    s = s.copy(bgType = 2, hasImg = true)
                    build()
                }
            } catch (e: Exception) { Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show() }
        }
    }
}
