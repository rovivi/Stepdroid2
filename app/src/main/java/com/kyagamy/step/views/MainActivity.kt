package com.kyagamy.step.views

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.kyagamy.step.R
import com.kyagamy.step.databinding.ActivityMainBinding
import com.kyagamy.step.fragments.CategoryFragament
import com.kyagamy.step.fragments.songs.SongsListFragment
import com.kyagamy.step.ui.compose.SongsListScreen
import com.kyagamy.step.ui.ui.theme.StepDroidTheme
import com.kyagamy.step.fragments.songs.FragmentStartMenu
import com.kyagamy.step.utils.EdgeToEdgeHelper

class MainActivity : FullScreenActivity() {
    // Reference to "name" TextView using synthetic properties.
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(LayoutInflater.from(this))
    }
    // Reference to "name" TextView using the binding class instance.
    private lateinit var fragmentCategory: CategoryFragament
    private var positionCategory = 2
    private val manager = supportFragmentManager

    // Flag to enable/disable Compose version
    private val useComposeVersion = true // Set to false to use old fragment version

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove title bar completely
        supportActionBar?.hide()

        setContentView(binding.root)

        // EdgeToEdgeHelper is already called by FullScreenActivity
        // We can add custom edge-to-edge handling for the main activity if needed
        EdgeToEdgeHelper.setupCustomEdgeToEdge(
            this,
            binding.root,
            applyTopInset = false,
            applyBottomInset = false
        )

        fragmentCategory = CategoryFragament()
        showFragmentCategory()

        // Video setup
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
        transaction.replace(R.id.fragment_holder, fragmentCategory)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun showFragmentSongList(category: String) {
        if (useComposeVersion) {
            // New Compose version
            showComposeSongList(category)
        } else {
            // Old Fragment version
            val transaction = manager.beginTransaction()
            val fragment = SongsListFragment(category)
            transaction.replace(R.id.fragment_holder, fragment)
            transaction.addToBackStack("changetocategory")
            transaction.commit()
        }
    }

    private fun showComposeSongList(category: String) {
        // Create a fragment that holds the Compose content
        val composeFragment = ComposeFragment.newInstance(category)
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.fragment_holder, composeFragment)
        transaction.addToBackStack("changetocategory")
        transaction.commit()
    }

    fun changeCategory(category: String, categoryPosition: Int) {
        positionCategory = categoryPosition
        Toast.makeText(this, "value:${category}", Toast.LENGTH_SHORT).show()
        showFragmentSongList(category)
    }

    // Helper function to show the start menu dialog
    fun showStartMenuDialog(songId: Int) {
        val dialog = FragmentStartMenu.newInstance(songId)
        dialog.show(manager, "StartMenuDialog")
    }
}

// New Fragment to hold Compose content
class ComposeFragment : Fragment() {

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String): ComposeFragment {
            val fragment = ComposeFragment()
            val args = Bundle()
            args.putString(ARG_CATEGORY, category)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val category = arguments?.getString(ARG_CATEGORY) ?: ""

        return ComposeView(requireContext()).apply {
            setContent {
                StepDroidTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(0.dp)
                    ) {
                        SongsListScreen(
                            channel = category,
                            onBack = {
                                (activity as? MainActivity)?.onBackPressed()
                            },
                            onSongClick = { songId ->
                                (activity as? MainActivity)?.showStartMenuDialog(songId)
                            }
                        )
                    }
                }
            }
        }
    }
}