package com.example.camera

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity() {

    val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    val STORAGE_PERMISSION = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE
        , Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val FLAG_PERM_CAMERA = 98
    val FLAG_PERM_STORAGE = 99

    val FLAG_REQ_CAMERA = 101
    val FLAG_REQ_GALLERY = 102
    var photoURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(isPermitted(STORAGE_PERMISSION, FLAG_PERM_STORAGE)){
            setViews()
        }
    }

    private fun setViews()  {
        buttonCamera.setOnClickListener {
            openCamera()
        }
        buttonGallery.setOnClickListener {
            openGallery()
        }
    }


    private fun openCamera()    {
        if(isPermitted(CAMERA_PERMISSION,FLAG_PERM_CAMERA)){
            dispatchTakePictureIntent()
        }
    }

    private fun openGallery()   {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        startActivityForResult(intent, FLAG_REQ_GALLERY)
    }

    private fun createImageUri(filename:String, mimeType:String) : Uri? {
        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun dispatchTakePictureIntent() {

        // 카메라 인텐트 생성
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        createImageUri(newFileName(), "image/jpeg")?.let {
                uri -> photoURI = uri
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, FLAG_REQ_CAMERA)
        }
    }

    private fun newFileName() : String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())
        return filename
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
            when(requestCode){
                FLAG_REQ_CAMERA -> {
                    if (photoURI != null) {
                        val bitmap = loadBitmapFromMediaStoreBy(photoURI!!)
                        imagePreview.setImageBitmap(bitmap)
                        photoURI = null // 사용 후 null 처리
                    }
                }
                FLAG_REQ_GALLERY -> {
                    val uri = data?.data
                    Glide.with(this).load(uri).into(imagePreview)
                }
            }
        }
    }

// Uri 로 미디어 스토어의 이미지를 불러오는 함수를 작성
fun loadBitmapFromMediaStoreBy(photoUri: Uri): Bitmap? {
    var image: Bitmap? = null
    try {
        image = if (Build.VERSION.SDK_INT > 27) { // Api 버전별 이미지 처리
            val source: ImageDecoder.Source =
                ImageDecoder.createSource(this.contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(this.contentResolver, photoUri)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return image
}

    /**
     * 권한처리
     */

    private fun isPermitted(permissions:Array<String>, flag:Int) : Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                val result = ContextCompat.checkSelfPermission(this, permission)
                if (result != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, flag)
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode)  {
            FLAG_PERM_CAMERA -> {
                var checked = true
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) {
                        checked = false
                        break
                    }
                }
                if (checked)    {
                    openCamera()
                }
            }
            // P. 이 코드가 없으면 처음 파일 접근 permission 을 받고 CAMERA를 누르면 반응이 없다.
            FLAG_PERM_STORAGE -> {
                for (grant in grantResults) {
                    if(grant != PackageManager.PERMISSION_GRANTED)  {
                        finish()
                        return
                    }
                }
                setViews()
            }
        }
    }
}