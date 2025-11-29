package com.moimiApp.moimi

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class RestaurantActivity : BaseActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvLocation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant)

        setupDrawer()

        tvName = findViewById(R.id.tv_res_name)
        tvTime = findViewById(R.id.tv_res_time)
        tvLocation = findViewById(R.id.tv_res_location)
        val btnReserve = findViewById<Button>(R.id.btn_confirm_reservation)

        // 예약하기 버튼
        btnReserve.setOnClickListener {
            val title = tvName.text.toString() + " 예약"
            val location = tvLocation.text.toString()
            val timeText = tvTime.text.toString() // 예: "9/22 17:00"

            // 1. 모임 생성 API용 데이터 (YYYY-MM-DD HH:MM:00)
            val meetingTime = getFormattedDateTime(timeText)
            val meetingRequest = CreateMeetingRequest(
                title = title,
                location = location,
                dateTime = meetingTime,
                reservationRequired = true
            )

            // 2. 일정 추가 API용 데이터 (날짜와 시간을 분리)
            val (datePart, timePart) = parseDateAndTime(timeText)
            val scheduleRequest = AddScheduleRequest(
                date = datePart, // "2025-09-22"
                time = timePart, // "17:00"
                title = title,
                location = location
            )

            // 3. API 호출 시작 (순차 실행)
            createReservationAndSchedule(meetingRequest, scheduleRequest)
        }
    }

    // [핵심] 예약(모임) 생성 후 -> 성공하면 -> 내 일정에도 추가하는 함수
    private fun createReservationAndSchedule(
        meetingReq: CreateMeetingRequest,
        scheduleReq: AddScheduleRequest
    ) {
        val token = getAuthToken() // ✅ 실제 토큰 가져오기

        // [단계 1] 모임 생성 (예약)
        RetrofitClient.instance.createMeeting(token, meetingReq)
            .enqueue(object : Callback<MeetingCreationResponse> {
                override fun onResponse(call: Call<MeetingCreationResponse>, response: Response<MeetingCreationResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {

                        // [단계 2] 예약 성공 시 -> 내 일정(스케줄) 추가 API 호출
                        saveToMySchedule(scheduleReq)

                    } else {
                        Toast.makeText(this@RestaurantActivity, "예약 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<MeetingCreationResponse>, t: Throwable) {
                    Toast.makeText(this@RestaurantActivity, "서버 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // [단계 2] 내 일정(앱 캘린더)에 저장하는 함수
    private fun saveToMySchedule(request: AddScheduleRequest) {
        val token = getAuthToken()

        RetrofitClient.scheduleInstance.addSchedule(token, request)
            .enqueue(object : Callback<ScheduleResponse> {
                override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                    // 일정 추가까지 완료되면 종료
                    Toast.makeText(this@RestaurantActivity, "✅ 예약 완료 및 내 일정에 추가되었습니다!", Toast.LENGTH_LONG).show()
                    finish() // 화면 종료 -> 메인으로 이동
                }

                override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                    // 모임은 만들어졌는데 일정 추가만 실패한 경우
                    Toast.makeText(this@RestaurantActivity, "예약은 되었으나 일정 추가에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    // 날짜 포맷 변환기 (모임 생성용: YYYY-MM-DD HH:MM:00)
    private fun getFormattedDateTime(timeText: String): String {
        val (date, time) = parseDateAndTime(timeText)
        return "$date $time:00"
    }

    // 날짜/시간 분리기 (일정 추가용: "2025-09-22", "17:00")
    private fun parseDateAndTime(timeText: String): Pair<String, String> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // "9/22 17:00"을 공백으로 분리 -> ["9/22", "17:00"]
        val parts = timeText.split(" ")

        val datePart = parts[0] // "9/22"
        val timePart = parts[1] // "17:00"

        val dates = datePart.split("/") // ["9", "22"]
        val month = dates[0].padStart(2, '0') // "09"
        val day = dates[1].padStart(2, '0')   // "22"

        val fullDate = "$currentYear-$month-$day" // "2025-09-22"

        return Pair(fullDate, timePart)
    }
}