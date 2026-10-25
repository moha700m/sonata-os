package sa.moha.sonataos.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import kotlinx.coroutines.sync.withLock

enum class ObdStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class ObdState(
    val status: ObdStatus = ObdStatus.DISCONNECTED,
    val demo: Boolean = false,
    val values: Map<Pid, Double> = emptyMap(),
    val voltage: Double? = null,
    val dtcs: List<String> = emptyList(),
    val message: String = "",
    val recording: Boolean = false,
    val supportsMaf: Boolean = true,
)

class ObdManager(private val context: Context, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(ObdState())
    val state: StateFlow<ObdState> = _state
    private var transport: ObdTransport? = null
    private var job: Job? = null
    private var recorder: java.io.Writer? = null
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private suspend fun cmd(tr: ObdTransport, c: String, t: Long = 2500) = mutex.withLock { tr.send(c, t) }

    private val fastPids = listOf(Pid.RPM, Pid.SPEED, Pid.THROTTLE, Pid.ENGINE_LOAD, Pid.MAF)
    private val slowPids = listOf(Pid.COOLANT_TEMP, Pid.INTAKE_TEMP, Pid.FUEL_LEVEL, Pid.STFT_B1, Pid.LTFT_B1, Pid.MODULE_VOLTAGE, Pid.MAP)

    @SuppressLint("MissingPermission")
    fun connect(mac: String, ble: Boolean, demo: Boolean) {
        disconnect()
        _state.value = ObdState(status = ObdStatus.CONNECTING, demo = demo, message = "جاري الاتصال…")
        job = scope.launch(Dispatchers.IO) {
            try {
                val tr: ObdTransport = if (demo || mac.isBlank()) DemoTransport() else {
                    val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                        ?: error("الجهاز ما فيه بلوتوث")
                    if (ble) BleTransport(context, adapter, mac) else SppTransport(adapter, mac)
                }
                transport = tr
                tr.connect()
                for (c in ObdCommands.init) { cmd(tr, c, 3000); delay(if (c == "ATZ") 800 else 80) }
                cmd(tr, "0100", 8000) // triggers protocol search
                _state.value = _state.value.copy(status = ObdStatus.CONNECTED, demo = tr is DemoTransport, message = if (tr is DemoTransport) "وضع تجريبي (محاكاة)" else "متصل")
                refreshDtcs()
                var cycle = 0
                while (isActive) {
                    val pids = if (cycle % 5 == 0) fastPids + slowPids else fastPids
                    val vals = _state.value.values.toMutableMap()
                    for (p in pids) {
                        val raw = cmd(tr, p.command)
                        ObdParser.parse(p, raw)?.let { vals[p] = it }
                    }
                    val volt = if (cycle % 5 == 0) ObdParser.parseVoltage(cmd(tr, ObdCommands.VOLTAGE)) ?: vals[Pid.MODULE_VOLTAGE] else _state.value.voltage
                    _state.value = _state.value.copy(values = vals, voltage = volt, supportsMaf = vals.containsKey(Pid.MAF))
                    recorder?.let { w -> w.write("${System.currentTimeMillis()}," + Pid.entries.joinToString(",") { vals[it]?.let { v -> "%.2f".format(v) } ?: "" } + "\n") }
                    cycle++
                    delay(if (tr is DemoTransport) 120 else 50)
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(status = ObdStatus.ERROR, message = "فشل الاتصال: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    suspend fun refreshDtcs() {
        val tr = transport ?: return
        val list = ObdParser.parseDtcs(cmd(tr, ObdCommands.READ_DTC, 5000))
        _state.value = _state.value.copy(dtcs = list)
    }

    /** Mode 04. UI must ask for explicit confirmation first (also turns off MIL and resets readiness monitors). */
    fun clearDtcsConfirmed() {
        scope.launch(Dispatchers.IO) {
            val tr = transport ?: return@launch
            cmd(tr, ObdCommands.CLEAR_DTC, 5000)
            delay(500)
            refreshDtcs()
        }
    }

    fun toggleRecording() {
        if (recorder != null) { recorder?.close(); recorder = null; _state.value = _state.value.copy(recording = false); return }
        val dir = File(context.getExternalFilesDir(null), "obd").apply { mkdirs() }
        val f = File(dir, "obd_${System.currentTimeMillis()}.csv")
        recorder = f.bufferedWriter().also { it.write("time," + Pid.entries.joinToString(",") { it.name } + "\n") }
        _state.value = _state.value.copy(recording = true, message = "تسجيل في ${f.name}")
    }

    fun disconnect() {
        job?.cancel(); job = null
        transport?.close(); transport = null
        recorder?.close(); recorder = null
        _state.value = ObdState()
    }
}
