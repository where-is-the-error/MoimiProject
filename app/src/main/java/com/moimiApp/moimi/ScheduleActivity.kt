package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class ScheduleActivity : BaseActivity() {

    private val scheduleList = mutableListOf<ScheduleItem>()
    private lateinit var adapter: ScheduleAdapter

    private var currentLoadedDate: String = ""
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    // ❌ [삭제] 하드코딩된 토큰 변수 삭제 (BaseActivity 기능을 쓸 것임)
    // private val myToken = "Bearer 여기에_실제_토큰_입력"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        // 0. 리절트 런처 (일정 추가 후 새로고침)
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                fetchSchedules(currentLoadedDate)
                Toast.makeText(this, "일정 목록을 새로고침했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 1. 공통 메뉴 연결
        setupDrawer()

        // 2. 뷰 연결
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val tvSelectedDate = findViewById<TextView>(R.id.tv_selected_date)
        val rvSchedule = findViewById<RecyclerView>(R.id.rv_schedule_list)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add_schedule)

        // 3. 리사이클러뷰 설정
        rvSchedule.layoutManager = LinearLayoutManager(this)

        adapter = ScheduleAdapter(
            scheduleList,
            onItemClick = { item ->
                Toast.makeText(this, "${item.title} 상세 보기 준비", Toast.LENGTH_SHORT).show()
            },
            onItemLongClick = { item ->
                showDeleteDialog(item)
            }
        )
        rvSchedule.adapter = adapter

        // 4. 달력 클릭 이벤트
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            tvSelectedDate.text = "${month + 1}월 ${dayOfMonth}일 일정"
            val dateString = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth)

            currentLoadedDate = dateString
            fetchSchedules(dateString)
        }

        // 5. 초기 화면 설정
        val today = Calendar.getInstance()
        val todayString = String.format("%d-%02d-%02d",
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH)
        )
        currentLoadedDate = todayString
        fetchSchedules(todayString)

        // 6. 일정 추가 버튼
        fabAdd.setOnClickListener {
            val intent = Intent(this, AddScheduleActivity::class.java)
            resultLauncher.launch(intent)
        }
    }

    // 일정 삭제 확인 팝업
    private fun showDeleteDialog(item: ScheduleItem) {
        AlertDialog.Builder(this)
            .setTitle("일정 삭제 확인")
            .setMessage("'${item.title}' 일정을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                Toast.makeText(this, "API 구현 후 삭제됩니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 서버에서 일정 가져오기
    private fun fetchSchedules(date: String) {

        // ✅ [수정됨] BaseActivity의 getAuthToken() 사용하여 진짜 토큰 가져오기
        val token = getAuthToken()

        RetrofitClient.scheduleInstance.getSchedules(token, date)
            .enqueue(object : Callback<ScheduleResponse> {
                override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverData = response.body()!!.schedules
                        scheduleList.clear()

                        if (!serverData.isNullOrEmpty()) {
                            for (item in serverData) {
                                scheduleList.add(
                                    ScheduleItem(item.time, item.title, item.location)
                                )
                            }
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@ScheduleActivity, "일정 로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                    Toast.makeText(this@ScheduleActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                    Log.e("ScheduleActivity", "Error: ${t.message}")
                }
            })
    }
}