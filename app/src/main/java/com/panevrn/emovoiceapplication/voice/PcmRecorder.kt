package com.panevrn.emovoiceapplication.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs

class PcmRecorder(private val context: Context) {
    private var isRecording = false
    private lateinit var audioRecord: AudioRecord
    private lateinit var pcmFile: File
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        pcmFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm")

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("PCM_RECORDER", "❌ Нет разрешения RECORD_AUDIO")
            return
        }

        // Запрашиваем аудио-фокус
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { }
                .build()
        )
        Log.d("PCM_DEBUG", "Аудио фокус: $result")

        audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .build()

        audioRecord.startRecording()
        Log.d("PCM_DEBUG", "Состояние записи: ${audioRecord.recordingState}")
        if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e("PCM_DEBUG", "‼️ AudioRecord не начал записывать!")
        }
        Log.d("PCM_RECORDER", "Старт записи. Файл: ${pcmFile.absolutePath}")
        isRecording = true

        Thread {
            try {
                Log.d("PCM_RECORDER", "Начат поток записи")
                FileOutputStream(pcmFile).use { outputStream ->
                    val buffer = ShortArray(bufferSize / 2)
                    while (isRecording) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        val maxAmplitude = buffer.take(read).maxOfOrNull { abs(it.toInt()) } ?: 0
                        Log.d("PCM_DEBUG", "Макс. амплитуда: $maxAmplitude")
                        if (read > 0) {
                            val byteBuffer = ByteArray(read * 2)
                            for (i in 0 until read) {
                                byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                                byteBuffer[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
                            }
                            outputStream.write(byteBuffer)
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun stopRecording(): File {
        isRecording = false
        audioRecord.stop()
        audioRecord.release()
        Log.d("PcmRecorder", "Файл записан: ${pcmFile.absolutePath}, размер: ${pcmFile.length()} байт")
        return pcmFile
    }
}