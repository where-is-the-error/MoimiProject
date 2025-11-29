package com.moimiApp.moimi

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddScheduleActivity : BaseActivity() {

    private var selectedDate = ""
    private var selectedTime = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        setupDrawer()

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val etTitle = findViewById<TextInputEditText>(R.id.et_schedule_title)
        val tvDate = findViewById<TextView>(R.id.tv_input_date)
        val tvTime = findViewById<TextView>(R.id.tv_input_time)
        val etLocation = findViewById<TextInputEditText>(R.id.et_schedule_location)
        val btnSave = findViewById<Button>(R.id.btn_save_schedule)

        btnBack.setOnClickListener { finish() }

        // 날짜 선택
        tvDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = String.format("%d-%02d-%02d", year, month + 1, day)
                tvDate.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 시간 선택
        tvTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                tvTime.text = selectedTime
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        // 저장 버튼 클릭
        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val locationName = etLocation.text.toString()

            if (title.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. 서버 전송
            val request = AddScheduleRequest(selectedDate, selectedTime, title, locationName)

            // ✅ [수정됨] getAuthToken() 사용하여 실제 토큰 전송
            val token = getAuthToken()

            RetrofitClient.scheduleInstance.addSchedule(token, request)
                .enqueue(object : Callback<ScheduleResponse> {
                    override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@AddScheduleActivity, "저장 성공!", Toast.LENGTH_SHORT).show()

                            // 알림 예약 실행
                            scheduleAlarms(title, selectedDate, selectedTime)

                            // 위치 알림 등록
                            if (locationName.isNotEmpty()) {
                                registerLocationAlert(locationName)
                            }

                            // ✅ [추가됨] 목록 새로고침을 위한 성공 신호 보내기
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this@AddScheduleActivity, "저장 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                        Toast.makeText(this@AddScheduleActivity, "오류 발생", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    // (아래 알림/위치 관련 함수들은 그대로 유지)
    private fun scheduleAlarms(title: String, date: String, time: String) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val eventTime = Calendar.getInstance()
        try {
            eventTime.time = format.parse("$date $time")!!
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val alarm30min = eventTime.clone() as Calendar
        alarm30min.add(Calendar.MINUTE, -30)
        setAlarm(alarm30min.timeInMillis, "30분 전 알림: $title")

        val alarm1Week = eventTime.clone() as Calendar
        alarm1Week.add(Calendar.DAY_OF_YEAR, -7)
        setAlarm(alarm1Week.timeInMillis, "1주일 전 알림: $title")
    }

    private fun setAlarm(triggerTime: Long, message: String) {
        if (triggerTime < System.currentTimeMillis()) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("msg", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerLocationAlert(address: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                val lat = addresses[0].latitude
                val lng = addresses[0].longitude
                // TODO: Geofencing 등록 로직
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}