package com.freelibrary.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentSettingsAboutBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import dagger.hilt.android.AndroidEntryPoint

/** The About page: version, licence and the links to the source and the notices. */
@AndroidEntryPoint
class AboutSettingsFragment : Fragment() {
    private var _binding: FragmentSettingsAboutBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pageRoot.applySystemBarInsetsAsPadding()
        binding.pageToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.aboutVersion.text = getString(R.string.about_version, versionName())
        binding.aboutSource.setOnClickListener { openLink(AboutLinks.SOURCE) }
        binding.aboutNotices.setOnClickListener { openLink(AboutLinks.NOTICES) }
    }

    @Suppress("DEPRECATION")
    private fun versionName(): String {
        val context = requireContext()
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.about_no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
