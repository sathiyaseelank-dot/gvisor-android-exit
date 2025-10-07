package com.example.vpnapp

import android.net.VpnService
import android.util.Log
import com.celzero.firestack.intra.Bridge
import com.celzero.firestack.intra.Mark
import com.celzero.firestack.intra.PreMark
import com.celzero.firestack.backend.*

open class MyVpnBridge(private val vpnService: VpnService) : Bridge {

    // ------------------------- Controller / Socket Protection -------------------------
    override fun bind4(who: String?, addrport: String?, fd: Long) {
        vpnService.protect(fd.toInt())
        Log.i("VPN-Bridge", "bind4 called: $who / $addrport")
    }

    override fun bind6(who: String?, addrport: String?, fd: Long) {
        vpnService.protect(fd.toInt())
        Log.i("VPN-Bridge", "bind6 called: $who / $addrport")
    }

    override fun protect(who: String?, fd: Long) {
        vpnService.protect(fd.toInt())
        Log.i("VPN-Bridge", "protect called: $who")
    }

    // ------------------------- Console / Logging -------------------------
    override fun log(level: Int, msg: Gostr?) {
        Log.i("VPN-Bridge", "Log level $level: ${msg?.toString() ?: "null"}")
    }

    // ------------------------- Flow / Listener (stub) -------------------------
    override fun flow(p1: Int, p2: Int, p3: Gostr?, p4: Gostr?, p5: Gostr?, p6: Gostr?, p7: Gostr?, p8: Gostr?): Mark {
        return Mark()
    }

    override fun inflow(p1: Int, p2: Int, p3: Gostr?, p4: Gostr?): Mark = Mark()
    override fun preflow(p1: Int, p2: Int, p3: Gostr?, p4: Gostr?): PreMark = PreMark()
    override fun postFlow(mark: Mark) {}

    // ------------------------- DNS / Proxy / Server Listeners (stub) -------------------------
    override fun onDNSAdded(g: Gostr?) {}
    override fun onDNSRemoved(g: Gostr?) {}
    override fun onDNSStopped() {}
    override fun onQuery(g1: Gostr?, g2: Gostr?, fd: Long): DNSOpts = DNSOpts()
    override fun onResponse(summary: DNSSummary?) {}
    override fun onUpstreamAnswer(summary: DNSSummary?, g: Gostr?): DNSOpts = DNSOpts()

    override fun onProxiesStopped() {}
    override fun onProxyAdded(g: Gostr?) {}
    override fun onProxyRemoved(g: Gostr?) {}
    override fun onProxyStopped(g: Gostr?) {}

    override fun onSvcComplete(summary: ServerSummary?) {}
    override fun svcRoute(s1: String?, s2: String?, s3: String?, s4: String?, s5: String?): Tab = Tab()
//    override fun onSocketClosed(summary: SocketSummary?) {}
    override fun onSocketClosed(summary: com.celzero.firestack.intra.SocketSummary?) {
        summary?.let {
            Log.i("VPN-Bridge", "Socket ${it.id} closed, proto=${it.proto}, rx=${it.rx}, tx=${it.tx}")
        }
    }
}
