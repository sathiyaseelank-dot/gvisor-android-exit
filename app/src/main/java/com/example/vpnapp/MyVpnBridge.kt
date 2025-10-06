package com.example.vpnapp

import android.net.VpnService
import android.util.Log
import com.celzero.firestack.intra.Bridge
import com.celzero.firestack.intra.Mark
import com.celzero.firestack.intra.PreMark
import com.celzero.firestack.backend.*

class MyVpnBridge(private val vpnService: VpnService) : Bridge {

    companion object {
        private const val TAG = "MyVpnBridge"
    }

    // ------------------------- Controller / Socket Protection -------------------------
    override fun bind4(who: String?, addrport: String?, fd: Long) {
        try {
            if (fd > 0) {
                val success = vpnService.protect(fd.toInt())
                Log.d(TAG, "IPv4 bind protection for $who/$addrport: $success")
            }
        } catch (e: Exception) {
            Log.w(TAG, "IPv4 bind protection failed for $who/$addrport: ${e.message}")
        }
    }

    override fun bind6(who: String?, addrport: String?, fd: Long) {
        try {
            if (fd > 0) {
                val success = vpnService.protect(fd.toInt())
                Log.d(TAG, "IPv6 bind protection for $who/$addrport: $success")
            }
        } catch (e: Exception) {
            Log.w(TAG, "IPv6 bind protection failed for $who/$addrport: ${e.message}")
        }
    }

    override fun protect(who: String?, fd: Long) {
        try {
            if (fd > 0) {
                val success = vpnService.protect(fd.toInt())
                Log.d(TAG, "Socket protection for $who: $success")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Socket protection failed for $who: ${e.message}")
        }
    }

    // ------------------------- Console / Logging -------------------------
    override fun log(level: Int, msg: Gostr?) {
        val message = msg?.string() ?: ""
        when (level) {
            0, 1 -> Log.v(TAG, message)  // Very verbose / Verbose
            2 -> Log.d(TAG, message)     // Debug
            3 -> Log.i(TAG, message)     // Info
            4 -> Log.w(TAG, message)     // Warning
            5 -> Log.e(TAG, message)     // Error
            6 -> Log.e(TAG, "STACK: $message")  // Stack traces
            7 -> Log.i(TAG, "USER: $message")   // User notifications
            else -> Log.v(TAG, message)
        }
    }

    // ------------------------- Flow / Listener (Traffic Management) -------------------------
    override fun flow(uid: Int, pid: Int, src: Gostr?, dst: Gostr?, domain: Gostr?, probeid: Gostr?, blocklist: Gostr?, summary: Gostr?): Mark {
        val mark = Mark()
        
        // Log the flow for monitoring gVisor traffic
        Log.d(TAG, "Flow: uid=$uid, pid=$pid, src=${src?.string()}, dst=${dst?.string()}, domain=${domain?.string()}")
        
        // For gVisor integration, we can customize traffic handling here
        // This is where we can implement traffic filtering, routing decisions, etc.
        
        return mark
    }

    override fun inflow(uid: Int, pid: Int, src: Gostr?, dst: Gostr?): Mark {
        val mark = Mark()
        Log.v(TAG, "Inbound flow: uid=$uid, pid=$pid, src=${src?.string()}, dst=${dst?.string()}")
        return mark
    }

    override fun preflow(uid: Int, pid: Int, src: Gostr?, dst: Gostr?): PreMark {
        val preMark = PreMark()
        Log.v(TAG, "Pre-flow: uid=$uid, pid=$pid, src=${src?.string()}, dst=${dst?.string()}")
        return preMark
    }

    override fun postFlow(mark: Mark?) {
        Log.v(TAG, "Post-flow processing: $mark")
    }

    // ------------------------- DNS Listeners (gVisor DNS Integration) -------------------------
    override fun onDNSAdded(resolver: Gostr?) {
        Log.i(TAG, "DNS resolver added: ${resolver?.string()}")
    }

    override fun onDNSRemoved(resolver: Gostr?) {
        Log.i(TAG, "DNS resolver removed: ${resolver?.string()}")
    }

    override fun onDNSStopped() {
        Log.i(TAG, "DNS service stopped")
    }

    override fun onQuery(query: Gostr?, qtype: Gostr?, fd: Long): DNSOpts {
        val opts = DNSOpts()
        Log.v(TAG, "DNS query: ${query?.string()}, type: ${qtype?.string()}")
        
        // Here we can customize DNS behavior for gVisor
        // For example, blocking certain domains, redirecting queries, etc.
        
        return opts
    }

    override fun onResponse(summary: DNSSummary?) {
        summary?.let {
            Log.v(TAG, "DNS response received")
        }
    }

    override fun onUpstreamAnswer(summary: DNSSummary?, upstream: Gostr?): DNSOpts {
        val opts = DNSOpts()
        Log.v(TAG, "Upstream DNS answer from ${upstream?.string()}")
        return opts
    }

    // ------------------------- Proxy Listeners (gVisor Proxy Integration) -------------------------
    override fun onProxiesStopped() {
        Log.i(TAG, "All proxies stopped")
    }

    override fun onProxyAdded(proxy: Gostr?) {
        Log.i(TAG, "Proxy added: ${proxy?.string()}")
    }

    override fun onProxyRemoved(proxy: Gostr?) {
        Log.i(TAG, "Proxy removed: ${proxy?.string()}")
    }

    override fun onProxyStopped(proxy: Gostr?) {
        Log.i(TAG, "Proxy stopped: ${proxy?.string()}")
    }

    // ------------------------- Server Listeners -------------------------
    override fun onSvcComplete(summary: ServerSummary?) {
        summary?.let {
            Log.d(TAG, "Service completed")
        }
    }

    override fun svcRoute(proto: String?, src: String?, dst: String?, domain: String?, uid: String?): Tab {
        val tab = Tab()
        Log.v(TAG, "Service route: proto=$proto, src=$src, dst=$dst, domain=$domain, uid=$uid")
        
        // This is where we can implement custom routing logic for gVisor
        // Different services can be routed through different paths
        
        return tab
    }

    // ------------------------- Socket Listener (gVisor Network Stats) -------------------------
    override fun onSocketClosed(summary: com.celzero.firestack.intra.SocketSummary?) {
        summary?.let {
            Log.d(TAG, "Socket closed: id=${it.id}, proto=${it.proto}, rx=${it.rx} bytes, tx=${it.tx} bytes, duration=${it.duration}ms")
            
            // This provides insight into gVisor's network activity
            // Can be used for bandwidth monitoring, connection tracking, etc.
        }
    }
}
