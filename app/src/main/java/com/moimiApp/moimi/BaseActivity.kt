package com.moimiApp.moimi

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

open class BaseActivity : AppCompatActivity() {

    protected val tMapApiKey = Constants.TMAP_API_KEY

    protected val prefsManager: SharedPreferencesManager by lazy {
        SharedPreferencesManager(this)
    }

    protected fun getAuthToken(): String {
        val token = prefsManager.getToken() ?: ""
        return if (token.isNotEmpty()) "Bearer $token" else ""
    }

    protected fun setupDrawer() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        val headerView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.header_container)

        val savedName = prefsManager.getUserName() ?: "게스트"
        val savedId = prefsManager.getUserId() ?: "로그인 해주세요"

        headerView?.findViewById<TextView>(R.id.tv_user_name)?.text = savedName
        headerView?.findViewById<TextView>(R.id.tv_user_id)?.text = savedId

        findViewById<ImageView>(R.id.btn_menu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        headerView?.findViewById<ImageView>(R.id.btn_close_drawer)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        setupMenuLinks(drawerLayout)
    }

    private fun setupMenuLinks(drawerLayout: DrawerLayout) {
        val menuIds = mapOf(
            R.id.menu_chatting to ChatListActivity::class.java,
            R.id.menu_route to RouteActivity::class.java,
            R.id.menu_restaurant to RestaurantActivity::class.java,
            R.id.menu_location_share to LocationShareActivity::class.java
        )

        menuIds.forEach { (id, activityClass) ->
            findViewById<TextView>(id)?.setOnClickListener {
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }
        }

        // [수정됨] 일정 메뉴 클릭 시 ScheduleActivity로 이동하도록 변경
        findViewById<TextView>(R.id.menu_schedule)?.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<TextView>(R.id.tv_logout)?.setOnClickListener {
            prefsManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}