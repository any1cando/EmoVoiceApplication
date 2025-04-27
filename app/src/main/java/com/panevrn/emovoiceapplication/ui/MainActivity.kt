package com.panevrn.emovoiceapplication.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.activity.viewModels
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.panevrn.emovoiceapplication.databinding.ActivityMainBinding
import com.panevrn.emovoiceapplication.dto.EmotionResponse
import com.panevrn.emovoiceapplication.viewmodel.EmotionViewModel
import com.panevrn.emovoiceapplication.voice.AudioConverter
import com.panevrn.emovoiceapplication.voice.PcmRecorder
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var recorder: PcmRecorder
    private val viewModel: EmotionViewModel by viewModels()

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val recordButton = binding.recordButton
        val stopButton = binding.stopButton
        val tvInfo = binding.tvInfo
        val pbLoad = binding.pbLoad

        recorder = PcmRecorder(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION) // 1
        }

        recordButton.setOnClickListener {
            recorder.startRecording()
            Toast.makeText(this, "Запись началась", Toast.LENGTH_SHORT).show()
            recordButton.isEnabled = false
            stopButton.isEnabled = true
        }

        stopButton.setOnClickListener {
            val pcmFile = recorder.stopRecording()
            val wavFile = File(
                applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "test.wav"
            )

            AudioConverter.convertPcmToWav(pcmFile, wavFile)

            Toast.makeText(this, "Файл сохранён: ${wavFile.absolutePath}", Toast.LENGTH_LONG).show()

            pbLoad.visibility = View.VISIBLE
            viewModel.analyzeEmotion(wavFile)

            recordButton.isEnabled = true
            stopButton.isEnabled = false
        }

        observeViewModel(tvInfo, pbLoad)
    }


    private fun observeViewModel(tvInfo: TextView, pbLoad: ProgressBar) {
        viewModel.isLoading.observe(this) { isLoading ->
            pbLoad.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.emotionData.observe(this) { emotionResponse ->
            if (emotionResponse != null) {
                tvInfo.text = formatEmotionText(emotionResponse)
            } else {
                binding.tvInfo.text = "Ошибка обработки данных"
            }
        }
    }


    private fun formatEmotionText(emotionResponse: EmotionResponse): String {
        return """
            Радость: ${"%.3f".format(emotionResponse.positive)}
            Грусть: ${"%.3f".format(emotionResponse.sad)}
            Нейтральность: ${"%.3f".format(emotionResponse.neutral)}
            Злость: ${"%.3f".format(emotionResponse.angry)}
        """.trimIndent()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!permissionToRecordAccepted) {
                Toast.makeText(this, "Разрешение на запись отклонено!", Toast.LENGTH_SHORT).show()
            }
        }
    }

}