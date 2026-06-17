package com.example.supabase

import com.example.BuildConfig

object Supabase {
    val supabaseUrl = BuildConfig.SUPABASE_URL
    val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

    val isConfigured: Boolean
        get() = !supabaseUrl.contains("your-project") && !supabaseKey.contains("your-anon")
}
