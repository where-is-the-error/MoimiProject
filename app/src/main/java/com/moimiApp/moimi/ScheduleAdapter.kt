package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(
    private val scheduleList: List<ScheduleItem>,
    private val onItemClick: (ScheduleItem) -> Unit,
    private val onItemLongClick: (ScheduleItem) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(scheduleList[position])
    }

    override fun getItemCount() = scheduleList.size

    class ScheduleViewHolder(
        itemView: View,
        private val clickListener: (ScheduleItem) -> Unit,
        private val longClickListener: (ScheduleItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        val tvTime: TextView = itemView.findViewById(R.id.tv_schedule_time)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_schedule_title)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_schedule_location)

        private lateinit var currentItem: ScheduleItem

        init {
            itemView.setOnClickListener { clickListener(currentItem) }
            itemView.setOnLongClickListener {
                longClickListener(currentItem)
                true
            }
        }

        fun bind(item: ScheduleItem) {
            this.currentItem = item
            tvTime.text = item.time

            // [수정] 제목 옆에 모임장 표시
            if (item.isLeader) {
                tvTitle.text = "👑 ${item.title} (내 모임)"
            } else if (item.leaderName.isNotEmpty()) {
                tvTitle.text = "${item.title} (👑${item.leaderName})"
            } else {
                tvTitle.text = item.title
            }

            // [수정] 장소 아래에 참여자 명단 표시
            val members = if (item.memberNames.isNotEmpty()) {
                "\n참여자: " + item.memberNames.joinToString(", ")
            } else {
                ""
            }
            tvLocation.text = "${item.location}$members"
        }
    }
}