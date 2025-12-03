package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
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
import com.google.gson.Gson
import com.google.gson.JsonObject
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

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) refreshData()
        }

        setupDrawer()

        val calendarView = findViewById<android.widget.CalendarView>(R.id.calendarView)
        val tvSelectedDate = findViewById<TextView>(R.id.tv_selected_date)
        val rvSchedule = findViewById<RecyclerView>(R.id.rv_schedule_list)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add_schedule)
        swipeRefresh = findViewById(R.id.swipe_refresh_layout)

        // ✅ [+] 버튼 클릭 시 다이얼로그 띄우기
        fabAdd.setOnClickListener { showAddOrJoinDialog() }

        rvSchedule.layoutManager = LinearLayoutManager(this)
        adapter = ScheduleAdapter(
            scheduleList,
            onItemClick = { item ->
                val bottomSheet = ScheduleDetailBottomSheet.newInstance(
                    item.title, currentLoadedDate, item.time, item.location, item.id, item.inviteCode ?: ""
                )
                bottomSheet.show(supportFragmentManager, ScheduleDetailBottomSheet.TAG)
            },
            onSettingsClick = { item ->
                showManageDialog(item)
            }
        )
        rvSchedule.adapter = adapter

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val monthStr = (month + 1).toString().padStart(2, '0')
            val dayStr = dayOfMonth.toString().padStart(2, '0')
            tvSelectedDate.text = "${month + 1}월 ${dayOfMonth}일 일정"
            currentLoadedDate = "$year-$monthStr-$dayStr"
            refreshData()
        }

        val today = Calendar.getInstance()
        val monthStr = (today.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val dayStr = today.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        currentLoadedDate = "${today.get(Calendar.YEAR)}-$monthStr-$dayStr"

        refreshData()

        swipeRefresh.setOnRefreshListener { refreshData() }
    }

    // ✅ [기능 1] 생성 vs 참여 선택 다이얼로그
    private fun showAddOrJoinDialog() {
        val options = arrayOf("새 일정 만들기", "초대 코드로 참여하기")
        AlertDialog.Builder(this)
            .setTitle("일정 추가")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // 일정 만들기
                        val intent = Intent(this, AddScheduleActivity::class.java)
                        resultLauncher.launch(intent)
                    }
                    1 -> { // 코드로 참여
                        showJoinByCodeDialog()
                    }
                }
            }
            .show()
    }

    // ✅ [기능 2] 코드 입력 다이얼로그
    private fun showJoinByCodeDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "6자리 숫자 코드 입력"
        input.gravity = android.view.Gravity.CENTER
        input.setPadding(50, 50, 50, 50)

        AlertDialog.Builder(this)
            .setTitle("초대 코드 입력")
            .setView(input)
            .setPositiveButton("참여") { _, _ ->
                val code = input.text.toString().trim()
                if (code.length == 6) {
                    joinScheduleByCode(code)
                } else {
                    Toast.makeText(this, "코드를 올바르게 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ✅ [기능 3] API 호출로 참여하기
    private fun joinScheduleByCode(code: String) {
        val token = getAuthToken()
        val request = JoinByCodeRequest(code)

        RetrofitClient.scheduleInstance.joinScheduleByCode(token, request)
            .enqueue(object : Callback<JoinScheduleResponse> {
                override fun onResponse(call: Call<JoinScheduleResponse>, response: Response<JoinScheduleResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val msg = response.body()?.message ?: "일정에 참여했습니다."
                        Toast.makeText(this@ScheduleActivity, msg, Toast.LENGTH_SHORT).show()
                        refreshData()
                    } else {
                        val errorMsg = try {
                            val errorBody = response.errorBody()?.string()
                            val json = Gson().fromJson(errorBody, JsonObject::class.java)
                            json.get("message").asString
                        } catch (e: Exception) {
                            "참여 실패 (코드를 확인하세요)"
                        }
                        Toast.makeText(this@ScheduleActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<JoinScheduleResponse>, t: Throwable) {
                    Toast.makeText(this@ScheduleActivity, "서버 통신 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showManageDialog(item: ScheduleItem) {
        val options = arrayOf("일정 수정", "멤버 관리", "일정 삭제")
        AlertDialog.Builder(this)
            .setTitle("'${item.title}' 관리")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(item)
                    1 -> showMemberManageDialog(item)
                    2 -> deleteSchedule(item.id)
                }
            }
            .show()
    }

    private fun showEditDialog(item: ScheduleItem) {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val inputTitle = EditText(this).apply { hint = "제목"; setText(item.title) }
        val inputLocation = EditText(this).apply { hint = "장소"; setText(item.location) }

        layout.addView(inputTitle)
        layout.addView(inputLocation)

        AlertDialog.Builder(this)
            .setTitle("일정 수정")
            .setView(layout)
            .setPositiveButton("수정") { _, _ ->
                val newTitle = inputTitle.text.toString()
                val newLoc = inputLocation.text.toString()
                updateSchedule(item.id, newTitle, newLoc)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateSchedule(id: String, title: String, location: String) {
        val token = getAuthToken()
        val req = UpdateScheduleRequest(null, null, title, location)

        RetrofitClient.scheduleInstance.updateSchedule(token, id, req).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ScheduleActivity, "수정 완료", Toast.LENGTH_SHORT).show()
                    refreshData()
                }
            }
            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {}
        })
    }

    private fun showMemberManageDialog(item: ScheduleItem) {
        if (item.members.isEmpty()) {
            Toast.makeText(this, "참여자가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val memberNames = item.members.map { it.name }.toTypedArray<CharSequence>()

        AlertDialog.Builder(this)
            .setTitle("멤버 관리 (터치하여 강퇴)")
            .setItems(memberNames) { _, which ->
                val targetMember = item.members[which]
                if (targetMember.id == item.leaderId) {
                    Toast.makeText(this, "본인은 강퇴할 수 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    confirmKickMember(item.id, targetMember)
                }
            }
            .show()
    }

    private fun confirmKickMember(scheduleId: String, member: MemberInfo) {
        AlertDialog.Builder(this)
            .setTitle("멤버 강퇴")
            .setMessage("${member.name}님을 내보내시겠습니까?")
            .setPositiveButton("내보내기") { _, _ ->
                kickMember(scheduleId, member.id)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun kickMember(scheduleId: String, userId: String) {
        val token = getAuthToken()
        RetrofitClient.scheduleInstance.kickMember(token, scheduleId, userId).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ScheduleActivity, "멤버를 내보냈습니다.", Toast.LENGTH_SHORT).show()
                    refreshData()
                }
            }
            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {}
        })
    }

    private fun deleteSchedule(id: String) {
        val token = getAuthToken()
        RetrofitClient.scheduleInstance.deleteSchedule(token, id).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ScheduleActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    refreshData()
                }
            }
            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {}
        })
    }

    private fun refreshData() {
        fetchSchedules(currentLoadedDate)
    }

    private fun fetchSchedules(date: String) {
        val token = getAuthToken()
        if(token.isEmpty()) return
        RetrofitClient.scheduleInstance.getSchedules(token, date).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                swipeRefresh.isRefreshing = false
                if(response.isSuccessful && response.body()?.success == true) {
                    scheduleList.clear()
                    response.body()?.schedules?.let { scheduleList.addAll(it) }
                    adapter.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) { swipeRefresh.isRefreshing = false }
        })
    }
}