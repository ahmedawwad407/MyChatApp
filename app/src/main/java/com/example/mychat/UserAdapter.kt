package com.example.mychat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mychat.UserAdapter.*
import com.example.mychat.databinding.ItemUserBinding


class UserAdapter(val userListener: UserListener): ListAdapter<User, UserViewHolder>(UserDiffCallback()){


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position), userListener)
    }

    class UserViewHolder private constructor(private val binding: ItemUserBinding):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User, userListener: UserListener){
            binding.user = user
            binding.userListener = userListener
            binding.executePendingBindings()
        }

        companion object{
            fun from(parent: ViewGroup): UserViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemUserBinding.inflate(layoutInflater, parent, false)
                return UserViewHolder(binding)
            }
        }
    }

    class UserDiffCallback: DiffUtil.ItemCallback<User>(){
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.sid == newItem.sid
        }
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    class UserListener(val callback: (user: User)-> Unit){
        fun onClick(user: User) = callback(user)
    }

}