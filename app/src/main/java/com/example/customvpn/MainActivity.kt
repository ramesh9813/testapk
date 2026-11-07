package com.example.customvpn

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var serverInput: EditText
    private lateinit var dnsInput: EditText
    private lateinit var statusText: TextView
    private lateinit var connectBtn: Button
    private var isConnected = false

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            statusText.text = "Status: VPN Permission Denied"
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val connected = intent?.getBooleanExtra("connected", false) ?: false
            updateUiState(connected)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverInput = findViewById(R.id.serverInput)
        dnsInput = findViewById(R.id.dnsInput)
        statusText = findViewById(R.id.statusText)
        connectBtn = findViewById(R.id.connectBtn)

        connectBtn.setOnClickListener {
            if (isConnected) {
                stopVpnService()
            } else {
                prepareAndStartVpn()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(LocalVpnService.ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }
        updateUiState(LocalVpnService.isRunning)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statusReceiver)
    }

    private fun prepareAndStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, LocalVpnService::class.java).apply {
            action = LocalVpnService.ACTION_CONNECT
            putExtra("server", serverInput.text.toString().trim())
            putExtra("dns", dnsInput.text.toString().trim())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateUiState(true)
    }

    private fun stopVpnService() {
        val intent = Intent(this, LocalVpnService::class.java).apply {
            action = LocalVpnService.ACTION_DISCONNECT
        }
        startService(intent)
        updateUiState(false)
    }

    private fun updateUiState(connected: Boolean) {
        isConnected = connected
        if (connected) {
            statusText.text = "Status: Connected (TUN Interface Active)"
            connectBtn.text = "Disconnect VPN"
        } else {
            statusText.text = "Status: Disconnected"
            connectBtn.text = "Connect VPN"
        }
    }
}
