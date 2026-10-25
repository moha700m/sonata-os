package sa.moha.sonataos.obd

/**
 * Pure parser for ELM327 responses to standard SAE J1979 Mode 01 / 03 PIDs.
 * Read-only by design: only Mode 01 (live data), 03 (stored DTCs), 04 (clear DTCs, user-confirmed)
 * and AT setup commands are ever sent. See [ObdCommands.isAllowed].
 */
object ObdParser {

    /** Strips echo, prompts, spaces, "SEARCHING..." and returns hex data bytes from all ECU lines. */
    fun cleanLines(raw: String): List<String> = raw
        .replace("\r", "\n")
        .split("\n")
        .map { it.trim().replace(">", "").replace(" ", "").uppercase() }
        .filter { it.isNotEmpty() && !it.startsWith("SEARCHING") && !it.startsWith("BUSINIT") }
        .map { line -> if (line.length > 2 && line[1] == ':' ) line.substring(2) else line } // CAN multi-frame "0:"

    fun isError(raw: String): Boolean {
        val u = raw.uppercase()
        return listOf("NO DATA", "UNABLE TO CONNECT", "ERROR", "STOPPED", "CAN ERROR", "?", "BUS BUSY")
            .any { u.contains(it) }
    }

    /**
     * Extract data bytes for a Mode 01 PID response like "41 0C 1A F8".
     * Returns null if no matching response found.
     */
    fun dataBytes(raw: String, pid: Int): IntArray? {
        if (isError(raw)) return null
        val header = "41" + "%02X".format(pid)
        for (line in cleanLines(raw)) {
            if (!line.all { it.isHexDigitChar() }) continue
            val idx = line.indexOf(header)
            if (idx < 0) continue
            val hex = line.substring(idx + 4)
            if (hex.length < 2) continue
            return IntArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16) }
        }
        return null
    }

    private fun Char.isHexDigitChar() = this in '0'..'9' || this in 'A'..'F'

    fun parse(pid: Pid, raw: String): Double? {
        val d = dataBytes(raw, pid.code) ?: return null
        if (d.size < pid.bytes) return null
        val a = d[0].toDouble()
        val b = if (d.size > 1) d[1].toDouble() else 0.0
        return when (pid) {
            Pid.ENGINE_LOAD, Pid.THROTTLE, Pid.FUEL_LEVEL -> a * 100.0 / 255.0
            Pid.COOLANT_TEMP, Pid.INTAKE_TEMP, Pid.AMBIENT_TEMP -> a - 40.0
            Pid.STFT_B1, Pid.LTFT_B1 -> (a - 128.0) * 100.0 / 128.0
            Pid.RPM -> (a * 256.0 + b) / 4.0
            Pid.SPEED -> a
            Pid.MAF -> (a * 256.0 + b) / 100.0
            Pid.MODULE_VOLTAGE -> (a * 256.0 + b) / 1000.0
            Pid.MAP -> a
        }
    }

    /** Parses ELM "ATRV" response such as "12.6V". */
    fun parseVoltage(raw: String): Double? =
        Regex("""(\d{1,2}\.\d)\s*V""", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.get(1)?.toDoubleOrNull()

    /** Parses Mode 03 response into DTC strings like P0301. Handles CAN (43 NN ...) and legacy (43 ...). */
    fun parseDtcs(raw: String): List<String> {
        if (isError(raw)) return emptyList()
        val joined = cleanLines(raw).filter { l -> l.all { it in '0'..'9' || it in 'A'..'F' } }.joinToString("")
        val start = joined.indexOf("43")
        if (start < 0) return emptyList()
        var hex = joined.substring(start + 2)
        // CAN: first byte after 43 is count of DTCs when length is odd number of pairs
        if ((hex.length / 2) % 2 == 1) hex = hex.substring(2)
        val out = mutableListOf<String>()
        var i = 0
        while (i + 4 <= hex.length) {
            val chunk = hex.substring(i, i + 4)
            i += 4
            if (chunk == "0000") continue
            if (chunk.startsWith("43")) continue
            out += decodeDtc(chunk)
        }
        return out.distinct()
    }

    fun decodeDtc(fourHex: String): String {
        val v = fourHex.toInt(16)
        val type = when ((v shr 14) and 0x3) { 0 -> 'P'; 1 -> 'C'; 2 -> 'B'; else -> 'U' }
        val d1 = (v shr 12) and 0x3
        return type + d1.toString() + "%03X".format(v and 0xFFF)
    }
}

enum class Pid(val code: Int, val bytes: Int, val unit: String, val labelAr: String) {
    ENGINE_LOAD(0x04, 1, "%", "حمل المحرك"),
    COOLANT_TEMP(0x05, 1, "°م", "حرارة المحرك"),
    STFT_B1(0x06, 1, "%", "تعديل الوقود القصير"),
    LTFT_B1(0x07, 1, "%", "تعديل الوقود الطويل"),
    MAP(0x0B, 1, "kPa", "ضغط الشفط"),
    RPM(0x0C, 2, "rpm", "دورات المحرك"),
    SPEED(0x0D, 1, "كم/س", "السرعة"),
    INTAKE_TEMP(0x0F, 1, "°م", "حرارة الهواء الداخل"),
    MAF(0x10, 2, "g/s", "تدفق الهواء"),
    THROTTLE(0x11, 1, "%", "دواسة البنزين"),
    FUEL_LEVEL(0x2F, 1, "%", "مستوى البنزين"),
    MODULE_VOLTAGE(0x42, 2, "V", "فولت البطارية"),
    AMBIENT_TEMP(0x46, 1, "°م", "حرارة الجو");

    val command: String get() = "01" + "%02X".format(code)
}

object ObdCommands {
    val init = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
    const val READ_DTC = "03"
    const val CLEAR_DTC = "04"
    const val VOLTAGE = "ATRV"

    /** Whitelist: AT setup, Mode 01 reads, Mode 03 read DTC, Mode 04 clear (UI requires confirmation). */
    fun isAllowed(cmd: String): Boolean {
        val c = cmd.trim().uppercase().replace(" ", "")
        if (c in init || c == VOLTAGE || c == READ_DTC || c == CLEAR_DTC) return true
        return Regex("^01[0-9A-F]{2}$").matches(c)
    }
}
