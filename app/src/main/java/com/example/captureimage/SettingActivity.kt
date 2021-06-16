package com.example.captureimage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.MenuItem

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_setting)
    }


    class ClickImagePreferenceFragment: PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.setting_main)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when(item.itemId) {
            android.R.id.home ->{
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}