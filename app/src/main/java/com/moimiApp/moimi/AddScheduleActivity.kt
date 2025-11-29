package com.moimiApp.moimi

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val locationName = result.data?.getStringExtra("locationName")
            if (locationName != null) {
                etLocation.setText(locationName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        setupDrawer()

        // 뷰 연결
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

        btnBack.setOnClickListener { finish() }

        // 장소 검색 연결
        etLocation.isFocusable = false
        etLocation.setOnClickListener {
            val intent = Intent(this, SearchLocationActivity::class.java)
            searchLauncher.launch(intent)
        }

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

        // 저장 버튼
        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val locationName = etLocation.text.toString()

            if (title.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "필수 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. 선택된 유형 확인
            val type = if (rgType.checkedRadioButtonId == R.id.rb_checklist) "CHECKLIST" else "MEETING"

            val request = AddScheduleRequest(
                date = selectedDate,
                time = selectedTime,
                title = title,
                location = locationName,
                type = type // 서버로 유형 전송
            )
            val token = getAuthToken()

            RetrofitClient.scheduleInstance.addSchedule(token, request)
                .enqueue(object : Callback<ScheduleResponse> {
                    override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@AddScheduleActivity, "저장 성공!", Toast.LENGTH_SHORT).show()

                            // 2. 알림 예약 실행
                            scheduleCustomAlarms(title, selectedDate, selectedTime)

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

    private fun scheduleCustomAlarms(title: String, date: String, time: String) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val eventTime = Calendar.getInstance()
        try {
            eventTime.time = format.parse("$date $time")!!
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val eventMillis = eventTime.timeInMillis

        // 체크된 항목에 따라 알림 등록
        if (cb1h.isChecked) setAlarm(eventMillis - 60 * 60 * 1000, "1시간 전 알림: $title")
        if (cb2h.isChecked) setAlarm(eventMillis - 2 * 60 * 60 * 1000, "2시간 전 알림: $title")
        if (cb1d.isChecked) setAlarm(eventMillis - 24 * 60 * 60 * 1000, "1일 전 알림: $title")
        if (cb7d.isChecked) setAlarm(eventMillis - 7 * 24 * 60 * 60 * 1000, "7일 전 알림: $title")
    }

    private fun setAlarm(triggerTime: Long, message: String) {
        if (triggerTime < System.currentTimeMillis()) return // 이미 지난 시간은 알림 안 함

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("msg", message)
        }

        // RequestCode를 유니크하게 만들기 위해 시간을 사용 (알림 겹침 방지)
        val uniqueId = (System.currentTimeMillis() % 100000).toInt() + (Math.random() * 1000).toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            uniqueId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: SecurityException) {
            // 안드로이드 12 이상에서 정확한 알림 권한 필요할 수 있음
            Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
}