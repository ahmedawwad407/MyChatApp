package com.example.mychat


import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mychat.MainFragment.SocketEvent.ROOM_JOIN
import com.example.mychat.MainFragment.SocketEvent.ROOM_PUBLIC_NAME
import com.example.mychat.databinding.FragmentLoginBinding
import com.google.gson.Gson


class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val socket = MainActivity.getSocket()
    private lateinit var nickname: String

    companion object{
        val EVENT_LOGIN = "login"
        val EVENT_ADD_USER = "add_user"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_login, container, false
        )

        binding.apply {
            signInButton.setOnClickListener {
                val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
                login()
            }
            usernameInput.setOnEditorActionListener(OnEditorActionListener { textView, id, keyEvent ->
                if (id == R.integer.login || id == EditorInfo.IME_NULL) {
                    login()
                    return@OnEditorActionListener true
                }
                false
            })
        }

        if(!socket.connected()){
            socket.connect()
        }
        socket.on(EVENT_LOGIN){ args ->
            requireActivity().runOnUiThread {
                val userJson = args[0] as String
                socket.emit(ROOM_JOIN, userJson, ROOM_PUBLIC_NAME)
                val user =  Gson().fromJson(userJson, User::class.java)
                findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToMainFragment(
                        user
                    )
                )
            }
        }


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        socket.off(EVENT_LOGIN)
    }


   private fun login(){

        if(socket.connected()){
            nickname = binding.usernameInput.text.toString().trim()
            if(nickname.isEmpty()){
                binding.usernameInput.apply {
                    setError(getString(R.string.error_field_required))
                    requestFocus()
                    return
                }
            }
            binding.signInButton.isEnabled = false
            socket.emit(EVENT_ADD_USER, nickname)
        }else{
            Toast.makeText(requireContext(), "Check your internet connection", Toast.LENGTH_SHORT).show()
        }

   }
}