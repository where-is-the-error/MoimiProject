package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skt.tmap.TMapData
import com.skt.tmap.TMapView
import com.skt.tmap.poi.TMapPOIItem

class SearchLocationActivity : BaseActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvList: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: LocationAdapter
    private var tMapView: TMapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_location)

        try {
            tMapView = TMapView(this)
            tMapView?.setSKTMapApiKey(tMapApiKey)
            val container = LinearLayout(this)
            container.visibility = View.GONE
            container.addView(tMapView)
            (window.decorView as? android.view.ViewGroup)?.addView(container)
        } catch (e: Exception) { Log.e("Search", "Init Error", e) }

        etSearch = findViewById(R.id.et_search_keyword)
        rvList = findViewById(R.id.rv_search_results)
        progressBar = findViewById(R.id.progress_bar)
        val btnSearch = findViewById<Button>(R.id.btn_search_confirm)

        rvList.layoutManager = LinearLayoutManager(this)

        adapter = LocationAdapter(emptyList()) { selectedPoi ->
            val intent = Intent().apply {
                putExtra("locationName", selectedPoi.name)
                // ⭐ [수정] 좌표 정보 추가 반환
                putExtra("lat", selectedPoi.frontLat?.toDoubleOrNull() ?: 0.0)
                putExtra("lon", selectedPoi.frontLon?.toDoubleOrNull() ?: 0.0)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
        rvList.adapter = adapter

        btnSearch.setOnClickListener { searchLocation() }
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation()
                true
            } else false
        }
    }

    private fun searchLocation() {
        val keyword = etSearch.text.toString().trim()
        if (keyword.isEmpty()) {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        val tmapData = TMapData()
        tmapData.findAllPOI(keyword, object : TMapData.OnFindAllPOIListener {
            override fun onFindAllPOI(poiList: ArrayList<TMapPOIItem>?) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (!poiList.isNullOrEmpty()) {
                        val mappedList = poiList.map { item ->
                            Poi(
                                name = item.poiName,
                                frontLat = item.poiPoint.latitude.toString(),
                                frontLon = item.poiPoint.longitude.toString()
                            )
                        }
                        adapter.updateList(mappedList)
                    } else {
                        Toast.makeText(this@SearchLocationActivity, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                        adapter.updateList(emptyList())
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        tMapView = null
    }
}