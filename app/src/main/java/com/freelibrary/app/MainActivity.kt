package com.freelibrary.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.freelibrary.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Temporary launcher activity used to verify the build pipeline end to end
 * (Gradle -> Hilt -> ViewBinding -> install on device). Real navigation and
 * the home screen UI (Phase 2) will replace this.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.statusText.text = getString(R.string.build_verification_message)
    }
}
