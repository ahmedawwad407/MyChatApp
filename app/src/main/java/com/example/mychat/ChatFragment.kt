package com.example.mychat

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mychat.databinding.FragmentChatBinding
import com.github.nkzawa.emitter.Emitter
import com.google.gson.Gson


class ChatFragment : Fragment() {

    private val TYPING_TIMER_LENGTH = 600L
    private val TAG = ChatFragment::class.java.simpleName
    private lateinit var mCurrentUser: User
    private lateinit var mWithUser: User
    private lateinit var room: String

    private val mMessages = mutableListOf<Message>()
    private lateinit var mChatType:String


    companion object Chat {
        val CHAT_TYPE_PRIVATE = "private"
        val CHAT_TYPE_ROOM = "room"

        val SOCKET_EVENT_PRIVATE_NEW_MESSAGE = "private_new_message"
        val SOCKET_EVENT_PRIVATE_MESSAGES_LIST = "private_messages_list"

        val SOCKET_EVENT_ROOM_NEW_MESSAGE = "room_new_message"
        val SOCKET_EVENT_ROOM_MESSAGES_LIST = "room_messages_list"


        val SOCKET_EVENT_PRIVATE_TYPING = "private_typing"
        val SOCKET_EVENT_PRIVATE_STOP_TYPING = "private_stop_typing"

        val SOCKET_EVENT_ROOM_TYPING = "room_typing"
        val SOCKET_EVENT_ROOM_STOP_TYPING = "room_stop_typing"

        val SOCKET_EVENT_ROOM_JOIN = "join_room"
        val SOCKET_EVENT_ROOM_LEAVE = "leave_room"
    }

    private val mSocket = MainActivity.getSocket()
    private lateinit var mBinding: FragmentChatBinding
    private var mTyping: Boolean = false;
    private val mTypingHandler = Handler(Looper.getMainLooper())
    private lateinit var mAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)
        initViews()

        val args = ChatFragmentArgs.fromBundle(requireArguments())
        mCurrentUser = args.currentUser
        mChatType = args.type
        when(mChatType){
            CHAT_TYPE_PRIVATE -> {
                mWithUser = args.withUser!!
                (requireActivity() as ActivityListener).setTitle(
                    getString(
                        R.string.chatting_with,
                        mWithUser.name
                    )
                )
            }

            CHAT_TYPE_ROOM -> {
                room = args.room!!
                setHasOptionsMenu(true)
                (requireActivity() as ActivityListener).setTitle(
                    getString(
                        R.string.room_chat,
                        room
                    )
                )

                addLog(getResources().getString(R.string.message_welcome, room))
            }
        }

        setupChatSocket()

        return mBinding.root
    }


    fun initViews() {

        mBinding.apply {
            sendButton.setOnClickListener { sendMessage() }
            mAdapter = MessageAdapter()
            messages.adapter = mAdapter
            messageInput.apply {
                setOnEditorActionListener(OnEditorActionListener { view, id, event ->
                    if (id == R.integer.send || id == EditorInfo.IME_NULL) {
                        sendMessage()
                        return@OnEditorActionListener true
                    }
                    false
                })

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (!mSocket.connected()) return
                        if (!mTyping) {
                            mTyping = true

                            if (mChatType == CHAT_TYPE_PRIVATE){
                                val jsonMessage = Gson().toJson(Message(
                                        Message.TYPE_ACTION,
                                        null,
                                        mCurrentUser,
                                        mWithUser
                                    )
                                )
                                mSocket.emit(SOCKET_EVENT_PRIVATE_TYPING, jsonMessage)
                            } else{
                                val jsonMessage = Gson().toJson(
                                    Message(
                                        Message.TYPE_ACTION, null, mCurrentUser, User(
                                            room,
                                            ""
                                        )
                                    )
                                )
                                mSocket.emit(SOCKET_EVENT_ROOM_TYPING, jsonMessage, room)
                            }


                        }
                        mTypingHandler.removeCallbacks(onTypingTimeout)
                        mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH)
                    }

                    override fun afterTextChanged(s: Editable) {
                    }
                })
            }
        }

    }

    fun setupChatSocket(){
        if(!mSocket.connected()){
            mSocket.connect()
        }
        registerSocketMessageEvents()
        requestMessagesList()
       
    }

    fun requestMessagesList(){
        when(mChatType){
            CHAT_TYPE_PRIVATE -> {
                mSocket.emit(SOCKET_EVENT_PRIVATE_MESSAGES_LIST, mWithUser.sid)
            }

            CHAT_TYPE_ROOM -> {
                mSocket.emit(SOCKET_EVENT_ROOM_MESSAGES_LIST, room)
            }

        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        unregisterSocketMessageEvents()
        mMessages.clear()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_chat, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.action_leave) {
            findNavController().navigate(
                ChatFragmentDirections.actionChatFragmentToMainFragment(
                    mCurrentUser
                )
            )

            val userJson = Gson().toJson(mCurrentUser)
            mSocket.emit(SOCKET_EVENT_ROOM_LEAVE, userJson, room)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun registerSocketMessageEvents() {
        mSocket.apply {
            when(mChatType){
                CHAT_TYPE_PRIVATE -> {
                    on(SOCKET_EVENT_PRIVATE_NEW_MESSAGE, onNewMessage)
                    on(SOCKET_EVENT_PRIVATE_MESSAGES_LIST, onMessagesList)
                    on(SOCKET_EVENT_PRIVATE_TYPING, onTyping)
                    on(SOCKET_EVENT_PRIVATE_STOP_TYPING, onStopTyping)
                }


                CHAT_TYPE_ROOM -> {
                    on(room + SOCKET_EVENT_ROOM_NEW_MESSAGE, onNewMessage)
                    on(room + SOCKET_EVENT_ROOM_JOIN, onRoomJoined)
                    on(room + SOCKET_EVENT_ROOM_LEAVE, onRoomLeaved)
                    on(room + SOCKET_EVENT_ROOM_TYPING, onTyping)
                    on(room + SOCKET_EVENT_ROOM_STOP_TYPING, onStopTyping)
                    on(SOCKET_EVENT_ROOM_MESSAGES_LIST, onMessagesList)
                }
            }

        }
    }

    private fun unregisterSocketMessageEvents() {
        mSocket.apply {
            when(mChatType){
                CHAT_TYPE_PRIVATE -> {
                    off(SOCKET_EVENT_PRIVATE_NEW_MESSAGE, onNewMessage)
                    off(SOCKET_EVENT_PRIVATE_MESSAGES_LIST, onMessagesList)
                    off(SOCKET_EVENT_PRIVATE_TYPING, onTyping)
                    off(SOCKET_EVENT_PRIVATE_STOP_TYPING, onStopTyping)
                }


                CHAT_TYPE_ROOM -> {
                    off(room + SOCKET_EVENT_ROOM_NEW_MESSAGE, onNewMessage)
                    off(room + SOCKET_EVENT_ROOM_JOIN, onRoomJoined)
                    off(room + SOCKET_EVENT_ROOM_LEAVE, onRoomLeaved)
                    off(room + SOCKET_EVENT_ROOM_TYPING, onTyping)
                    off(room + SOCKET_EVENT_ROOM_STOP_TYPING, onStopTyping)
                    off(SOCKET_EVENT_ROOM_MESSAGES_LIST, onMessagesList)

                }
            }
        }
    }

    val onRoomJoined = Emitter.Listener { args ->
        Log.d(TAG, SOCKET_EVENT_ROOM_JOIN)
        val user = Gson().fromJson(args[0] as String, User::class.java)
       // addLog(getResources().getString(R.string.message_user_joined, user.name));
        val message =  Gson().fromJson(args[0] as String, Message::class.java)
        mMessages.add(message)
        updateAdapter()
    }

    val onRoomLeaved = Emitter.Listener { args ->
        Log.d(TAG, SOCKET_EVENT_ROOM_JOIN)
       // val user = Gson().fromJson(args[0] as String, User::class.java)
      //  addLog(getResources().getString(R.string.message_user_left, user.name));
        val message =  Gson().fromJson(args[0] as String, Message::class.java)
        mMessages.add(message)
        updateAdapter()
    }

    private val onMessagesList = Emitter.Listener { args ->
        Log.d(TAG, "onMessagesList")
        val messages =  Gson().fromJson(args[0] as String, Array<Message>::class.java).toList()
        mMessages.addAll(messages)
        updateAdapter()
    }

    private val onNewMessage = Emitter.Listener { args ->
        Log.d(TAG, "onNewMessage")
        val message =  Gson().fromJson(args[0] as String, Message::class.java)
        removeTyping(message)
        mMessages.add(message)
        updateAdapter()
    }


    private fun addLog(message: String) {
        mMessages.add(Message(Message.TYPE_LOG, message, null, null))
        updateAdapter()
    }

    private val onTyping = Emitter.Listener { args ->
        Log.d(TAG, "onTyping")
        val message =  Gson().fromJson(args[0] as String, Message::class.java)
        mMessages.add(message)
        updateAdapter()
    }

    private val onStopTyping = Emitter.Listener { args ->
        Log.d(TAG, "onStopTyping")
        val message =  Gson().fromJson(args[0] as String, Message::class.java)
        mMessages.remove(message)
        updateAdapter()
    }

    private fun sendMessage() {
        if (!mSocket.connected()) return
        mTyping = false
        val message: String = mBinding.messageInput.getText().toString().trim()
        mBinding.messageInput.apply {
            if (TextUtils.isEmpty(message)) {
                requestFocus()
                return
            }
            setText("")
        }

        var to :User?= null
        when(mChatType){
            CHAT_TYPE_PRIVATE -> to = mWithUser
            CHAT_TYPE_ROOM -> to = User("", room)
        }

        val jsonMessage = Gson().toJson(Message(Message.TYPE_MESSAGE, message, mCurrentUser, to))

        when(mChatType){
            CHAT_TYPE_PRIVATE -> mSocket.emit(SOCKET_EVENT_PRIVATE_NEW_MESSAGE, jsonMessage)
            CHAT_TYPE_ROOM -> mSocket.emit(SOCKET_EVENT_ROOM_NEW_MESSAGE, jsonMessage)
        }

    }


    private fun removeTyping(message: Message) {
        for (i in mMessages.indices.reversed()) {
            val (type, _, username1) = mMessages[i]
            if (type == Message.TYPE_ACTION && username1 == message.from) {
                mMessages.removeAt(i)
            }
        }
    }

    private fun scrollToBottom() {
        mBinding.messages.scrollToPosition(mAdapter.itemCount - 1)
    }

    private val onTypingTimeout: Runnable = Runnable {
        if (!mTyping) return@Runnable
        mTyping = false

        if (mChatType == CHAT_TYPE_PRIVATE ) {
            val jsonMessage =  Gson().toJson(Message (
                                Message.TYPE_ACTION,
                        null,
                        mCurrentUser,
                        mWithUser))
            mSocket.emit(SOCKET_EVENT_PRIVATE_STOP_TYPING, jsonMessage)
        } else{
            val jsonMessage =  Gson().toJson(Message(Message.TYPE_ACTION, null, mCurrentUser, User(room, "")))
            mSocket.emit(SOCKET_EVENT_ROOM_STOP_TYPING, jsonMessage, room)
        }



    }

    private fun updateAdapter(){
        requireActivity().runOnUiThread{
            mAdapter.submitList(mMessages)
            mAdapter.notifyDataSetChanged()
            scrollToBottom()
        }
    }
}
