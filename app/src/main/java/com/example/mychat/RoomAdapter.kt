package com.example.mychat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mychat.RoomAdapter.RoomViewHolder
import com.example.mychat.UserAdapter.*
import com.example.mychat.databinding.ItemRoomBinding
import com.example.mychat.databinding.ItemUserBinding


class RoomAdapter(val roomListener: RoomListener): ListAdapter<String, RoomViewHolder>(RoomDiffCallback()){


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        return RoomViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position), roomListener)
    }

    class RoomViewHolder private constructor(private val binding: ItemRoomBinding):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(room: String, roomListener: RoomListener){
            binding.room = room
            binding.roomListener = roomListener
            binding.executePendingBindings()
        }

        companion object{
            fun from(parent: ViewGroup): RoomViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemRoomBinding.inflate(layoutInflater, parent, false)
                return RoomViewHolder(binding)
            }
        }
    }

    class RoomDiffCallback: DiffUtil.ItemCallback<String>(){
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }

    class RoomListener(val callback: (room: String)-> Unit){
        fun onClick(room: String) = callback(room)
    }

}