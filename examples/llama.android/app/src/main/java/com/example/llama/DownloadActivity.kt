package com.example.llama

import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.getSystemService
import com.example.llama.ui.theme.LlamaAndroidTheme
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class DownloadActivity(
    downloadManager: DownloadManager? = null,
) : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extFilesDir = getExternalFilesDir(null)


        val modelListFilePath = Path(extFilesDir!!.path + "/model_list.json")
        var models = listOf<Downloadable>()
        if (Files.exists(modelListFilePath) and Files.isRegularFile(modelListFilePath) and Files.isReadable(modelListFilePath)){
            val jsonString = modelListFilePath.toFile().readText()
            models = json.decodeFromString(jsonString)
        }

        @Composable
        fun Greeting(name: String, modifier: Modifier = Modifier) {
//            Text(
//                text = "Hello $name!",
//                modifier = modifier
//            )
            Column {
                for (model in models) {
                    Downloadable.Button(viewModel, downloadManager, model)
                }
            }
        }

        @Composable
        fun GreetingPreview() {
            LlamaAndroidTheme {
                Greeting("Android")
            }
        }
        setContent {
            LlamaAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

