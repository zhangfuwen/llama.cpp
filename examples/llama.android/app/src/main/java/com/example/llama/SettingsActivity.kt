package com.example.llama

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val listPreference = findPreference<ListPreference>("selected_model")

            val extFilesDir = activity?.getExternalFilesDir(null)
            // Dynamically populate the list entries and values
            val modelListFilePath = Path(extFilesDir!!.path + "/model_list.json")
            if (Files.exists(modelListFilePath) and Files.isRegularFile(modelListFilePath) and Files.isReadable(modelListFilePath)){
                val jsonString = modelListFilePath.toFile().readText()
                var models : List<Downloadable> = json.decodeFromString(jsonString)
                listPreference?.entries = models
                    .filter { it.destination.exists() }
                    .map { it.name }
                    .toTypedArray()
                listPreference?.entryValues = models
                    .filter { it.destination.exists() }
                    .map { json.encodeToString(it) }
                    .toTypedArray()
            }
        }
    }
}
