package com.example.vpnapp

import android.content.Context
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
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
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private var tunnel: Tunnel? = null
    private var bridge: Bridge? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTunnel()
            ACTION_STOP -> stopTunnel()
        }
        return START_STICKY
    }

    private fun startTunnel() {
        val builder = Builder()
        builder.setSession("FirestackVPN")
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)

        tunInterface = builder.establish()
        val fd = tunInterface?.fd ?: return
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