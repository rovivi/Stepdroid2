package com.kyagamy.step

import android.content.DialogInterface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kyagamy.step.databinding.ActivityMainBinding
import com.kyagamy.step.fragments.CategoryFragament
import com.kyagamy.step.fragments.songs.SongsList


class MainActivity : FullScreenActivity() {
    // Reference to "name" TextView using synthetic properties.
    private  lateinit var binding :ActivityMainBinding

// Reference to "name" TextView using the binding class instance.




    private lateinit var fragmentCategory: CategoryFragament
    private lateinit var text: TextView
    private lateinit var button: Button
    private var positionCategory = 2
    private val manager = supportFragmentManager


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
        fragmentCategory = CategoryFragament.newInstance(positionCategory)




        showFragmentCategory()

        //video
        val rawId = R.raw.ssmbg
        val path = "android.resource://$packageName/$rawId"
        binding.bgVideo.setOnPreparedListener {
            it.isLooping = true
            it.setVolume(0f, 0f)
        }
        binding.bgVideo.setVideoURI(Uri.parse(path))
        binding.bgVideo.start()


    }

    override fun onPause() {
        super.onPause()
        binding.bgVideo.pause()
    }

    override fun onStop() {
        super.onStop()
        binding.bgVideo.pause()
    }

    override fun onPostResume() {
        super.onPostResume()
        binding.bgVideo.start()
    }

    override fun onBackPressed() {
        if (manager.fragments.size > 1)
            super.onBackPressed()
        else {
            AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Confirm")
                .setMessage("Are you sure you want to close StepDroid")
                .setPositiveButton("Yes",
                    DialogInterface.OnClickListener { _, _ -> finish() })
                .setNegativeButton("No", null)
                .show()
        }
    }


    fun showFragmentCategory() {
        val transaction = manager.beginTransaction()
       // transaction.setCustomAnimations(R.anim.fragment_fade_enter, R.anim.fragment_fade_exit)
        transaction.replace(R.id.fragment_holder, fragmentCategory)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun showFragmentSongList(category: String) {
        val transaction = manager.beginTransaction()
        val fragment = SongsList.newInstance(category, "")
       // transaction.setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_fade_exit)
        transaction.replace(R.id.fragment_holder, fragment)
        transaction.addToBackStack("changetocategory")
        transaction.commit()
    }

    fun changeCategory(category: String, categoryPosition: Int) {
        positionCategory = categoryPosition
        Toast.makeText(this, "value:${category}", Toast.LENGTH_SHORT).show()
        showFragmentSongList(category)
    }



}
