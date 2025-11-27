package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(
    private val chatList: List<ChatRoom>,
    private val onItemClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        // 회원님이 만드신 'item_chat_list_row.xml'을 가져옵니다.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list_row, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = chatList[position]

        // 데이터를 화면에 넣기
        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.lastMessage
        // holder.tvTime.text = item.time // (XML에 시간 ID가 없으면 주석 처리)

        // 클릭하면 채팅방으로 이동
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = chatList.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // item_chat_list_row.xml에 있는 ID들을 연결
        val tvTitle: TextView = itemView.findViewById(R.id.tv_chat_list_username) // 이름
        val tvMessage: TextView = itemView.findViewById(R.id.tv_chat_list_preview) // 내용

        // 만약 XML에 시간 표시하는 TextView가 없다면 아래 줄은 지우세요.
        // val tvTime: TextView = itemView.findViewById(R.id.tv_chat_list_time)
    }
}