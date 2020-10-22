package com.example.faceapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        const val CAMERA_REQUEST_CODE = 1
        const val CAMERA_PERMISSION_REQUEST_CODE = 2
        private const val READ_REQUEST_CODE: Int = 42
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    /**
     * 写真選択ボタン
     */
    fun clickSelectPicture(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    /**
     * カメラ起動ボタン
     */
    fun clickPicture(view : View) {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            takePicture()
        }
    }

    /**
     * カメラアプリの起動
     */
    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    /**
     * 他アプリからのコールバック
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK){ return }

        if (requestCode == CAMERA_REQUEST_CODE) {
            Log.d("Debug", "カメラ画像取得")
            data?.extras?.get("data")?.let {
                cameraImage.setImageBitmap(it as Bitmap)
            }
        } else if(requestCode == READ_REQUEST_CODE) {
            Log.d("Debug", "画像取得")
            data?.data?.also { uri ->
                val inputStream = contentResolver?.openInputStream(uri)
                val image = BitmapFactory.decodeStream(inputStream)
                cameraImage.setImageBitmap(image as Bitmap)
            }
        }
    }

    /**
     * 判定ボタン
     */
    fun clickSubmit(view : View) = if (cameraImage.drawable == null){
        Toast.makeText(this, "カメラを起動して判定する画像を選択してください", Toast.LENGTH_LONG).show()
    }else{
        val bmp : Bitmap = cameraImage.drawable.toBitmap()

        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val jpgarr: ByteArray = baos.toByteArray()
        val handler = Handler()
        val url = "http://118.27.37.1/face-mobile"
        val media = "multipart/form-data".toMediaTypeOrNull()
        val requestBody: RequestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "person.jpeg", RequestBody.create(media, jpgarr))
                .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Debug", "通信失敗")
                runOnUiThread {
                    Toast.makeText(applicationContext, "通信に失敗しました。", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseText : String? = response.body?.string()
                handler.post {
                    Log.d("Debug", "通信成功")
                    Log.d("Debug", responseText)
                }
                runOnUiThread {
                    val showText = if(responseText.equals("unknown")){
                        "検出できませんでした。"
                    }else{
                        responseText + "さんと判定されました。"
                    }
                    Toast.makeText(applicationContext, showText, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            }
        }
    }
}
