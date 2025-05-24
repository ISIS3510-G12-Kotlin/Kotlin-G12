package com.example.explorandes.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.explorandes.MainActivity
import com.example.explorandes.R
import com.example.explorandes.api.ApiClient
import com.example.explorandes.models.User
import com.example.explorandes.utils.SessionManager
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.explorandes.utils.ConnectivityHelper
import android.util.Log
import com.example.explorandes.fragments.VisitedFragment

class AccountFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView
    private lateinit var userRole: TextView
    private lateinit var editProfileOption: LinearLayout
    private lateinit var languageOption: LinearLayout
    private lateinit var notificationsOption: LinearLayout
    private lateinit var signOutOption: LinearLayout
    private lateinit var loadingView: View
    private lateinit var historyOption: LinearLayout

    private lateinit var sessionManager: SessionManager
    private var currentUser: User? = null
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        rootView = view

        sessionManager = SessionManager(requireContext())

        profileImage = view.findViewById(R.id.profileImage)
        userName = view.findViewById(R.id.userName)
        userRole = view.findViewById(R.id.userRole)
        editProfileOption = view.findViewById(R.id.editProfileOption)
        languageOption = view.findViewById(R.id.languageOption)
        notificationsOption = view.findViewById(R.id.notificationsOption)
        signOutOption = view.findViewById(R.id.signOutOption)
        loadingView = view.findViewById(R.id.loadingView)
        historyOption = view.findViewById(R.id.historyOption)

        setupClickListeners()
        loadUserData()

        return view
    }

    private fun loadUserData() {
        showLoading(true)

        val userId = sessionManager.getUserId()
        if (userId <= 0) {
            Toast.makeText(requireContext(), "No se encontró información de usuario", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        val cachedUserName = sessionManager.getCachedUserName()
        val cachedEmail = sessionManager.getEmail() ?: ""
        userName.text = cachedUserName
        userRole.text = "Estudiante"

        val cachedProfilePicUrl = sessionManager.getCachedProfilePicUrl()
        if (!cachedProfilePicUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(cachedProfilePicUrl)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .circleCrop()
                .into(profileImage)
        } else {
            profileImage.setImageResource(R.drawable.profile_placeholder)
        }

        showLoading(false)

        if (ConnectivityHelper(requireContext()).isInternetAvailable()) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.getUserById(userId)
                    if (response.isSuccessful && response.body() != null) {
                        currentUser = response.body()
                        updateUI()
                    } else if (response.code() == 401) {
                        sessionManager.logout()
                        navigateToLogin()
                    }
                } catch (e: Exception) {
                    Log.e("AccountFragment", "Error de conexión: ${e.message}")
                }
            }
        } else {
            if (isAdded && view != null) {
                Snackbar.make(
                    requireView(),
                    "Sin conexión. Mostrando datos almacenados localmente.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateUI() {
        if (!isAdded || view == null) return
        currentUser?.let { user ->
            val displayName = if (!user.firstName.isNullOrEmpty() || !user.lastName.isNullOrEmpty()) {
                val firstName = user.firstName ?: ""
                val lastName = user.lastName ?: ""
                "$firstName $lastName".trim()
            } else {
                user.username
            }
            userName.text = displayName
            userRole.text = "Estudiante"

            if (!user.profileImageUrl.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(user.profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.profile_placeholder)
            }
        }
    }

    private fun setupClickListeners() {
        editProfileOption.setOnClickListener { navigateToEditProfile() }
        languageOption.setOnClickListener { navigateToLanguageSettings() }
        notificationsOption.setOnClickListener { navigateToNotificationSettings() }
        signOutOption.setOnClickListener { signOut() }

        historyOption.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, VisitedFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun navigateToEditProfile() {
        currentUser?.let { user ->
            val fragment = EditProfileFragment()
            fragment.setUser(user)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        } ?: Toast.makeText(requireContext(), "Información de usuario no disponible", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLanguageSettings() {
        val fragment = LanguageFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToNotificationSettings() {
        val fragment = NotificationsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun signOut() {
        sessionManager.logout()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showLoading(isLoading: Boolean) {
        if (::loadingView.isInitialized) {
            loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}
