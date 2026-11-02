package com.redbull.lab.sample

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import com.redbull.lab.sample.presentation.SampleViewModel
import kotlinx.coroutines.launch

/** Displays the SDK-backed catalog while leaving business work in the ViewModel. */
class SampleActivity : ComponentActivity() {
    private val viewModel: SampleViewModel by lazy {
        ViewModelProvider(this, (application as SampleApplication).viewModelFactory)
            .get(SampleViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        val progress = findViewById<ProgressBar>(R.id.loading)
        val content = findViewById<TextView>(R.id.content)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progress.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
                    content.text = when {
                        state.errorMessage != null -> state.errorMessage
                        state.products.isEmpty() && state.isLoading -> "Loading products…"
                        state.products.isEmpty() -> "No products returned."
                        else -> state.products.joinToString("\n\n") { "${it.title} — $${it.price}" }
                    }
                }
            }
        }
    }
}
