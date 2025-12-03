package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class LocationUser(
    val userId: String,
    val userName: String,
    val isOnline: Boolean = true
)

class LocationUserAdapter(private val userList: MutableList<LocationUser>) : RecyclerView.Adapter<LocationUserAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.location_user, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount() = userList.size

    fun updateUser(userId: String, userName: String) {
        val existingIndex = userList.indexOfFirst { it.userId == userId }
        if (existingIndex == -1) {
            userList.add(LocationUser(userId, userName))
            notifyItemInserted(userList.size - 1)
        }
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ✅ XML ID와 정확히 일치
        val imgProfile: ImageView = itemView.findViewById(R.id.iv_user_profile)
        val tvName: TextView = itemView.findViewById(R.id.tv_user_name)
        val viewStatus: View = itemView.findViewById(R.id.view_status_dot)

        fun bind(user: LocationUser) {
            tvName.text = user.userName
            imgProfile.setImageResource(R.drawable.profile) // 기본 이미지 (필요시 Glide 사용)

            // 온라인 상태 표시
            viewStatus.setBackgroundResource(
                if (user.isOnline) R.drawable.bg_circle_filled_red
                else R.drawable.bg_circle_stroke_red
            )
        }
    }
}