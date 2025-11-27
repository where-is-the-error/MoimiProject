package com.moimiApp.moimi

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.skt.tmap.TMapView

// Retrofit 관련 임포트
import retrofit2.Callback
import retrofit2.Response

// OpenWeatherMap API 통신에 필요한 것들을 임포트 (DataModels와 WeatherClient)
import com.moimiApp.moimi.WeatherData
import com.moimiApp.moimi.WeatherClient

class MainActivity : AppCompatActivity() {
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
        // 5. 화면에 지도 추가 (꽉 채우기 설정 포함)
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        tMapView.layoutParams = params
        mapContainer.addView(tMapView)

        // 6. ☀️ 날씨 API 호출 추가 (새로운 코드)
        // 예시로 "Seoul"의 날씨를 가져옵니다.
        fetchWeather("Seoul")

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

    private fun checkPermissionAndStartService() {
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(this, LocationService::class.java)
            startForegroundService(intent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(this, LocationService::class.java)
            startForegroundService(intent)
        }
    }
    private fun fetchWeather(location: String) {
        // ⚠️ 주의: YOUR_API_KEY를 실제 OpenWeatherMap API 키로 교체해야 합니다!
        val YOUR_API_KEY = "4511add96f9a93c2529d1e72c19aac6f"

        // Retrofit 인스턴스를 직접 생성하지 않고, RetrofitClient.kt에서 만든
        // 전역 객체 WeatherClient를 사용합니다.
        WeatherClient.instance.getCurrentWeatherData(location, "metric", YOUR_API_KEY)
            .enqueue(object : retrofit2.Callback<WeatherData> {

                // 1. 서버 응답이 왔을 때
                override fun onResponse(call: retrofit2.Call<WeatherData>, response: retrofit2.Response<WeatherData>) {
                    if (response.isSuccessful) {
                        val data = response.body()
                        data?.let {
                            // 날씨 정보 추출
                            val city = it.name
                            val temp = it.main.temp
                            val description = it.weather.firstOrNull()?.description ?: "정보 없음"

                            // TODO: 이 정보를 화면의 TextView 등에 표시하는 로직을 추가
                            println("도시: $city, 현재 기온: $temp°C, 날씨: $description")

                            // 예시: Toast 메시지로 확인
                            Toast.makeText(this@MainActivity, "$city 날씨: $description, 기온: $temp°C", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // HTTP 오류 (예: API 키 오류, 도시 이름 오류 등)
                        println("API 호출 실패: HTTP ${response.code()}")
                        Toast.makeText(this@MainActivity, "날씨 정보 로드 실패 (HTTP 오류)", Toast.LENGTH_SHORT).show()
                    }
                }

                // 2. 네트워크 연결 자체에 문제가 있을 때
                override fun onFailure(call: retrofit2.Call<WeatherData>, t: Throwable) {
                    // t.message를 통해 오류 메시지 확인 가능
                    println("네트워크 오류: ${t.message}")
                    Toast.makeText(this@MainActivity, "네트워크 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }
}