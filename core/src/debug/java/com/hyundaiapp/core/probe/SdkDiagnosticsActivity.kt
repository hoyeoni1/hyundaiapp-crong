package com.hyundaiapp.core.probe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.Executors

/** Local debug harness, excluded from release APKs. Does not contact a backend. */
class SdkDiagnosticsActivity : Activity() {
    private val main = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor()
    private var signals: VehicleSignals? = null
    private lateinit var output: TextView
    private var selectedProfile = "cruise"
    private var mode = "stopped"
    private val refresh = object : Runnable {
        override fun run() {
            publish()
            main.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 16, 24, 16) }
        layout.addView(TextView(this).apply {
            text = "LOCAL SDK DIAGNOSTICS (debug only)\nStart live reads this device's vehicle/location signals. No server upload."
            textSize = 18f
        })
        fun button(label: String, action: () -> Unit) {
            layout.addView(Button(this).apply { text = label; setOnClickListener { action() } })
        }
        button("Start live — cruise signals") { selectedProfile = "cruise"; startLive() }
        button("Start live — charging signals") { selectedProfile = "charging"; startLive() }
        button("Start fake — local baseline") { startFake() }
        button("Stop / release SDK") { signals?.stop(); mode = "stopped"; publish() }
        output = TextView(this).apply { textSize = 24f; setTextIsSelectable(true) }
        layout.addView(output)
        setContentView(ScrollView(this).apply { addView(layout) })
        main.post(refresh)
        selectedProfile = intent.getStringExtra("profile") ?: "cruise"
        if (intent.getBooleanExtra("autostart", false)) startLive()
    }

    private fun options() = if (selectedProfile == "charging") PleosSignalOptions.chachelin() else PleosSignalOptions.cruiseBreaker()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectedProfile = intent.getStringExtra("profile") ?: "cruise"
        if (intent.getBooleanExtra("autostart", false)) startLive()
    }

    private fun startLive() {
        signals?.stop()
        signals = null
        if (SignalPermissions.SPEED in SignalPermissions.missing(this, options())) {
            mode = "awaiting-speed-permission"
            requestPermissions(arrayOf(SignalPermissions.SPEED), 100)
            return
        }
        mode = "live"
        signals = PleosVehicleSignals(applicationContext, options()).also { it.start() }
        publish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            // Start even after denial, so the diagnostics can prove PERMISSION_DENIED behavior.
            mode = "live"
            signals = PleosVehicleSignals(applicationContext, options()).also { it.start() }
            publish()
        }
    }

    private fun startFake() {
        signals?.stop()
        mode = "fake"
        signals = FakeVehicleSignals().also {
            it.start(); it.setVehicleSpeedKph(60f); it.setCruiseState(CruiseControlState.ACTIVATED)
            it.setBrakePressed(false); it.setSocPercent(50)
            it.setLocation(LocationObservation(GeoPoint(37.3948, 127.1112)))
        }
        publish()
    }

    private fun publish() {
        val snapshot = signals?.snapshot?.value ?: VehicleSnapshot()
        val report = JSONObject().put("mode", mode).put("profile", selectedProfile)
            .put("lifecycle", snapshot.lifecycle.name).put("updatedAt", System.currentTimeMillis())
        fun <T> add(name: String, reading: SignalReading<T>) {
            report.put(name, JSONObject().put("status", reading.status.name)
                .put("value", reading.value?.toString() ?: JSONObject.NULL)
                .put("sequence", reading.sequence).put("observedAtMillis", reading.observedAtMillis ?: JSONObject.NULL)
                .put("error", reading.errorMessage ?: JSONObject.NULL))
        }
        add("speed", snapshot.vehicleSpeedKph); add("cruise", snapshot.cruiseState)
        add("targetSpeed", snapshot.cruiseTargetSpeedKph); add("brake", snapshot.brakePressed)
        add("steering", snapshot.steeringAngle); add("location", snapshot.location)
        add("traffic", snapshot.routeTraffic); add("soc", snapshot.socPercent)
        add("port", snapshot.chargePortOpen); add("chargeSwitch", snapshot.chargeSwitchOn)
        val text = report.toString(2)
        output.text = buildString {
            append("Mode: $mode | Profile: $selectedProfile | ${snapshot.lifecycle}\n\n")
            for (name in listOf("speed", "cruise", "targetSpeed", "brake", "steering", "soc", "port", "chargeSwitch", "location", "traffic")) {
                val reading = report.getJSONObject(name)
                append("$name: ${reading.getString("status")}  ${reading.opt("value")}\n")
                if (!reading.isNull("error")) append("  ${reading.getString("error")}\n")
            }
            append("\nFull JSON: tools/windows-test.ps1 read\n")
        }
        Log.i("SdkDiagnostics", report.toString())
        writer.execute {
            val temporary = java.io.File(filesDir, "sdk-diagnostics.tmp")
            temporary.writeText(text)
            temporary.renameTo(java.io.File(filesDir, "sdk-diagnostics.json"))
        }
    }

    override fun onDestroy() {
        main.removeCallbacks(refresh)
        signals?.stop()
        writer.shutdown()
        super.onDestroy()
    }
}
