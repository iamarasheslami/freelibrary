package com.freelibrary.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentSettingsBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import dagger.hilt.android.AndroidEntryPoint

/**
 * The Settings screen: a list of parent rows, each opening the page for its topic (profile,
 * appearance, catalog sync, storage, privacy, backup and about). The rows hold no state of their own.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsRoot.applySystemBarInsetsAsPadding()
        binding.settingsToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.profileRow.setOnClickListener { open(SettingsFragmentDirections.actionSettingsFragmentToEditProfileFragment()) }
        binding.appearanceRow.setOnClickListener { open(SettingsFragmentDirections.actionSettingsFragmentToAppearanceSettingsFragment()) }
        binding.syncRow.setOnClickListener { open(SettingsFragmentDirections.actionSettingsFragmentToSyncSettingsFragment()) }
        binding.storageRow.setOnClickListener { open(SettingsFragmentDirections.actionSettingsFragmentToStorageSettingsFragment()) }
        binding.privacyRow.setOnClickListener { open(SettingsFragmentDirections.actionSettingsFragmentToPrivacySettingsFragment()) }
        binding.backupRow.setOnClickListener { open(SettingsFragmentDirections.actionSettingsFragmentToBackupSettingsFragment()) }
        binding.aboutRow.setOnClickListener { open(SettingsFragmentDirections.actionSettingsFragmentToAboutSettingsFragment()) }
    }

    /** Opens a page. A second tap while the page is opening is ignored. */
    private fun open(directions: NavDirections) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.settingsFragment) return
        navController.navigate(directions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
