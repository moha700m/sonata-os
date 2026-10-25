package sa.moha.sonataos

import org.junit.Assert.*
import org.junit.Test
import sa.moha.sonataos.obd.*

class ObdParserTest {
    @Test fun rpm() = assertEquals(1726.0, ObdParser.parse(Pid.RPM, "41 0C 1A F8\r\r>")!!, 0.01)
    @Test fun rpmNoSpacesWithEcho() = assertEquals(1726.0, ObdParser.parse(Pid.RPM, "010C\r410C1AF8\r>")!!, 0.01)
    @Test fun speed() = assertEquals(80.0, ObdParser.parse(Pid.SPEED, "41 0D 50")!!, 0.01)
    @Test fun coolant() = assertEquals(90.0, ObdParser.parse(Pid.COOLANT_TEMP, "41 05 82")!!, 0.01)
    @Test fun load() = assertEquals(100.0, ObdParser.parse(Pid.ENGINE_LOAD, "41 04 FF")!!, 0.01)
    @Test fun fuelTrim() = assertEquals(0.0, ObdParser.parse(Pid.STFT_B1, "41 06 80")!!, 0.01)
    @Test fun fuelTrimNegative() = assertEquals(-100.0, ObdParser.parse(Pid.LTFT_B1, "41 07 00")!!, 0.01)
    @Test fun maf() = assertEquals(5.12, ObdParser.parse(Pid.MAF, "41 10 02 00")!!, 0.001)
    @Test fun moduleVoltage() = assertEquals(13.9, ObdParser.parse(Pid.MODULE_VOLTAGE, "41 42 36 4C")!!, 0.01)
    @Test fun fuelLevel() = assertEquals(50.2, ObdParser.parse(Pid.FUEL_LEVEL, "41 2F 80")!!, 0.1)
    @Test fun noData() = assertNull(ObdParser.parse(Pid.RPM, "NO DATA\r>"))
    @Test fun searching() = assertEquals(80.0, ObdParser.parse(Pid.SPEED, "SEARCHING...\r41 0D 50\r>")!!, 0.01)
    @Test fun truncated() = assertNull(ObdParser.parse(Pid.RPM, "41 0C 1A"))
    @Test fun multiEcuTakesFirst() = assertEquals(80.0, ObdParser.parse(Pid.SPEED, "41 0D 50\r41 0D 51")!!, 0.01)
    @Test fun voltage() = assertEquals(12.6, ObdParser.parseVoltage("12.6V")!!, 0.001)
    @Test fun dtcCan() = assertEquals(listOf("P0171", "P0420"), ObdParser.parseDtcs("43 02 01 71 04 20"))
    @Test fun dtcLegacy() = assertEquals(listOf("P0133", "C0035"), ObdParser.parseDtcs("43 01 33 40 35 00 00"))
    @Test fun dtcNone() = assertEquals(emptyList<String>(), ObdParser.parseDtcs("43 00"))
    @Test fun dtcTypes() {
        assertEquals("U0100", ObdParser.decodeDtc("C100"))
        assertEquals("B0001", ObdParser.decodeDtc("8001"))
    }
    @Test fun readOnlyWhitelist() {
        assertTrue(ObdCommands.isAllowed("010C")); assertTrue(ObdCommands.isAllowed("03")); assertTrue(ObdCommands.isAllowed("ATRV"))
        assertFalse(ObdCommands.isAllowed("2E F1 90 00")) // UDS write
        assertFalse(ObdCommands.isAllowed("08 01")) // Mode 08 actuator test
        assertFalse(ObdCommands.isAllowed("ATSH7E0"))
        assertFalse(ObdCommands.isAllowed("3101FF00"))
    }
    @Test fun dtcArabic() {
        assertTrue(DtcDescriptions.describe("P0420").contains("الكتلايزر"))
        assertTrue(DtcDescriptions.describe("P1326").contains("هيونداي"))
    }
}
