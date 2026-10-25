package sa.moha.sonataos.obd

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

interface ObdTransport {
    suspend fun connect()
    /** Sends one command and returns raw response up to the '>' prompt. */
    suspend fun send(cmd: String, timeoutMs: Long = 2500): String
    fun close()
}

/** Classic Bluetooth SPP (most cheap ELM327 v1.5/v2.1 adapters). */
@SuppressLint("MissingPermission")
class SppTransport(private val adapter: BluetoothAdapter, private val mac: String) : ObdTransport {
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override suspend fun connect() = withContext(Dispatchers.IO) {
        val dev = adapter.getRemoteDevice(mac)
        adapter.cancelDiscovery()
        val s = runCatching { dev.createRfcommSocketToServiceRecord(sppUuid).also { it.connect() } }
            .getOrElse {
                // Fallback used by many OBD apps for buggy stacks
                val m = dev.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                (m.invoke(dev, 1) as BluetoothSocket).also { it.connect() }
            }
        socket = s; input = s.inputStream; output = s.outputStream
    }

    override suspend fun send(cmd: String, timeoutMs: Long): String = withContext(Dispatchers.IO) {
        require(ObdCommands.isAllowed(cmd)) { "Blocked non read-only command: $cmd" }
        val out = output ?: error("not connected"); val inp = input ?: error("not connected")
        while (inp.available() > 0) inp.read()
        out.write((cmd + "\r").toByteArray()); out.flush()
        val sb = StringBuilder()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (inp.available() > 0) {
                val c = inp.read().toChar()
                if (c == '>') break
                sb.append(c)
            } else delay(10)
        }
        sb.toString()
    }

    override fun close() { runCatching { socket?.close() }; socket = null }
}

/** BLE ELM327 clones (Vgate iCar Pro BLE, Veepeak BLE...). Uses common FFF0/FFE0 serial services. */
@SuppressLint("MissingPermission")
class BleTransport(private val context: Context, private val adapter: BluetoothAdapter, private val mac: String) : ObdTransport {
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private val rx = Channel<String>(Channel.UNLIMITED)
    private val ready = CompletableDeferred<Unit>()
    private val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val cb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) g.discoverServices()
            else if (!ready.isCompleted) ready.completeExceptionally(IllegalStateException("BLE disconnected"))
        }
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val chars = g.services.flatMap { it.characteristics }
            val notify = chars.firstOrNull { it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 }
            val write = chars.firstOrNull {
                it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
            }
            if (notify == null || write == null) { ready.completeExceptionally(IllegalStateException("no serial characteristic")); return }
            g.setCharacteristicNotification(notify, true)
            notify.getDescriptor(cccd)?.let { d ->
                @Suppress("DEPRECATION")
                d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION") g.writeDescriptor(d)
            }
            writeChar = write
            ready.complete(Unit)
        }
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION") rx.trySend(String(c.value ?: return))
        }
    }

    override suspend fun connect() {
        gatt = adapter.getRemoteDevice(mac).connectGatt(context, false, cb, BluetoothDevice.TRANSPORT_LE)
        withTimeout(10_000) { ready.await() }
        delay(300)
    }

    override suspend fun send(cmd: String, timeoutMs: Long): String {
        require(ObdCommands.isAllowed(cmd)) { "Blocked non read-only command: $cmd" }
        while (rx.tryReceive().isSuccess) { /* drain */ }
        val c = writeChar ?: error("not connected")
        @Suppress("DEPRECATION")
        c.value = (cmd + "\r").toByteArray()
        @Suppress("DEPRECATION") gatt?.writeCharacteristic(c)
        val sb = StringBuilder()
        withTimeoutOrNull(timeoutMs) {
            while (!sb.contains('>')) sb.append(rx.receive())
        }
        return sb.toString().substringBefore('>')
    }

    override fun close() { runCatching { gatt?.close() }; gatt = null }
}

/** Simulated ELM327 that produces realistic-looking city driving so the UI works without a car. */
class DemoTransport : ObdTransport {
    private var t = 0.0
    private var coolant = 35.0
    private var fuel = 62.0
    private val dtcs = mutableListOf("0171", "0420") // P0171, P0420
    override suspend fun connect() { delay(400) }

    override suspend fun send(cmd: String, timeoutMs: Long): String {
        require(ObdCommands.isAllowed(cmd))
        delay(15)
        val c = cmd.uppercase().replace(" ", "")
        t += 0.05
        val speed = (55 + 45 * sin(t / 3) + 8 * sin(t * 1.7)).coerceIn(0.0, 140.0)
        val rpm = (800 + speed * 22 + 300 * sin(t * 2.3)).coerceIn(700.0, 6000.0)
        coolant = min(91.0, coolant + 0.15)
        fuel = max(5.0, fuel - 0.0008)
        fun h(v: Int) = "%02X".format(v.coerceIn(0, 255))
        return when {
            c.startsWith("AT") -> if (c == "ATRV") "%.1fV".format(13.9 + 0.2 * sin(t)) else if (c == "ATZ") "ELM327 v1.5 (DEMO)" else "OK"
            c == "03" -> if (dtcs.isEmpty()) "4300" else "43" + h(dtcs.size) + dtcs.joinToString("")
            c == "04" -> { dtcs.clear(); "44" }
            c == "010C" -> { val v = (rpm * 4).toInt(); "410C" + h(v / 256) + h(v % 256) }
            c == "010D" -> "410D" + h(speed.toInt())
            c == "0105" -> "4105" + h((coolant + 40).toInt())
            c == "0104" -> "4104" + h(((25 + 30 * abs(sin(t))) * 2.55).toInt())
            c == "0111" -> "4111" + h(((14 + 25 * abs(sin(t * 1.3))) * 2.55).toInt())
            c == "010F" -> "410F" + h(78)
            c == "0110" -> { val v = ((3 + speed * 0.18 + Random.nextDouble(0.5)) * 100).toInt(); "4110" + h(v / 256) + h(v % 256) }
            c == "012F" -> "412F" + h((fuel * 2.55).toInt())
            c == "0106" -> "4106" + h(128 + (4 * sin(t * 4)).toInt())
            c == "0107" -> "4107" + h(133)
            c == "010B" -> "410B" + h(35 + (20 * abs(sin(t))).toInt())
            c == "0142" -> { val v = ((13.9 + 0.2 * sin(t)) * 1000).toInt(); "4142" + h(v / 256) + h(v % 256) }
            c == "0146" -> "4146" + h(38 + 40)
            else -> "NO DATA"
        }
    }

    override fun close() {}
}
