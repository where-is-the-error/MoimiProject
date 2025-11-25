package com.example.moimi

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.moimiApp.moimi.R

// **[중요]: 여기서는 com.moimiApp.moimi.R 임포트를 삭제했습니다.
// R 클래스는 프로젝트 패키지(com.example.moimi)에서 자동 참조됩니다.**

class MainActivity : AppCompatActivity() {

    // **lateinit 대신 by lazy를 사용하여 변수가 처음 접근될 때 findViewById()를 수행합니다.**
    // 이 방식이 더 간결하고 권장되는 방식입니다.
    private val cb1: CheckBox by lazy { findViewById(R.id.cb1) }
    private val cb2: CheckBox by lazy { findViewById(R.id.cb2) }
    private val cb3: CheckBox by lazy { findViewById(R.id.cb3) }
    private val checkButton: Button by lazy { findViewById(R.id.checkButton) }
    private val notificationIcon: ImageView by lazy { findViewById(R.id.notificationIcon) }
    private val menuIcon: ImageView by lazy { findViewById(R.id.menuIcon) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // **findViewById() 호출은 by lazy 블록 안에서 이미 처리되었습니다.**
        // 이제 변수에 바로 리스너를 붙이면 됩니다.

        // 버튼 클릭 리스너 설정
        checkButton.setOnClickListener {
            // 선택된 항목들을 리스트에 담아 joinToString으로 깔끔하게 연결합니다.
            val selectedItems = mutableListOf<String>()
            if (cb1.isChecked) selectedItems.add(cb1.text.toString())
            if (cb2.isChecked) selectedItems.add(cb2.text.toString())
            if (cb3.isChecked) selectedItems.add(cb3.text.toString())

            val selected = selectedItems.joinToString(" ") // 공백으로 연결

            Toast.makeText(this, "선택: $selected", Toast.LENGTH_SHORT).show()
        }

        // 알림 아이콘 클릭 리스너 설정
        notificationIcon.setOnClickListener {
            Toast.makeText(this, "알림이 없습니다", Toast.LENGTH_SHORT).show()
        }

        // 메뉴 아이콘 클릭 리스너 설정
        menuIcon.setOnClickListener {
            Toast.makeText(this, "메뉴 클릭됨", Toast.LENGTH_SHORT).show()
        }
    }
}