package com.example.medora.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.medora.MainActivity
import java.io.File

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = try {
            File(filesDir, CrashReporter.FILE_NAME).readText()
        } catch (e: Exception) {
            "تعذّر قراءة التقرير: ${e.message}"
        }
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(pad, pad * 2, pad, pad)

        val title = TextView(this)
        title.text = "⚠️ حدث خطأ في التطبيق"
        title.textSize = 20f
        title.setTypeface(title.typeface, Typeface.BOLD)

        val body = TextView(this)
        body.text = report
        body.textSize = 11f
        body.typeface = Typeface.MONOSPACE
        body.textDirection = View.TEXT_DIRECTION_LTR
        body.setTextIsSelectable(true)

        val scroll = ScrollView(this)
        scroll.addView(body)

        val copy = Button(this)
        copy.text = "نسخ التقرير"
        copy.setOnClickListener {
            val cm = getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(ClipData.newPlainText("medora-crash", report))
            Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show()
        }

        val share = Button(this)
        share.text = "مشاركة"
        share.setOnClickListener {
            val send = Intent(Intent.ACTION_SEND)
            send.type = "text/plain"
            send.putExtra(Intent.EXTRA_TEXT, report)
            startActivity(Intent.createChooser(send, null))
        }

        val restart = Button(this)
        restart.text = "فتح التطبيق من جديد"
        restart.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
            finish()
        }

        root.addView(title)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(copy)
        root.addView(share)
        root.addView(restart)
        setContentView(root)
    }
}
