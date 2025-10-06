package com.example.vpnapp

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    // Activity result launcher for VPN permission
    private val vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find buttons by their IDs
        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)

        // Set OnClickListener for startButton
        startButton.setOnClickListener {
            Log.d(TAG, "Start button clicked!")
            requestVpnPermission()
        }

        // Set OnClickListener for stopButton
        stopButton.setOnClickListener {
            Log.d(TAG, "Stop button clicked!")
            stopVpnService()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun requestVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            // Permission already granted
            startVpnService()
        }
    }

    private fun startVpnService() {
        try {
            val intent = Intent(this, MyVpnService::class.java)
            intent.action = MyVpnService.ACTION_START
            startService(intent)
            Toast.makeText(this, "Starting VPN service...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "VPN service started")
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
            Toast.makeText(this, "Stopping VPN service...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "VPN service stop requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop VPN service: ${e.message}", e)
            Toast.makeText(this, "Failed to stop VPN", Toast.LENGTH_SHORT).show()
        }
    }
}
