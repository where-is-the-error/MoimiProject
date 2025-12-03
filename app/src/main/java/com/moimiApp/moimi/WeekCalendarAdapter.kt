package com.moimiApp.moimi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeekCalendarAdapter(
    private val days: List<Date>,
    private var eventDates: Set<String> // "yyyy-MM-dd" 형태의 날짜들 (일정 있는 날)
) : RecyclerView.Adapter<WeekCalendarAdapter.DayViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayNameFormat = SimpleDateFormat("E", Locale.KOREAN) // 월, 화...
    private val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault()) // 1, 2...

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayName: TextView = view.findViewById(R.id.tv_day_name)
        val tvDateNumber: TextView = view.findViewById(R.id.tv_date_number)
        val viewMarker: View = view.findViewById(R.id.view_event_marker) // 🔴 마커 뷰 연결
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val date = days[position]
        val dateStr = dateFormat.format(date)

        holder.tvDayName.text = dayNameFormat.format(date)
        holder.tvDateNumber.text = dayNumberFormat.format(date)

        // 1. 요일별 기본 색상 설정
        val cal = Calendar.getInstance()
        cal.time = date
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val defaultTextColor = when (dayOfWeek) {
            Calendar.SATURDAY -> Color.BLUE
            Calendar.SUNDAY -> Color.RED
            else -> Color.parseColor("#333333") // 평일 검정색
        }

        holder.tvDayName.setTextColor(if(dayOfWeek == Calendar.SUNDAY) Color.RED else if(dayOfWeek == Calendar.SATURDAY) Color.BLUE else Color.parseColor("#888888"))

        // 2. ⭐ [핵심] 일정 마커 표시 로직
        if (eventDates.contains(dateStr)) {
            // 일정이 있는 날: 빨간 동그라미 배경 표시 & 날짜 글씨 흰색
            holder.viewMarker.visibility = View.VISIBLE
            holder.viewMarker.setBackgroundResource(R.drawable.bg_circle_filled_red)
            holder.tvDateNumber.setTextColor(Color.WHITE)
        } else {
            // 일정이 없는 날: 마커 숨김 & 원래 색상 복구
            holder.viewMarker.visibility = View.GONE
            holder.tvDateNumber.setTextColor(defaultTextColor)
        }
    }

    override fun getItemCount() = days.size

    // 외부에서 일정 데이터 갱신 시 호출
    fun updateEvents(newEvents: Set<String>) {
        eventDates = newEvents
        notifyDataSetChanged()
    }
}