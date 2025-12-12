package com.moimiApp.moimi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.moimiApp.moimi.databinding.ActivityNotificationBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private val notiList = mutableListOf<NotificationItem>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar("알림 센터") // BaseActivity 기능

        // 전체 삭제 버튼 클릭 리스너
        binding.btnDeleteAllRead.setOnClickListener {
            showDeleteAllDialog()
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notiList)
        binding.rvNotifications.adapter = adapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val token = getAuthToken()
        RetrofitClient.notificationInstance.getNotifications(token)
            .enqueue(object : Callback<NotificationResponse> {
                override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        notiList.clear()
                        response.body()?.notifications?.let {
                            // 안 읽은 알림이 맨 위로 오도록 정렬 (서버에서 정렬하지만, 한 번 더 확인)
                            val sortedList = it.sortedBy { item -> item.is_read }
                            notiList.addAll(sortedList)
                        }
                        updateEmptyView()
                        adapter.notifyDataSetChanged()
                    }
                }
                override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                    Toast.makeText(this@NotificationActivity, "알림 로드 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateEmptyView() {
        binding.tvEmptyNotification.visibility = if (notiList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("알림 삭제")
            .setMessage("읽은 알림을 모두 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteAllReadNotifications()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteAllReadNotifications() {
        val token = getAuthToken()
        RetrofitClient.notificationInstance.deleteAllRead(token).enqueue(object : Callback<CommonResponse> {
            override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@NotificationActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    fetchNotifications() // 목록 새로고침
                }
            }
            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {}
        })
    }

    // ✅ [신규] 채팅 요청 수락 API 호출
    private fun acceptChatRequest(roomId: String, requesterName: String) {
        val token = getAuthToken()
        val userId = prefsManager.getUserId() ?: return
        // 상태를 'attended'로 변경 요청
        val request = UpdateParticipantStatusRequest(userId, "attended")

        RetrofitClient.instance.updateParticipantStatus(token, roomId, request)
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@NotificationActivity, "${requesterName}님과의 대화가 시작됩니다.", Toast.LENGTH_SHORT).show()
                        // 채팅방으로 이동
                        val intent = Intent(this@NotificationActivity, ChatRoomActivity::class.java)
                        intent.putExtra("roomId", roomId)
                        intent.putExtra("roomTitle", "${requesterName}님과의 대화")
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@NotificationActivity, "채팅 요청 수락 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    Toast.makeText(this@NotificationActivity, "서버 통신 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // 어댑터 클래스
    inner class NotificationAdapter(private val items: List<NotificationItem>) : RecyclerView.Adapter<NotificationAdapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val layoutRoot: View = itemView.findViewById(R.id.layout_notification_root)
            val tvMsg: TextView = itemView.findViewById(R.id.tv_noti_message)
            val btnMarkRead: TextView = itemView.findViewById(R.id.btn_mark_read)
            val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_noti)

            val layoutActions: View = itemView.findViewById(R.id.layout_actions)
            val btnAccept: Button = itemView.findViewById(R.id.btn_accept)
            val btnDecline: Button = itemView.findViewById(R.id.btn_decline)

            fun bind(item: NotificationItem) {
                tvMsg.text = item.message

                // 읽음 상태 UI
                if (item.is_read) {
                    layoutRoot.setBackgroundColor(Color.parseColor("#F0F0F0"))
                    tvMsg.setTextColor(Color.parseColor("#888888"))
                    btnMarkRead.visibility = View.GONE
                } else {
                    layoutRoot.setBackgroundResource(R.drawable.bg_input_rounded)
                    tvMsg.setTextColor(Color.parseColor("#000000"))
                    btnMarkRead.visibility = View.VISIBLE
                }

                btnMarkRead.setOnClickListener { markAsRead(item) }
                btnDelete.setOnClickListener { deleteNotification(item) }

                // 타입별 동작
                when (item.type) {
                    "CHAT_REQUEST" -> {
                        layoutActions.visibility = View.VISIBLE
                        btnAccept.text = "채팅 수락"
                        btnDecline.text = "거절/닫기"
                        btnAccept.setOnClickListener {
                            val roomId = item.metadata?.get("roomId")
                            val requesterName = item.metadata?.get("requesterName") ?: "알 수 없음"
                            if (roomId != null) {
                                // 알림을 읽음 처리하고, 채팅 요청을 수락
                                markAsRead(item)
                                acceptChatRequest(roomId, requesterName)
                            }
                        }
                    }
                    "SCHEDULE_INVITE" -> {
                        layoutActions.visibility = View.VISIBLE
                        btnAccept.text = "일정 참여"
                        btnDecline.text = "거절/닫기"
                        btnAccept.setOnClickListener {
                            val scheduleId = item.metadata?.get("scheduleId")
                            if (scheduleId != null) {
                                markAsRead(item)
                                joinSchedule(scheduleId)
                            }
                        }
                    }
                    else -> layoutActions.visibility = View.GONE
                }

                btnDecline.setOnClickListener {
                    layoutActions.visibility = View.GONE
                    markAsRead(item)
                }
            }
        }
    }

    private fun markAsRead(item: NotificationItem) {
        val token = getAuthToken()
        if (item._id == null) return
        RetrofitClient.notificationInstance.markAsRead(token, item._id)
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    if(response.isSuccessful) fetchNotifications()
                }
                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {}
            })
    }

    private fun deleteNotification(item: NotificationItem) {
        val token = getAuthToken()
        if (item._id == null) return
        RetrofitClient.notificationInstance.deleteNotification(token, item._id)
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    if(response.isSuccessful) fetchNotifications()
                }
                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {}
            })
    }

    private fun joinSchedule(scheduleId: String) {
        val token = getAuthToken()
        RetrofitClient.scheduleInstance.joinSchedule(token, scheduleId)
            .enqueue(object : Callback<JoinScheduleResponse> {
                override fun onResponse(call: Call<JoinScheduleResponse>, response: Response<JoinScheduleResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@NotificationActivity, "참여 완료!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@NotificationActivity, ScheduleActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@NotificationActivity, "참여 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<JoinScheduleResponse>, t: Throwable) {}
            })
    }
}