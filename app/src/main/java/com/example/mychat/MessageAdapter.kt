package com.example.mychat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mychat.databinding.ItemActionBinding
import com.example.mychat.databinding.ItemLogBinding
import com.example.mychat.databinding.ItemMessageBinding


class MessageAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var viewHolder : RecyclerView.ViewHolder? = null
        when (viewType) {
            Message.TYPE_MESSAGE -> viewHolder =  MessageViewHolder.from(parent)
            Message.TYPE_LOG -> viewHolder =  LogViewHolder.from(parent)
            Message.TYPE_ACTION -> viewHolder = ActionViewHolder.from(parent)
        }
        return viewHolder!!
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (viewHolder.itemViewType) {
            Message.TYPE_MESSAGE ->  (viewHolder as  MessageViewHolder).bind(getItem(position))
            Message.TYPE_LOG ->(viewHolder as  LogViewHolder).bind(getItem(position))
            Message.TYPE_ACTION -> (viewHolder as  ActionViewHolder).bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    class LogViewHolder private constructor(private val binding: ItemLogBinding):
        RecyclerView.ViewHolder(binding.root){

        fun bind(message: Message){
             binding.message = message
             binding.executePendingBindings()
        }

        companion object{
            fun from(parent: ViewGroup): LogViewHolder{
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemLogBinding.inflate(inflater, parent, false)
                return LogViewHolder(binding)
            }
        }
    }

    class ActionViewHolder private constructor(private val binding: ItemActionBinding):
        RecyclerView.ViewHolder(binding.root){

        fun bind(message: Message){
             binding.message = message
             binding.executePendingBindings()
        }

        companion object{
            fun from(parent: ViewGroup): ActionViewHolder{
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemActionBinding.inflate(inflater, parent, false)
                return ActionViewHolder(binding)
            }
        }
    }

    class MessageViewHolder private constructor(private val binding: ItemMessageBinding,
        private val colors: IntArray): RecyclerView.ViewHolder(binding.root){

        fun bind(message: Message){
            binding.message = message
            binding.usernameView.setTextColor(getUsernameColor(message.from!!.name))
            binding.executePendingBindings()
        }

        private fun getUsernameColor(username: String): Int {
            var hash = 7
            var i = 0
            val len = username.length
            while (i < len) {
                hash = username.codePointAt(i) + (hash shl 5) - hash
                i++
            }
            val index = Math.abs(hash % colors.size)
            return colors[index]
        }

        companion object{
            fun from(parent: ViewGroup): MessageViewHolder{
                val colors =  parent.context.getResources().getIntArray(R.array.username_colors)
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemMessageBinding.inflate(inflater, parent, false)
                return MessageViewHolder(binding, colors)
            }
        }
    }

    class MessageDiffUtil: DiffUtil.ItemCallback<Message>(){
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
