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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list_row, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(chatRooms[position])
    }

    override fun getItemCount(): Int = chatRooms.size

    inner class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ⚠️ [수정됨] XML 파일에 있는 정확한 ID 사용
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_chat_list_username)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_chat_list_preview)

        fun bind(chatRoom: ChatRoom) {
            tvTitle.text = chatRoom.title
            tvMessage.text = chatRoom.lastMessage

            itemView.setOnClickListener {
                onItemClick(chatRoom)
            }
        }
    }
}