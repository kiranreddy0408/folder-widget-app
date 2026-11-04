package com.kiran.appfolder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class FolderWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) { ids.forEach { update(c, m, it) } }
    override fun onAppWidgetOptionsChanged(c: Context, m: AppWidgetManager, id: Int, o: Bundle) { update(c, m, id) }
    override fun onDeleted(c: Context, ids: IntArray) { ids.forEach { Store.delete(c, it) } }

    companion object {
        fun update(c: Context, m: AppWidgetManager, id: Int) {
            val s = Store.load(c, id)
            val (wd, hd) = Render.sizeDp(m, id)
            val d = c.resources.displayMetrics.density
            val wp = (wd * d).toInt().coerceIn(100, 1200)
            val hp = (hd * d).toInt().coerceIn(100, 1200)
            val pad = (8 * d).toInt()
            val rv = RemoteViews(c.packageName, R.layout.widget_folder)
            rv.setImageViewBitmap(R.id.bg, Render.bg(c, s, id, wp, hp))
            if (s.showTitle) {
                rv.setViewVisibility(R.id.head, View.VISIBLE)
                rv.setImageViewBitmap(R.id.head, Render.head(c, s, wp - 2 * pad))
            } else rv.setViewVisibility(R.id.head, View.GONE)
            val si = Intent(c, GridService::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            si.data = Uri.parse(si.toUri(Intent.URI_INTENT_SCHEME))
            rv.setRemoteAdapter(R.id.grid, si)
            rv.setInt(R.id.grid, "setNumColumns", Render.cols(s, wd))
            rv.setPendingIntentTemplate(R.id.grid, PendingIntent.getActivity(c, id * 2 + 1,
                Intent(c, LaunchActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))
            val cfg = PendingIntent.getActivity(c, id * 2,
                Intent(c, ConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.head, cfg)
            rv.setOnClickPendingIntent(R.id.bg, cfg)
            m.updateAppWidget(id, rv)
            m.notifyAppWidgetViewDataChanged(id, R.id.grid)
        }
    }
}

class GridService : RemoteViewsService() {
    override fun onGetViewFactory(i: Intent): RemoteViewsFactory =
        Factory(applicationContext, i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0))

    class Factory(val c: Context, val id: Int) : RemoteViewsFactory {
        private var items: List<Pair<String, android.graphics.Bitmap>> = emptyList()
        override fun onCreate() {}
        override fun onDataSetChanged() {
            val s = Store.load(c, id)
            val (wd, _) = Render.sizeDp(AppWidgetManager.getInstance(c), id)
            val d = c.resources.displayMetrics.density
            val colW = ((wd * d).toInt() - (16 * d).toInt()) / Render.cols(s, wd)
            items = s.pkgs.mapNotNull { p -> Render.tile(c, s, p, colW)?.let { p to it } }
        }
        override fun onDestroy() { items = emptyList() }
        override fun getCount() = items.size
        override fun getViewAt(i: Int): RemoteViews {
            val rv = RemoteViews(c.packageName, R.layout.item_app)
            rv.setImageViewBitmap(R.id.icon, items[i].second)
            rv.setOnClickFillInIntent(R.id.icon, Intent().putExtra("pkg", items[i].first))
            return rv
        }
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(i: Int) = i.toLong()
        override fun hasStableIds() = true
    }
}
