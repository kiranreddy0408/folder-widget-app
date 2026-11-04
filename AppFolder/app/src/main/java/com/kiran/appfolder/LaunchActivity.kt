package com.kiran.appfolder

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

class LaunchActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val i = intent.getStringExtra("pkg")?.let { packageManager.getLaunchIntentForPackage(it) }
        if (i != null) startActivity(i) else Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
        finish()
    }
}
