package com.chani.porumi.data

import com.chani.porumi.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.coil.Coil3Integration
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

const val PHOTO_BUCKET = "pet-photos"

val isSupabaseConfigured: Boolean
    get() = AppConfig.SUPABASE_URL.isNotBlank() && AppConfig.SUPABASE_KEY.isNotBlank()

internal fun createPorumiSupabase(): SupabaseClient =
    createSupabaseClient(
        supabaseUrl = AppConfig.SUPABASE_URL,
        supabaseKey = AppConfig.SUPABASE_KEY,
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Functions)
        install(Coil3Integration)
    }
