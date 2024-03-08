package com.example.llama

import android.app.Application
import android.net.Uri
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class MyApplication : Application() {
    override fun onCreate() {
        recreateModelConfigFile()
        super.onCreate()
    }

    fun recreateModelConfigFile() {
        val extFilesDir = getExternalFilesDir(null)
        var models = listOf(
            Downloadable(
                "Phi-2 7B (Q4_0, 1.6 GiB)",
                Uri.parse("https://huggingface.co/ggml-org/models/resolve/main/phi-2/ggml-model-q4_0.gguf?download=true"),
                File(extFilesDir, "phi-2-q4_0.gguf"),
                "",
                ""
            ),
            Downloadable(
                "TinyLlama 1.1B (f16, 2.2 GiB)",
                Uri.parse("https://huggingface.co/ggml-org/models/resolve/main/tinyllama-1.1b/ggml-model-f16.gguf?download=true"),
                File(extFilesDir, "tinyllama-1.1-f16.gguf"),
                "",
                ""
            ),
            Downloadable(
                "Phi 2 DPO (Q3_K_M, 1.48 GiB)",
                Uri.parse("https://huggingface.co/TheBloke/phi-2-dpo-GGUF/resolve/main/phi-2-dpo.Q3_K_M.gguf?download=true"),
                File(extFilesDir, "phi-2-dpo.Q3_K_M.gguf")
            ),
            Downloadable(
                "minicpm 2B",
                Uri.parse("https://huggingface.co/runfuture/MiniCPM-2B-dpo-q4km-gguf/resolve/main/MiniCPM-2B-dpo-q4km-gguf.gguf"),
                File(extFilesDir, "MiniCPM-2B-dpo-q4km-gguf.gguf"),
                "<用户>",
                "<AI>"
            ),
            Downloadable(
                "Mistral 7B-3bits",
                Uri.parse("https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q2_K.gguf?download=true"),
                File(extFilesDir, "mistral-7b-instruct-v0.2.Q2_K.gguf"),
                "<s>[INST] ",
                " [/INST]"
            ),
            Downloadable(
                "Gemma 2B-4bits",
                Uri.parse("https://huggingface.co/lmstudio-ai/gemma-2b-it-GGUF/resolve/main/gemma-2b-it-q4_k_m.gguf?download=true"),
                File(extFilesDir, "gemma-2b-it-q4_k_m.gguf"),
                "<|im_start|>system\n" +
                        "you are a helpful assistant<|im_end|>\n" +
                        "<|im_start|>user\n",
                "<|im_end|>\n" +
                        "<|im_start|>assistant",
            ),
            Downloadable(
                "Qwen1.5-1.8B",
                Uri.parse("https://huggingface.co/Qwen/Qwen1.5-1.8B-Chat-GGUF/resolve/main/qwen1_5-1_8b-chat-q4_0.gguf?download=true"),
                File(extFilesDir, "qwen1_5-1_8b-chat-q4_0.gguf"),
                "<|im_start|>system\n" +
                        "you are a helpful assistant<|im_end|>\n" +
                        "<|im_start|>user\n",
                "<|im_end|>\n" +
                        "<|im_start|>assistant",
            ),
        )

        val modelListFilePath = Path(extFilesDir!!.path + "/model_list.json")
        if (Files.exists(modelListFilePath) and Files.isRegularFile(modelListFilePath) and Files.isReadable(
                modelListFilePath
            )
        ) {
//            val jsonString = modelListFilePath.toFile().readText()
//            models = json.decodeFromString(jsonString)
        } else {
            modelListFilePath.createFile().writeText(json.encodeToString(models))
        }
    }
}
