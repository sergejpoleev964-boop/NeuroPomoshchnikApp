package ru.neuropomoshchnik.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceController(
    context: Context,
    private val status: (String) -> Unit,
    private val recognized: (String) -> Unit,
    private val finished: () -> Unit,
    private val voicesReady: (List<String>) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var ready = false
    private var listening = false
    private var released = false

    init {
        val appContext = context.applicationContext

        tts = TextToSpeech(appContext) { result ->
            if (released) return@TextToSpeech

            ready = result == TextToSpeech.SUCCESS
            if (ready) {
                val languageResult = tts?.setLanguage(Locale("ru", "RU"))
                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    status("Русский язык озвучивания недоступен")
                } else {
                    val available = russianVoices()
                    voicesReady(available)
                    status("Озвучивание готово")
                }
            } else {
                status("Ошибка запуска синтезатора речи")
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    status("Слушаю вас…")
                }

                override fun onBeginningOfSpeech() {
                    status("Распознаю речь…")
                }

                override fun onRmsChanged(value: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    listening = false
                    status("Обрабатываю ответ…")
                }

                override fun onError(error: Int) {
                    listening = false
                    if (released) return

                    status("Ошибка распознавания: $error")
                    if (error != SpeechRecognizer.ERROR_CLIENT) {
                        mainHandler.postDelayed({
                            if (!released) finished()
                        }, 1000L)
                    }
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()

                    if (text.isNotBlank()) {
                        recognized(text)
                    } else {
                        mainHandler.postDelayed({
                            if (!released) finished()
                        }, 500L)
                    }
                }

                override fun onPartialResults(results: Bundle?) {}
                override fun onEvent(type: Int, params: Bundle?) {}
            })
        } else {
            status("Распознавание речи недоступно")
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                mainHandler.post { if (!released) status("Говорю…") }
            }

            override fun onDone(id: String?) {
                mainHandler.post { if (!released) finished() }
            }

            override fun onError(id: String?) {
                mainHandler.post {
                    if (!released) {
                        status("Ошибка озвучивания")
                        finished()
                    }
                }
            }
        })
    }

    fun listen() {
        if (released) return
        val engine = recognizer ?: run {
            status("Нет службы распознавания речи")
            return
        }
        if (listening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        try {
            listening = true
            engine.startListening(intent)
        } catch (e: Exception) {
            listening = false
            status("Не удалось включить микрофон: ${e.message}")
            mainHandler.postDelayed({
                if (!released) finished()
            }, 1000L)
        }
    }

    fun speak(text: String, voiceName: String? = null) {
        val engine = tts
        if (released || text.isBlank()) return
        if (!ready || engine == null) {
            status("Синтезатор речи ещё не готов")
            finished()
            return
        }

        engine.stop()

        if (!voiceName.isNullOrBlank()) {
            engine.voices
                ?.firstOrNull {
                    it.name == voiceName && it.locale.language == "ru"
                }
                ?.let { engine.voice = it }
        }

        engine.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "neuro_answer"
        )
    }

    fun russianVoices(): List<String> {
        return tts?.voices
            ?.filter { it.locale.language == "ru" }
            ?.map { it.name }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
    }

    fun stop() {
        listening = false
        recognizer?.cancel()
        tts?.stop()
        mainHandler.removeCallbacksAndMessages(null)
    }

    fun release() {
        released = true
        listening = false
        mainHandler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        tts?.shutdown()
        tts = null
    }
}
