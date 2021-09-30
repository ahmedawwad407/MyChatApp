package com.example.mychat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mychat.databinding.FragmentMainBinding
import com.github.nkzawa.emitter.Emitter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson


class MainFragment : Fragment() {

    private val TAG = MainFragment::class.java.simpleName
    private var mCurrentUser: User? = null
    private val mUsers = mutableListOf<User>()
    private val mRooms = mutableListOf<String>()
    private lateinit var mUserAdapter: UserAdapter
    private lateinit var mRoomAdapter: RoomAdapter

    private val mSocket = MainActivity.getSocket()
    private lateinit var mBinding: FragmentMainBinding

    companion object SocketEvent {
        val USER_JOINED = "user_joined"
        val USER_LEFT = "user_left"
        val USERS_LIST = "users_list"
        val ROOM_JOIN = "join_room"
        val ROOM_LIST = "rooms_list"
        val ROOM_PUBLIC_NAME = "Public"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)


        if(mCurrentUser == null){
            arguments?.let {
                if (!it.isEmpty) {
                    val args = MainFragmentArgs.fromBundle(it)
                    mCurrentUser = args.currentUser
                }
            }
        }

        if (mCurrentUser == null) {
            findNavController().navigate(MainFragmentDirections.actionMainFragmentToLoginFragment())
        } else {
            (requireActivity() as ActivityListener).setTitle(getString(R.string.welcome,  mCurrentUser?.name))
            setHasOptionsMenu(true)
            initViews()
            setupChatSocket()
        }

        return mBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketUserEvents()
        mUsers.clear()
        mRooms.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.action_logout) {
            findNavController().navigate(MainFragmentDirections.actionMainFragmentToLoginFragment())
            mSocket.emit(USER_LEFT)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    fun initViews() {

        mBinding.joinRoomFab.setOnClickListener{
            createDialog(getString(R.string.join))
        }
        mBinding.usersRecyclerview.apply {
            mUserAdapter = UserAdapter(UserAdapter.UserListener { user ->
                findNavController().navigate(
                    MainFragmentDirections.actionMainFragmentToChatFragment(
                        mCurrentUser!!, user, ChatFragment.CHAT_TYPE_PRIVATE
                    )
                )
            })
            adapter = mUserAdapter

            addItemDecoration(
                DividerItemDecoration(
                    context,
                    (layoutManager as LinearLayoutManager).orientation
                )
            )
        }

        mBinding.roomsRecyclerview.apply {
            (layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
            mRoomAdapter = RoomAdapter(RoomAdapter.RoomListener{ room ->
                findNavController().navigate(
                    MainFragmentDirections.actionMainFragmentToChatFragment(
                        mCurrentUser!!, room = room, type = ChatFragment.CHAT_TYPE_ROOM
                    )
                )
            })
            adapter = mRoomAdapter

            addItemDecoration(
                DividerItemDecoration(
                    context,
                    (layoutManager as LinearLayoutManager).orientation
                )
            )

        }
    }

    fun setupChatSocket() {
        if (!mSocket.connected()) {
            mSocket.connect()
        }
        registerSocketUserEvents()
        mSocket.emit(USERS_LIST)
        mSocket.emit(ROOM_LIST)
    }

    fun registerSocketUserEvents() {
        mSocket.apply {
            on(USER_JOINED, onUserJoined)
            on(USER_LEFT, onUserLeft)
            on(USERS_LIST, onUsersList)
            on(ROOM_JOIN, onRoomJoined)
            on(ROOM_LIST, onRoomsList)
        }
    }

    fun unregisterSocketUserEvents() {
        mSocket.apply {
            off(USER_JOINED, onUserJoined)
            off(USER_LEFT, onUserLeft)
            off(USERS_LIST, onUsersList)
            off(ROOM_JOIN, onRoomJoined)
            off(ROOM_LIST, onRoomsList)
        }
    }

    val onUserJoined = Emitter.Listener { args ->
        Log.d(TAG, USER_JOINED)
        val user = Gson().fromJson(args[0] as String, User::class.java)
        mUsers.add(user)
        updateUserAdapter()
    }

    val onUserLeft = Emitter.Listener { args ->
        Log.d(TAG, USER_LEFT)
        val user = Gson().fromJson(args[0] as String, User::class.java)
        mUsers.remove(user)
        updateUserAdapter()
    }

    val onUsersList = Emitter.Listener { args ->
        Log.d(TAG, USERS_LIST)
        val users = Gson().fromJson(args[0] as String, Array<User>::class.java).toList()
        mUsers.addAll(users)
        updateUserAdapter()
    }

    val onRoomJoined = Emitter.Listener { args ->
        Log.d(TAG, ROOM_JOIN)
        val room = args[0] as String
        if(room !in mRooms){
            mRooms.add(room)
            updateRoomAdapter()
        }

    }


    val onRoomsList = Emitter.Listener { args ->
        Log.d(TAG, ROOM_LIST)
        val rooms = Gson().fromJson(args[0] as String, Array<String>::class.java).toList()
        for(room in rooms){
            if(room !in mRooms && room != mCurrentUser?.sid){
                mRooms.add(room)
            }
        }
        updateRoomAdapter()
    }

    private fun updateUserAdapter() {
        requireActivity().runOnUiThread {
            mUserAdapter.submitList(mUsers)
            mUserAdapter.notifyDataSetChanged()
        }
    }

    private fun updateRoomAdapter() {
        requireActivity().runOnUiThread {
            mRoomAdapter.submitList(mRooms)
            mRoomAdapter.notifyDataSetChanged()
        }
    }

    private fun createDialog(positiveBtnName: String) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_room, null)
        val roomInputLayout = view.findViewById<TextInputLayout>(R.id.room_input_layout)
        val roomEdittext = view.findViewById<TextInputEditText>(R.id.room_edit_text)

        builder.setView(view)
        builder.setPositiveButton(positiveBtnName) { dialog, which ->
            val roomName = roomEdittext.text.toString()
            val userJson = Gson().toJson(mCurrentUser)
            mSocket.emit(ROOM_JOIN, userJson, roomName)
        }

        builder.setNeutralButton(R.string.cancel) { dialog, which ->
            dialog.dismiss()
        }

        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()

        // initially disable the positive button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        // edit text text change listener
        roomEdittext.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.isNullOrBlank()) {
                    roomInputLayout.error = "Room name is required."
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                } else {
                    roomInputLayout.error = ""
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })
    }
}
