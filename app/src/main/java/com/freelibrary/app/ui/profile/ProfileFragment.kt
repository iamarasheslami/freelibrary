package com.freelibrary.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.freelibrary.app.databinding.FragmentProfileBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding

/** The Profile tab. For now only its title; the reader's shelves and the settings come next. */
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.profileRoot.applySystemBarInsetsAsPadding()
        ViewCompat.setAccessibilityHeading(binding.profileTitle, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
