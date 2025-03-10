package com.panevrn.emovoiceapplication.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording() {
        outputFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.wav")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC) // Используем микрофон
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP) // Формат 3GP (лучше для аудио)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // Кодек AMR-NB (лучше для голоса)
            setAudioSamplingRate(16000) // 16kHz
            setAudioChannels(1) // Моно
            setOutputFile(outputFile?.absolutePath)

            prepare()
            start()
        }
    }

    fun stopRecording(): File? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return outputFile
    }
}
