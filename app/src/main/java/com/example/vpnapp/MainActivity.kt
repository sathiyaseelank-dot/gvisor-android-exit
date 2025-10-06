package com.example.vpnapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log // Import Log
import android.widget.Button // Import Button

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity" // Define a TAG for logging

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
            // Add your VPN start logic here
        }

        // Set OnClickListener for stopButton
        stopButton.setOnClickListener {
            Log.d(TAG, "Stop button clicked!")
            // Add your VPN stop logic here
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
