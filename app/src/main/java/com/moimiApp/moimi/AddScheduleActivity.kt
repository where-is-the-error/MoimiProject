package com.moimiApp.moimi

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

    private lateinit var etLocation: TextInputEditText
    private lateinit var rgType: RadioGroup

    // 알림 체크박스들
    private lateinit var cb1h: CheckBox
    private lateinit var cb2h: CheckBox
    private lateinit var cb1d: CheckBox
    private lateinit var cb7d: CheckBox

    // 장소 검색 결과 처리
    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val locationName = result.data?.getStringExtra("locationName")
            etLocation.setText(locationName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        // BaseActivity의 기능을 쓰되, 여기서는 커스텀 툴바를 직접 제어
        // setupDrawer() // 필요 시 사용

        // 1. 뷰 초기화
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val etTitle = findViewById<TextInputEditText>(R.id.et_schedule_title)
        val tvDate = findViewById<TextView>(R.id.tv_input_date)
        val tvTime = findViewById<TextView>(R.id.tv_input_time)
        val btnSave = findViewById<Button>(R.id.btn_save_schedule)

        etLocation = findViewById(R.id.et_schedule_location)
        rgType = findViewById(R.id.rg_schedule_type)

        cb1h = findViewById(R.id.cb_1hour)
        cb2h = findViewById(R.id.cb_2hour)
        cb1d = findViewById(R.id.cb_1day)
        cb7d = findViewById(R.id.cb_7day)

        // 2. 뒤로가기 버튼 기능 적용 ✅
        btnBack.setOnClickListener { finish() }

        // 3. 장소 검색 (직접 입력 방지 & 클릭 시 이동)
        etLocation.isFocusable = false
        etLocation.setOnClickListener {
            val intent = Intent(this, SearchLocationActivity::class.java)
            searchLauncher.launch(intent)
        }

        // 4. 날짜 선택 다이얼로그
        tvDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                // 월은 0부터 시작하므로 +1
                selectedDate = String.format("%d-%02d-%02d", year, month + 1, day)
                tvDate.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 5. 시간 선택 다이얼로그
        tvTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                tvTime.text = selectedTime
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        // 6. 저장 버튼 클릭
        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val locationName = etLocation.text.toString()

            // 필수 입력 체크
            if (title.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "필수 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 일정 유형 (모임 or 체크리스트)
            val type = if (rgType.checkedRadioButtonId == R.id.rb_checklist) "CHECKLIST" else "MEETING"

            val request = AddScheduleRequest(
                date = selectedDate,
                time = selectedTime,
                title = title,
                location = locationName,
                type = type
            )
            val token = getAuthToken()

            // 서버 전송
            RetrofitClient.scheduleInstance.addSchedule(token, request)
                .enqueue(object : Callback<ScheduleResponse> {
                    override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@AddScheduleActivity, "저장 성공!", Toast.LENGTH_SHORT).show()

                            // 알람 예약 로직 호출
                            scheduleCustomAlarms(title, selectedDate, selectedTime)

                            // 초대 코드 및 ID 받기
                            val scheduleId = response.body()?.scheduleId ?: ""
                            val inviteCode = response.body()?.inviteCode ?: ""

                            // 상세 정보 바텀시트 띄우기
                            val bottomSheet = ScheduleDetailBottomSheet.newInstance(
                                title = title,
                                date = selectedDate,
                                time = selectedTime,
                                location = locationName,
                                scheduleId = scheduleId,
                                inviteCode = inviteCode
                            )

                            // 바텀시트 닫힐 때 화면 종료 및 결과 반환
                            bottomSheet.onDismissListener = {
                                setResult(RESULT_OK)
                                finish()
                            }
                            bottomSheet.show(supportFragmentManager, ScheduleDetailBottomSheet.TAG)

                        } else {
                            Toast.makeText(this@AddScheduleActivity, "저장 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                        Log.e("AddSchedule", "Error", t)
                        Toast.makeText(this@AddScheduleActivity, "오류 발생", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    // 알람 예약 로직
    private fun scheduleCustomAlarms(title: String, date: String, time: String) {
        val dateTimeString = "$date $time"
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val scheduleTime: Calendar = Calendar.getInstance()

        try {
            format.parse(dateTimeString)?.let {
                scheduleTime.time = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val scheduleMillis = scheduleTime.timeInMillis

        // 체크박스 상태에 따라 알람 예약
        if (cb1h.isChecked) {
            setAlarm(scheduleMillis - 3600 * 1000, "1시간 전: $title")
        }
        if (cb2h.isChecked) {
            setAlarm(scheduleMillis - 2 * 3600 * 1000, "2시간 전: $title")
        }
        if (cb1d.isChecked) {
            setAlarm(scheduleMillis - 24 * 3600 * 1000, "1일 전: $title")
        }
        if (cb7d.isChecked) {
            setAlarm(scheduleMillis - 7 * 24 * 3600 * 1000, "7일 전: $title")
        }
    }

    private fun setAlarm(triggerTime: Long, message: String) {
        if (triggerTime < System.currentTimeMillis()) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ScheduleAlarmReceiver::class.java).apply {
            putExtra("title", "모이미 일정 알림")
            putExtra("message", message)
        }

        val requestCode = (triggerTime / 1000).toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("AddSchedule", "알람 설정 완료: $message")
        } catch (e: SecurityException) {
            Log.e("AddSchedule", "알람 권한 오류", e)
            Toast.makeText(this, "알람 설정 권한이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}