package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(
    private val scheduleList: List<ScheduleItem>,
    private val onItemClick: (ScheduleItem) -> Unit,
    private val onSettingsClick: (ScheduleItem) -> Unit // ⭐ 설정 클릭 리스너 추가
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view, onItemClick, onSettingsClick)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(scheduleList[position])
    }

    override fun getItemCount() = scheduleList.size

    class ScheduleViewHolder(
        itemView: View,
        private val clickListener: (ScheduleItem) -> Unit,
        private val settingsClickListener: (ScheduleItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTime: TextView = itemView.findViewById(R.id.tv_schedule_time)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_schedule_title)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_schedule_location)
        private val btnSettings: ImageView = itemView.findViewById(R.id.btn_schedule_settings)

        fun bind(item: ScheduleItem) {
            tvTime.text = item.time
            tvTitle.text = if (item.isLeader) "👑 ${item.title}" else item.title

            val members = if (item.memberNames.isNotEmpty()) "\n참여자: ${item.memberNames.joinToString(", ")}" else ""
            tvLocation.text = "${item.location}$members"

            // ⭐ 모임장일 때만 설정 버튼 표시
            if (item.isLeader) {
                btnSettings.visibility = View.VISIBLE
                btnSettings.setOnClickListener { settingsClickListener(item) }
            } else {
                btnSettings.visibility = View.GONE
            }

            itemView.setOnClickListener { clickListener(item) }
        }
    }
}