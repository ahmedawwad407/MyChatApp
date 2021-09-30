package com.example.mychat

import com.google.gson.annotations.SerializedName

data class Message(val type: Int,
                   val message: String?,
                   @SerializedName("from_") val  from: User?,
                   val to:User?){
    companion object {
        const val TYPE_MESSAGE = 0
        const val TYPE_LOG = 1
        const val TYPE_ACTION = 2
    }
}
