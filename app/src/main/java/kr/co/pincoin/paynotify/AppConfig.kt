package kr.co.pincoin.paynotify

import android.content.Context

/**
 * 원격 서버 전송/알림 설정을 런타임에 보관/변경한다 (SharedPreferences).
 * 저장된 값이 없으면 BuildConfig(빌드 디폴트) 값을 사용한다.
 * 판매용 빌드의 디폴트는 비어 있으며, 각 단말에서 설정 화면으로 입력한다.
 */
class AppConfig(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("paynotify_config", Context.MODE_PRIVATE)

    private fun get(key: String, default: String) = prefs.getString(key, default)!!
    private fun put(key: String, value: String) = prefs.edit().putString(key, value.trim()).apply()

    var url: String
        get() = get(K_URL, BuildConfig.PAYMENT_NOTIFY_URL)
        set(v) = put(K_URL, v)

    /** 인증 헤더명. 예: "Authorization", "X-API-Key". 비우면 인증 헤더 없음. */
    var authHeader: String
        get() = get(K_AUTH_HEADER, BuildConfig.PAYMENT_AUTH_HEADER)
        set(v) = put(K_AUTH_HEADER, v)

    /** 인증 스킴/접두어. 예: "Token", "Bearer", "Basic", 또는 빈값. */
    var authScheme: String
        get() = get(K_AUTH_SCHEME, BuildConfig.PAYMENT_AUTH_SCHEME)
        set(v) = put(K_AUTH_SCHEME, v)

    var token: String
        get() = get(K_TOKEN, BuildConfig.PAYMENT_NOTIFY_TOKEN)
        set(v) = put(K_TOKEN, v)

    /** "form" | "json" | "multipart" */
    var format: String
        get() = get(K_FORMAT, BuildConfig.PAYMENT_NOTIFY_FORMAT)
        set(v) = prefs.edit().putString(K_FORMAT, v.trim().lowercase()).apply()

    var fieldAccount: String
        get() = get(K_F_ACCOUNT, BuildConfig.PAYMENT_FIELD_ACCOUNT)
        set(v) = put(K_F_ACCOUNT, v)

    var fieldReceived: String
        get() = get(K_F_RECEIVED, BuildConfig.PAYMENT_FIELD_RECEIVED)
        set(v) = put(K_F_RECEIVED, v)

    var fieldName: String
        get() = get(K_F_NAME, BuildConfig.PAYMENT_FIELD_NAME)
        set(v) = put(K_F_NAME, v)

    var fieldMethod: String
        get() = get(K_F_METHOD, BuildConfig.PAYMENT_FIELD_METHOD)
        set(v) = put(K_F_METHOD, v)

    var fieldAmount: String
        get() = get(K_F_AMOUNT, BuildConfig.PAYMENT_FIELD_AMOUNT)
        set(v) = put(K_F_AMOUNT, v)

    var fieldBalance: String
        get() = get(K_F_BALANCE, BuildConfig.PAYMENT_FIELD_BALANCE)
        set(v) = put(K_F_BALANCE, v)

    var telegramBotToken: String
        get() = get(K_TG_TOKEN, BuildConfig.TELEGRAM_BOT_TOKEN)
        set(v) = put(K_TG_TOKEN, v)

    var telegramChannelId: String
        get() = get(K_TG_CHANNEL, BuildConfig.TELEGRAM_CHANNEL_ID)
        set(v) = put(K_TG_CHANNEL, v)

    companion object {
        private const val K_URL = "url"
        private const val K_AUTH_HEADER = "auth_header"
        private const val K_AUTH_SCHEME = "auth_scheme"
        private const val K_TOKEN = "token"
        private const val K_FORMAT = "format"
        private const val K_F_ACCOUNT = "field_account"
        private const val K_F_RECEIVED = "field_received"
        private const val K_F_NAME = "field_name"
        private const val K_F_METHOD = "field_method"
        private const val K_F_AMOUNT = "field_amount"
        private const val K_F_BALANCE = "field_balance"
        private const val K_TG_TOKEN = "telegram_bot_token"
        private const val K_TG_CHANNEL = "telegram_channel_id"

        val FORMATS = listOf("form", "json", "multipart")
        val FORMAT_LABELS = listOf(
            "form  (x-www-form-urlencoded)",
            "json  (application/json)",
            "multipart  (form-data)"
        )
    }
}
