package com.moimiApp.moimi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LocationAdapter(
    private var items: List<Poi>,
    private val onClick: (Poi) -> Unit
) : RecyclerView.Adapter<LocationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // [수정] 우리가 만든 레이아웃의 ID 사용
        val tvName: TextView = view.findViewById(R.id.tv_place_name)
        val tvAddress: TextView = view.findViewById(R.id.tv_place_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // [수정] item_search_result 레이아웃 연결
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name ?: "이름 없음"
        holder.tvAddress.text = "좌표: ${item.frontLat}, ${item.frontLon}"

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<Poi>) {
        items = newItems
        notifyDataSetChanged()
    }
}