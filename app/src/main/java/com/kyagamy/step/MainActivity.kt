package com.kyagamy.step

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.codekidlabs.storagechooser.StorageChooser
import com.gigamole.infinitecycleviewpager.HorizontalInfiniteCycleViewPager
import com.gigamole.infinitecycleviewpager.OnInfiniteCyclePageTransformListener
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.kyagamy.step.adapters.CategoryAdapter
import com.kyagamy.step.room.entities.Category
import com.kyagamy.step.viewModels.CategoryViewModel
import com.kyagamy.step.viewModels.SongViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var editWordView: EditText
    private lateinit var songsModel: SongViewModel
    private lateinit var categoryModel: CategoryViewModel
    private lateinit var text: TextView
    private lateinit var button: Button
    private lateinit var cycle : HorizontalInfiniteCycleViewPager

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        //
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                Toast.makeText(this@MainActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
                Toast.makeText(
                    this@MainActivity,
                    "Permission Denied\n$deniedPermissions",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            .check();




        songsModel = ViewModelProvider(this).get(SongViewModel::class.java)
        categoryModel= ViewModelProvider(this).get(CategoryViewModel::class.java)

        text = findViewById(R.id.textView2)
        button=findViewById(R.id.btnOpenLoadSong)

//        val data:List<Category> = categoryModel.allCategory.value!!



        cycle = findViewById(R.id.cycle)
        cycle.setOnClickListener(View.OnClickListener { v:View->
            run {
                Toast.makeText(this, "awa"+cycle.realItem,Toast.LENGTH_LONG).show()
            }
        })







        var paths = ""
        text.setOnClickListener {
            lifecycleScope.launch {
                for (i in 1..100) {
                    delay(1000)
                    text.text = i.toString().plus("Seconds");
                }
            }

            val chooser: StorageChooser = StorageChooser.Builder()
                .withActivity(this@MainActivity)
                .withFragmentManager(this.fragmentManager)
                .setDialogTitle("Choose Stepdroid Destination Folder")
                .withMemoryBar(true)
                .build()

            chooser.show()

            chooser.setOnSelectListener { path ->
                run {
                    paths = path + ""
                    val sharedPref = this?.getPreferences(Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString(getString(R.string.base_path), paths)
                        commit()
                    }
                    Toast.makeText(
                        this,
                        "Se ha asignado la fuente de las canciones" + paths,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        button.setOnClickListener{
            lifecycleScope.launch {
                reloadSongs()
            }
        }
        val list = ArrayList<Category>()
        val adapter = CategoryAdapter(list,this)

        cycle.adapter=adapter

        categoryModel.allCategory.observe(this, Observer { words ->
            // Update the cached copy of the words in the adapter.
            words?.let { adapter?.setSongs(it) }
        })


        cycle.onFocusChangeListener= View.OnFocusChangeListener { view: View, b: Boolean ->
            Toast.makeText(this,"awa",Toast.LENGTH_LONG).show()
        }


    }

    suspend fun changeName() {
        for (i in 1..100) {
            delay(1000)
            text.text = i.toString().plus("Seconds");
        }
    }

    suspend fun reloadSongs(){

        val sharedPref = this?.getPreferences(Context.MODE_PRIVATE)
        var basePath=sharedPref.getString(getString(R.string.base_path),"no hay na ue")
        basePath +="${File.separator}stepdroid${File.separator}songs"
        basePath ="${File.separator}storage${File.separator}3839-2A39${File.separator}stepdroid2${File.separator}songs"

        var file = File(basePath)
        if (!file.exists()){
            file.mkdir()
            Toast.makeText(this, "No songs yet :(, please put in $basePath",Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this,""+file.isDirectory(),Toast.LENGTH_LONG).show()

        var category= file.listFiles()
        if (category!= null && category!!.isEmpty()){
                Toast.makeText(this, "No songs yet :(, please put in $basePath",Toast.LENGTH_LONG).show()
                return
        }
        //clean all category chanels
        categoryModel.deleteAll()


        category?.filter { x-> x.isDirectory }?.forEach { cate->
            run {
                // se valida si existe una carpeta de info con sonidito
                var sound = File("${cate.absolutePath+File.separator}info${File.separator}sound.ogg" )
                if (!sound.exists())
                    sound = File("${cate.absolutePath+File.separator}info${File.separator}sound.mp3" )

                val banner = File("${cate.absolutePath+File.separator}banner.png" )

                val catego= Category(
                    cate.name,
                    cate.path,
                    if (banner.exists()) banner.path else null ,
                    if (sound.exists()) sound.path else null )


                //No se añade si no tiene canciones
                var hasSong = false

                cate.listFiles()?.filter { x-> x.isDirectory }?.forEach { subFolder ->
                    run {

                        val fileSong = subFolder.listFiles()
                        var ssc = fileSong.filter { ssc ->
                            ssc.name.toLowerCase().endsWith("ssc")
                        }.firstOrNull()
                        if (ssc != null) {
                            hasSong=true


                        }


                    }
                }






                if (hasSong)
                    categoryModel.insert(catego)

            }
        }
    }


    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_CONTACTS
                )
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    70
                )

            }
        }
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            if (!ActivityCompat.shouldShowRequestPermissionRationale(
//                    this,
//                    Manifest.permission.READ_CONTACTS
//                )
//            ) {
//                ActivityCompat.requestPermissions(
//                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                    70
//                )
//
//            }
//        }
    }


}
