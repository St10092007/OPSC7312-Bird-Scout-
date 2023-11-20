package com.example.birding.Core

data class User(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val profilePictureBase64: String = ""
)