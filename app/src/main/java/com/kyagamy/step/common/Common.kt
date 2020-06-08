package com.kyagamy.step.Common

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Point
import android.os.AsyncTask
import android.os.Environment
import android.provider.Settings.Global.getString
import android.util.DisplayMetrics
import android.view.WindowManager
import com.kyagamy.step.R
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException

class Common {
    companion object {
        private val commonTime = 41.6f

        var HIDE_PAD = false
        var WIDTH = 0
        var HEIGHT = 0
        var OFFSET = 0
        var HIDENAVBAR = false
        var AnimateFactor = 100
        var START_Y = 0.115f
        var testingRadars = false
        var DRAWSTATS = false
        var Compression2D = 0
        var EVALUATE_ON_SECUNDARY_THREAD = true
        var ANIM_AT_START = true
        var RELOAD_SONGS = false
        var APP_NAME = "StepDroid"


        val PIU_ARROW_NAMES =
            arrayOf("down_left_", "up_left_", "center_", "up_right_", "down_right_")
        private val JudgeSJ = doubleArrayOf(41.6, 41.6, 41.6, 83.3 + commonTime)
        private val JudgeEJ = doubleArrayOf(41.6, 41.6, 41.6, 58.3 + commonTime)
        private val JudgeNJ = doubleArrayOf(41.6, 41.6, 41.6, 41.6 + commonTime)
        private val JudgeHJ = doubleArrayOf(41.6, 41.6, 41.6, 25.5 + commonTime)
        private val JudgeVJ = doubleArrayOf(33.3, 33.3, 33.3, 8.5 + commonTime)
        private val JudgeXJ =
            doubleArrayOf(16.6, 16.6, 16.6, 16.6, 16.6 + commonTime)
        private val JudgeUJ = doubleArrayOf(
            8.3, 8.3, 8.3, 8.3, 8.3 + commonTime
        )
        val JUDMENT =
            arrayOf(JudgeSJ, JudgeEJ, JudgeNJ, JudgeHJ, JudgeVJ, JudgeXJ, JudgeUJ)




        fun getRandomNumberInRange(min: Int, max: Int): Int {
            require(min < max) { "max must be greater than min" }
            val r = Random()
            return r.nextInt(max - min + 1) + min
        }




        @Throws(Exception::class)
        fun convertStreamToString(`is`: FileInputStream?): String? {
            val reader = BufferedReader(InputStreamReader(`is`))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            reader.close()
            return sb.toString()
        }

        fun second2Beat(second: Double, BPM: Double): Double {
            return second / (60 / BPM)
        }

        fun beat2Second(beat: Double, BPM: Double): Double {
            return beat * (60 / BPM)
        }

        fun changeCharInPosition(
            position: Int,
            ch: Char,
            str: String
        ): String? {
            val charArray = str.toCharArray()
            charArray[position] = ch
            return String(charArray)
        }


        private fun getScreenResolution(context: Context): Point? {
            val wm =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            return Point(metrics.widthPixels, metrics.heightPixels)
        }


        fun isAppInLowMemory(context: Context): Boolean {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            return memoryInfo.lowMemory
        }


        @Throws(IOException::class)
        fun is2String(inputStream: InputStream): String? {
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } != -1) {
                result.write(buffer, 0, length)
            }
            return result.toString("UTF-8")
        }


//    fun bgaExist(url: String?): Boolean {
//        return try {
//            checkBga().execute(url).get()
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//            false
//        } catch (e: ExecutionException) {
//            e.printStackTrace()
//            false
//        }
//    }
////
////
//    class checkBga :  AsyncTask<String?, Void?, Boolean>() {
//        override fun onPreExecute() {}
//        protected override fun doInBackground(vararg params: String): Boolean {
//            return try {
//                HttpURLConnection.setFollowRedirects(false)
//                val con =
//                    URL(params[0]).openConnection() as HttpURLConnection
//                con.requestMethod = "HEAD"
//                println(con.responseCode)
//                con.responseCode == HttpURLConnection.HTTP_OK
//            } catch (e: Exception) {
//                e.printStackTrace()
//                false
//            }
//        }
//
//        override fun onPostExecute(result: Boolean) {
//            if (result == true) {
//                //Toast.makeText(MainActivity.this, "File exists!", Toast.LENGTH_SHORT).show();
//            } else {
//                //Toast.makeText(MainActivity.this, "File does not exist!", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }


        fun compareRecords(
            context: Context,
            nameSongs: String?,
            percent: Float
        ): Boolean {
            val sharedPref =
                context.getSharedPreferences("stepmix", Context.MODE_PRIVATE)
            val oldRecord = sharedPref.getFloat(nameSongs, 0f)
            return oldRecord <= percent
        }


        fun writeRecord(
            context: Context,
            nameSongs: String?,
            value: Float
        ) {
            val sharedPref =
                context.getSharedPreferences("stepmix", Context.MODE_PRIVATE)
            sharedPref.edit().putFloat(nameSongs, value).clear().apply()
        }

        fun getRecords(
            context: Context,
            nameSongs: String?
        ): String? {
            val sharedPref =
                context.getSharedPreferences("stepmix", Context.MODE_PRIVATE)
            val result = sharedPref.getFloat(nameSongs, -1f)
            return if (result != -1f) result.toString() + "" else "N/A"
        }



    }

}