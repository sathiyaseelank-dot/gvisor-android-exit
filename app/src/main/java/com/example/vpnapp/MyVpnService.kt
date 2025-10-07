package com.example.vpnapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
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
        private const val CHANNEL_ID = "VPN_SERVICE_CHANNEL"
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private var tunnel: Tunnel? = null
    private var bridge: Bridge? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Start foreground service without type for now to avoid permission issues
                startForeground(NOTIFICATION_ID, createNotification())
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
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VPN Active")
            .setContentText("Firestack VPN is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startTunnel() {
        val builder = Builder()
        builder.setSession("FirestackVPN")
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)

        tunInterface = builder.establish()
        val fileDescriptor = tunInterface?.fileDescriptor ?: return
        
        val fd = tunInterface?.getFd() ?: return
        val mtu = 1500L

        scope.launch {
            try {
                val bridge = MyVpnBridge(this@MyVpnService)
                val defaultDNS = Intra.newBuiltinDefaultDNS() // Correct way to get DefaultDNS
                val session = "firestack"
                val resolver = "1.1.1.1"
                val engine = Settings.Ns46

                tunnel = Intra.newTunnel(fd.toLong(), mtu, session, resolver, defaultDNS, bridge)
                tunnel?.restart(fd.toLong(), mtu, engine)

                Log.i("VPN", "Tunnel started successfully")
            } catch (e: Exception) {
                Log.e("VPN", "Failed to start tunnel: ${e.message}", e)
            }
        }
    }

    private fun stopTunnel() {
        try {
            Log.i("VPN", "Stopping Firestack tunnel")
            tunnel?.disconnect()
            tunInterface?.close()
            bridge = null
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            Log.w("VPN", "Error stopping tunnel: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            Log.i("VPN", "Stopping Firestack tunnel")
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