package com.kyagamy.step.fragments.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kyagamy.step.adapters.SongAdapter
import com.kyagamy.step.common.RecyclerItemClickListener
import com.kyagamy.step.databinding.FragmentSongsListBinding
import com.kyagamy.step.room.entities.Song
import com.kyagamy.step.viewmodels.LevelViewModel
import com.kyagamy.step.viewmodels.SongViewModel
import com.kyagamy.step.views.MainActivity
import kotlinx.coroutines.launch

class SongsListFragment(private val channel: String) : Fragment() {

    private val songsModel: SongViewModel by viewModels()
    private var currentSong: Song? = null
    private var _binding: FragmentSongsListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsListBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupBackButton()
        setupSpinner()
        observeSongs()
        return binding.root
    }

    private fun setupRecyclerView() {
        val songAdapter = SongAdapter(requireContext(), viewModels<LevelViewModel>().value, viewLifecycleOwner)
        binding.songsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
            addOnItemTouchListener(
                RecyclerItemClickListener(
                    activity,
                    this,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View?, position: Int) {
                            lifecycleScope.launch {
                                currentSong = songAdapter.getSong(position)
                                showStartSongFragment(false)
                            }
                        }

                        override fun onItemLongClick(view: View?, position: Int) {}
                    })
            )
        }
    }

    private fun setupBackButton() {
        val mainActivity = activity as? MainActivity
        binding.backToChanelsButton.setOnClickListener {
            mainActivity?.showFragmentCategory()
        }
    }

    private fun setupSpinner() {
        val options = listOf("Name", "Artist", "BPM")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.sortOptions.adapter = adapter
        binding.sortOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> observeSongsBy { songsModel.songByCategory(channel) }
                    1 -> observeSongsBy { songsModel.songByCategoryAuthor(channel) }
                    2 -> observeSongsBy { songsModel.songByCategoryBPM(channel) }
                }
            }
        }
    }

    private fun observeSongs() {
        songsModel.songByCategory(channel).observe(viewLifecycleOwner) { songs ->
            songs?.let { (binding.songsRecyclerView.adapter as SongAdapter).setSongs(it) }
        }
    }

    private fun observeSongsBy(source: () -> LiveData<List<Song>>) {
        source().observe(viewLifecycleOwner) { songs ->
            songs?.let { (binding.songsRecyclerView.adapter as SongAdapter).setSongs(it) }
        }
    }

    private fun showStartSongFragment(type: Boolean) {
        currentSong?.let {
            val newFragment = FragmentStartMenu.newInstance(it.song_id)
            activity?.supportFragmentManager?.let { fragmentManager ->
                newFragment.show(fragmentManager, "Notice")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
