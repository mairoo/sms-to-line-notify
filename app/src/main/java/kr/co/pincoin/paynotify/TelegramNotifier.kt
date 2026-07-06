package kr.co.pincoin.paynotify

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * 은행 문자 이외의 SMS, 배터리/부팅 상태 등을 텔레그램 채널로 전달한다.
 */
object TelegramNotifier {
    private const val TAG = "TelegramNotifier"

    suspend fun send(context: Context, message: String) = withContext(Dispatchers.IO) {
        val config = AppConfig(context)
        val botToken = config.telegramBotToken.trim()
        val channelId = config.telegramChannelId.trim()
        if (botToken.isEmpty() || channelId.isEmpty()) {
            Log.d(TAG, "텔레그램 미설정 — 전송 건너뜀")
            return@withContext
        }

        var con: HttpURLConnection? = null
        try {
            val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
            con = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("chat_id", channelId)
                put("text", message)
                put("parse_mode", "HTML")
            }

            val bytes = payload.toString().toByteArray(StandardCharsets.UTF_8)
            con.setFixedLengthStreamingMode(bytes.size)
            con.outputStream.use { it.write(bytes) }

            val code = con.responseCode
            if (code != 200) {
                val error = con.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "텔레그램 메시지 전송 실패: $error")
            } else {
                Log.d(TAG, "텔레그램 메시지 전송 성공")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "텔레그램 API 연결 실패", ex)
        } finally {
            con?.disconnect()
        }
    }
}
