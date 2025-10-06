package com.celzero

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var modeRadioGroup: RadioGroup
    private lateinit var startStopButton: Button

    companion object {
        var isVpnRunning = false
    }

    private val vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        modeRadioGroup = findViewById(R.id.mode_radio_group)
        startStopButton = findViewById(R.id.start_stop_button)

        startStopButton.setOnClickListener {
            if (isVpnRunning) {
                stopVpnService()
            } else {
                prepareAndStartVpn()
            }
        }
        updateUI()
    }

    private fun prepareAndStartVpn() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val selectedModeId = modeRadioGroup.checkedRadioButtonId
        val selectedMode = if (selectedModeId == R.id.client_mode_radio) {
            MyVpnService.MODE_CLIENT
        } else {
            MyVpnService.MODE_EXIT_NODE
        }

        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_START
            putExtra(MyVpnService.EXTRA_MODE, selectedMode)
        }
        startService(intent)
        isVpnRunning = true
        updateUI()
    }

    private fun stopVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_STOP
        }
        startService(intent)
        isVpnRunning = false
        updateUI()
    }

    private fun updateUI() {
        if (isVpnRunning) {
            statusText.text = "Status: Connected"
            startStopButton.text = "Stop VPN"
            modeRadioGroup.isEnabled = false
            findViewById<RadioButton>(R.id.client_mode_radio).isEnabled = false
            findViewById<RadioButton>(R.id.exit_node_mode_radio).isEnabled = false
        } else {
            statusText.text = "Status: Disconnected"
            startStopButton.text = "Start VPN"
            modeRadioGroup.isEnabled = true
            findViewById<RadioButton>(R.id.client_mode_radio).isEnabled = true
            findViewById<RadioButton>(R.id.exit_node_mode_radio).isEnabled = true
        }
    }
}