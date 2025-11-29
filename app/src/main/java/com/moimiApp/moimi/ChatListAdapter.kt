package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(
    private val chatRooms: List<ChatRoom>,
    private val onItemClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatRoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        // item_chat_list_row.xml 로딩
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list_row, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(chatRooms[position])
    }

        // 데이터 바인딩
        holder.tvTitle.text = item.title       // 채팅방 이름 (예: 동양식당 팟)
        holder.tvMessage.text = item.lastMessage // 마지막 메시지

        // 클릭 이벤트
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = chatList.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // XML ID와 정확히 일치시킴
        val tvTitle: TextView = itemView.findViewById(R.id.tv_chat_list_username) // 채팅방 이름
        val tvMessage: TextView = itemView.findViewById(R.id.tv_chat_list_preview) // 미리보기 내용

        // (XML에 시간 표시하는 뷰가 없으므로 제거됨)
    }
}