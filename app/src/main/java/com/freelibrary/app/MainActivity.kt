package com.freelibrary.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.freelibrary.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the app's navigation graph. All actual screens are
 * fragments managed by the NavHostFragment declared in activity_main.xml.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
