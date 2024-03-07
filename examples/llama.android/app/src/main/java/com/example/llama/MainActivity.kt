package com.example.llama

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.format.Formatter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.example.llama.ui.theme.LlamaAndroidTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class MainActivity(
    activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
) : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val tag: String? = this::class.simpleName

    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy {
        clipboardManager ?: getSystemService<ClipboardManager>()!!
    }

    private val viewModel: MainViewModel by viewModels()

    // Get a MemoryInfo object for the device's current memory status.
    private fun availableMemory(): ActivityManager.MemoryInfo {
        return ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        WindowCompat.setDecorFitsSystemWindows(window, true)
        val flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        window.decorView.systemUiVisibility = flags

        val free = Formatter.formatFileSize(this, availableMemory().availMem)
        val total = Formatter.formatFileSize(this, availableMemory().totalMem)

        viewModel.log("Current memory: $free / $total")
        viewModel.log("Downloads directory: ${getExternalFilesDir(null)}")
        viewModel.log("existing models:\n ${getExternalFilesDir(null)?.list()?.joinToString("\n")}")

        val extFilesDir = getExternalFilesDir(null)

        val models = listOf(
            Downloadable(
                "Phi-2 7B (Q4_0, 1.6 GiB)",
                Uri.parse("https://huggingface.co/ggml-org/models/resolve/main/phi-2/ggml-model-q4_0.gguf?download=true"),
                File(extFilesDir, "phi-2-q4_0.gguf"),
            ),
            Downloadable(
                "TinyLlama 1.1B (f16, 2.2 GiB)",
                Uri.parse("https://huggingface.co/ggml-org/models/resolve/main/tinyllama-1.1b/ggml-model-f16.gguf?download=true"),
                File(extFilesDir, "tinyllama-1.1-f16.gguf"),
            ),
            Downloadable(
                "Phi 2 DPO (Q3_K_M, 1.48 GiB)",
                Uri.parse("https://huggingface.co/TheBloke/phi-2-dpo-GGUF/resolve/main/phi-2-dpo.Q3_K_M.gguf?download=true"),
                File(extFilesDir, "phi-2-dpo.Q3_K_M.gguf")
            ),
            Downloadable(
                "minicpm 2B",
                Uri.parse("https://huggingface.co/runfuture/MiniCPM-2B-dpo-q4km-gguf/resolve/main/MiniCPM-2B-dpo-q4km-gguf.gguf"),
                File(extFilesDir, "MiniCPM-2B-dpo-q4km-gguf.gguf")
            ),
        )


        val composeView = ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // The setContent call takes a Composable lambda extension which can render Composable UI.
            setContent {
                // We then render a simple Text component from Compose.
                LlamaAndroidTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainCompose(
                            viewModel,
                            clipboardManager,
                            downloadManager,
                            models,
                        )
                    }

                }
            }
        }
        var linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.addView(composeView)
//        linearLayout.addView(TextView(this))
        setContentView(linearLayout)

//        Llm.instance().loadedModel.observe(this@MainActivity) {
//            if(it!=null) {
//                supportActionBar?.title = if (it.isEmpty()) "no model" else it.split("/").last()
//            }
//        }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener (this)

        val selectedModel = sharedPreferences.getString("selected_model", null )
        if (selectedModel!= null) {
            var model = json.decodeFromString<Downloadable>(selectedModel)
            supportActionBar?.title = model.name
            viewModel.load(model.destination.absolutePath)
        } else {
            supportActionBar?.title = "no model selected"
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }

            R.id.menu_item_download -> {
                startActivity(Intent(this, DownloadActivity::class.java))
                return true
            }

            R.id.menu_item_log -> {
//                startActivity(Intent(this, DownloadActivity::class.java))
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "selected_model") {
            val selectedModel = sharedPreferences?.getString("selected_model", "no model selected")
            val downloadable = json.decodeFromString<Downloadable>(selectedModel!!)
            supportActionBar?.title = downloadable.name
            if(downloadable.destination.exists()) {
                viewModel.load(downloadable.destination.absolutePath)
                val free = Formatter.formatFileSize(this, availableMemory().availMem)
                val total = Formatter.formatFileSize(this, availableMemory().totalMem)
                viewModel.log("Current memory: $free / $total")
            } else {
                Log.e(tag, "selected model does not exists, ${downloadable.destination.absolutePath}")
            }
        }
    }
    @Composable
    fun TickingText(modifier: Modifier) {
        val freeMemory = remember { mutableStateOf(0L) }
        val totalMemory = remember { mutableStateOf(0L) }

        LaunchedEffect(Unit) {
            while (true) {
                delay(1000) // Wait for 1 second
                freeMemory.value = availableMemory().availMem
                totalMemory.value = availableMemory().totalMem
            }
        }

        Column (modifier = modifier){
            Text(text = "Memory: ${Formatter.formatFileSize(this@MainActivity,freeMemory.value)}/${Formatter.formatFileSize(this@MainActivity,totalMemory.value)}",
                color=Color.White
            )
        }
    }

    @Composable
    fun MainCompose(
        viewModel: MainViewModel,
        clipboard: ClipboardManager,
        dm: DownloadManager,
        models: List<Downloadable>,
    ) {
        TickingText(modifier=Modifier.wrapContentSize(align = Alignment.TopStart).padding(8.dp).alpha(0.5f).zIndex(20f).background(Color.Black).padding(8.dp))
        Column {
            val scrollState = rememberLazyListState()

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(state = scrollState) {
                    items(viewModel.messages) {
                        Text(
//                  MarkdownText(
                            it,
                            style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                            color = Color.Black,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(
                                    Color(android.graphics.Color.parseColor("#aaff1f")),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(20.dp, 8.dp)

                        )
                    }
                }
            }
            OutlinedTextField(
                value = viewModel.message,
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                onValueChange = { viewModel.updateMessage(it) },
                label = { Text("Message") },
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(20.dp, 0.dp, 20.dp, 0.dp)
                    .fillMaxWidth()
            ) {
                Button({ viewModel.send() },
                    Modifier
                        .padding(4.dp, 0.dp)
                        .weight(1f)) { Text("Send") }
                Button(
                    { viewModel.bench(8, 4, 1) },
                    Modifier
                        .padding(4.dp, 0.dp)
                        .weight(1f)
                ) { Text("Bench") }
                Button({ viewModel.clear() },
                    Modifier
                        .padding(4.dp, 0.dp)
                        .weight(1f)) { Text("Clear") }
                Button({
                    viewModel.messages.joinToString("\n").let {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", it))
                    }
                },
                    Modifier
                        .padding(4.dp, 0.dp)
                        .weight(1f)) { Text("Copy") }
            }

//        Column {
//            for (model in models) {
//                Downloadable.Button(viewModel, dm, model)
//            }
//        }
        }


    }
}



