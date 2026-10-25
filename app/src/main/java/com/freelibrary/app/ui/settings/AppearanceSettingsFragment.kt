package com.freelibrary.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.freelibrary.app.databinding.FragmentSettingsAppearanceBinding
import com.freelibrary.app.domain.model.ThemeMode
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.freelibrary.app.ui.theme.AppScale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The Appearance page: theme and app size. Controls react to the reader's own taps only, so showing a stored value never writes it back. */
@AndroidEntryPoint
class AppearanceSettingsFragment : Fragment() {
    private var _binding: FragmentSettingsAppearanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pageRoot.applySystemBarInsetsAsPadding()
        binding.pageToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.themeSystem.setOnClickListener { viewModel.setThemeMode(ThemeMode.SYSTEM) }
        binding.themeLight.setOnClickListener { viewModel.setThemeMode(ThemeMode.LIGHT) }
        binding.themeDark.setOnClickListener { viewModel.setThemeMode(ThemeMode.DARK) }

        binding.sizeGroup.check(SettingsLabels.sizeButtonId(AppScale.current(requireContext())))
        binding.sizeGroup.addOnButtonCheckedListener { _, id, checked ->
            val size = SettingsLabels.sizeForButtonId(id)
            if (checked && size != null && size != AppScale.current(requireContext())) {
                AppScale.save(requireContext(), size)
                requireActivity().recreate()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val themeButton = SettingsLabels.themeButtonId(state.themeMode)
                    if (binding.themeGroup.checkedButtonId != themeButton) binding.themeGroup.check(themeButton)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsAppearanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
