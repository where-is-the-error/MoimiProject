package com.moimiApp.moimi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.moimiApp.moimi.databinding.ActivityMyPageBinding
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class MyPageActivity : BaseActivity() {

    private lateinit var binding: ActivityMyPageBinding
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                // 선택한 이미지를 미리보기로 보여줄 때도 캐시 옵션 적용 (선택 사항)
                Glide.with(this).load(uri).circleCrop().into(binding.ivMyProfile)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar("내 정보 수정")
        setupDrawer()

        val myName = prefsManager.getUserName() ?: ""
        val myId = prefsManager.getUserId() ?: ""
        val myProfileUrl = prefsManager.getUserProfileImg()

        binding.etMyName.setText(myName)
        binding.tvMyEmail.text = myId

        if (!myProfileUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(myProfileUrl)
                .circleCrop()
                .placeholder(R.drawable.profile)
                .into(binding.ivMyProfile)
        } else {
            Glide.with(this).load(R.drawable.profile).circleCrop().into(binding.ivMyProfile)
        }

        binding.ivMyProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        binding.btnSaveProfile.setOnClickListener {
            val newName = binding.etMyName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateName(newName)
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri!!)
            }
        }
    }

    private fun updateName(newName: String) {
        val token = getAuthToken()
        val userId = prefsManager.getUserId() ?: return
        val body = mapOf("name" to newName)

        RetrofitClient.userInstance.updateProfile(token, userId, body).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                if (response.isSuccessful) {
                    prefsManager.saveUserName(newName)
                    setupDrawer() // 드로어 메뉴의 이름도 갱신
                    Toast.makeText(this@MyPageActivity, "이름이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                Toast.makeText(this@MyPageActivity, "이름 수정 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadImage(uri: Uri) {
        val token = getAuthToken()
        val userId = prefsManager.getUserId() ?: return
        val file = uriToFile(uri)
        if (file == null) {
            Toast.makeText(this, "이미지 변환 실패", Toast.LENGTH_SHORT).show()
            return
        }

        val mediaType = MediaType.parse("image/*")
        val requestFile = RequestBody.create(mediaType, file)
        val body = MultipartBody.Part.createFormData("profileImage", file.name, requestFile)

        RetrofitClient.userInstance.uploadProfileImage(token, userId, body).enqueue(object : Callback<UploadProfileResponse> {
            override fun onResponse(call: Call<UploadProfileResponse>, response: Response<UploadProfileResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val newUrl = response.body()?.profileImgUrl
                    if (newUrl != null) {
                        prefsManager.saveUserProfileImg(newUrl)
                        setupDrawer() // 드로어 프로필 갱신

                        // ★ [핵심] 캐시 무시하고 즉시 새로고침
                        runOnUiThread {
                            Glide.with(this@MyPageActivity)
                                .load(newUrl)
                                .diskCacheStrategy(DiskCacheStrategy.NONE) // 디스크 캐시 끄기
                                .skipMemoryCache(true) // 메모리 캐시 끄기
                                .circleCrop()
                                .into(binding.ivMyProfile)

                            Toast.makeText(this@MyPageActivity, "프로필 사진 업데이트 완료!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@MyPageActivity, "사진 업로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UploadProfileResponse>, t: Throwable) {
                Log.e("MyPage", "Upload Error", t)
                Toast.makeText(this@MyPageActivity, "서버 통신 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("profile", ".jpg", cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}