
package com.example.vpnapp

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.celzero.firestack.intra.Bridge
import com.celzero.firestack.intra.Intra
import com.celzero.firestack.intra.Mark
import com.celzero.firestack.intra.Tunnel
import com.celzero.firestack.backend.Backend
import com.celzero.firestack.backend.Gostr
import com.celzero.firestack.intra.SocketSummary
import kotlinx.coroutines.launch
// WireGuard Exit Node Integration for gVisor VPN
class WireGuardExitNodeService : MyVpnService() {

    companion object {
        private const val WG_SERVER_PORT = 51820
        private const val WG_NETWORK = "10.8.0.0/24"
        private const val WG_SERVER_IP = "10.8.0.1"
        private const val SERVER_PRIVATE_KEY = "8G9FZuEyf4xcw3fP/0O2RlgmvWlzj5TDZym5Nrl0v38="
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startExitNode()
            }
            ACTION_STOP -> stopTunnel()
        }
        return START_STICKY
    }

    fun startExitNode() {
        val builder = Builder()
        builder.setSession("gVisor-WG-ExitNode")
            .addAddress(WG_SERVER_IP, 24)
            .addRoute("10.8.0.0", 24)  // Route client traffic
            .setMtu(1420)  // WireGuard optimal MTU
            .setBlocking(false)

        tunInterface = builder.establish()
        val fd = tunInterface?.fd
        if (fd == null) {
            Log.e("WG-Exit", "Failed to get TUN file descriptor")
            return
        }
        Log.i("WG-Exit", "TUN file descriptor: $fd")

        scope.launch {
            try {
                bridge = WireGuardExitBridge(this@WireGuardExitNodeService)
                val defaultDNS = Intra.newBuiltinDefaultDNS()

                // Configure interface addresses for exit node
                val ifaddrs = "$WG_SERVER_IP/24"
                val fakedns = "$WG_SERVER_IP:53"

                // Create tunnel with WireGuard exit node configuration
                tunnel = Intra.connect(fd.toLong(), 1420L, ifaddrs, fakedns, defaultDNS, bridge!!)

                // Enable features needed for exit node operation
                Intra.experimental(true)  // Enable WireGuard support
                Intra.transparency(true, true)  // Enable NAT traversal
                Intra.loopback(true)  // Handle local connections

                // Start WireGuard server
                startWireGuardServer()

                Log.i("WG-Exit", "WireGuard exit node started on port $WG_SERVER_PORT")

                // Debugging logs
                Log.i("WG-Exit", "Tunnel connected: ${tunnel?.isConnected()}")
                Log.i("WG-Exit", "Proxies: ${tunnel?.getProxies()?.refreshProxies()?.v()}")

            } catch (e: Exception) {
                Log.e("WG-Exit", "Failed to start exit node: ${e.message}", e)
            }
        }
    }



    private fun startWireGuardServer() {
        val wgConfig = buildWireGuardConfig()
        try {
            val proxies = tunnel?.getProxies()
            proxies?.addProxy(Backend.strOf("wg-exit"), Backend.strOf(wgConfig))
            Log.i("WG-Exit", "WireGuard server configured")
        } catch (e: Exception) {
            Log.e("WG-Exit", "Failed to configure WireGuard server: ${e.message}", e)
        }
    }

    private fun buildWireGuardConfig(): String {
        return """
            PrivateKey = $SERVER_PRIVATE_KEY
            ListenPort = $WG_SERVER_PORT
            Address = 10.8.0.1/24
            DNS = 8.8.8.8
        """.trimIndent()
    }
}

class WireGuardExitBridge(vpnService: VpnService) : MyVpnBridge(vpnService) {

    override fun onSocketClosed(summary: com.celzero.firestack.intra.SocketSummary?) {
        super.onSocketClosed(summary)
        summary?.let {
            // Track client connections for exit node monitoring
            Log.i("WG-Exit", "Client session: ${it.proto} ${it.rx}↓ ${it.tx}↑ bytes")
            updateExitNodeStats(it)
        }
    }

    override fun flow(uid: Int, pid: Int, src: Gostr?, dst: Gostr?,
                     domain: Gostr?, probeid: Gostr?, blocklist: Gostr?, summary: Gostr?): Mark {

        // Custom routing for exit node clients
        val mark = super.flow(uid, pid, src, dst, domain, probeid, blocklist, summary)

        // Log client traffic for monitoring
        Log.v("WG-Exit", "Routing: uid=${uid} ${src?.string()} -> ${dst?.string()}")

        return mark
    }

    private fun updateExitNodeStats(summary: com.celzero.firestack.intra.SocketSummary) {
        // Update exit node statistics
        // This could be sent to a monitoring dashboard
    }
}
