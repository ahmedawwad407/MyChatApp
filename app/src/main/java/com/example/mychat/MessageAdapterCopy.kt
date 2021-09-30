package com.example.mychat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class MessageAdapterCopy(context: Context, messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapterCopy.ViewHolder>() {

    private val mMessages: List<Message>
    private val mUsernameColors: IntArray

    init {
        mMessages = messages
        mUsernameColors = context.getResources().getIntArray(R.array.username_colors)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var layout = -1
        when (viewType) {
            Message.TYPE_MESSAGE -> layout = R.layout.item_message
            Message.TYPE_LOG -> layout = R.layout.item_log
            Message.TYPE_ACTION -> layout = R.layout.item_action
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val message: Message = mMessages[position]
        viewHolder.setMessage(message.message)
        viewHolder.setUsername(message.from!!.name)
    }

    override fun getItemCount(): Int {
        return mMessages.size
    }

    override fun getItemViewType(position: Int): Int {
        return mMessages[position].type
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mUsernameView: TextView?
        private val mMessageView: TextView?

        fun setUsername(username: String) {
            if (null == mUsernameView) return
            mUsernameView.text = username
            mUsernameView.setTextColor(getUsernameColor(username))
        }

        fun setMessage(message: String?) {
            if (null == mMessageView) return
            mMessageView.text = message
        }

        private fun getUsernameColor(username: String): Int {
            var hash = 7
            var i = 0
            val len = username.length
            while (i < len) {
                hash = username.codePointAt(i) + (hash shl 5) - hash
                i++
            }
            val index = Math.abs(hash % mUsernameColors.size)
            return mUsernameColors[index]
        }

        init {
            mUsernameView = itemView.findViewById(R.id.username)
            mMessageView = itemView.findViewById(R.id.message)
        }
    }

}
