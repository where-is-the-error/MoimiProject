package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 뷰 타입 상수
    companion object {
        private const val TYPE_ME = 1
        private const val TYPE_OTHER = 2
    }

    // 어떤 뷰를 쓸지 결정 (내 거냐 남의 거냐)
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isMe) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ME) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble_me, parent, false)
            MeViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble_other, parent, false)
            OtherViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is MeViewHolder) {
            holder.bind(msg)
        } else if (holder is OtherViewHolder) {
            holder.bind(msg)
        }
    }

    override fun getItemCount() = messages.size

    // 내 메시지 뷰홀더
    class MeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tv_bubble_me_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_bubble_me_time)

        fun bind(msg: ChatMessage) {
            tvContent.text = msg.content
            tvTime.text = msg.time
        }
    }

    // 상대 메시지 뷰홀더
    class OtherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_bubble_other_name)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_bubble_other_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_bubble_other_time)

        fun bind(msg: ChatMessage) {
            tvName.text = msg.senderName
            tvContent.text = msg.content
            tvTime.text = msg.time
        }
    }
}