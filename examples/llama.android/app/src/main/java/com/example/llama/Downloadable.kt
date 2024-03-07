package com.example.llama

import android.app.DownloadManager
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.core.database.getLongOrNull
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import android.graphics.Color as AndroidColor
import java.io.File

@Serializer(forClass = Uri::class)
object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uri {
        return Uri.parse(decoder.decodeString())
    }
}

@Serializer(forClass = File::class)
object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("File", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) {
        encoder.encodeString(value.path)
    }

    override fun deserialize(decoder: Decoder): File {
        return File(decoder.decodeString())
    }
}

@Serializable
data class Downloadable(
    val name: String,
    @Contextual val source: Uri,
    @Contextual val destination: File
) {
    companion object {
        @JvmStatic
        private val tag: String? = this::class.qualifiedName

        sealed interface State
        data object Ready : State
        data class Downloading(val id: Long) : State
        data class Downloaded(val downloadable: Downloadable) : State
        data class Error(val message: String) : State

        @JvmStatic
        @Composable
        fun Button(viewModel: MainViewModel, dm: DownloadManager, item: Downloadable) {
            var status: State by remember {
                mutableStateOf(
                    if (item.destination.exists()) Downloaded(item)
                    else Ready
                )
            }
            var progress by remember { mutableDoubleStateOf(0.0) }

            val coroutineScope = rememberCoroutineScope()

            suspend fun waitForDownload(result: Downloading, item: Downloadable): State {
                while (true) {
                    val cursor = dm.query(DownloadManager.Query().setFilterById(result.id))

                    if (cursor == null) {
                        Log.e(tag, "dm.query() returned null")
                        return Error("dm.query() returned null")
                    }

                    if (!cursor.moveToFirst() || cursor.count < 1) {
                        cursor.close()
                        Log.i(
                            tag,
                            "cursor.moveToFirst() returned false or cursor.count < 1, download canceled?"
                        )
                        return Ready
                    }

                    val pix = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val tix = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val sofar = cursor.getLongOrNull(pix) ?: 0
                    val total = cursor.getLongOrNull(tix) ?: 1
                    cursor.close()

                    if (sofar == total) {
                        return Downloaded(item)
                    }

                    progress = (sofar * 1.0) / total

                    delay(1000L)
                }
            }

            fun onClick() {
                when (val s = status) {
                    is Downloaded -> {
//                        viewModel.load(item.destination.path)
                    }

                    is Downloading -> {
                        coroutineScope.launch {
                            status = waitForDownload(s, item)
                        }
                    }

                    else -> {
                        item.destination.delete()

                        val request = DownloadManager.Request(item.source).apply {
                            setTitle("Downloading model")
                            setDescription("Downloading model: ${item.name}")
                            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                            setDestinationUri(item.destination.toUri())
                        }

                        viewModel.log("Saving ${item.name} to ${item.destination.path}")
                        Log.i(tag, "Saving ${item.name} to ${item.destination.path}")

                        val id = dm.enqueue(request)
                        status = Downloading(id)
                        onClick()
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(20.dp, 4.dp, 20.dp, 4.dp)
                    .fillMaxWidth()
                    .background(Color(AndroidColor.parseColor("#ffd0dfdf")))
                    .padding(8.dp)

                ) {
                Text(
                    "${item.name}",
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .wrapContentSize()
                        .fillMaxWidth().weight(1f)
                )
                Button(
                    onClick = { onClick() },
                    enabled = status !is Downloading && status !is Downloaded,
                    modifier = Modifier.padding(8.dp, 0.dp)
                )
                {
                    Text(
                        when (status) {
                            is Downloading -> "${(progress * 100).toInt()}%"
                            is Downloaded -> "Downloaded"
                            is Ready -> "Ready"
                            is Error -> "Error"
                        }
                        )
                }

            }

        }

    }
}

val json = Json {
    serializersModule = SerializersModule {
        contextual(Uri::class, UriSerializer)
        contextual(File::class, FileSerializer)
    }
}

