package kr.co.pincoin.paynotify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> handleSms(appContext, intent)
            Intent.ACTION_BATTERY_LOW -> notifyAsync { TelegramNotifier.send(appContext, "배터리 부족") }
            Intent.ACTION_BATTERY_OKAY -> notifyAsync { TelegramNotifier.send(appContext, "배터리 정상화") }
            Intent.ACTION_BOOT_COMPLETED -> {
                val mainActivity = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(mainActivity)
                notifyAsync { TelegramNotifier.send(appContext, "부팅 완료") }
            }
        }
    }

    private fun handleSms(appContext: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // 긴 문자는 여러 PDU 로 분할되므로 모든 조각의 본문을 이어붙인다.
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val phone = messages[0].originatingAddress ?: return

        val payment = parse(phone, body)
        notifyAsync {
            if (payment != null) {
                PaymentNotifier.send(appContext, payment)
            } else {
                TelegramNotifier.send(appContext, body.replace("[Web발신]", ""))
            }
        }
    }

    /**
     * 발신 번호별 은행 문자 포맷을 정규식으로 파싱한다.
     * 매칭되지 않으면 null (→ 텔레그램 폴백).
     */
    private fun parse(phone: String, body: String): Payment? = when (phone) {
        KOOKMIN -> Regex("""\[KB](.*)\s.*\s(.*)\s(.*)\s(.*)\s잔액(.*)""").find(body)?.let {
            val g = it.groupValues
            Payment("0", g[1], g[2], g[3], g[4], g[5])
        }

        NONGHUP -> Regex("""농협 (\D*)([,\d]+)원\s(\d\d/\d\d \d\d:\d\d) [\d-*]+ (.*) 잔액(.*)원""").find(body)?.let {
            val g = it.groupValues
            Payment("1", g[3], g[4], g[1], g[2], g[5])
        }

        SHINHAN -> Regex("""신한(.*)\s.*\s(.*)[ ]+(.*)\s잔액[ ]+(.*)\s+(.*)""").find(body)?.let {
            val g = it.groupValues
            Payment("2", g[1], g[5], g[2], g[3], g[4])
        }

        WOORI -> Regex("""우리 (.*)\s.*\s(.*) (.*)원\s(.*)\s잔액[ ]+(.*)원""").find(body)?.let {
            val g = it.groupValues
            Payment("3", g[1], g[4], g[2], g[3], g[5])
        }

        IBK -> Regex("""(\d\d\d\d/\d\d/\d\d \d\d:\d\d)\s(.*)[ ]+(.*)원\s잔액[ ]+(.*)원\s(.*)""").find(body)?.let {
            val g = it.groupValues
            Payment("4", g[1], g[5], g[2], g[3], g[4])
        }

        else -> null
    }

    /**
     * BroadcastReceiver 는 onReceive() 가 리턴하면 프로세스가 종료될 수 있다.
     * goAsync() 로 실행을 연장하고 네트워크 작업이 끝나면 finish() 로 마무리한다.
     */
    private fun notifyAsync(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val KOOKMIN = "16449999"
        private const val NONGHUP = "15882100"
        private const val SHINHAN = "15778000"
        private const val WOORI = "15885000"
        private const val IBK = "15662566"
    }
}
