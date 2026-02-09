package com.jayanth.voicetotext

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var resultText: EditText
    private lateinit var wordCountText: TextView
    private lateinit var clearText: TextView
    private lateinit var convertButton: Button

    private val WORD_LIMIT = 500
    private var confirmedText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val homeLayout = findViewById<View>(R.id.homeLayout)
        val converterLayout = findViewById<View>(R.id.converterLayout)
        val startButton = findViewById<Button>(R.id.startButton)

        resultText = findViewById(R.id.resultText)
        wordCountText = findViewById(R.id.wordCountText)
        clearText = findViewById(R.id.clearText)
        convertButton = findViewById(R.id.convertButton)

        startButton.setOnClickListener {
            homeLayout.visibility = View.GONE
            converterLayout.visibility = View.VISIBLE
        }

        convertButton.setOnClickListener {
            checkPermissionAndStart()
        }

        clearText.setOnClickListener {
            confirmedText = ""
            resultText.setText("")
            wordCountText.text = "0 / $WORD_LIMIT"
        }
    }

    /* ---------- Permission ---------- */

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
        } else {
            startSpeechSession()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startSpeechSession()
        }
    }

    /* ---------- Speech Session (ONE PHRASE) ---------- */

    private fun startSpeechSession() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {

            // Free-form dictation (best for natural speech)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            // Force English (India) for better accent matching
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")

            // Tell Android this is dictation, not commands
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)

            // Ask for multiple interpretations (we pick best)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }


        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                speechRecognizer.destroy()
            }

            override fun onResults(results: Bundle?) {

                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: return

// Pick the longest result (usually most accurate sentence)
                val text = matches.maxByOrNull { it.length } ?: return


                confirmedText = (confirmedText + " " + text).trim()
                updateUI()
                speechRecognizer.destroy()
            }

            override fun onPartialResults(results: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    /* ---------- UI ---------- */

    private fun updateUI() {
        val words = confirmedText
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        if (words.size >= WORD_LIMIT) {
            resultText.setText(words.take(WORD_LIMIT).joinToString(" "))
            wordCountText.text = "Limit reached!"
            return
        }

        resultText.setText(confirmedText)
        resultText.setSelection(resultText.text.length)
        wordCountText.text = "${words.size} / $WORD_LIMIT"
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            speechRecognizer.destroy()
        } catch (_: Exception) {}
    }
}
