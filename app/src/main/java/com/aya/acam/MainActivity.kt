package com.aya.acam

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.aya.acam.utils.PermissionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity(), View.OnSystemUiVisibilityChangeListener,
    View.OnTouchListener {

    private var hideSystemUiJob: Job? = null
    val navController by lazy {
        findNavController(R.id.fragment_container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideSystemUI()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        window.decorView.setOnTouchListener(this)
        window.decorView.setOnSystemUiVisibilityChangeListener(this)

        PermissionUtils.requsetReadMediaPermission(this)
        PermissionUtils.requsetWriteMediaPermission(this)
        if (!PermissionUtils.requestRecordPermission(this)) {
//            starCameraFragment()
        }

    }

    private fun starCameraFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CameraFragment.newInstance())
            .commit()

    }

    private fun navigateToCameraFragment() {
        navController.navigate(R.id.cameraFragment)
    }

    private fun starGalleryFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GalleryFragment.newInstance())
            .commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionUtils.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    starCameraFragment()
                } else {
                    // 用户拒绝了相机权限，可能需要显示一个提示或者提供另外的方案
                    Timber.e("User denied camera permission")
                }
            }

            PermissionUtils.RAED_MEDIA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("get RAED_MEDIA_PERMISSION_REQUEST_CODE ")
                    //TODO:優化權限獲取流程
                    PermissionUtils.requestCameraPermission(this)
                } else {
                    // 用户拒绝了相机权限，可能需要显示一个提示或者提供另外的方案
                    Timber.e("User denied read media permission")
                }
            }
        }
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Workaround for Android Q memory leak issue in IRequestFinishCallback$Stub.
            // (https://issuetracker.google.com/issues/139738913)
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    private fun hideSystemUI() {
        return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 在 Android 11 及以上版本中使用以下方式隐藏系统 UI
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            Timber.d("hideSystemUI")
            this.window?.decorView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun hideSystemUIDelay(timeout: Long) {
        return
        hideSystemUiJob?.cancel()
        // 启动新的协程来重新倒计时
        hideSystemUiJob = lifecycleScope.launch {
            delay(timeout)
            hideSystemUI()
        }
    }

    override fun onSystemUiVisibilityChange(visibility: Int) {
        Timber.d("onSystemUiVisibilityChange: visibility= $visibility")
        if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
            // TODO: The system bars are visible. Make any desired
            // adjustments to your UI, such as showing the action bar or
            // other navigational controls.

        } else {
            // TODO: The system bars are NOT visible. Make any desired
            // adjustments to your UI, such as hiding the action bar or
            // other navigational controls.
            hideSystemUIDelay(5000L)
        }
    }


    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        hideSystemUIDelay(5000L)
        return false
    }

}