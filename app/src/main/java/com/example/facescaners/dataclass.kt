package com.example.facescaners

data class AuthRequest(val token: String)
data class AuthResponse(val message: String, val user: UserInfo)
data class UserInfo(val name: String, val email: String)