package com.freelibrary.app.ui.profile.avatar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentAvatarCreatorBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The avatar creator: a preview that stays on top, tabs for the groups of choices and a grid of tiles to pick from. Save hands the
 * finished picture back to the profile editor, which treats it like a chosen photo. This class only shows
 * [AvatarCreatorViewModel]'s state and passes the reader's taps on.
 */
@AndroidEntryPoint
class AvatarCreatorFragment : Fragment() {
    private val viewModel: AvatarCreatorViewModel by viewModels()

    private var _binding: FragmentAvatarCreatorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAvatarCreatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.avatarCreatorRoot.applySystemBarInsetsAsPadding()
        binding.avatarCreatorToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val adapter = AvatarTileAdapter(onPick = viewModel::pick)
        val layoutManager = GridLayoutManager(requireContext(), SPAN_COUNT)
        layoutManager.spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = adapter.spanOf(position, SPAN_COUNT)
            }
        binding.avatarTiles.layoutManager = layoutManager
        binding.avatarTiles.adapter = adapter
        binding.avatarTiles.itemAnimator = null

        AvatarTab.entries.forEach { tab ->
            binding.avatarTabs.addTab(binding.avatarTabs.newTab().setText(tab.label).setTag(tab))
        }
        binding.avatarTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    (tab.tag as? AvatarTab)?.let(viewModel::selectTab)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            },
        )
        binding.avatarRandomize.setOnClickListener { viewModel.randomize() }
        binding.avatarSave.setOnClickListener { viewModel.save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.avatarPreview.showDesign(state.design)
                    adapter.submitList(avatarRows(state.tab, state.design))
                    val selected = binding.avatarTabs.getTabAt(AvatarTab.entries.indexOf(state.tab))
                    if (selected != null && !selected.isSelected) selected.select()
                    binding.avatarSave.isEnabled = !state.saving
                    state.picture?.let { picture ->
                        setFragmentResult(RESULT_KEY, bundleOf(RESULT_PICTURE to picture.toString()))
                        findNavController().navigateUp()
                    }
                    if (state.failed) {
                        viewModel.failureShown()
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(R.string.avatar_error_save)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.avatarTiles.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** The key and the bundle entry the profile editor listens for. */
        const val RESULT_KEY = "avatar_creator_result"
        const val RESULT_PICTURE = "picture"
        private const val SPAN_COUNT = 4
    }
}
