package com.moimiApp.moimi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class ScheduleDetailBottomSheet : BottomSheetDialogFragment() {

    var onDismissListener: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_schedule_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString("title") ?: ""
        val date = arguments?.getString("date") ?: ""
        val time = arguments?.getString("time") ?: ""
        val location = arguments?.getString("location") ?: ""
        val scheduleId = arguments?.getString("scheduleId") ?: ""
        val inviteCode = arguments?.getString("inviteCode") ?: ""

        val tvTitle = view.findViewById<TextView>(R.id.tv_bs_title)
        val tvDateTime = view.findViewById<TextView>(R.id.tv_bs_date_time)
        val tvLocation = view.findViewById<TextView>(R.id.tv_bs_location)
        val tvCode = view.findViewById<TextView>(R.id.tv_bs_invite_code)
        val btnCopy = view.findViewById<ImageView>(R.id.btn_copy_code)

        // 버튼 ID 확인: activity_schedule_detail.xml이 아니라 bottom_sheet_schedule_detail.xml에 있어야 함
        val btnInviteLink = view.findViewById<MaterialButton>(R.id.btn_bs_invite)

        tvTitle.text = title
        tvDateTime.text = "$date  |  $time"
        tvLocation.text = location
        tvCode.text = inviteCode

        // 6자리 코드 복사
        btnCopy.setOnClickListener {
            copyToClipboard("초대 코드", inviteCode)
        }

        // 링크 공유 버튼 (레이아웃에 버튼이 있다면 동작)
        btnInviteLink?.setOnClickListener {
            if (scheduleId.isNotEmpty()) {
                shareInviteLink(title, scheduleId)
            }
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label 복사됨: $text", Toast.LENGTH_SHORT).show()
    }

    private fun shareInviteLink(title: String, scheduleId: String) {
        val inviteUrl = "http://moimi.app/invite/$scheduleId"
        val shareText = """
            [모이미 초대장]
            '$title' 일정에 초대합니다!
            
            링크: $inviteUrl
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "초대 링크 공유"))
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    companion object {
        const val TAG = "ScheduleDetailBottomSheet"

        // ⭐ [중요] 이 함수가 있어야 AddScheduleActivity에서 호출 가능합니다.
        fun newInstance(
            title: String,
            date: String,
            time: String,
            location: String,
            scheduleId: String,
            inviteCode: String
        ): ScheduleDetailBottomSheet {
            val fragment = ScheduleDetailBottomSheet()
            fragment.arguments = Bundle().apply {
                putString("title", title)
                putString("date", date)
                putString("time", time)
                putString("location", location)
                putString("scheduleId", scheduleId)
                putString("inviteCode", inviteCode)
            }
            return fragment
        }
    }
}