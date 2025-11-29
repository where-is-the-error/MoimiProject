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
        setContentView(R.layout.activity_meeting_list)

        setupDrawer()

        val rvMeetingList = findViewById<RecyclerView>(R.id.rv_meeting_list_container)
        rvMeetingList.layoutManager = LinearLayoutManager(this)

        adapter = MeetingListAdapter(meetingList) { clickedItem ->
            val intent = Intent(this, LocationShareActivity::class.java)
            intent.putExtra("meetingId", clickedItem.id)
            intent.putExtra("meetingTitle", clickedItem.title)
            startActivity(intent)
        }
        rvMeetingList.adapter = adapter

        fetchMeetings()
    }

    private fun fetchMeetings() {
        val token = getAuthToken()

        RetrofitClient.instance.getMeetings(token)
            .enqueue(object : Callback<MeetingListResponse> {
                override fun onResponse(call: Call<MeetingListResponse>, response: Response<MeetingListResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        meetingList.clear()

                        // [수정] 빌드 오류 해결: 안전하게 null 체크 후 추가
                        response.body()?.meetings?.let {
                            meetingList.addAll(it)
                        }

                        adapter.notifyDataSetChanged()

                        if (meetingList.isEmpty()) {
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