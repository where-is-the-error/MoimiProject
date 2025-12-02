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
    private var eventDates: Set<String> // "yyyy-MM-dd" 형태의 날짜들
) : RecyclerView.Adapter<WeekCalendarAdapter.DayViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayNameFormat = SimpleDateFormat("E", Locale.KOREAN) // 월, 화...
    private val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault()) // 1, 2...

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayName: TextView = view.findViewById(R.id.tv_day_name)
        val tvDateNumber: TextView = view.findViewById(R.id.tv_date_number)
        val viewMarker: View = view.findViewById(R.id.view_event_marker)
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

        // 1. 기본 요일별 색상 설정 (일정이 없을 때의 기본값)
        val cal = Calendar.getInstance()
        cal.time = date
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        when (dayOfWeek) {
            Calendar.SATURDAY -> {
                holder.tvDayName.setTextColor(Color.BLUE)
                holder.tvDateNumber.setTextColor(Color.BLUE)
            }
            Calendar.SUNDAY -> {
                holder.tvDayName.setTextColor(Color.RED)
                holder.tvDateNumber.setTextColor(Color.RED)
            }
            else -> {
                holder.tvDayName.setTextColor(Color.parseColor("#888888"))
                holder.tvDateNumber.setTextColor(Color.parseColor("#333333"))
            }
        }

        // 2. 일정 마커 표시 로직 (수정됨)
        if (eventDates.contains(dateStr)) {
            // 일정이 있으면: 꽉 찬 원형 배경 + 흰색 숫자
            holder.viewMarker.visibility = View.VISIBLE
            holder.viewMarker.setBackgroundResource(R.drawable.bg_circle_filled_red)
            holder.tvDateNumber.setTextColor(Color.WHITE)
        } else {
            // 일정이 없으면: 마커 숨김 (글자색은 위에서 설정한 요일별 색상 유지)
            holder.viewMarker.visibility = View.GONE
        }
    }

    override fun getItemCount() = days.size

    fun updateEvents(newEvents: Set<String>) {
        eventDates = newEvents
        notifyDataSetChanged()
    }
}