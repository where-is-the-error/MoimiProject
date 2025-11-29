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
    private val myToken = "Bearer 여기에_실제_토큰_입력"

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
            RetrofitClient.scheduleInstance.addSchedule(myToken, request)
                .enqueue(object : Callback<ScheduleResponse> {
                    override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@AddScheduleActivity, "저장 성공!", Toast.LENGTH_SHORT).show()

                            // ✅ [추가됨] 알림 예약 기능 실행
                            scheduleAlarms(title, selectedDate, selectedTime)

                            // ✅ [추가됨] 위치 알림 등록 (주소를 좌표로 변환)
                            if (locationName.isNotEmpty()) {
                                registerLocationAlert(locationName)
                            }

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

    // ⏰ [기능 1] 시간 알림 예약 (1주일 전, 30분 전)
    private fun scheduleAlarms(title: String, date: String, time: String) {
        // 날짜+시간 문자열을 Calendar 객체로 변환
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val eventTime = Calendar.getInstance()
        try {
            eventTime.time = format.parse("$date $time")!!
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // 1. 30분 전 알림 설정
        val alarm30min = eventTime.clone() as Calendar
        alarm30min.add(Calendar.MINUTE, -30)
        setAlarm(alarm30min.timeInMillis, "30분 전 알림: $title")

        // 2. 1주일 전 알림 설정
        val alarm1Week = eventTime.clone() as Calendar
        alarm1Week.add(Calendar.DAY_OF_YEAR, -7)
        setAlarm(alarm1Week.timeInMillis, "1주일 전 알림: $title")
    }

    // 실제 알람매니저에 등록하는 함수
    private fun setAlarm(triggerTime: Long, message: String) {
        if (triggerTime < System.currentTimeMillis()) return // 이미 지난 시간은 패스

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("msg", message) // 알림에 띄울 메시지 전달
        }

        // 고유 ID를 현재 시간으로 만들어서 여러 알림이 겹치지 않게 함
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 정확한 시간에 알림 울리기
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: SecurityException) {
            // 권한(SCHEDULE_EXACT_ALARM)이 필요한 경우 예외 처리
            Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 📍 [기능 2] 위치 도착 알림 (주소 -> 좌표 변환 후 등록)
    private fun registerLocationAlert(address: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            // 주소 이름으로 위도/경도 찾기 (최대 1개 결과)
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                val lat = addresses[0].latitude
                val lng = addresses[0].longitude

                // TODO: 여기서 GeofencingClient를 사용해 위치 감지 등록
                // (Geofencing 코드는 복잡해서 별도 설정이 필요하지만, 좌표는 이렇게 구합니다)
                // Log.d("Geofence", "좌표 발견: $lat, $lng - 알림 등록 준비 완료")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}