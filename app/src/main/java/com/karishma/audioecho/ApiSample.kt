package com.karishma.audioecho

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.common.util.PlatformVersion
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.material.snackbar.Snackbar

class ApiSample : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_sample)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun onClickSnapshot(view: View) {
        Log.d(TAG, "onClickSnapshot()")

        if (activityRecognitionPermissionApproved()) {
            requestSnapshot()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    // Permission is checked in onClickSnapshot before this method is called.
    @SuppressLint("MissingPermission")
    private fun requestSnapshot() {
        Log.d(TAG, "requestSnapshot()")

        val task = Awareness.getSnapshotClient(this).detectedActivity

        task.addOnCompleteListener { taskResponse ->
            if (taskResponse.isSuccessful) {
                val detectedActivityResponse = taskResponse.result
                val activityRecognitionResult = detectedActivityResponse.activityRecognitionResult
                Log.d(TAG, "Snapshot successfully retrieved: $activityRecognitionResult")
                printSnapshotResult(detectedActivityResponse.activityRecognitionResult)
            } else {
                Log.d(TAG, "Data was not able to be retrieved: ${taskResponse.exception}")
            }
        }
    }

    private fun activityRecognitionPermissionApproved(): Boolean {
        // Permission check for 29+.
        return if (PlatformVersion.isAtLeastQ()) {
            PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            true
        }
    }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted && PlatformVersion.isAtLeastQ()) {
                // Permission denied on Android platform that supports runtime permissions.
                displayPermissionSettingsSnackBar()
            } else {
                // Permission was granted (either by approval or Android version below Q).
                binding.output.text = getString(R.string.permission_approved)
            }
        }

    private fun displayPermissionSettingsSnackBar() {
        Snackbar.make(
            binding.mainActivity,
            R.string.permission_rational,
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.action_settings) {
                // Build intent that displays the App settings screen.
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts(
                    "package",
                    BuildConfig.APPLICATION_ID,
                    null
                )
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            .show()
    }

    private fun printSnapshotResult(activityRecognitionResult: ActivityRecognitionResult) {
        val timestamp = Calendar.getInstance().time.toString()
        val output = "Current Snapshot ($timestamp):\n\n$activityRecognitionResult"
        binding.output.text = output
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
