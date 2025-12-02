package com.moimiApp.moimi

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
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
import java.util.Calendar

class AddScheduleActivity : BaseActivity() {
    // ... (변수 선언 기존과 동일)
    private var selectedDate = ""
    private var selectedTime = ""
    private lateinit var etLocation: TextInputEditText
    private lateinit var rgType: RadioGroup
    private lateinit var cb1h: CheckBox
    private lateinit var cb2h: CheckBox
    private lateinit var cb1d: CheckBox
    private lateinit var cb7d: CheckBox

    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val locationName = result.data?.getStringExtra("locationName")
            etLocation.setText(locationName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        setupDrawer()

        // 뷰 초기화
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

        etLocation.isFocusable = false
        etLocation.setOnClickListener {
            val intent = Intent(this, SearchLocationActivity::class.java)
            searchLauncher.launch(intent)
        }

        tvDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = String.format("%d-%02d-%02d", year, month + 1, day)
                tvDate.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        tvTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                tvTime.text = selectedTime
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val locationName = etLocation.text.toString()

            if (title.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "필수 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = if (rgType.checkedRadioButtonId == R.id.rb_checklist) "CHECKLIST" else "MEETING"

            val request = AddScheduleRequest(
                date = selectedDate,
                time = selectedTime,
                title = title,
                location = locationName,
                type = type
            )
            val token = getAuthToken()

            RetrofitClient.scheduleInstance.addSchedule(token, request)
                .enqueue(object : Callback<ScheduleResponse> {
                    override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@AddScheduleActivity, "저장 성공!", Toast.LENGTH_SHORT).show()
                            scheduleCustomAlarms(title, selectedDate, selectedTime)

                            // ⭐ 이 부분이 중요합니다. newInstance 인자가 6개여야 합니다.
                            val scheduleId = response.body()?.scheduleId ?: ""
                            val inviteCode = response.body()?.inviteCode ?: ""

                            val bottomSheet = ScheduleDetailBottomSheet.newInstance(
                                title = title,
                                date = selectedDate,
                                time = selectedTime,
                                location = locationName,
                                scheduleId = scheduleId,
                                inviteCode = inviteCode
                            )

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

    // scheduleCustomAlarms, setAlarm 함수는 기존과 동일
    private fun scheduleCustomAlarms(title: String, date: String, time: String) { /*...*/ }
    private fun setAlarm(triggerTime: Long, message: String) { /*...*/ }
}