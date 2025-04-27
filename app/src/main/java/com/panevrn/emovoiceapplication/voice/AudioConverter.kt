package com.panevrn.emovoiceapplication.voice

import android.util.Log
import java.io.File
import java.io.FileOutputStream

object AudioConverter {

    fun convertPcmToWav(pcmFile: File, wavFile: File) {
        val sampleRate = 16000
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * (bitsPerSample / 8)

        val pcmData = pcmFile.readBytes()
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36

        val header = ByteArray(44)

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        for (i in 0..3) {
            header[4 + i] = ((totalDataLen shr (i * 8)) and 0xFF).toByte()
        }

        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // "fmt " subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        header[16] = 16
        header[20] = 1
        header[22] = channels.toByte()
        header[24] = (sampleRate and 0xFF).toByte()
        header[25] = ((sampleRate shr 8) and 0xFF).toByte()
        header[26] = ((sampleRate shr 16) and 0xFF).toByte()
        header[27] = ((sampleRate shr 24) and 0xFF).toByte()

        header[28] = (byteRate and 0xFF).toByte()
        header[29] = ((byteRate shr 8) and 0xFF).toByte()
        header[30] = ((byteRate shr 16) and 0xFF).toByte()
        header[31] = ((byteRate shr 24) and 0xFF).toByte()

        header[32] = (channels * (bitsPerSample / 8)).toByte()
        header[34] = bitsPerSample.toByte()

        // "data" subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        for (i in 0..3) {
            header[40 + i] = ((totalAudioLen shr (i * 8)) and 0xFF).toByte()
        }

        FileOutputStream(wavFile).use { fos ->
            fos.write(header)
            fos.write(pcmData)
        }

        Log.d("AudioConverter", "Конвертация завершена. Файл: ${wavFile.absolutePath}, размер: ${wavFile.length()} байт")
    }
}
