package com.example.facescaners

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/google/mobile")
    suspend fun authWithGoogle(@Body request: AuthRequest): AuthResponse
}