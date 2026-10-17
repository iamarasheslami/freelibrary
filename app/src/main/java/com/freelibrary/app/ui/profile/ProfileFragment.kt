package com.freelibrary.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.databinding.FragmentProfileBinding
import com.freelibrary.app.domain.model.BookCard
import com.freelibrary.app.domain.model.ProfileSection
import com.freelibrary.app.ui.booklist.toArgs
import com.freelibrary.app.ui.cover.CoverTypefaces
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The Profile tab: the reader's picture and name, then three sliders (Continue Reading, Want to
 * Read, Finished) that work like Home's. This class only shows [ProfileViewModel]'s state and
 * passes the reader's taps on.
 */
@AndroidEntryPoint
class ProfileFragment : Fragment() {
    @Inject lateinit var imageLoader: ImageLoader

    @Inject lateinit var avatarStore: AvatarStore

    private val viewModel: ProfileViewModel by viewModels()

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
        binding.profileSettings.setOnClickListener { openSettings() }

        val adapter =
            ProfileAdapter(
                typefaces = CoverTypefaces.from(requireContext()),
                imageLoader = imageLoader,
                avatarStore = avatarStore,
                onBookClick = ::openBook,
                onViewMore = ::openList,
            )
        binding.profileList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { adapter.submitList(profileRows(it)) }
            }
        }
    }

    /** Opens the full list of a shelf. A second tap while the page is opening is ignored. */
    private fun openList(section: ProfileSection) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.profileFragment) return
        val args = section.viewMore.toArgs()
        navController.navigate(
            ProfileFragmentDirections.actionProfileFragmentToBookListFragment(
                kind = args.kind,
                titleRes = section.titleRes,
                bookshelfName = args.bookshelfName,
            ),
        )
    }

    /** Opens a book's own page. A second tap while the page is opening is ignored. */
    private fun openBook(book: BookCard) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.profileFragment) return
        navController.navigate(ProfileFragmentDirections.actionProfileFragmentToBookPageFragment(bookId = book.bookId))
    }

    /** Opens Settings. A second tap while the page is opening is ignored. */
    private fun openSettings() {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.profileFragment) return
        navController.navigate(ProfileFragmentDirections.actionProfileFragmentToSettingsFragment())
    }

    override fun onDestroyView() {
        binding.profileList.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
