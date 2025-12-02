package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.CalendarView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        // 일정 추가 후 돌아왔을 때 새로고침
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("ScheduleActivity", "일정 추가 후 돌아옴. 데이터 새로고침.")
                refreshData()
            }
        }

        setupDrawer()

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val tvSelectedDate = findViewById<TextView>(R.id.tv_selected_date)
        val rvSchedule = findViewById<RecyclerView>(R.id.rv_schedule_list)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add_schedule)
        swipeRefresh = findViewById(R.id.swipe_refresh_layout)

        fabAdd.setOnClickListener {
            showAddOrJoinDialog()
        }

        rvSchedule.layoutManager = LinearLayoutManager(this)

        adapter = ScheduleAdapter(
            scheduleList,
            onItemClick = { item ->
                val bottomSheet = ScheduleDetailBottomSheet.newInstance(
                    item.title,
                    currentLoadedDate,
                    item.time,
                    item.location,
                    item.id,
                    item.inviteCode ?: ""
                )
                bottomSheet.show(supportFragmentManager, ScheduleDetailBottomSheet.TAG)
            },
            onItemLongClick = { item ->
                showDeleteDialog(item)
            }
        )
        rvSchedule.adapter = adapter

        // 달력 날짜 선택 리스너
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val monthStr = (month + 1).toString().padStart(2, '0')
            val dayStr = dayOfMonth.toString().padStart(2, '0')

            tvSelectedDate.text = "${month + 1}월 ${dayOfMonth}일 일정"
            currentLoadedDate = "$year-$monthStr-$dayStr"

            Log.d("ScheduleActivity", "달력 선택됨: $currentLoadedDate")
            refreshData()
        }

        // 초기 날짜(오늘) 설정
        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR)
        val month = (today.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = today.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')

        currentLoadedDate = "$year-$month-$day"
        tvSelectedDate.text = "${today.get(Calendar.MONTH) + 1}월 ${today.get(Calendar.DAY_OF_MONTH)}일 일정"

        Log.d("ScheduleActivity", "초기 날짜 설정: $currentLoadedDate")
        refreshData()

        swipeRefresh.setOnRefreshListener {
            Log.d("ScheduleActivity", "새로고침 요청")
            refreshData()
        }
    }

    private fun showAddOrJoinDialog() {
        val options = arrayOf("새 일정 만들기", "초대 코드로 참여하기")
        AlertDialog.Builder(this)
            .setTitle("일정 추가")
            .setItems(options) { _, which ->
                if (which == 0) {
                    val intent = Intent(this, AddScheduleActivity::class.java)
                    resultLauncher.launch(intent)
                } else {
                    showJoinByCodeDialog()
                }
            }
            .show()
    }

    private fun showJoinByCodeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_schedule, null)
        val etCode = dialogView.findViewById<EditText>(R.id.et_invite_code)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel_join)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm_join)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code.length == 6) {
                joinSchedule(code)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "6자리 코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun joinSchedule(code: String) {
        val token = getAuthToken()
        RetrofitClient.scheduleInstance.joinScheduleByCode(token, JoinByCodeRequest(code))
            .enqueue(object : Callback<JoinScheduleResponse> {
                override fun onResponse(call: Call<JoinScheduleResponse>, response: Response<JoinScheduleResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ScheduleActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                        refreshData()
                    } else {
                        Toast.makeText(this@ScheduleActivity, response.body()?.message ?: "참여 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<JoinScheduleResponse>, t: Throwable) {
                    Toast.makeText(this@ScheduleActivity, "통신 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun refreshData() {
        fetchSchedules(currentLoadedDate)
    }

    private fun fetchSchedules(date: String) {
        val token = getAuthToken()
        if (token.isEmpty()) {
            Log.e("ScheduleActivity", "토큰 없음. 로그인 필요.")
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            swipeRefresh.isRefreshing = false
            return
        }

        Log.d("ScheduleActivity", "서버로 일정 요청: $date")

        RetrofitClient.scheduleInstance.getSchedules(token, date)
            .enqueue(object : Callback<ScheduleResponse> {
                override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                    swipeRefresh.isRefreshing = false

                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverData = response.body()!!.schedules
                        scheduleList.clear()

                        if (!serverData.isNullOrEmpty()) {
                            Log.d("ScheduleActivity", "일정 로드 성공: ${serverData.size}개")
                            scheduleList.addAll(serverData)
                        } else {
                            Log.d("ScheduleActivity", "일정 로드 성공: 0개 (일정 없음)")
                            Toast.makeText(this@ScheduleActivity, "해당 날짜에 일정이 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        Log.e("ScheduleActivity", "서버 응답 실패: ${response.code()} ${response.message()}")
                        Toast.makeText(this@ScheduleActivity, "일정 로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                    swipeRefresh.isRefreshing = false
                    Log.e("ScheduleActivity", "네트워크 오류", t)
                    Toast.makeText(this@ScheduleActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showDeleteDialog(item: ScheduleItem) {
        AlertDialog.Builder(this)
            .setTitle("일정 삭제")
            .setMessage("'${item.title}' 일정을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> Toast.makeText(this, "삭제 기능 준비중", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("취소", null)
            .show()
    }
}