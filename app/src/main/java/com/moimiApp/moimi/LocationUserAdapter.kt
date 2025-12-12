package com.moimiApp.moimi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

// 서버 응답 구조에 맞게 수정
data class LocationUserItem(
    val userId: String,
    val name: String,
    val profileImg: String?,
    var isSharing: Boolean = false
)

class LocationUserAdapter(
    private val myUserId: String,
    private var userList: MutableList<LocationUserItem>,
    private val onRequestClick: (String) -> Unit // userId 전달
) : RecyclerView.Adapter<LocationUserAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.location_user, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount() = userList.size

    fun updateList(newList: List<LocationUserItem>) {
        userList = newList.toMutableList()
        notifyDataSetChanged()
    }

    fun updateUserStatus(userId: String, isSharing: Boolean) {
        val index = userList.indexOfFirst { it.userId == userId }
        if (index != -1) {
            userList[index].isSharing = isSharing
            notifyItemChanged(index)
        }
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.iv_user_profile)
        val tvName: TextView = itemView.findViewById(R.id.tv_user_name)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status_sharing)
        val btnRequest: MaterialButton = itemView.findViewById(R.id.btn_request_share)

        fun bind(item: LocationUserItem) {
            tvName.text = item.name

            // 프로필 이미지
            if (!item.profileImg.isNullOrEmpty()) {
                Glide.with(itemView.context).load(item.profileImg).circleCrop().into(ivProfile)
            } else {
                ivProfile.setImageResource(R.drawable.profile)
            }

            // 본인인 경우: 버튼 숨김, 상태만 표시
            if (item.userId == myUserId) {
                btnRequest.visibility = View.GONE
                tvStatus.visibility = View.VISIBLE
                if (item.isSharing) {
                    tvStatus.text = "● 공유 중 (나)"
                    tvStatus.setTextColor(Color.parseColor("#4CAF50")) // 녹색
                } else {
                    tvStatus.text = "● 공유 꺼짐"
                    tvStatus.setTextColor(Color.parseColor("#888888")) // 회색
                }
            }
            // 타인인 경우
            else {
                if (item.isSharing) {
                    // 공유 중 -> 지도 보라고 안내
                    btnRequest.visibility = View.GONE
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = "● 공유 중"
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    // 공유 꺼짐 -> 요청 버튼 표시
                    tvStatus.visibility = View.GONE
                    btnRequest.visibility = View.VISIBLE
                    btnRequest.setOnClickListener {
                        onRequestClick(item.userId)
                    }
                }
            }
        }
    }
}