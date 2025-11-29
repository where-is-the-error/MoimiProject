package com.moimiApp.moimi

import android.content.Context // Context import 추가
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

open class BaseActivity : AppCompatActivity() {

    //✅ [추가] 여기에 키를 한 번만 적어두면, 모든 자식이 공짜로 씁니다.
    // protected: 나(BaseActivity)와 내 자식들만 쓸 수 있다는 뜻
    protected val tMapApiKey = "QMIWUEYojt1y1hE2AgzXj3f1l0VH6IbI70yQTihL"
    protected fun checkWifiandUpdateUI(mapContainer: ViewGroup,tMapView: TMapView){
        if (isWifiConnected){
            tMapView.visibility = View.VISIBLE
            removeWifiWarning(mapContainer)
        }else{
            tMapView.visibility = View.GONE
            showWifiWarning(mapContainer)
    // [추가 1] SharedPreferencesManager 초기화 (여기에 추가해야 합니다)
    protected val prefsManager: SharedPreferencesManager by lazy {
        SharedPreferencesManager(this)
    }

    // [추가 2] 토큰을 쉽게 가져오는 함수 (여기에 추가해야 합니다)
    protected fun getAuthToken(): String {
        val token = prefsManager.getToken() ?: ""
        // API 호출 헤더에 필요한 "Bearer " 접두사를 붙여서 반환
        return if (token.isNotEmpty()) "Bearer $token" else ""
    }
    protected lateinit var drawerLayout: DrawerLayout

    protected fun setupDrawer() {
        // 1. 뷰 찾기
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)

        val btnLogo = findViewById<ImageView>(R.id.btn_home_logo)
        val btnBell = findViewById<ImageView>(R.id.btn_notification)
        val btnMenu = findViewById<ImageView>(R.id.btn_menu)

        // 2. 네비게이션 헤더 내부 버튼 연결
        if (navigationView.headerCount > 0) {
            val headerView = navigationView.getHeaderView(0)

            // [수정됨] X 버튼: 이제 누르면 메뉴만 닫힙니다. (화면 이동 X)
            val btnClose = headerView.findViewById<ImageView>(R.id.btn_close_drawer)
            btnClose?.setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // 메뉴: 채팅
            val menuChatting = headerView.findViewById<TextView>(R.id.menu_chatting)
            menuChatting?.setOnClickListener {
                val intent = Intent(this, ChatListActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // 메뉴: 길찾기
            val menuRoute = headerView.findViewById<TextView>(R.id.menu_route)
            menuRoute?.setOnClickListener {
                val intent = Intent(this, RouteActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // 메뉴: 음식점 예약
            val menuRestaurant = headerView.findViewById<TextView>(R.id.menu_restaurant)
            menuRestaurant?.setOnClickListener {
                val intent = Intent(this, RestaurantActivity::class.java) // 이동할 곳 연결
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // 메뉴: 일정
            val menuSchedule = headerView.findViewById<TextView>(R.id.menu_schedule)
            menuSchedule?.setOnClickListener {
                val intent = Intent(this, ScheduleActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // 메뉴: 위치공유
            val menuLocation = headerView.findViewById<TextView>(R.id.menu_location_share)
            menuLocation?.setOnClickListener {
                val intent = Intent(this, LocationShareActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // 메뉴: 로그아웃
            val menuLogout = headerView.findViewById<TextView>(R.id.tv_logout)
            menuLogout?.setOnClickListener {
                Toast.makeText(this, "로그아웃", Toast.LENGTH_SHORT).show()
                drawerLayout.closeDrawer(GravityCompat.END)
            }
        }

        // 3. 상단 툴바 버튼 동작
        btnLogo?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        btnBell?.setOnClickListener {
            val intent = Intent(this, ChatListActivity::class.java)
            startActivity(intent)
        }

        btnMenu?.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }
    }

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    private fun showWifiWarning(container: ViewGroup) {
        if (container.findViewWithTag<TextView>("wifi_warning") != null) return

        val warningText = TextView(this).apply {
            text = "와이파이를 연결해주세요"
            textSize = 20f
            setTextColor(Color.RED)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F0F0F0"))
            tag = "wifi_warning"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        container.addView(warningText)
    }
    private fun
*/
    // 모든 화면에서 공통으로 메뉴 기능을 설정하는 함수
    protected fun setupDrawer() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        // 1. 메뉴 열기 버튼
        findViewById<ImageView>(R.id.btn_menu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // 2. 메뉴 닫기 버튼
        findViewById<ImageView>(R.id.btn_close_drawer)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // --- 3. 메뉴 항목 클릭 이벤트 ---

        // (1) Chatting -> ChatListActivity
        findViewById<TextView>(R.id.menu_chatting)?.setOnClickListener {
            moveActivity(ChatListActivity::class.java)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // (2) 길찾기 -> RouteDetailActivity
        findViewById<TextView>(R.id.menu_route)?.setOnClickListener {
            moveActivity(RouteDetailActivity::class.java)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // (3) 음식점 예약 -> RestaurantActivity
        findViewById<TextView>(R.id.menu_restaurant)?.setOnClickListener {
            moveActivity(RestaurantActivity::class.java)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // (4) 일정 -> (준비중 메시지)
        findViewById<TextView>(R.id.menu_schedule)?.setOnClickListener {
            Toast.makeText(this, "📅 일정 화면으로 이동합니다 (준비중)", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // (5) 위치공유 -> LocationShareActivity
        findViewById<TextView>(R.id.menu_location_share)?.setOnClickListener {
            moveActivity(LocationShareActivity::class.java)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // (6) 로그아웃 -> LoginActivity
        findViewById<TextView>(R.id.tv_logout)?.setOnClickListener {
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    // 화면 이동을 도와주는 함수
    private fun moveActivity(targetClass: Class<*>) {
        if (this::class.java == targetClass) {
            Toast.makeText(this, "현재 보고 있는 화면입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, targetClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }
}