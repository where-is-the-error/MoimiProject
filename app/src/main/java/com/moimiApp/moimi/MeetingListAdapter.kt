package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MeetingListAdapter(
    private val meetingList: List<MeetingItem>,
    private val onItemClick: (MeetingItem) -> Unit
) : RecyclerView.Adapter<MeetingListAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meeting_list_row, parent, false)

        // 뷰 홀더 생성 시 클릭 리스너 전달
        return Holder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(meetingList[position])
    }

    override fun getItemCount(): Int = meetingList.size

    inner class Holder(
        itemView: View,
        private val itemClick: (MeetingItem) -> Unit // 👈 여기에 private val 추가!
    ) : RecyclerView.ViewHolder(itemView) {

        val title = itemView.findViewById<TextView>(R.id.tv_meeting_title)
        val time = itemView.findViewById<TextView>(R.id.tv_meeting_time)
        val location = itemView.findViewById<TextView>(R.id.tv_meeting_location)

        fun bind(item: MeetingItem) {
            title.text = item.title
            time.text = item.dateTime
            location.text = item.location

            itemView.setOnClickListener { itemClick(item) }
        }
    }
}