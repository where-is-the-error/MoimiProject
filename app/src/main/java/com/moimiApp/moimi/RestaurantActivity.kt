package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract // 캘린더 연동용 import
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat // 날짜 포맷용 import
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RestaurantActivity : BaseActivity() {

    private val myToken = "Bearer 여기에_실제_토큰_입력"

    // TextView에서 데이터를 가져오기 위해 필드로 선언
    private lateinit var tvName: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvLocation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant)

        setupDrawer()

        // 2. 화면 요소 연결
        tvName = findViewById(R.id.tv_res_name)
        tvTime = findViewById(R.id.tv_res_time)
        tvLocation = findViewById(R.id.tv_res_location)
        val btnReserve = findViewById<Button>(R.id.btn_confirm_reservation)

        // 3. 예약하기 버튼 클릭 이벤트 (API 호출 및 캘린더 연동 로직)
        btnReserve.setOnClickListener {
            val timeText = tvTime.text.toString() // 예: "9/22 17:00"

            // 1. 서버 포맷으로 변환 (예: 2025-09-22 17:00:00)
            val formattedDateTime = getFormattedDateTime(timeText)

            // 2. 서버 요청 DTO 생성
            val request = CreateMeetingRequest(
                title = tvName.text.toString() + " 예약",
                location = tvLocation.text.toString(),
                dateTime = formattedDateTime,
                reservationRequired = true
            )

            // 3. API 호출
            createReservation(request)
        }
    }

    // [추가] 텍스트뷰의 날짜를 서버 포맷(YYYY-MM-DD HH:MM:SS)으로 변환하는 함수
    private fun getFormattedDateTime(timeText: String): String {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // "9/22 17:00" -> "09-22 17:00" 변환
        val parts = timeText.split(" ")
        val dateParts = parts[0].split("/")

        // 월과 일이 한 자리수일 때 앞에 0을 붙임 (padStart)
        val monthPart = dateParts[0].padStart(2, '0')
        val dayPart = dateParts[1].padStart(2, '0')
        val timePart = parts[1]

        // 최종 포맷: YYYY-MM-DD HH:MM:00
        return "$currentYear-$monthPart-$dayPart $timePart:00"
    }

    // [추가] 예약(모임 생성) API 호출 함수
    private fun createReservation(request: CreateMeetingRequest) {

        RetrofitClient.instance.createMeeting(myToken, request)
            .enqueue(object : Callback<MeetingCreationResponse> {
                override fun onResponse(
                    call: Call<MeetingCreationResponse>,
                    response: Response<MeetingCreationResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@RestaurantActivity, "✅ 예약이 완료되었습니다!", Toast.LENGTH_LONG).show()

                        // ⭐ 예약 성공 시 캘린더 연동 함수 호출 ⭐
                        addEventToCalendar(
                            request.title,
                            request.location,
                            request.dateTime
                        )

                        finish()
                    } else {
                        Toast.makeText(this@RestaurantActivity, "예약 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MeetingCreationResponse>, t: Throwable) {
                    Toast.makeText(this@RestaurantActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // [추가] 캘린더에 일정 추가하는 함수
    private fun addEventToCalendar(title: String, location: String, dateTimeString: String) {

        // 1. 서버 포맷("YYYY-MM-DD HH:MM:SS")을 Date 객체로 변환
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val eventDate: Date = try {
            formatter.parse(dateTimeString) ?: return
        } catch (e: Exception) {
            Toast.makeText(this, "캘린더 시간 변환 오류", Toast.LENGTH_SHORT).show()
            return
        }

        val startMillis = eventDate.time
        val endMillis = startMillis + (2 * 60 * 60 * 1000) // 2시간 후로 종료 시간 설정 (임시)

        // 2. 캘린더 인텐트 생성
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            .putExtra(CalendarContract.Events.DESCRIPTION, "Moimi 앱을 통해 예약되었습니다.")
            .putExtra(CalendarContract.Events.ALL_DAY, false)

        // 3. 캘린더 앱 실행 (Manifest에 권한 등록 필수)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "캘린더 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}