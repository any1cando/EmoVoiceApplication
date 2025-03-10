package com.panevrn.emovoiceapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.panevrn.emovoiceapplication.retrofit.RetrofitClient
import com.panevrn.emovoiceapplication.voice.PcmRecorder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var tvInfo: TextView
    private lateinit var recordButton: Button
    private lateinit var stopButton: Button
    private lateinit var recorder: PcmRecorder

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvInfo = findViewById(R.id.tvInfo)
        recordButton = findViewById(R.id.recordButton)
        stopButton = findViewById(R.id.stopButton)
        recorder = PcmRecorder(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        }

        recordButton.setOnClickListener {
            recorder.startRecording()
            Toast.makeText(this, "Запись началась", Toast.LENGTH_SHORT).show()
            recordButton.isEnabled = false
            stopButton.isEnabled = true
        }

        stopButton.setOnClickListener {
            val pcmFile = recorder.stopRecording()
            val wavFile = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.wav")

            convertPcmToWav(pcmFile, wavFile)

            Toast.makeText(this, "Файл сохранён: ${wavFile.absolutePath}", Toast.LENGTH_LONG).show()

            // 🔹 Отправляем файл на сервер
            sendFileToServer(wavFile)

            recordButton.isEnabled = true
            stopButton.isEnabled = false
        }

//        stopButton.setOnClickListener {
//            val pcmFile = recorder.stopRecording()
//            if (pcmFile.length() == 0L) {
//                Toast.makeText(this, "Ошибка: PCM-файл пустой!", Toast.LENGTH_LONG).show()
//                return@setOnClickListener
//            }
//
//            val wavFile = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.wav")
//
//            convertPcmToWav(pcmFile, wavFile)
//
//            Toast.makeText(this, "Файл сохранён: ${wavFile.absolutePath}", Toast.LENGTH_LONG).show()
//
//            sendFileToServer(wavFile)
//
//            recordButton.isEnabled = true
//            stopButton.isEnabled = false
//        }

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


    private fun sendFileToServer(audioFile: File) {
        val requestFile = RequestBody.create("audio/wav".toMediaTypeOrNull(), audioFile)
        val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

        RetrofitClient.apiService.uploadAudio(filePart).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        val responseBody = it.string()

                        // 🔹 Выводим JSON в лог для отладки
                        Log.d("SERVER_RESPONSE", "Ответ от сервера: $responseBody")

                        // 🔹 Парсим JSON и формируем строку для вывода
                        val formattedResponse = parseJsonToText(responseBody)

                        // 🔹 Обновляем `TextView` на главном потоке
                        runOnUiThread {
                            tvInfo.text = formattedResponse
                        }
                    }
                } else {
                    Log.e("SERVER_RESPONSE", "Ошибка сервера: ${response.code()} ${response.message()}")
                    runOnUiThread {
                        tvInfo.text = "Ошибка сервера"
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("SERVER_RESPONSE", "Ошибка соединения: ${t.message}")
                runOnUiThread {
                    tvInfo.text = "Ошибка соединения: ${t.message}"
                }
            }
        })
    }


    private fun parseJsonToText(jsonString: String): String {
        return try {
            val jsonObject = JSONObject(jsonString)

            // 🔹 Меняем названия ключей с английского на русский
            val emotionsMap = mapOf(
                "angry" to "Злость",
                "sad" to "Грусть",
                "neutral" to "Нейтральность",
                "positive" to "Радость"
            )

            // 🔹 Формируем строку в формате: "Радость: 0.1\nГрусть: 0.4\n..."
            val result = StringBuilder()
            for ((key, label) in emotionsMap) {
                if (jsonObject.has(key)) {
                    val value = jsonObject.getDouble(key)
                    result.append("$label: ${"%.3f".format(value)}\n")  // 🔹 Округляем до 3 знаков
                }
            }

            // 🔹 Если данных нет, выводим сообщение
            if (result.isEmpty()) "Данные отсутствуют" else result.toString()

        } catch (e: Exception) {
            Log.e("JSON_PARSE", "Ошибка парсинга JSON", e)
            "Ошибка обработки данных"
        }
    }





    private fun convertPcmToWav(pcmFile: File, wavFile: File) {
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

        Log.d("convertPcmToWav", "Конвертация завершена. Файл: ${wavFile.absolutePath}, размер: ${wavFile.length()} байт")
    }

}
