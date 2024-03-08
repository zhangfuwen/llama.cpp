package com.example.llama

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.documentfile.provider.DocumentFile
import com.example.llama.ui.theme.LlamaAndroidTheme
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

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
        if (Files.exists(modelListFilePath) and Files.isRegularFile(modelListFilePath) and Files.isReadable(
                modelListFilePath
            )
        ) {
            val jsonString = modelListFilePath.toFile().readText()
            models = json.decodeFromString(jsonString)
        }
        val dirPickHelper = DirPickHelper(this@DownloadActivity)
        val filePickHelper = FilePickHelper(this@DownloadActivity)

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
                Row (modifier = Modifier.padding(20.dp,0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                    ){
                    Button(onClick = {
                        showConfirmDialog(this@DownloadActivity, positiveAction = {
                            modelListFilePath.deleteIfExists()
                            (application as MyApplication).recreateModelConfigFile()
                            this@DownloadActivity.recreate()
                        })
                    }, modifier = Modifier.padding(0.dp, 0.dp)) {
                        Text("清除配置文件")
                    }
                    Button(onClick = {
                        with(dirPickHelper) {
                            block = { uri ->
                                uri?.let {
                                    val folder = DocumentFile.fromTreeUri(activity, uri)
                                    val file = folder?.createFile("text/json", "export.json")

                                    file?.uri?.let { fileUri ->
                                        val outputStream =
                                            activity.contentResolver.openOutputStream(fileUri)
                                        outputStream.use { stream ->
                                            if (modelListFilePath.exists() && modelListFilePath.isReadable()) {
                                                stream?.write(modelListFilePath.readBytes())
                                            }
                                        }
                                    }
                                }
                            }
                            launch()
                        }

                    }, modifier = Modifier.padding(0.dp, 0.dp)) {
                        Text("导出")
                    }
                    Button(onClick = {
                        filePickHelper.handler = {
                            it?.let {
                                val inputStream =
                                    filePickHelper.activity.contentResolver.openInputStream(it)
                                modelListFilePath.deleteIfExists()
                                modelListFilePath.outputStream().write(inputStream?.readBytes())
                                filePickHelper.activity.recreate()
                            }

                        }
                        filePickHelper.launch()
                    }, modifier = Modifier.padding(0.dp, 0.dp)) {
                        Text("导入")
                    }

                    Text(
                        text = AnnotatedString("配置商店", spanStyle = (SpanStyle(
                            textDecoration = TextDecoration.Underline
                        ))),
                        modifier = Modifier
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zhangfuwen/llama.cpp"))
                            application.startActivity(intent)
                        }
                    )
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

