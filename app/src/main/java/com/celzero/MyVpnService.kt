package com.celzero

import android.app.ForegroundService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import govpn.GoVpn
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null

    companion object {
        const val ACTION_START = "com.celzero.START_VPN"
        const val ACTION_STOP = "com.celzero.STOP_VPN"
        const val EXTRA_MODE = "com.celzero.MODE"
        const val MODE_CLIENT = "CLIENT"
        const val MODE_EXIT_NODE = "EXIT_NODE"
        private const val NOTIFICATION_CHANNEL_ID = "MyVpnServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "MyVpnService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START) {
            val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_CLIENT
            startVpn(mode)
        } else if (action == ACTION_STOP) {
            stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(mode: String) {
        Log.d(TAG, "Starting VPN in mode: $mode")
        if (vpnThread?.isAlive == true) {
            Log.d(TAG, "VPN already running")
            return
        }
        vpnThread = Thread {
            runVpnConnection(mode)
        }
        vpnThread?.start()
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN")
        vpnThread?.interrupt()
        try {
            GoVpn.stop()
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN", e)
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun runVpnConnection(mode: String) {
        try {
            val config = readAssetFile(if (mode == MODE_CLIENT) "client_1.conf" else "server.conf")
            if (config.isNullOrEmpty()) {
                Log.e(TAG, "Failed to read config file")
                return
            }

            val builder = Builder()
            val session = builder
                .setSession("MyGvisorVpn")
                .addAddress("10.8.0.1", 24)
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .establish()

            vpnInterface = session
            val fd = vpnInterface?.fileDescriptor?.asInt() ?: throw IOException("Failed to get file descriptor")

            Log.d(TAG, "Starting GoVpn with fd: $fd")
            GoVpn.run(config, fd.toLong()) // Assuming GoVpn.run takes config string and fd

        } catch (e: Exception) {
            Log.e(TAG, "VPN connection error", e)
        } finally {
            stopVpn()
        }
    }

    private fun readAssetFile(fileName: String): String? {
        return try {
            assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading asset file: $fileName", e)
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, getForegroundServiceType())
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(this, 0, notificationIntent, android.app.PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("gVisor VPN Active")
            .setContentText("VPN service is running.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a real icon
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "My VPN Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
    
    private fun getForegroundServiceType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundService.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
        } else {
            0
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
