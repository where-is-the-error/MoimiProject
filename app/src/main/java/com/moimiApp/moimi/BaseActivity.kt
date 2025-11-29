package com.moimiApp.moimi // 패키지명 확인!

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

open class BaseActivity : AppCompatActivity() {

    // [추가 1] SharedPreferencesManager 초기화
    protected val prefsManager: SharedPreferencesManager by lazy {
        SharedPreferencesManager(this)
    }

    // [추가 2] 토큰을 쉽게 가져오는 함수
    protected fun getAuthToken(): String {
        val token = prefsManager.getToken() ?: ""
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

            // [닫기 버튼]
            val btnClose = headerView.findViewById<ImageView>(R.id.btn_close_drawer)
            btnClose?.setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // ✅ [추가됨] 프로필 이미지 클릭 -> 내 정보 이동
            val ivProfile = headerView.findViewById<ImageView>(R.id.iv_profile_image)
            ivProfile?.setOnClickListener {
                val intent = Intent(this, MyPageActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // ✅ [추가됨] 이름 클릭 -> 내 정보 이동
            val tvName = headerView.findViewById<TextView>(R.id.tv_user_name)
            tvName?.setOnClickListener {
                val intent = Intent(this, MyPageActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }


            // [메뉴: 채팅]
            val menuChatting = headerView.findViewById<TextView>(R.id.menu_chatting)
            menuChatting?.setOnClickListener {
                val intent = Intent(this, ChatListActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // ✅ [추가됨] 메뉴: 내 모임 -> MeetingListActivity로 이동
            val menuMeetingList = headerView.findViewById<TextView>(R.id.menu_meeting_list)
            menuMeetingList?.setOnClickListener {
                val intent = Intent(this, MeetingListActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // [메뉴: 길찾기]
            val menuRoute = headerView.findViewById<TextView>(R.id.menu_route)
            menuRoute?.setOnClickListener {
                val intent = Intent(this, RouteActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // [메뉴: 음식점 예약]
            val menuRestaurant = headerView.findViewById<TextView>(R.id.menu_restaurant)
            menuRestaurant?.setOnClickListener {
                val intent = Intent(this, RestaurantActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // [메뉴: 일정]
            val menuSchedule = headerView.findViewById<TextView>(R.id.menu_schedule)
            menuSchedule?.setOnClickListener {
                val intent = Intent(this, ScheduleActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // [메뉴: 위치공유]
            val menuLocation = headerView.findViewById<TextView>(R.id.menu_location_share)
            menuLocation?.setOnClickListener {
                val intent = Intent(this, LocationShareActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            // [메뉴: 로그아웃]
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

    // 뒤로가기 버튼 처리
    override fun onBackPressed() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}