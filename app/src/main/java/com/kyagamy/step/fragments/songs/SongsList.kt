package com.kyagamy.step.fragments.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kyagamy.step.views.MainActivity
import com.kyagamy.step.R
import com.kyagamy.step.adapters.SongAdapter
import com.kyagamy.step.common.RecyclerItemClickListener
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewmodels.LevelViewModel
import com.kyagamy.step.viewmodels.SongViewModel


private const val channel = "param1"

class SongsList : Fragment() {
    // TODO: Rename and change types of parameters


    private var param1: String? = null
    private lateinit var songsModel: SongViewModel
    private lateinit var songsRecyclerView: RecyclerView

    var currentSong: Song? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(channel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        songsModel = ViewModelProvider(this).get(SongViewModel::class.java)

        val songAdapter = SongAdapter(
            this.requireContext(),
            ViewModelProvider(this).get(LevelViewModel::class.java),
            viewLifecycleOwner
        )
        val view = inflater.inflate(R.layout.fragment_songs_list, container, false)
        songsRecyclerView = view.findViewById(R.id.songs_recyclerView)



        songsRecyclerView.layoutManager = LinearLayoutManager(requireActivity().applicationContext)

        songsRecyclerView.adapter = songAdapter
        songsModel.songByCategory(param1 ?: "") .observe(viewLifecycleOwner, { words ->
            words?.let { songAdapter.setSongs(it) }
        })

        songsRecyclerView.addOnItemTouchListener(
            RecyclerItemClickListener(activity,
                songsRecyclerView,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int) {
                        lifecycleScope.run {
                            try {
                                currentSong = songAdapter.getSong(position)
                                showStartSongFragment(false)
                            } catch (ex: java.lang.Exception) {

                            }
                        }
                    }

                    override fun onItemLongClick(
                        view: View?,
                        position: Int
                    ) {
                    }
                })
        )

        //
        val mainActivity = activity as MainActivity

        val buttonBack = view.findViewById<Button>(R.id.backToChanelsButton)
        buttonBack.setOnClickListener {
            mainActivity.showFragmentCategory()
        }



        //filter

        val spinner = view.findViewById<Spinner>(R.id.sortOptions)
        val arraySpinner = arrayOf(
            "Name", "Artist", "BPM"
        )






        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            mainActivity,
            android.R.layout.simple_spinner_item, arraySpinner
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.adapter = adapter;
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {


            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                when (position){
                    0->{
                        songsModel.songByCategory(param1 ?: "") .observe(viewLifecycleOwner, { words ->
                            words?.let { songAdapter.setSongs(it) }
                        })
                    }
                    1->{
                        songsModel.songByCategoryAuthor(param1 ?: "") .observe(viewLifecycleOwner, { words ->
                            words?.let { songAdapter.setSongs(it) }
                        })
                    }
                    2->{
                        songsModel.songByCategoryBPM(param1 ?: "") .observe(viewLifecycleOwner, { words ->
                            words?.let { songAdapter.setSongs(it) }
                        })
                    }
                }
            }
        }
        return view
    }


    fun showStartSongFragment(type: Boolean) {
        val newFragment = FragmentStartMenu.newInstance(currentSong!!.song_id)
        activity?.supportFragmentManager?.let { newFragment.show(it, "Notice") }
        // playSoundPool(spOpenWindow);
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SongsList().apply {
                arguments = Bundle().apply {
                    putString(channel, param1)
                }
            }
    }
}