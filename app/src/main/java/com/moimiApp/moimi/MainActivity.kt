package com.moimiApp.moimi

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.skt.tmap.TMapView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.skt.tmap.TMapView

class MainActivity : BaseActivity() {

    private lateinit var tMapView: TMapView
    private val tMapKey = "QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 위치 서비스 권한 체크 및 시작 (기존 코드 유지)
        checkPermissionAndStartService()

        // 2. 지도 컨테이너 연결 (XML에서 ImageView 'map' 대신 FrameLayout 'map_container'로 변경했어야 함)
        // 만약 XML ID가 아직 'map'이면 R.id.map_container 부분을 R.id.map으로, 타입은 ViewGroup으로 맞춰야 합니다.
        val mapContainer = findViewById<ViewGroup>(R.id.map_container)

        // 3. TMap 생성
        val tMapView = TMapView(this)
        tMapView.setSKTMapApiKey("QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL") // 👈 실제 키 입력 필수!

        // 4. 지도 설정 (준비되면 서울 시청 중심으로 이동)
        tMapView.setOnMapReadyListener {
            tMapView.zoomLevel = 13
            tMapView.setCenterPoint(126.9780, 37.5665) // 기본 위치: 서울 시청
        }

        // 5. 화면에 지도 추가
        mapContainer.addView(tMapView)

        // ⚠️ [주의] 지도가 터치를 소비하기 때문에, 기존처럼 단순 setOnClickListener는 작동하지 않을 수 있습니다.
        // 지도를 "클릭"해서 RouteActivity로 넘어가고 싶다면 아래처럼 터치 리스너를 쓰거나,
        // 지도 위에 투명 버튼을 겹쳐야 합니다.
        // (일단 지도를 자유롭게 움직여야 하므로, 클릭 이동 기능은 주석 처리해 둡니다.)

        /*
        tMapView.setOnClickListener {
            val intent = Intent(this, RouteActivity::class.java)
            startActivity(intent)
        }
        */
    }

    private fun updateDummyUI() {
        val tvWeather = findViewById<TextView>(R.id.tv_weather_info)
        tvWeather.text = "24°C 맑음"

        val tvTransport = findViewById<TextView>(R.id.tv_transport_info)
        tvTransport.text = "강남역까지 택시"

        // 혹시 XML에 tv_transport_time ID가 있다면 주석 해제
        // val tvTime = findViewById<TextView>(R.id.tv_transport_time)
        // tvTime.text = "약 25분 소요"
    }

    private fun checkPermissionAndStartService() {
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            startLocationService()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationService()
        }
    }
}