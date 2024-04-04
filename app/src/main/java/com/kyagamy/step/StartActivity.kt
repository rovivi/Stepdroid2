package com.kyagamy.step

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codekidlabs.storagechooser.StorageChooser
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.kyagamy.step.databinding.ActivityStartBinding
import kotlinx.coroutines.launch

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupButtons()
        checkPermissions()
        validateRoute()
    }

    private fun setupButtons() {
        with(binding) {
            buttonStart.setOnClickListener { navigateTo(MainActivity::class.java) }
            dragStartButton.setOnClickListener { navigateTo(DragStepActivity::class.java) }
            reloadSings.setOnClickListener { navigateTo(LoadingSongActivity::class.java) }
            buttonDS.setOnClickListener { navigateTo(DownloadUpdateFiles::class.java) }
        }
    }

    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this, destination)
        startActivity(intent)
    }

    private fun checkPermissions() {
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                showToast("Permission Granted")
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
                showToast("Permission Denied\n$deniedPermissions")
            }
        }

        TedPermission.create()
            .setPermissionListener(permissionListener)
            .setDeniedMessage("If you reject permission, you cannot use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
            .setPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .check()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun validateRoute() {
        val sharedPref = getSharedPreferences("pref", Context.MODE_PRIVATE)
        val basePath = sharedPref.getString(getString(R.string.base_path), "noPath")
        if (basePath == "noPath") {
            showStorageChooser()
        }
    }

    private fun showStorageChooser() {
        val chooser = StorageChooser.Builder()
            .withActivity(this)
            .withFragmentManager(fragmentManager) // Make sure you are using the correct fragmentManager here
            .setDialogTitle("Choose Destination Folder")
            .withMemoryBar(true)
            .build()

        chooser.show()
        chooser.setOnSelectListener { path ->
            lifecycleScope.launch {
                saveBasePath(path)
                navigateTo(LoadingSongActivity::class.java)
            }
        }
    }

    private fun saveBasePath(path: String) {
        val sharedPref = getSharedPreferences("pref", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(getString(R.string.base_path), path)
            apply()
        }
    }
}