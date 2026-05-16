package com.pim.app.daemon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class StatusActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "PIM 数据采集\n状态: 运行中\n\n待上传: --\n上次上传: --"
        tv.setPadding(48, 48, 48, 48)
        setContentView(tv)
    }
}
