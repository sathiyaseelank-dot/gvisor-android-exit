package com.example.vpnapp

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                startActivityForResult(intent, 0)
            } else {
                onActivityResult(0, RESULT_OK, null)
            }
        }
        val stopButton = findViewById<Button>(R.id.stopButton)
        stopButton.setOnClickListener {
            val intent = Intent(this, MyVpnService::class.java)
            intent.action = MyVpnService.ACTION_STOP
            startService(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val intent = Intent(this, MyVpnService::class.java)
            intent.action = MyVpnService.ACTION_START
            startService(intent)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
