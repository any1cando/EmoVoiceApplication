package com.panevrn.emovoiceapplication.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.*

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
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord.startRecording()
        isRecording = true

        Thread {
            try {
                FileOutputStream(pcmFile).use { outputStream ->
                    val buffer = ShortArray(bufferSize / 2) // Используем ShortArray для точных данных
                    while (isRecording) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
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
