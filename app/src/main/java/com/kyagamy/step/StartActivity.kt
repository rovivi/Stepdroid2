package com.kyagamy.step

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.codekidlabs.storagechooser.StorageChooser
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission

class StartActivity : AppCompatActivity() {

    private lateinit var  buttonStart :Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        buttonStart = findViewById(R.id.button_start)
        var intentSongList =  Intent(this,MainActivity::class.java)

        buttonStart.setOnClickListener{
            startActivity(intentSongList)
        }

        val intent = Intent(this,LoadingSongActivity::class.java)
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
        val sharedPref = this.getSharedPreferences("pref",Context.MODE_PRIVATE)
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
                lifecycleScope.run{
                    val paths = path + ""
                    with(sharedPref.edit()) {
                        putString(getString(R.string.base_path), paths)
                        commit()
                    }
                    startActivity(intent)
                }
            }
        }
        else {
            // Toast
          //   startActivity(intent)
        }
    }
}
