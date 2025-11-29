package com.moimiApp.moimi

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.skt.tmap.TMapView

open class BaseActivity : AppCompatActivity() {

    // 🟢 [수정] 상수로 교체
    protected val tMapApiKey = Constants.TMAP_API_KEY

    // SharedPreferencesManager (세션 관리)
    protected val prefsManager: SharedPreferencesManager by lazy {
        SharedPreferencesManager(this)
    }

    // 토큰 가져오기 헬퍼 함수
    protected fun getAuthToken(): String {
        val token = prefsManager.getToken() ?: ""
        return if (token.isNotEmpty()) "Bearer $token" else ""
    }

    // 메뉴 및 로고 설정 함수 (하나로 통합됨)
    protected fun setupDrawer() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout) ?: return

        // 로그인한 사용자 정보로 프로필 텍스트 변경
        val savedName = prefsManager.getUserName() ?: "게스트"
        val savedId = prefsManager.getUserId() ?: "로그인 해주세요"

        findViewById<TextView>(R.id.tv_user_name)?.text = savedName
        findViewById<TextView>(R.id.tv_user_id)?.text = savedId

        // 1. [로고 클릭] 메인 화면으로 이동
        findViewById<ImageView>(R.id.btn_home_logo)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // 2. [메뉴 버튼] 열기
        findViewById<ImageView>(R.id.btn_menu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // 3. [닫기 버튼] 닫기
        findViewById<ImageView>(R.id.btn_close_drawer)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // 4. 메뉴 항목 클릭 이벤트 연결
        findViewById<TextView>(R.id.menu_chatting)?.setOnClickListener {
            moveActivity(ChatListActivity::class.java)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<TextView>(R.id.menu_route)?.setOnClickListener {
            moveActivity(RouteActivity::class.java)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<TextView>(R.id.menu_restaurant)?.setOnClickListener {
            moveActivity(RestaurantActivity::class.java)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<TextView>(R.id.menu_schedule)?.setOnClickListener {
            moveActivity(ScheduleActivity::class.java)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<TextView>(R.id.menu_location_share)?.setOnClickListener {
            moveActivity(LocationShareActivity::class.java)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // 로그아웃
        findViewById<TextView>(R.id.tv_logout)?.setOnClickListener {
            prefsManager.clearSession()
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    // 화면 이동 헬퍼 함수
    private fun moveActivity(targetClass: Class<*>) {
        if (this::class.java == targetClass) return
        val intent = Intent(this, targetClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    // 와이파이 체크 및 UI 업데이트
    protected fun checkWifiandUpdateUI(mapContainer: ViewGroup, tMapView: TMapView) {
        if (isWifiConnected()) {
            tMapView.visibility = View.VISIBLE
            removeWifiWarning(mapContainer)
        } else {
            tMapView.visibility = View.GONE
            showWifiWarning(mapContainer)
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

    private fun removeWifiWarning(container: ViewGroup) {
        val warningView = container.findViewWithTag<View>("wifi_warning")
        if (warningView != null) {
            container.removeView(warningView)
        }
    }
}