package sa.moha.sonataos

import org.junit.Assert.*
import org.junit.Test
import sa.moha.sonataos.voice.VoiceCommand
import sa.moha.sonataos.voice.VoiceCommandParser as P

class VoiceParserTest {
    @Test fun home() = assertEquals(VoiceCommand.NavigateHome, P.parse("وديني البيت"))
    @Test fun work() = assertEquals(VoiceCommand.NavigateWork, P.parse("خذني الدوام"))
    @Test fun place() = assertEquals(VoiceCommand.Navigate("مطار الملك خالد"), P.parse("وديني مطار الملك خالد"))
    @Test fun call() = assertEquals(VoiceCommand.Call("ابوي"), P.parse("اتصل على أبوي"))
    @Test fun music() = assertEquals(VoiceCommand.PlayMusic, P.parse("شغل الأغاني"))
    @Test fun next() = assertEquals(VoiceCommand.NextTrack, P.parse("الأغنية الجاية"))
    @Test fun pause() = assertEquals(VoiceCommand.PauseMusic, P.parse("وقف الأغنية"))
    @Test fun obd() = assertEquals(VoiceCommand.ShowObd, P.parse("افتح الحساسات"))
    @Test fun park() = assertEquals(VoiceCommand.SaveParking, P.parse("احفظ الموقف"))
    @Test fun findPark() = assertEquals(VoiceCommand.FindParking, P.parse("وين وقفت"))
    @Test fun prayer() = assertEquals(VoiceCommand.NextPrayer, P.parse("متى يأذن"))
    @Test fun weather() = assertEquals(VoiceCommand.Weather, P.parse("كيف الجو"))
    @Test fun app() = assertEquals(VoiceCommand.OpenApp("سناب"), P.parse("افتح سناب"))
    @Test fun unknown() = assertTrue(P.parse("قصيدة عن البحر") is VoiceCommand.Unknown)
}
