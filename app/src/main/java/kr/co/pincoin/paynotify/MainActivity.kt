package kr.co.pincoin.paynotify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Filter
import android.widget.Spinner
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var config: AppConfig

    private lateinit var etUrl: EditText
    private lateinit var etAuthHeader: AutoCompleteTextView
    private lateinit var etAuthScheme: AutoCompleteTextView
    private lateinit var etToken: EditText
    private lateinit var spinnerFormat: Spinner
    private lateinit var etFieldAccount: EditText
    private lateinit var etFieldReceived: EditText
    private lateinit var etFieldName: EditText
    private lateinit var etFieldMethod: EditText
    private lateinit var etFieldAmount: EditText
    private lateinit var etFieldBalance: EditText
    private lateinit var etTelegramToken: EditText
    private lateinit var etTelegramChannel: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 버전은 타이틀바 서브타이틀에 표시 (body 는 폼만)
        actionBar?.subtitle = getString(R.string.app_version)

        config = AppConfig(this)

        etUrl = findViewById(R.id.et_url)
        etAuthHeader = findViewById(R.id.et_auth_header)
        etAuthScheme = findViewById(R.id.et_auth_scheme)
        etToken = findViewById(R.id.et_token)
        spinnerFormat = findViewById(R.id.spinner_format)
        etFieldAccount = findViewById(R.id.et_field_account)
        etFieldReceived = findViewById(R.id.et_field_received)
        etFieldName = findViewById(R.id.et_field_name)
        etFieldMethod = findViewById(R.id.et_field_method)
        etFieldAmount = findViewById(R.id.et_field_amount)
        etFieldBalance = findViewById(R.id.et_field_balance)
        etTelegramToken = findViewById(R.id.et_telegram_token)
        etTelegramChannel = findViewById(R.id.et_telegram_channel)

        spinnerFormat.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            AppConfig.FORMAT_LABELS
        )

        // 프리셋 드롭다운 + 직접 입력 (custom 값 허용)
        setupDropdown(etAuthHeader, listOf("Authorization", "X-API-Key", "X-Auth-Token", "Api-Key"))
        setupDropdown(etAuthScheme, listOf("Token", "Bearer", "Basic"))

        // 헤더명이 비면(인증 없음) 스킴·토큰은 비활성화
        etAuthHeader.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, before: Int) {}
            override fun afterTextChanged(s: Editable?) = updateAuthFieldsEnabled()
        })

        loadConfig()
        updateAuthFieldsEnabled()

        findViewById<Button>(R.id.btn_save).setOnClickListener { saveConfig() }

        requestIgnoreBatteryOptimizations()
    }

    private fun loadConfig() {
        etUrl.setText(config.url)
        etAuthHeader.setText(config.authHeader)
        etAuthScheme.setText(config.authScheme)
        etToken.setText(config.token)
        val formatIndex = AppConfig.FORMATS.indexOf(config.format).coerceAtLeast(0)
        spinnerFormat.setSelection(formatIndex)
        etFieldAccount.setText(config.fieldAccount)
        etFieldReceived.setText(config.fieldReceived)
        etFieldName.setText(config.fieldName)
        etFieldMethod.setText(config.fieldMethod)
        etFieldAmount.setText(config.fieldAmount)
        etFieldBalance.setText(config.fieldBalance)
        etTelegramToken.setText(config.telegramBotToken)
        etTelegramChannel.setText(config.telegramChannelId)
    }

    private fun saveConfig() {
        config.url = etUrl.text.toString()
        config.authHeader = etAuthHeader.text.toString()
        config.authScheme = etAuthScheme.text.toString()
        config.token = etToken.text.toString()
        config.format = AppConfig.FORMATS[spinnerFormat.selectedItemPosition]
        config.fieldAccount = etFieldAccount.text.toString()
        config.fieldReceived = etFieldReceived.text.toString()
        config.fieldName = etFieldName.text.toString()
        config.fieldMethod = etFieldMethod.text.toString()
        config.fieldAmount = etFieldAmount.text.toString()
        config.fieldBalance = etFieldBalance.text.toString()
        config.telegramBotToken = etTelegramToken.text.toString()
        config.telegramChannelId = etTelegramChannel.text.toString()
        Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
    }

    /** 헤더명이 비어 있으면(인증 없음) 스킴·토큰 값을 비우고 필드를 비활성화한다. */
    private fun updateAuthFieldsEnabled() {
        val enabled = etAuthHeader.text.toString().isNotBlank()
        etAuthScheme.isEnabled = enabled
        etToken.isEnabled = enabled
        if (!enabled) {
            etAuthScheme.setText("")
            etToken.setText("")
        }
    }

    /**
     * 프리셋을 드롭다운으로 제공하되 직접 입력도 허용하는 필드. 탭하면 목록이 열린다.
     * 기본 ArrayAdapter 는 현재 입력값으로 목록을 필터링해 1개만 남을 수 있으므로,
     * 필터를 비활성화해 항상 전체 프리셋을 보여준다.
     */
    private fun setupDropdown(view: AutoCompleteTextView, options: List<String>) {
        val adapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_dropdown_item_1line, options
        ) {
            private val passthrough = object : Filter() {
                override fun performFiltering(constraint: CharSequence?) =
                    FilterResults().apply { values = options; count = options.size }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) =
                    notifyDataSetChanged()
            }

            override fun getFilter(): Filter = passthrough
        }
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
        view.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) view.showDropDown() }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            // 단말이 항상 충전 상태가 아니므로 Doze 로부터 백그라운드 수신을 지키려면 예외가 필요.
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, 0)
        }
    }
}
