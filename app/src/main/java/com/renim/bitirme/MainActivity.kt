package com.renim.bitirme

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

// osmdroid için
import org.osmdroid.config.Configuration
// PreferenceManager için (androidx.preference kütüphanesini ekledikten sonra kullanılabilir)
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid config: load preferences & set user-agent
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        // BuildConfig yerine paket adını kullanmak daha güvenli (derleyici hatalarını engeller)
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
    }
}
