package com.aya.acam.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import timber.log.Timber

object PermissionUtils {

    val CAMERA_PERMISSION_REQUEST_CODE = 100
    val RAED_MEDIA_PERMISSION_REQUEST_CODE = 101

    fun checkPermission(@NonNull context: Context, @NonNull permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(
        @NonNull activity: Activity,
        @NonNull permission: String,
        @NonNull permissionCode: Int
    ): Boolean {
        val permissionsArray = arrayOf(permission)
        return requestPermission(activity, permissionsArray, permissionCode)
    }

    private fun requestPermission(
        @NonNull activity: Activity,
        @NonNull permissions: Array<String>,
        @NonNull permissionCode: Int
    ): Boolean {
        permissions.forEach { action ->
            if (!checkPermission(activity.applicationContext, action)) {
                Timber.d("request permission: $action")

                ActivityCompat.requestPermissions(
                    activity,
                    permissions,
                    permissionCode
                )
                return true
            }
            Timber.d("already has permission: $action")
        }
        return false
    }

    fun requestCameraPermission(@NonNull activity: Activity): Boolean {
        return requestPermission(
            activity,
            android.Manifest.permission.CAMERA,
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    fun requestRecordPermission(@NonNull activity: Activity): Boolean {
        val recordGroupPermission =
            arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        return requestPermission(
            activity,
            recordGroupPermission,
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    fun requsetReadMediaPermission(@NonNull activity: Activity): Boolean {
        val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activity.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )

        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        return requestPermission(
            activity,
            mediaPermission,
            RAED_MEDIA_PERMISSION_REQUEST_CODE
        )
    }

    fun requsetWriteMediaPermission(@NonNull activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        ) {
            return requestPermission(
                activity,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                RAED_MEDIA_PERMISSION_REQUEST_CODE
            )
        }
        return false
    }

}