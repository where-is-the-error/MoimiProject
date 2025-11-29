package com.moimiApp.moimi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_location)

        // ⭐ [수정] 최신 SDK 인증 방식: TMapView 인스턴스 생성 후 키 설정
        // 화면에 보여주지 않더라도 이 코드가 있어야 TMapData 검색이 작동합니다.
        val tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(tMapApiKey)

        etSearch = findViewById(R.id.et_search_keyword)
        rvList = findViewById(R.id.rv_search_results)
        progressBar = findViewById(R.id.progress_bar)
        val btnSearch = findViewById<Button>(R.id.btn_search_confirm)

        rvList.layoutManager = LinearLayoutManager(this)

        adapter = LocationAdapter(emptyList()) { selectedPoi ->
            val intent = Intent().apply {
                putExtra("locationName", selectedPoi.name)
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
            } else {
                false
            }
        }
    }

    private fun searchLocation() {
        val keyword = etSearch.text.toString().trim()
        if (keyword.isEmpty()) {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        Log.d("TMAP_SEARCH", "검색 시작: $keyword")

        val tmapData = TMapData()

        // ⭐ [수정] 최신 SDK 리스너 이름: OnFindAllPOIListener
        tmapData.findAllPOI(keyword, object : TMapData.OnFindAllPOIListener {
            override fun onFindAllPOI(poiList: ArrayList<TMapPOIItem>?) {
                runOnUiThread {
                    progressBar.visibility = View.GONE

                    if (!poiList.isNullOrEmpty()) {
                        Log.d("TMAP_SEARCH", "검색 성공: ${poiList.size}개 발견")

                        val mappedList = poiList.map { item ->
                            Poi(
                                name = item.poiName,
                                frontLat = item.poiPoint.latitude.toString(),
                                frontLon = item.poiPoint.longitude.toString()
                            )
                        }
                        adapter.updateList(mappedList)
                    } else {
                        Log.d("TMAP_SEARCH", "검색 결과 없음 (0개)")
                        Toast.makeText(this@SearchLocationActivity, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                        adapter.updateList(emptyList())
                    }
                }
            }
        })
    }
}