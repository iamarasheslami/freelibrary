package com.freelibrary.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.freelibrary.app.databinding.FragmentHomeBinding

/**
 * Placeholder for the home screen (continue-reading, saved-books, and curated-shelf
 * sliders described in the product spec). The real screen replaces it in Phase 2.
 *
 * The catalog sync no longer starts here: MainActivity starts it once per app start.
 */
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
