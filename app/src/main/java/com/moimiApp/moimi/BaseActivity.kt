package com.moimiApp.moimi

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide

open class BaseActivity : AppCompatActivity() {

    protected val tMapApiKey = Constants.TMAP_API_KEY

    protected val prefsManager: SharedPreferencesManager by lazy {
        SharedPreferencesManager(this)
    }

    protected fun getAuthToken(): String {
        val token = prefsManager.getToken() ?: ""
        return if (token.isNotEmpty()) "Bearer $token" else ""
    }

    /**
     * ⭐ [최적화] 상단 바 설정 (뒤로가기 버튼 기능 포함)
     */
    protected fun setupToolbar(title: String, showBack: Boolean = true) {
        val tvTitle = findViewById<TextView>(R.id.tv_common_title)
        val btnBack = findViewById<ImageView>(R.id.btn_back)

        // 제목 설정
        tvTitle?.text = title

        // 뒤로가기 버튼 설정
        if (btnBack != null) {
            if (showBack) {
                btnBack.visibility = View.VISIBLE
                btnBack.setOnClickListener {
                    finish() // 현재 액티비티 종료 (뒤로가기)
                }
            } else {
                btnBack.visibility = View.GONE
            }
        }
    }

    protected fun setupDrawer() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        val headerView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.header_container)

        val savedName = prefsManager.getUserName() ?: "게스트"
        val savedProfileUrl = prefsManager.getUserProfileImg()

        headerView?.findViewById<TextView>(R.id.tv_user_name)?.text = savedName

        val ivProfile = headerView?.findViewById<ImageView>(R.id.iv_profile_image)
        if (ivProfile != null) {
            if (!savedProfileUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(savedProfileUrl)
                    .circleCrop()
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(ivProfile)
            } else {
                ivProfile.setImageResource(R.drawable.profile)
            }
        }

        headerView?.findViewById<TextView>(R.id.tv_user_id)?.apply {
            text = "계정 >"
            setOnClickListener {
                startActivity(Intent(this@BaseActivity, MyPageActivity::class.java))
                drawerLayout.closeDrawer(GravityCompat.END)
            }
        }

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
            R.id.menu_meeting_list to MeetingListActivity::class.java,
            R.id.menu_route to RouteActivity::class.java,
            R.id.menu_restaurant to RestaurantActivity::class.java,
            R.id.menu_location_share to LocationShareActivity::class.java,
            R.id.menu_schedule to ScheduleActivity::class.java
        )

        menuIds.forEach { (id, activityClass) ->
            findViewById<TextView>(id)?.setOnClickListener {
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }
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