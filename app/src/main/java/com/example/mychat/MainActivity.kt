package com.example.mychat

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.mychat.MainActivity.*
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URISyntaxException


class MainActivity : AppCompatActivity(), ActivityListener {

    private lateinit var navController: NavController

    object ChatSocket{
        lateinit var socket: Socket
        init {
            try{
                socket = IO.socket("http://192.168.3.120:5000")
            }catch (e: URISyntaxException){
                Log.e(TAG, e.message.toString())
            }
        }
    }

    companion object{
        private val TAG = MainActivity.javaClass.simpleName
        fun getSocket() = ChatSocket.socket
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration
            .Builder(
                R.id.loginFragment,
                R.id.mainFragment)
            .build()
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        registerSocketConnectionEvents()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterSocketConnectionEvents()
        getSocket().disconnect()
    }

    private fun registerSocketConnectionEvents() {
        getSocket().apply {
            on(Socket.EVENT_CONNECT, onConnect)
            on(Socket.EVENT_DISCONNECT, onDisconnect)
            on(Socket.EVENT_CONNECT_ERROR, onConnectError)
        }
    }

    private fun unregisterSocketConnectionEvents() {
        getSocket().apply {
            off(Socket.EVENT_CONNECT, onConnect)
            off(Socket.EVENT_DISCONNECT, onDisconnect)
            off(Socket.EVENT_CONNECT_ERROR, onConnectError)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    val onConnect = Emitter.Listener { args ->
        Log.d(TAG, Socket.EVENT_CONNECT)
    }

    val onDisconnect = Emitter.Listener { args ->
        Log.d(TAG, Socket.EVENT_DISCONNECT)
    }

    val onConnectError = Emitter.Listener { args ->
        Log.d(TAG, Socket.EVENT_CONNECT_ERROR)
    }

    override fun setTitle(title: String) {
        supportActionBar?.title = title
    }

}

interface ActivityListener{
    fun setTitle(title: String)
}