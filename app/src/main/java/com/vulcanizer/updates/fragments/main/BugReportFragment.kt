package com.vulcanizer.updates.fragments.main

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.vulcanizer.updates.MainActivity
import com.vulcanizer.updates.R
import com.vulcanizer.updates.fragments.tweaks.runShellCommandForResult
import com.vulcanizer.updates.utils.TelegramBot
import dev.oneuiproject.oneui.layout.DrawerLayout
import java.io.BufferedReader
import java.io.InputStreamReader

class BugReportFragment : Fragment() {

    private lateinit var telegramUsernameInputLayout: TextInputLayout
    private lateinit var telegramUsernameEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var errorTextView: TextView
    private lateinit var titleInputLayout: TextInputLayout
    private lateinit var vulcanRomInputLayout: TextInputLayout

    private lateinit var descriptionInputLayout: TextInputLayout
    private lateinit var sharedPreferences: SharedPreferences
    private val COOLDOWN_TIME = 3600000 // 1 hour in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences =
            requireActivity().getSharedPreferences("BugReportPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bugreport, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize views
        telegramUsernameInputLayout = view.findViewById(R.id.text_input_telegram_username)
        telegramUsernameEditText = view.findViewById(R.id.edit_telegram_username)
        submitButton = view.findViewById(R.id.button_submit)
        errorTextView = view.findViewById(R.id.error_text_view)
        titleInputLayout = view.findViewById(R.id.text_input_bug_report_title)
        vulcanRomInputLayout = view.findViewById(R.id.text_input_vulcan_rom)
        descriptionInputLayout = view.findViewById(R.id.text_input_problem_description)



        // Set up the submit button click listener
        submitButton.setOnClickListener {
            submitBugReport()
        }

        // Add TextWatcher to the Telegram username EditText
        telegramUsernameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isTelegramUsernameValid(s.toString())) {
                    clearError(telegramUsernameInputLayout)
                } else {
                    showError(
                        telegramUsernameInputLayout,
                        "Invalid Telegram username. Please use the format @username."
                    )
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Add TextWatcher for other input fields
        setupTextWatcher(view.findViewById(R.id.edit_title), titleInputLayout)
        setupTextWatcher(view.findViewById(R.id.edit_vulcan_rom), vulcanRomInputLayout)
        setupTextWatcher(view.findViewById(R.id.edit_description), descriptionInputLayout)



        val binding = (requireActivity() as MainActivity).binding
        binding.drawerLayoutMain.setTitle("Bug Report", "Bug Report")
    }

    private fun setupTextWatcher(editText: EditText, inputLayout: TextInputLayout) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty()) {
                    clearError(inputLayout)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val binding = (requireActivity() as MainActivity).binding
            binding.drawerLayoutMain.setTitle("Bug Report", "Bug Report")
            scrollToTop()
        }
    }

    private fun scrollToTop() {
        val scrollView =
            (requireActivity() as MainActivity).findViewById<DrawerLayout>(R.id.drawer_layout_main)
        scrollView?.scrollTo(0, 0)
    }

    private fun isTelegramUsernameValid(username: String): Boolean {
        // Regex for validating Telegram username
        val regex = Regex("^@([A-Za-z0-9_]{5,32})$")
        return regex.matches(username)
    }

    private fun showError(inputLayout: TextInputLayout, message: String) {
        // Show error message on specific field
        inputLayout.error = message
        submitButton.isEnabled = false
    }

    private fun showError(view: View, message: String) {
        if (view is TextInputLayout) {
            view.error = message
        } else if (view is Spinner) {
            view.setBackgroundResource(R.drawable.border_red) // Set red border
        } else if (view is RadioGroup) {
            view.setBackgroundResource(R.drawable.border_red) // Set red border
        }
        submitButton.isEnabled = false
    }

    private fun clearError(inputLayout: TextInputLayout) {
        // Clear error message on specific field
        inputLayout.error = null
        submitButton.isEnabled = true
    }

    private fun clearError(view: View) {
        if (view is TextInputLayout) {
            view.error = null
        } else if (view is Spinner) {
            view.setBackgroundResource(android.R.color.transparent) // Reset background
        } else if (view is RadioGroup) {
            view.setBackgroundResource(android.R.color.transparent) // Reset background
        }
        submitButton.isEnabled = true
    }
    fun getVendorDevice(): String? {
        return try {
            // Execute the getprop command
            val process = Runtime.getRuntime().exec("getprop ro.product.vendor.device")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            // Read the output
            val vendorDevice = reader.readLine()

            // Wait for the process to complete
            process.waitFor()

            vendorDevice
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun submitBugReport() {
        Log.e("BugReport", "COmpiling")

        runShellCommandForResult("ksud module list", onSuccess =  {moduleMap -> sendToTelegram(moduleMap.keys.toString())}, onFailure = {})
    }



    private fun sendToTelegram(modules: String) {
        // Check cooldown
        val lastSubmissionTime = sharedPreferences.getLong("last_submission_time", 0)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastSubmissionTime < COOLDOWN_TIME) {
            showAlertDialog("You can submit a bug report only once every hour.")
            return
        }

        // Clear any previous errors
        clearError(telegramUsernameInputLayout)
        clearError(titleInputLayout)
        clearError(vulcanRomInputLayout)
        clearError(descriptionInputLayout)

        // Get references to the input fields
        val titleEditText: EditText = requireView().findViewById(R.id.edit_title)
        val vulcanRomEditText: EditText = requireView().findViewById(R.id.edit_vulcan_rom)
        val frequencyRadioGroup: RadioGroup = requireView().findViewById(R.id.radio_frequency)
        val formattingRadioGroup: RadioGroup = requireView().findViewById(R.id.radio_formatting)
        val descriptionEditText: EditText = requireView().findViewById(R.id.edit_description)

        // Get the values from the input fields
        val title = titleEditText.text.toString()
        val telegramUsername = telegramUsernameEditText.text.toString()
        val vulcanRom = vulcanRomEditText.text.toString()
        val frequency = when (frequencyRadioGroup.checkedRadioButtonId) {
            R.id.radio_all_time -> "Occurs all the time"
            R.id.radio_occasionally -> "Occurs occasionally"
            else -> null
        }
        val formatting = when (formattingRadioGroup.checkedRadioButtonId) {
            R.id.radio_formatting_yes -> "Yes"
            R.id.radio_formatting_no -> "No"
            R.id.radio_formatting_not_tested -> "Not Tested"
            else -> null
        }
        val description = descriptionEditText.text.toString()

        // Validate inputs
        val errors = mutableListOf<String>()

        if (title.isEmpty()) {
            errors.add("Title is required.")
            showError(titleInputLayout, "Title is required.")
        }
        if (telegramUsername.isEmpty() || !isTelegramUsernameValid(telegramUsername)) {
            errors.add("Invalid Telegram username. Please use the format @username.")
            showError(telegramUsernameInputLayout, "Invalid Telegram username. Please use the format @username.")
        }
        if (vulcanRom.isEmpty()) {
            errors.add("Vulcan ROM is required.")
            showError(vulcanRomInputLayout, "Vulcan ROM is required.")
        }
        if (frequency == null) {
            errors.add("Frequency of occurrence is required.")
            showError(frequencyRadioGroup, "Frequency of occurrence is required.")
        }
        if (formatting == null) {
            errors.add("Formatting issue is required.")
            showError(formattingRadioGroup, "Formatting issue is required.")
        }
        if (description.isEmpty()) {
            errors.add("Description is required.")
            showError(descriptionInputLayout, "Description is required.")
        }

        // If there are errors, show all error messages and return
        if (errors.isNotEmpty()) {
            errorTextView.text = errors.joinToString("\n") // Join all error messages with a newline
            errorTextView.visibility = View.VISIBLE
            return
        } else {
            errorTextView.visibility = View.GONE // Hide the error message if no errors
        }

        val bugReport = """
    Bug Report Details:
    --------------------
    Title: $title
    Telegram Username: $telegramUsername
    Device: ${getVendorDevice()}
    Vulcan ROM: $vulcanRom
    Kernel Module: $modules
    Frequency of Occurrence: $frequency
    Formatting Issue: $formatting
    Description: $description
    """.trimIndent() // This will remove leading whitespace

        Log.e("BugReport", bugReport)
        val telegrambot = TelegramBot()
        telegrambot.sendMessage(bugReport)

        // Save the current time as the last submission time
        sharedPreferences.edit().putLong("last_submission_time", currentTime).apply()

        // Show a confirmation dialog
        showAlertDialog("Bug report submitted successfully!")
    }


    private fun showAlertDialog(message: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
