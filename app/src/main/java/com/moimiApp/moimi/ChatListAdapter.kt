package com.moimiApp.moimi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ChatListAdapter(
    private val chatRooms: List<ChatRoomItem>,
    private val onItemClick: (ChatRoomItem) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatRoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_list_row, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(chatRooms[position])
    }

    override fun getItemCount(): Int = chatRooms.size

    inner class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProfile: ImageView = itemView.findViewById(R.id.iv_chat_profile) // ✅ 이미지뷰 연결
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_chat_list_username)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_chat_list_preview)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_chat_time)
        private val badge: View = itemView.findViewById(R.id.view_unread_badge)

        fun bind(chatRoom: ChatRoomItem) {
            tvTitle.text = chatRoom.title

            // ✅ [핵심] 프로필 이미지 로딩 (Glide 사용)
            if (!chatRoom.profileImg.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(chatRoom.profileImg) // 서버에서 받은 URL
                    .circleCrop()              // 동그랗게 자르기
                    .placeholder(R.drawable.profile) // 로딩 중에 보여줄 기본 이미지
                    .error(R.drawable.profile)       // 에러 나면 보여줄 기본 이미지
                    .into(ivProfile)
            } else {
                // 프사가 없으면 기본 이미지로 초기화 (재사용 문제 방지)
                Glide.with(itemView.context)
                    .load(R.drawable.profile)
                    .circleCrop()
                    .into(ivProfile)
            }

            // 메시지 처리 (상태 메시지 구별)
            // 서버에서 전달받는 상태 메시지 패턴: "수락을 기다리는 중입니다." 또는 "대화 요청을 수락해주세요!"
            val isStatusMessage = chatRoom.lastMessage.contains("기다리는 중입니다") || chatRoom.lastMessage.contains("수락해주세요") || chatRoom.lastMessage == "대화를 시작해보세요!"

            if (isStatusMessage) {
                tvMessage.text = if (chatRoom.lastMessage.isEmpty() || chatRoom.lastMessage == "대화를 시작해보세요!") "새로운 대화를 시작해보세요!" else chatRoom.lastMessage
                tvMessage.setTextColor(Color.parseColor("#FF8989")) // 브랜드 색상
            } else {
                tvMessage.text = chatRoom.lastMessage
                tvMessage.setTextColor(Color.parseColor("#888888")) // 일반 텍스트 색상
            }

            // 시간 표시
            if (!chatRoom.lastMessageTime.isNullOrEmpty()) {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val date = inputFormat.parse(chatRoom.lastMessageTime)
                    val outputFormat = SimpleDateFormat("a h:mm", Locale.getDefault())
                    tvTime.text = outputFormat.format(date)
                } catch (e: Exception) {
                    tvTime.text = ""
                }
            } else {
                tvTime.text = ""
            }

            // 읽음 배지
            badge.visibility = if (chatRoom.hasUnread) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(chatRoom) }
        }
    }
}