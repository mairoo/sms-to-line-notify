package kr.co.pincoin.paynotify

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * 파싱된 입금 정보. 어느 은행에서 왔는지(bankCode)와 파싱 결과를 담는다.
 */
data class Payment(
    val bankCode: String,
    val received: String,
    val name: String,
    val method: String,
    val amount: String,
    val balance: String,
)

/**
 * 파싱된 입금 정보를 원격 서버로 전달한다.
 * 전송 포맷(json/multipart/form)과 필드명은 BuildConfig 설정으로 제어한다.
 */
object PaymentNotifier {
    private const val TAG = "PaymentNotifier"

    suspend fun send(payment: Payment) = withContext(Dispatchers.IO) {
        // 설정된 필드명 -> 값. 전송 순서를 위해 삽입 순서를 유지한다.
        val fields = linkedMapOf(
            BuildConfig.PAYMENT_FIELD_ACCOUNT to payment.bankCode,
            BuildConfig.PAYMENT_FIELD_RECEIVED to payment.received,
            BuildConfig.PAYMENT_FIELD_NAME to payment.name,
            BuildConfig.PAYMENT_FIELD_METHOD to payment.method,
            BuildConfig.PAYMENT_FIELD_AMOUNT to payment.amount,
            BuildConfig.PAYMENT_FIELD_BALANCE to payment.balance,
        )

        val format = BuildConfig.PAYMENT_NOTIFY_FORMAT.trim().lowercase()

        var con: HttpURLConnection? = null
        try {
            con = (URL(BuildConfig.PAYMENT_NOTIFY_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Authorization", "Token ${BuildConfig.PAYMENT_NOTIFY_TOKEN}")
                doOutput = true
            }

            when (format) {
                "json" -> sendJson(con, fields)
                "multipart", "form-data" -> sendMultipart(con, fields)
                else -> sendUrlEncoded(con, fields) // "form" / "urlencoded"
            }

            val code = con.responseCode
            if (code !in 200..299) {
                Log.e(TAG, "failed to send PAYMENT notify: HTTP $code")
            } else {
                Log.d(TAG, "PAYMENT notify sent ($format)")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "failed to connect PAYMENT notify", ex)
        } finally {
            con?.disconnect()
        }
    }

    // application/x-www-form-urlencoded : key=value&key=value (URL 인코딩)
    private fun sendUrlEncoded(con: HttpURLConnection, fields: Map<String, String>) {
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val body = fields.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        writeBody(con, body.toByteArray(StandardCharsets.UTF_8))
    }

    // application/json : {"key":"value", ...}
    private fun sendJson(con: HttpURLConnection, fields: Map<String, String>) {
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        val payload = JSONObject()
        fields.forEach { (k, v) -> payload.put(k, v) }
        writeBody(con, payload.toString().toByteArray(StandardCharsets.UTF_8))
    }

    // multipart/form-data : 각 필드를 boundary 로 구분
    private fun sendMultipart(con: HttpURLConnection, fields: Map<String, String>) {
        val boundary = "----BroadcastSms" + UUID.randomUUID().toString().replace("-", "")
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        val body = buildString {
            fields.forEach { (k, v) ->
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"").append(k).append("\"\r\n\r\n")
                append(v).append("\r\n")
            }
            append("--").append(boundary).append("--\r\n")
        }
        writeBody(con, body.toByteArray(StandardCharsets.UTF_8))
    }

    private fun writeBody(con: HttpURLConnection, bytes: ByteArray) {
        con.setFixedLengthStreamingMode(bytes.size)
        con.outputStream.use { it.write(bytes) }
    }
}
