package com.example.vpnapp

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val VPN_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        startButton.setOnClickListener {
            Log.i(TAG, "Start VPN button clicked")
            requestVpnPermission()
        }

        stopButton.setOnClickListener {
            Log.i(TAG, "Stop VPN button clicked")
            stopVpnService()
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            Log.i(TAG, "Requesting VPN permission")
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            Log.i(TAG, "VPN permission already granted")
            startVpnService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVpnService()
            } else {
                Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "VPN permission denied by user")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun startVpnService() {
        try {
            val intent = Intent(this, MyVpnService::class.java)
            intent.action = MyVpnService.ACTION_START
            startService(intent)
            Toast.makeText(this, "Starting gVisor VPN...", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "gVisor VPN service start requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN service: ${e.message}", e)
            Toast.makeText(this, "Failed to start VPN", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVpnService() {
        try {
            val intent = Intent(this, MyVpnService::class.java)
            intent.action = MyVpnService.ACTION_STOP
            startService(intent)
            Toast.makeText(this, "Stopping VPN...", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "VPN service stop requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop VPN service: ${e.message}", e)
            Toast.makeText(this, "Failed to stop VPN", Toast.LENGTH_SHORT).show()
        }
    }
}
