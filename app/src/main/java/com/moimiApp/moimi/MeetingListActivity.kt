package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MeetingListActivity : BaseActivity() {

    private val meetingList = mutableListOf<MeetingItem>()
    private lateinit var adapter: MeetingListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting_list) // XML 파일명 확인

        setupDrawer() // 햄버거 메뉴 연결

        val rvMeetingList = findViewById<RecyclerView>(R.id.rv_meeting_list_container)
        rvMeetingList.layoutManager = LinearLayoutManager(this)

        // [수정됨] 클릭 시 LocationShareActivity로 이동
        adapter = MeetingListAdapter(meetingList) { clickedItem ->
            val intent = Intent(this, LocationShareActivity::class.java)
            // LocationShareActivity에서 받을 이름(Key)과 맞춰줍니다.
            intent.putExtra("meetingId", clickedItem.id)
            intent.putExtra("meetingTitle", clickedItem.title)
            startActivity(intent)
        }
        rvMeetingList.adapter = adapter

        // 데이터 불러오기
        fetchMeetings()
    }

    private fun fetchMeetings() {
        val token = getAuthToken()

        RetrofitClient.instance.getMeetings(token)
            .enqueue(object : Callback<MeetingListResponse> {
                override fun onResponse(call: Call<MeetingListResponse>, response: Response<MeetingListResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val meetings = response.body()!!.meetings

                        meetingList.clear()
                        meetingList.addAll(meetings)

                        adapter.notifyDataSetChanged()

                        if (meetings.isEmpty()) {
                            Toast.makeText(this@MeetingListActivity, "예정된 모임이 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MeetingListActivity, "목록 로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MeetingListResponse>, t: Throwable) {
                    Toast.makeText(this@MeetingListActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }
}