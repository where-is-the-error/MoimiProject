package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val myUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
        val message = messages[position]
        if (holder is MeViewHolder) {
            holder.bind(message)
        } else if (holder is OtherViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    // 내 메시지 뷰홀더
    inner class MeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tv_chat_message)
        val tvTime: TextView = itemView.findViewById(R.id.tv_chat_time)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.content
            tvTime.text = msg.time
        }
    }

    // 상대방 메시지 뷰홀더
    inner class OtherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.iv_chat_profile) // ✅ 프로필 이미지
        val tvName: TextView = itemView.findViewById(R.id.tv_chat_name)
        val tvMessage: TextView = itemView.findViewById(R.id.tv_chat_message)
        val tvTime: TextView = itemView.findViewById(R.id.tv_chat_time)

        fun bind(msg: ChatMessage) {
            tvName.text = msg.senderName
            tvMessage.text = msg.content
            tvTime.text = msg.time

            // ✅ Glide로 프로필 이미지 로드
            if (!msg.senderProfileImg.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(msg.senderProfileImg)
                    .circleCrop()
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(ivProfile)
            } else {
                Glide.with(itemView.context)
                    .load(R.drawable.profile)
                    .circleCrop()
                    .into(ivProfile)
            }
        }
    }
}