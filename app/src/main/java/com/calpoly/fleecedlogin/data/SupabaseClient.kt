package com.calpoly.fleecedlogin.data

import com.calpoly.fleecedlogin.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    private fun requireConfig(value: String, keyName: String): String {
        check(value.isNotBlank()) {
            "Missing Supabase config: $keyName. Set it in local.properties or environment."
        }
        return value
    }

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = requireConfig(BuildConfig.SUPABASE_URL.trim(), "SUPABASE_URL"),
            supabaseKey = requireConfig(BuildConfig.SUPABASE_ANON_KEY.trim(), "SUPABASE_ANON_KEY")
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
