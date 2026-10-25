package sa.moha.sonataos.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Wraps Android SpeechRecognizer (ar-SA). Depends on a recognition service on the device (e.g. Google app). */
class VoiceInput(private val ctx: Context) {
    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun listen(onPartial: (String) -> Unit, onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!available) { onError("ما فيه خدمة تعرّف صوت على الجهاز (ثبّت تطبيق Google)"); return }
        sr?.destroy()
        sr = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(b: Bundle) { onResult(b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: "") }
                override fun onPartialResults(b: Bundle) { b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial) }
                override fun onError(e: Int) { onError(if (e == SpeechRecognizer.ERROR_NO_MATCH || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) "ما فهمت عليك، عيد مرة ثانية" else "خطأ في الصوت ($e)") }
                override fun onReadyForSpeech(p: Bundle?) {}; override fun onBeginningOfSpeech() {}; override fun onRmsChanged(r: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}; override fun onEndOfSpeech() {}; override fun onEvent(t: Int, p: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
        }
    }

    fun speak(text: String) {
        val t = tts
        if (t == null) tts = TextToSpeech(ctx) { if (it == TextToSpeech.SUCCESS) { tts?.language = Locale("ar", "SA"); tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "s") } }
        else t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "s")
    }

    fun release() { sr?.destroy(); tts?.shutdown() }
}

/**
 * Optional LLM hook. Uses an OpenAI-compatible /chat/completions endpoint the USER configures in settings.
 * No key is embedded in the app. Only used for phrases the local parser doesn't understand.
 */
object LlmHook {
    suspend fun ask(endpoint: String, key: String, model: String, text: String): String? = withContext(Dispatchers.IO) {
        if (endpoint.isBlank() || key.isBlank()) return@withContext null
        runCatching {
            val c = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true; connectTimeout = 10000; readTimeout = 20000
                setRequestProperty("Content-Type", "application/json"); setRequestProperty("Authorization", "Bearer $key")
            }
            val body = JSONObject().put("model", model.ifBlank { "gpt-4o-mini" }).put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", "أنت مساعد سيارة مختصر يتكلم باللهجة السعودية. جاوب بجملة أو جملتين، والسواق يسوق."))
                .put(JSONObject().put("role", "user").put("content", text)))
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            JSONObject(c.inputStream.bufferedReader().readText()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }.getOrNull()
    }
}
