package com.daytodo.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.daytodo.app.R
import com.daytodo.app.app
import com.daytodo.app.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = HistoryAdapter()
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        lifecycleScope.launch {
            val repo = app().repo
            val entries = repo.history()
            val stats = repo.stats()
            binding.textSummary.text =
                getString(R.string.history_summary, stats.first, stats.second)
            binding.textEmpty.isVisible = entries.isEmpty()
            adapter.submit(entries)
        }
    }
}
