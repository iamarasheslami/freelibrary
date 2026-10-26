package com.freelibrary.app.ui.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.freelibrary.app.R
import com.freelibrary.app.databinding.FragmentFilterSheetBinding
import com.freelibrary.app.domain.model.BookFilterOptions
import com.freelibrary.app.domain.model.FilterOption
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The filter sheet: language (one or none) and genres (any number) are picked from drop-downs with a
 * search box, formats (three at most) are chips. The rows and chips are built once, when the catalog's
 * options arrive; after that they only follow the draft.
 */
@AndroidEntryPoint
class FilterSheetFragment : BottomSheetDialogFragment() {
    private val viewModel: BookFilterViewModel by viewModels()

    /** The choices on offer once they have been read; the drop-downs say "Loading" until then. */
    private var shownOptions: BookFilterOptions? = null

    private var _binding: FragmentFilterSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFilterSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        binding.filterReset.setOnClickListener { viewModel.reset() }
        binding.filterApply.setOnClickListener {
            viewModel.apply()
            dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.options.filterNotNull().collect { showOptions(it) } }
                launch {
                    viewModel.draft.collect { draft ->
                        check(binding.filterFormatChips, draft.formats)
                        showLanguage(draft.language)
                        showGenres(draft.genres)
                    }
                }
            }
        }
    }

    private fun showOptions(options: BookFilterOptions) {
        val locale = Locale.getDefault()
        shownOptions = options
        fill(binding.filterFormatChips, options.formats, ::formatLabel, viewModel::toggleFormat)
        binding.filterLanguageRow.setOnClickListener { pickLanguage(options, locale) }
        binding.filterGenreRow.setOnClickListener { pickGenres(options) }
        binding.filterLanguageSection.isVisible = options.languages.isNotEmpty()
        binding.filterFormatSection.isVisible = options.formats.isNotEmpty()
        binding.filterGenreSection.isVisible = options.genres.isNotEmpty()
        binding.filterNoOptions.isVisible = options.languages.isEmpty() && options.formats.isEmpty() && options.genres.isEmpty()
        val draft = viewModel.draft.value
        check(binding.filterFormatChips, draft.formats)
        showLanguage(draft.language)
        showGenres(draft.genres)
    }

    private fun showLanguage(code: String?) {
        binding.filterLanguageValue.text =
            when {
                shownOptions == null -> getString(R.string.filter_loading)
                code == null -> getString(R.string.filter_any_language)
                else -> FilterLabels.languageLabel(code, Locale.getDefault())
            }
    }

    private fun showGenres(genres: Set<String>) {
        binding.filterGenreValue.text =
            when {
                shownOptions == null -> getString(R.string.filter_loading)
                genres.isEmpty() -> getString(R.string.filter_any_genre)
                genres.size <= MAX_NAMED_GENRES -> genres.sorted().joinToString(", ") { FilterLabels.genreLabel(it) }
                else -> resources.getQuantityString(R.plurals.filter_genres_selected, genres.size, genres.size)
            }
    }

    private fun pickLanguage(
        options: BookFilterOptions,
        locale: Locale,
    ) {
        FilterPicker(
            context = requireContext(),
            title = R.string.filter_language,
            options = options.languages,
            label = { FilterLabels.languageLabel(it.value, locale) },
            multiple = false,
            onToggle = viewModel::chooseLanguage,
            onClear = viewModel::clearLanguage,
        ).show(viewLifecycleOwner.lifecycleScope, viewModel.selectedLanguages)
    }

    private fun pickGenres(options: BookFilterOptions) {
        FilterPicker(
            context = requireContext(),
            title = R.string.filter_genre,
            options = options.genres,
            label = { FilterLabels.genreLabel(it.value) },
            multiple = true,
            onToggle = viewModel::toggleGenre,
            onClear = viewModel::clearGenres,
        ).show(viewLifecycleOwner.lifecycleScope, viewModel.selectedGenres)
    }

    private fun formatLabel(option: FilterOption): String =
        FilterLabels.formatLabelRes(option.value)?.let(::getString) ?: option.value.uppercase(Locale.ROOT)

    private fun fill(
        group: ChipGroup,
        options: List<FilterOption>,
        label: (FilterOption) -> String,
        onClick: (String) -> Unit,
    ) {
        group.removeAllViews()
        options.forEach { option ->
            val chip = layoutInflater.inflate(R.layout.item_filter_chip, group, false) as Chip
            chip.text = label(option)
            chip.tag = option.value
            chip.setOnClickListener { onClick(option.value) }
            group.addView(chip)
        }
    }

    private fun check(
        group: ChipGroup,
        selected: Set<String>,
    ) {
        group.children.forEach { child -> (child as Chip).isChecked = (child.tag as String) in selected }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shownOptions = null
        _binding = null
    }

    companion object {
        private const val TAG = "filterSheet"
        private const val MAX_NAMED_GENRES = 2

        /** Opens the sheet unless it is already open (a second tap must not stack another). */
        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) FilterSheetFragment().show(fragmentManager, TAG)
        }
    }
}
