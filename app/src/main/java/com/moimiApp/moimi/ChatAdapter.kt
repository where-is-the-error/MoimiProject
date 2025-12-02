package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ME = 1
        private const val TYPE_OTHER = 2
    }

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

        // 날짜 구분선 표시 여부 결정
        var showDate = false
        if (position == 0) {
            showDate = true
        } else {
            val prevMsg = messages[position - 1]
            if (msg.rawDate != prevMsg.rawDate) {
                showDate = true
            }
        }

        if (holder is MeViewHolder) {
            holder.bind(msg, showDate)
        } else if (holder is OtherViewHolder) {
            holder.bind(msg, showDate)
        }
    }

    override fun getItemCount() = messages.size

    class MeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tv_bubble_me_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_bubble_me_time)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_chat_date_header)

        fun bind(msg: ChatMessage, showDate: Boolean) {
            tvContent.text = msg.content
            tvTime.text = msg.time

            if (showDate) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = msg.rawDate
            } else {
                tvDate.visibility = View.GONE
            }
        }
    }

    class OtherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_bubble_other_name)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_bubble_other_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_bubble_other_time)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_chat_date_header)

        fun bind(msg: ChatMessage, showDate: Boolean) {
            tvName.text = msg.senderName
            tvContent.text = msg.content
            tvTime.text = msg.time

            if (showDate) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = msg.rawDate
            } else {
                tvDate.visibility = View.GONE
            }
        }
    }
}