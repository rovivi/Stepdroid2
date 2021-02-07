package com.kyagamy.step

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.codekidlabs.storagechooser.StorageChooser
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        hideSystemUI()
        val intentSongList = Intent(this, MainActivity::class.java)
        val intent = Intent(this, LoadingSongActivity::class.java)
        val intentDrag = Intent(this, DragStepActivity::class.java)
        this.button_start.setOnClickListener {
            startActivity(intentSongList)
        }
        this.dragStartButton.setOnClickListener {
            startActivity(intentDrag)
        }


        //Se valida el permission
        val permissionListener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                Toast.makeText(this@StartActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
                Toast.makeText(
                    this@StartActivity,
                    "Permission Denied\n$deniedPermissions",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
            .setPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .check()
        //Route validation
        val sharedPref = this.getSharedPreferences("pref", Context.MODE_PRIVATE)
        val basePath = sharedPref.getString(getString(R.string.base_path), "noPath")
        if (basePath == "noPath") {
            val chooser: StorageChooser = StorageChooser.Builder()
                .withActivity(this@StartActivity)
                .withFragmentManager(this.fragmentManager)
                .setDialogTitle("Choose StepDroid Destination Folder")
                .withMemoryBar(true)
                .build()
            chooser.show()
            chooser.setOnSelectListener { path ->
                lifecycleScope.run {
                    val paths = path + ""
                    with(sharedPref.edit()) {
                        putString(getString(R.string.base_path), paths)
                        commit()
                    }
                    startActivity(intent)
                }
            }
        } else {
            // Toast
            // startActivity(intent)
        }
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}
