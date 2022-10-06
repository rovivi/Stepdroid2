package com.kyagamy.step

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import com.downloader.PRDownloader
//import com.downloader.PRDownloaderConfig
//import com.liulishuo.filedownloader.BaseDownloadTask
//import com.liulishuo.filedownloader.FileDownloadListener
//import com.liulishuo.filedownloader.FileDownloader



class DownloadUpdateFiles : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_update_files)
        //this.DownloadButton.setOnClickListener { download() }

//        FileDownloader.setupOnApplicationOnCreate(application)
//        FileDownloader.setup(this)
//        //PRDownloader.initialize(applicationContext);
//
//
//// Setting timeout globally for the download network requests:
//
//// Setting timeout globally for the download network requests:
//        val config = PRDownloaderConfig.newBuilder()
//            .setReadTimeout(30000)
//            .setConnectTimeout(15000)
//            .build()
//        PRDownloader.initialize(applicationContext, config)

    }


    private fun download() {

        val sharedPref = this.getSharedPreferences("pref", Context.MODE_PRIVATE)

        val urlCon = "https://gitlab.com/SSCFILES/p2/-/archive/master/p2-master.zip"
        val basePath = sharedPref.getString(getString(R.string.base_path), "noPath")


        val path = getExternalFilesDir("/pathhxx");


//
//
//        FileDownloader.getImpl().create(urlCon)
//            .setPath(path.toString())
//            .setListener(object : FileDownloadListener() {
//                override fun pending(task: BaseDownloadTask, soFarBytes: Int, totalBytes: Int) {
//                    textView5.text="$soFarBytes"
//                }
//                override fun started(task: BaseDownloadTask) {
//                    showToast("start")
//                }
//                override fun connected(
//                    task: BaseDownloadTask,
//                    etag: String,
//                    isContinue: Boolean,
//                    soFarBytes: Int,
//                    totalBytes: Int
//                ) {
//                    showToast("conet")
//                }
//
//                override fun progress(task: BaseDownloadTask, soFarBytes: Int, totalBytes: Int) {
//                    textView5.text="$soFarBytes"
//
//
//                }
//                override fun blockComplete(task: BaseDownloadTask) {
//
//                }
//                override fun retry(
//                    task: BaseDownloadTask,
//                    ex: Throwable,
//                    retryingTimes: Int,
//                    soFarBytes: Int
//                ) {
//                }
//
//                override fun completed(task: BaseDownloadTask) {showToast("Copletedd")}
//                override fun paused(task: BaseDownloadTask, soFarBytes: Int, totalBytes: Int) {}
//                override fun error(task: BaseDownloadTask, e: Throwable) {showToast("error")}
//                override fun warn(task: BaseDownloadTask) {}
//            }).start()
//        val file = File(path.toString() + "/file.ext")
//        try {
//            file.createNewFile()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }

////
        val request = DownloadManager.Request(Uri.parse(urlCon))

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        request.allowScanningByMediaScanner()

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(this,"downloa", "fil.zip");

        //request.setDestinationInExternalPublicDir("/", "rar.zip");

         val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)

    }


    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}