package com.freelibrary.app.ui.profile

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import com.freelibrary.app.R
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.databinding.FragmentEditProfileBinding
import com.freelibrary.app.ui.insets.applySystemBarInsetsAsPadding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Formatter
import java.util.Locale
import javax.inject.Inject

/** 1 January 1900, the earliest birthday the picker offers, as midnight UTC in milliseconds. */
private const val EARLIEST_BIRTHDAY_UTC_MILLIS = -2_208_988_800_000L

/**
 * The profile editor: picture, name and birthday, saved together with Save. Going back with
 * unsaved changes asks first. This class only shows [EditProfileViewModel]'s state and passes the
 * reader's taps on.
 */
@AndroidEntryPoint
class EditProfileFragment : Fragment() {
    @Inject lateinit var imageLoader: ImageLoader

    @Inject lateinit var avatarStore: AvatarStore

    private val viewModel: EditProfileViewModel by viewModels()

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val pickPicture =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.onPictureChosen(uri)
        }

    private lateinit var backCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.editProfileRoot.applySystemBarInsetsAsPadding()
        binding.editProfileToolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        backCallback =
            object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() = confirmDiscard()
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        val choose = View.OnClickListener { pickPicture.launch(arrayOf("image/*")) }
        binding.editAvatarCard.setOnClickListener(choose)
        binding.choosePictureButton.setOnClickListener(choose)
        binding.removePictureButton.setOnClickListener { viewModel.onPictureRemoved() }

        binding.nameInput.doAfterTextChanged { viewModel.onNameChanged(it?.toString().orEmpty()) }
        binding.birthdayInput.setOnClickListener { showBirthdayPicker() }
        binding.birthdayLayout.setEndIconOnClickListener { viewModel.onBirthdayPicked(null) }
        binding.saveButton.setOnClickListener { viewModel.save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: EditProfileState) {
        if (!state.loaded) return
        if (binding.nameInput.text?.toString() != state.name) {
            binding.nameInput.setText(state.name)
            binding.nameInput.setSelection(state.name.length)
        }
        binding.birthdayInput.setText(state.birthdayUtcMillis?.let(::formatBirthday).orEmpty())
        binding.birthdayLayout.isEndIconVisible = state.birthdayUtcMillis != null
        showPicture(state)
        binding.removePictureButton.isVisible = state.hasPicture
        binding.saveButton.isEnabled = state.dirty && !state.saving
        backCallback.isEnabled = state.dirty && !state.saving

        state.error?.let { error ->
            showError(error)
            viewModel.errorShown()
        }
        if (state.saved) {
            backCallback.isEnabled = false
            findNavController().navigateUp()
        }
    }

    private fun showPicture(state: EditProfileState) {
        when (val picture = state.picture) {
            is PictureDraft.Replace -> binding.editAvatar.showAvatarPicture(picture.source, null, imageLoader)
            PictureDraft.Remove -> binding.editAvatar.showAvatarPlaceholder()
            PictureDraft.Keep -> {
                val file = if (state.savedAvatarVersion != null) avatarStore.file() else null
                if (file == null) {
                    binding.editAvatar.showAvatarPlaceholder()
                } else {
                    binding.editAvatar.showAvatarPicture(file, "avatar-${state.savedAvatarVersion}", imageLoader)
                }
            }
        }
    }

    /** The picked day is midnight UTC, so it is also written in UTC; otherwise it could show as the day before. */
    private fun formatBirthday(utcMillis: Long): String =
        DateUtils.formatDateRange(
            requireContext(),
            Formatter(StringBuilder(), Locale.getDefault()),
            utcMillis,
            utcMillis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
            "UTC",
        ).toString()

    private fun showBirthdayPicker() {
        val constraints =
            CalendarConstraints.Builder()
                .setStart(EARLIEST_BIRTHDAY_UTC_MILLIS)
                .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                .setValidator(DateValidatorPointBackward.now())
                .build()
        val builder =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.edit_profile_birthday_picker_title)
                .setCalendarConstraints(constraints)
                .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)
        viewModel.state.value.birthdayUtcMillis?.let { builder.setSelection(it) }
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { viewModel.onBirthdayPicked(it) }
        picker.show(childFragmentManager, "birthday")
    }

    private fun confirmDiscard() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_profile_discard_title)
            .setMessage(R.string.edit_profile_discard_message)
            .setPositiveButton(R.string.edit_profile_discard) { _, _ ->
                backCallback.isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .setNegativeButton(R.string.edit_profile_keep_editing, null)
            .show()
    }

    private fun showError(error: EditProfileError) {
        val message =
            when (error) {
                EditProfileError.PICTURE_UNREADABLE -> R.string.edit_profile_error_picture
                EditProfileError.SAVE_FAILED -> R.string.edit_profile_error_save
            }
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
