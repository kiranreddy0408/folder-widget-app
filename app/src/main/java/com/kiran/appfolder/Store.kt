package com.kiran.appfolder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class FS(
    val pkgs: List<String> = emptyList(), val title: String = "My Folder", val showTitle: Boolean = true,
    val cols: Int = 0, val icon: Int = 52, val radius: Int = 28, val bgType: Int = 1,
    val c1: Int = 0xFF4F46E5.toInt(), val c2: Int = 0xFFEC4899.toInt(), val angle: Int = 135, val dim: Int = 25,
    val font: Int = 1, val label: Int = 12, val tcolor: Int = 0xFFFFFFFF.toInt(), val labels: Boolean = true,
    val shape: Int = 1, val hasImg: Boolean = false
)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("folders", Context.MODE_PRIVATE)
    fun imgFile(c: Context, id: Int) = File(c.filesDir, "bg$id.jpg")

    fun load(c: Context, id: Int): FS {
        val d = FS()
        val j = try { JSONObject(sp(c).getString("w$id", "{}") ?: "{}") } catch (e: Exception) { JSONObject() }
        val a = j.optJSONArray("pkgs")
        val l = ArrayList<String>()
        if (a != null) for (i in 0 until a.length()) l.add(a.getString(i))
        return FS(l, j.optString("title", d.title), j.optBoolean("showTitle", d.showTitle), j.optInt("cols", d.cols),
            j.optInt("icon", d.icon), j.optInt("radius", d.radius), j.optInt("bgType", d.bgType),
            j.optInt("c1", d.c1), j.optInt("c2", d.c2), j.optInt("angle", d.angle), j.optInt("dim", d.dim),
            j.optInt("font", d.font), j.optInt("label", d.label), j.optInt("tcolor", d.tcolor),
            j.optBoolean("labels", d.labels), j.optInt("shape", d.shape), j.optBoolean("hasImg", d.hasImg))
    }

    fun save(c: Context, id: Int, s: FS) {
        val j = JSONObject().put("pkgs", JSONArray(s.pkgs)).put("title", s.title).put("showTitle", s.showTitle)
            .put("cols", s.cols).put("icon", s.icon).put("radius", s.radius).put("bgType", s.bgType)
            .put("c1", s.c1).put("c2", s.c2).put("angle", s.angle).put("dim", s.dim).put("font", s.font)
            .put("label", s.label).put("tcolor", s.tcolor).put("labels", s.labels).put("shape", s.shape)
            .put("hasImg", s.hasImg)
        sp(c).edit().putString("w$id", j.toString()).apply()
    }

    fun delete(c: Context, id: Int) { sp(c).edit().remove("w$id").apply(); imgFile(c, id).delete() }
}
