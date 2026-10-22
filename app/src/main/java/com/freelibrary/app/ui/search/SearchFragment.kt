package com.freelibrary.app.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.freelibrary.app.R
import com.freelibrary.app.data.repository.SearchResult
import com.freelibrary.app.databinding.FragmentSearchBinding
import com.freelibrary.app.ui.filter.FilterSheetFragment
import com.freelibrary.app.ui.filter.showFilterState
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.freelibrary.shared.SearchScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The search screen: a query field, a Title / Author switch and the matching books. The search
 * itself runs in [SearchViewModel]; this class only shows its state. A tap on a result opens the
 * book's page.
 */
@AndroidEntryPoint
class SearchFragment : Fragment() {
    private val viewModel: SearchViewModel by viewModels()

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchRoot.applySystemBarInsetsAsPadding()
        binding.searchToolbar.setNavigationOnClickListener {
            hideKeyboard()
            findNavController().navigateUp()
        }
        binding.searchToolbar.inflateMenu(R.menu.menu_search)
        val filterItem = binding.searchToolbar.menu.findItem(R.id.action_filter)
        filterItem.showFilterState(requireContext(), viewModel.filter.value.isActive)
        binding.searchToolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_filter) {
                FilterSheetFragment.show(childFragmentManager)
                true
            } else {
                false
            }
        }
        binding.searchMessage.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE

        val adapter = SearchResultAdapter(::openBook)
        binding.searchResults.adapter = adapter
        binding.searchResults.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int,
                ) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) hideKeyboard()
                }
            },
        )

        binding.searchQuery.setText(viewModel.query.value)
        binding.searchQuery.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.searchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        binding.searchScope.check(
            if (viewModel.scope.value == SearchScope.AUTHOR) R.id.searchScopeAuthor else R.id.searchScopeTitle,
        )
        binding.searchScope.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.setScope(if (checkedId == R.id.searchScopeAuthor) SearchScope.AUTHOR else SearchScope.TITLE)
            }
        }

        if (savedInstanceState == null && viewModel.query.value.isEmpty()) showKeyboard()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it, adapter) } }
                launch { viewModel.filter.collect { filterItem.showFilterState(requireContext(), it.isActive) } }
            }
        }
    }

    private fun render(
        state: SearchState,
        adapter: SearchResultAdapter,
    ) {
        binding.searchProgress.visibility = if (state is SearchState.Loading) View.VISIBLE else View.INVISIBLE
        when (state) {
            SearchState.Idle -> showMessage(adapter, R.string.search_idle)
            SearchState.Loading -> Unit
            SearchState.NoResults ->
                showMessage(
                    adapter,
                    if (viewModel.filter.value.isActive) R.string.search_no_results_filtered else R.string.search_no_results,
                )
            SearchState.Failed -> showMessage(adapter, R.string.search_failed)
            is SearchState.Results -> {
                binding.searchMessage.isVisible = false
                adapter.submitList(state.results) { _binding?.searchResults?.scrollToPosition(0) }
            }
        }
    }

    private fun showMessage(
        adapter: SearchResultAdapter,
        @StringRes message: Int,
    ) {
        adapter.submitList(emptyList())
        binding.searchMessage.setText(message)
        binding.searchMessage.isVisible = true
    }

    /** Opens a book's own page. A second tap while the page is opening is ignored. */
    private fun openBook(result: SearchResult) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.searchFragment) return
        hideKeyboard()
        navController.navigate(SearchFragmentDirections.actionSearchFragmentToBookPageFragment(bookId = result.bookId))
    }

    private fun showKeyboard() {
        binding.searchQuery.requestFocus()
        binding.searchQuery.post {
            _binding?.let {
                requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE)
                    .let { service -> service as InputMethodManager }
                    .showSoftInput(it.searchQuery, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun hideKeyboard() {
        val service = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        service.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        binding.searchResults.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
