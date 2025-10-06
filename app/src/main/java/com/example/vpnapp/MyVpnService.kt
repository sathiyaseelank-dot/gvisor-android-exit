package com.example.vpnapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.celzero.firestack.intra.Bridge
import com.celzero.firestack.intra.DefaultDNS
import com.celzero.firestack.intra.Intra
import com.celzero.firestack.intra.Tunnel
import com.celzero.firestack.settings.Settings
import kotlinx.coroutines.*

class MyVpnService : VpnService() {
    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VPN_CHANNEL"
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private var tunnel: Tunnel? = null
    private var bridge: Bridge? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification("VPN Connected"))
                startTunnel()
            }
            ACTION_STOP -> stopTunnel()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Firestack VPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun startTunnel() {
        val builder = Builder()
        builder.setSession("FirestackVPN")
            .addAddress("10.111.222.1", 24)
            .addAddress("fd66:f83a:c650::1", 120)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer("10.111.222.3")
            .addDnsServer("fd66:f83a:c650::3")
            .setMtu(1500)

        tunInterface = builder.establish()
        val fd = tunInterface?.fd ?: return
        val mtu = 1500

        scope.launch {
            try {
                bridge = MyVpnBridge(this@MyVpnService)
                val defaultDNS = Intra.newBuiltinDefaultDNS()
                
                // Use the correct parameters for the tunnel - dual stack setup
                val ifaddrs = "10.111.222.1/24,fd66:f83a:c650::1/120"
                val fakedns = "10.111.222.3,fd66:f83a:c650::3"
                
                // Create tunnel with proper gVisor netstack integration
                tunnel = Intra.connect(fd.toLong(), mtu.toLong(), ifaddrs, fakedns, defaultDNS, bridge!!)
                
                // Enable gVisor-specific features
                Intra.experimental(true)  // Enable experimental features including WireGuard
                Intra.loopback(true)      // Enable loopback handling for local connections
                Intra.transparency(true, true)  // Enable endpoint-independent mapping/filtering
                Intra.logLevel(2, 3)      // Set appropriate log levels
                
                Log.i("VPN", "gVisor tunnel started successfully with netstack")
                
                // Update notification
                val notification = createNotification("VPN Active - gVisor netstack running")
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)
                
            } catch (e: Exception) {
                Log.e("VPN", "Failed to start gVisor tunnel: ${e.message}", e)
                stopTunnel()
            }
        }
    }

    private fun stopTunnel() {
        try {
            Log.i("VPN", "Stopping gVisor Firestack tunnel")
            tunnel?.disconnect()
            tunInterface?.close()
            bridge = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            Log.w("VPN", "Error stopping tunnel: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            Log.i("VPN", "Service destroyed - stopping gVisor tunnel")
            tunnel?.disconnect()
        } catch (e: Exception) {
            Log.w("VPN", "Error stopping tunnel: ${e.message}")
        } finally {
            tunInterface?.close()
            bridge = null
            scope.cancel()
        }
        super.onDestroy()
    }
}