package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// ScheduleItem은 DataModels.kt에 정의되어 있다고 가정합니다.

class ScheduleAdapter(
    private val scheduleList: List<ScheduleItem>,
    // 1. [추가] 짧게 누르기 콜백 (상세 보기/수정용)
    private val onItemClick: (ScheduleItem) -> Unit,
    // 2. [추가] 길게 누르기 콜백 (삭제 팝업용)
    private val onItemLongClick: (ScheduleItem) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)

        // 3. [수정] 뷰 홀더를 생성할 때 클릭 리스너를 전달합니다.
        return ScheduleViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        // 4. [수정] 데이터를 뷰 홀더의 bind 함수로 전달합니다.
        holder.bind(scheduleList[position])
    }

    override fun getItemCount() = scheduleList.size

    // 5. [수정] 뷰홀더 클래스 (클릭 이벤트 처리 로직 포함)
    class ScheduleViewHolder(
        itemView: View,
        private val clickListener: (ScheduleItem) -> Unit,
        private val longClickListener: (ScheduleItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        // ID 연결
        val tvTime: TextView = itemView.findViewById(R.id.tv_schedule_time)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_schedule_title)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_schedule_location)

        private lateinit var currentItem: ScheduleItem

        init {
            // 짧게 누르기: 상세 보기/수정 이벤트 호출
            itemView.setOnClickListener {
                clickListener(currentItem)
            }
            // 길게 누르기: 삭제 확인 팝업 이벤트 호출
            itemView.setOnLongClickListener {
                longClickListener(currentItem)
                true // 이벤트 소비
            }
        }

        fun bind(item: ScheduleItem) {
            // 데이터 업데이트 및 currentItem 저장
            this.currentItem = item
            tvTime.text = item.time
            tvTitle.text = item.title
            tvLocation.text = item.location
        }
    }
}