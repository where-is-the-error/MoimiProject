package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.moimiApp.moimi.databinding.ActivityNotificationBinding // 바인딩 클래스 임포트
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private val notiList = mutableListOf<NotificationItem>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. 뷰 바인딩 설정
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. 툴바 설정 (간결해짐)
        setupToolbar("알림")

        // 3. 리사이클러뷰 설정 (findViewById 제거)
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
                        val list = response.body()?.notifications
                        if (!list.isNullOrEmpty()) {
                            notiList.addAll(list)
                            binding.tvEmptyNotification.visibility = View.GONE
                        } else {
                            binding.tvEmptyNotification.visibility = View.VISIBLE
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
                override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                    Toast.makeText(this@NotificationActivity, "알림을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // 어댑터 내부 (여기서도 ItemNotificationBinding을 쓰면 좋지만, XML 변경 없이 호환성을 위해 유지)
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
            val tvMsg: TextView = itemView.findViewById(R.id.tv_noti_message)
            val layoutActions: View = itemView.findViewById(R.id.layout_actions)
            val btnAccept: Button = itemView.findViewById(R.id.btn_accept)
            val btnDecline: Button = itemView.findViewById(R.id.btn_decline)

            fun bind(item: NotificationItem) {
                tvMsg.text = item.message

                if (item.type == "CHAT_REQUEST") {
                    layoutActions.visibility = View.VISIBLE

                    btnAccept.setOnClickListener {
                        val roomId = item.metadata?.get("roomId")
                        val requesterName = item.metadata?.get("requesterName") ?: "상대방"

                        if (roomId != null) {
                            val intent = Intent(this@NotificationActivity, ChatRoomActivity::class.java)
                            intent.putExtra("roomId", roomId)
                            intent.putExtra("roomTitle", "${requesterName}님과의 대화")
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@NotificationActivity, "채팅방 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    btnDecline.setOnClickListener {
                        Toast.makeText(this@NotificationActivity, "요청을 거절했습니다.", Toast.LENGTH_SHORT).show()
                        layoutActions.visibility = View.GONE
                    }
                } else {
                    layoutActions.visibility = View.GONE
                }
            }
        }
    }
}