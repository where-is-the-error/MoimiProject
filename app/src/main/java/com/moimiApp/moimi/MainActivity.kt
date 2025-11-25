package com.moimiApp.moimi // ⚠️ 사용자 패키지명 (꼭 확인!)

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair // Pair 사용을 위해 import
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ⚠️ 아래 ID들(R.id.map, R.id.imageView 등)이 실제 XML에 있는지 꼭 확인하세요!
        // 만약 XML에 없으면 빨간 줄이 뜰 수 있으니, 없는 건 지우거나 주석 처리하세요.
        val mapImage = findViewById<ImageView>(R.id.map)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val imageView4 = findViewById<ImageView>(R.id.imageView4)
        val imageView5 = findViewById<ImageView>(R.id.imageView5)

        mapImage.setOnClickListener {
            val intent = Intent(this, MapDetailActivity::class.java)

            // 전환 애니메이션 설정 (공유 요소)
            val p1 = Pair.create<View, String>(mapImage, "map_transition")
            val p2 = Pair.create<View, String>(imageView, "image1_transition")
            val p3 = Pair.create<View, String>(imageView4, "image4_transition")
            val p4 = Pair.create<View, String>(imageView5, "image5_transition")

            val options = ActivityOptions.makeSceneTransitionAnimation(this, p1, p2, p3, p4)

            startActivity(intent, options.toBundle())
        }
    }
}