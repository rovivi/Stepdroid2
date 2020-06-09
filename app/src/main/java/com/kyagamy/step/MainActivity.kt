package com.kyagamy.step

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kyagamy.step.fragments.CategoryFragament
import com.kyagamy.step.fragments.songs.SongsList


class MainActivity : AppCompatActivity() {

    private lateinit var text: TextView
    private lateinit var button: Button
    private var positionCategory =2
    private  val manager = supportFragmentManager

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showFragmentCategory()
    }

    override fun onBackPressed() {
        if (manager.fragments.size>1)
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

    private fun showFragmentCategory (){
        val transaction = manager.beginTransaction()
        val  fragmentCategory =CategoryFragament.newInstance(positionCategory)
        transaction.setCustomAnimations(R.anim.fragment_fade_enter,R.anim.fragment_fade_exit)
        transaction.replace(R.id.fragment_holder, fragmentCategory)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun showFragmentTwo(category:String) {
        val transaction = manager.beginTransaction()
        val fragment = SongsList.newInstance(category,"")
        transaction.setCustomAnimations(R.anim.fragment_fade_enter,R.anim.fragment_fade_exit)
        transaction.add(R.id.fragment_holder, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun changeCategory (category:String,categoryPosition: Int){
        positionCategory= categoryPosition
        Toast.makeText(this, "value:${category}",Toast.LENGTH_SHORT).show()
        showFragmentTwo(category)
    }

}
