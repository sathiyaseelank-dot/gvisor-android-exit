package com.example.vpnapp

import android.content.Context
import android.net.VpnService
import android.util.Log
import com.celzero.firestack.intra.SocketSummary

class DefaultFirestackBridge(
    context: Context,
    vpnService: VpnService
) : FirestackBridge(context, vpnService) {

    override fun onSocketClosed(var1: SocketSummary?) {
        Log.d(TAG, "onSocketClosed: var1=$var1")
        // Concrete implementation for onSocketClosed
        // For now, just log it or leave it empty
    }
}