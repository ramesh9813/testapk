package com.example.customvpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class LocalVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.example.customvpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.customvpn.DISCONNECT"
        const val ACTION_STATUS = "com.example.customvpn.STATUS"
        private const val CHANNEL_ID = "CustomVpnChannel"
        @Volatile
        var isRunning: Boolean = false
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private var workerThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_CONNECT, null -> {
                val dns = intent?.getStringExtra("dns")?.takeIf { it.isNotEmpty() } ?: "1.1.1.1"
                startVpn(dns)
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn(dnsServer: String) {
        if (running.get()) return
        createNotificationChannel()
        startForeground(1, buildNotification("VPN Tunnel Active (DNS: $dnsServer)"))

        val builder = Builder()
            .setSession("CustomShieldVPN")
            .setMtu(1500)
            .addAddress("10.8.0.2", 24)
            .addDnsServer(dnsServer)
            .addDnsServer("8.8.8.8")
            // Route private tunnel subnet & DNS traffic via TUN interface
            .addRoute("10.8.0.0", 24)
            .addRoute(dnsServer, 32)

        vpnInterface = builder.establish()
        running.set(true)
        isRunning = true
        broadcastStatus(true)

        workerThread = Thread {
            runTunnelLoop()
        }.apply { start() }
    }

    private fun runTunnelLoop() {
        val fd = vpnInterface?.fileDescriptor ?: return
        val inputStream = FileInputStream(fd)
        val outputStream = FileOutputStream(fd)
        val packet = ByteBuffer.allocate(32767)

        try {
            while (running.get() && !Thread.currentThread().isInterrupted) {
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    // Packet inspection / custom UDP tunnel forwarding hook
                    packet.limit(length)
                    packet.clear()
                } else {
                    Thread.sleep(50)
                }
            }
        } catch (e: Exception) {
            // Tunnel closed
        } finally {
            try {
                inputStream.close()
                outputStream.close()
            } catch (_: Exception) {}
        }
    }

    private fun stopVpn() {
        running.set(false)
        isRunning = false
        workerThread?.interrupt()
        workerThread = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
        broadcastStatus(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun broadcastStatus(connected: Boolean) {
        val intent = Intent(ACTION_STATUS).apply {
            setPackage(packageName)
            putExtra("connected", connected)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CustomShield VPN Status",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CustomShield VPN")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
