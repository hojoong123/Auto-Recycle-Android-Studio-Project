package com.capstone.recyclehelper

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var binContainer: LinearLayout
    private lateinit var tvLastSync: TextView
    private lateinit var tvSystemStatus: TextView
    private lateinit var tvServerStatus: TextView
    private lateinit var tvInfoMessage: TextView
    private lateinit var btnRefresh: View
    private lateinit var btnInspection: View
    private lateinit var btnLogout: View
    private lateinit var deviceTabContainer: LinearLayout
    private var deviceList: List<DeviceResponse> = emptyList()
    private var selectedDeviceIndex: Int = 0
    private lateinit var tvAdminName: TextView

    private var currentDeviceId: Long = -1
    private val api = RetrofitClient.api

    private val authHeader: String
        get() = "Bearer " + (TokenStore.token ?: "")

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        createNotificationChannel()
        requestPostNotificationPermission()

        // ✅ LoginActivity에서 이미 토큰 받음. 바로 장치 로드.
        if (TokenStore.token.isNullOrEmpty()) {
            goToLogin(); return
        }
        tvServerStatus.text = "✅ 서버 연결 완료"
        tvServerStatus.setTextColor(Color.parseColor("#22C55E"))
        loadDevices()

        // ✅ STOMP 알림 구독
        TokenStore.adminId?.let { id ->
            StompManager.connect(id) { dto -> showSystemNotification(dto) }
        }
    }

    override fun onDestroy() {
        StompManager.disconnect()
        super.onDestroy()
    }

    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        binContainer = findViewById(R.id.binContainer)
        tvLastSync = findViewById(R.id.tvLastSync)
        tvSystemStatus = findViewById(R.id.tvSystemStatus)
        tvServerStatus = findViewById(R.id.tvServerStatus)
        tvInfoMessage = findViewById(R.id.tvInfoMessage)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnInspection = findViewById(R.id.btnInspection)

        swipeRefresh.setColorSchemeColors(Color.parseColor("#4ECCA3"))
        swipeRefresh.setOnRefreshListener { loadBins() }

        btnRefresh.setOnClickListener { loadBins() }

        // ✅ 점검 완료 → 서버에 실제로 전송
        btnInspection.setOnClickListener { sendInspectionDone() }

        btnLogout = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            StompManager.disconnect()
            TokenStore.clear()
            currentDeviceId = -1
            goToLogin()
        }
        deviceTabContainer = findViewById(R.id.deviceTabContainer)
        tvAdminName = findViewById(R.id.tvAdminName)
        val adminName = intent.getStringExtra("admin_name") ?: "관리자"
        tvAdminName.text = "👤 관리자: $adminName"
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent); finish()
    }

    private fun sendInspectionDone() {
        val req = InspectionDoneRequest(
            floor = TokenStore.floor,
            deviceId = if (currentDeviceId > 0) currentDeviceId else null,
            binId = null,
            message = "${TokenStore.floor ?: ""}층 점검 완료"
        )
        api.sendInspectionDone(req).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Snackbar.make(binContainer, "✅ 점검 완료 알림이 전송되었습니다.", Snackbar.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "전송 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@MainActivity, "전송 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadDevices() {
        api.getDevices(authHeader).enqueue(object : Callback<List<DeviceResponse>> {
            override fun onResponse(call: Call<List<DeviceResponse>>, response: Response<List<DeviceResponse>>) {
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    deviceList = response.body()!!
                    selectedDeviceIndex = 0
                    currentDeviceId = deviceList[0].id
                    buildDeviceTabs()
                    loadBins()
                } else {
                    tvServerStatus.text = "장치를 찾을 수 없습니다."
                }
            }
            override fun onFailure(call: Call<List<DeviceResponse>>, t: Throwable) {
                tvServerStatus.text = "장치 조회 실패"
                swipeRefresh.isRefreshing = false
            }
        })
    }

    private fun buildDeviceTabs() {
        deviceTabContainer.removeAllViews()
        for (i in deviceList.indices) {
            val tab = TextView(this)
            tab.text = "🗑️ 쓰레기통${i + 1}"
            tab.textSize = 14f
            tab.setPadding(40, 20, 40, 20)
            if (i == selectedDeviceIndex) {
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
                tab.setTextColor(Color.WHITE)
            } else {
                tab.setBackgroundResource(R.drawable.bg_tab_unselected)
                tab.setTextColor(Color.parseColor("#64748B"))
            }
            tab.setOnClickListener {
                selectedDeviceIndex = i
                currentDeviceId = deviceList[i].id
                buildDeviceTabs()
                loadBins()
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (i > 0) params.marginStart = 8
            tab.layoutParams = params
            deviceTabContainer.addView(tab)
        }
    }

    private fun loadBins() {
        if (currentDeviceId == -1L) return
        api.getBins(authHeader, currentDeviceId).enqueue(object : Callback<List<BinResponse>> {
            override fun onResponse(call: Call<List<BinResponse>>, response: Response<List<BinResponse>>) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    displayBins(response.body()!!)
                    updateSyncTime()
                    tvSystemStatus.text = "▪ 정상"
                }
            }
            override fun onFailure(call: Call<List<BinResponse>>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, "데이터 로드 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayBins(bins: List<BinResponse>) {
        binContainer.removeAllViews()
        val warningMessages = mutableListOf<String>()
        for (bin in bins) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_bin, binContainer, false)
            val tvBinIcon = view.findViewById<TextView>(R.id.tvBinIcon)
            val tvBinName = view.findViewById<TextView>(R.id.tvBinName)
            val tvSensorStatus = view.findViewById<TextView>(R.id.tvSensorStatus)
            val tvFillPercent = view.findViewById<TextView>(R.id.tvFillPercent)
            val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
            val tvWarning = view.findViewById<TextView>(R.id.tvWarning)
            val btnReset = view.findViewById<ImageButton>(R.id.btnReset)

            val typeCode = bin.trashTypeCode ?: "GENERAL"
            val percent = bin.fillPercent ?: 0
            val hasError = bin.errorFlag ?: false

            when (typeCode) {
                "PLASTIC" -> { tvBinIcon.text = "🥤"; tvBinName.text = "Plastic" }
                "CAN" -> { tvBinIcon.text = "🥫"; tvBinName.text = "Cans / Metal" }
                "GLASS" -> { tvBinIcon.text = "🍾"; tvBinName.text = "Glass" }
                "GENERAL" -> { tvBinIcon.text = "🗑️"; tvBinName.text = "General Waste" }
                else -> { tvBinIcon.text = "📦"; tvBinName.text = typeCode }
            }

            if (hasError) {
                tvSensorStatus.text = "센서 오류"
                tvSensorStatus.setTextColor(Color.parseColor("#EF4444"))
            } else {
                tvSensorStatus.text = "센서 정상 작동"
                tvSensorStatus.setTextColor(Color.parseColor("#94A3B8"))
            }

            tvFillPercent.text = "${percent}%"
            progressBar.progress = percent

            when {
                percent >= 80 -> {
                    tvFillPercent.setTextColor(Color.parseColor("#EF4444"))
                    progressBar.progressDrawable = getDrawable(R.drawable.progress_bar_red)
                    tvWarning.visibility = View.VISIBLE
                    tvWarning.text = "⚠ 경고 : 쓰레기통이 거의 다 찼습니다."
                    warningMessages.add("${tvBinName.text} 통이 ${percent}%로 거의 찼습니다.")
                }
                percent >= 60 -> {
                    tvFillPercent.setTextColor(Color.parseColor("#F59E0B"))
                    progressBar.progressDrawable = getDrawable(R.drawable.progress_bar_yellow)
                    tvWarning.visibility = View.GONE
                }
                else -> {
                    tvFillPercent.setTextColor(Color.parseColor("#22C55E"))
                    progressBar.progressDrawable = getDrawable(R.drawable.progress_bar_green)
                    tvWarning.visibility = View.GONE
                }
            }

            btnReset.setOnClickListener { resetBin(bin.id) }
            binContainer.addView(view)
        }

        if (warningMessages.isNotEmpty()) {
            tvInfoMessage.text = "안내 메시지 : ${warningMessages.joinToString("\n")}\n빈 통 센서 확인 해주세요."
        } else {
            tvInfoMessage.text = "안내 메시지 : 모든 통 상태가 양호합니다.\n정기 점검을 진행해주세요."
        }
    }

    private fun resetBin(binId: Long) {
        api.resetBin(authHeader, binId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Snackbar.make(binContainer, "✅ 통이 초기화되었습니다.", Snackbar.LENGTH_SHORT).show()
                    loadBins()
                } else {
                    Toast.makeText(this@MainActivity, "리셋 실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@MainActivity, "리셋 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateSyncTime() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvLastSync.text = "🕐 최근 동기화: ${sdf.format(Date())}"
    }

    // === 알림 채널 / 권한 / 표시 ===
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                "inspection_channel", "점검 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
            )
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @SuppressLint("MissingPermission")
    private fun showSystemNotification(dto: NotificationDto) {
        // ✅ Android 13+ 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            // 권한 없으면 그냥 Snackbar로 대체
            Snackbar.make(binContainer, "🔔 ${dto.title}", Snackbar.LENGTH_LONG).show()
            return
        }

        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, dto.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val noti = NotificationCompat.Builder(this, "inspection_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(dto.title)
            .setContentText(dto.message ?: "")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(this).notify(dto.id.toInt(), noti)
    }
}
