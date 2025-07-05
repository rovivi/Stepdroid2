package com.kyagamy.step.game.newplayer

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.kyagamy.step.databinding.ActivityEvaluationBinding
import com.kyagamy.step.views.FullScreenActivity
import com.kyagamy.step.utils.EdgeToEdgeHelper

class EvaluationActivity : FullScreenActivity() {

    private val binding: ActivityEvaluationBinding by lazy {
        ActivityEvaluationBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove title bar completely
        supportActionBar?.hide()

        setContentView(binding.root)

        // Use EdgeToEdgeHelper for evaluation screen with some padding to prevent UI overlap
        EdgeToEdgeHelper.setupCustomEdgeToEdge(
            this,
            binding.root,
            applyTopInset = true,
            applyBottomInset = true
        )

        // Rest of your evaluation activity code...
    }

}