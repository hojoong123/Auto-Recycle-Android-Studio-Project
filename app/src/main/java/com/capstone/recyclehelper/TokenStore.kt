package com.capstone.recyclehelper

object TokenStore {
    var token: String? = null
    var adminId: Long? = null
    var role: String? = null
    var floor: Int? = null
    var name: String? = null
    var username: String? = null

    fun clear() {
        token = null; adminId = null; role = null
        floor = null; name = null; username = null
    }
}

