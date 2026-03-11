package com.example.mineteh.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Singleton object for managing the Supabase client instance.
 * Provides centralized access to Supabase services (Auth, Database, Storage).
 */
object SupabaseClient {
    
    /**
     * The Supabase client instance. Must be initialized before use.
     */
    lateinit var client: SupabaseClient
        private set
    
    /**
     * Initializes the Supabase client with the provided configuration.
     * Should be called once during application startup.
     */
    fun initialize() {
        client = createSupabaseClient(
            supabaseUrl = "https://didpavzminvohszuuowu.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRpZHBhdnptaW52b2hzenV1b3d1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMTYwNDgsImV4cCI6MjA4NzU5MjA0OH0.iueZB9z5Z5YvKM98Gsy-ll--kLipCKXtmT0V7jHBA0Y"
        ) {
            install(Postgrest)
            install(Storage)
        }
    }
    
    /**
     * Convenience property for accessing Supabase Database (Postgrest) service.
     */
    val database: Postgrest
        get() = client.postgrest
    
    /**
     * Convenience property for accessing Supabase Storage service.
     */
    val storage: Storage
        get() = client.storage
}
