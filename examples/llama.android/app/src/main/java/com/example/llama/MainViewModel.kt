package com.example.llama

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(private val llm: Llm = Llm.instance()): ViewModel() {
    companion object {
        @JvmStatic
        private val NanosPerSecond = 1_000_000_000.0
    }

    private val tag: String? = this::class.simpleName

    var messages by mutableStateOf(listOf("Initializing..."))
        private set

    var message by mutableStateOf("")
        private set

    var info by mutableStateOf("")

    var prefix:String = ""
    var postfix:String=""

    override fun onCleared() {
        super.onCleared()

        viewModelScope.launch {
            try {
                llm.unload()
            } catch (exc: IllegalStateException) {
                messages += exc.message!!
            }
        }
    }

    fun send() {
        if(llm.loadedModel.value=="") {
            messages += "**系统：** 未选择并加载模型"
            return
        }
        val text = message
        message = ""

        // Add to messages console.
        messages += "**用户：** "+text
        messages += "**机器人：** "

        viewModelScope.launch {
            llm.send(prefix+text+postfix)
                .catch {
                    Log.e(tag, "send() failed", it)
                    messages += it.message!!
                }
                .collect { messages = messages.dropLast(1) + (messages.last() + it) }
        }
    }

    fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1) {
        viewModelScope.launch {
            try {
                val start = System.nanoTime()
                val warmupResult = llm.bench(pp, tg, pl, nr)
                val end = System.nanoTime()

                messages += warmupResult

                val warmup = (end - start).toDouble() / NanosPerSecond
                messages += "Warm up time: $warmup seconds, please wait..."

                if (warmup > 5.0) {
                    messages += "Warm up took too long, aborting benchmark"
                    return@launch
                }

                messages += llm.bench(512, 128, 1, 3)
            } catch (exc: IllegalStateException) {
                Log.e(tag, "bench() failed", exc)
                messages += exc.message!!
            }
        }
    }

    fun load(pathToModel: String, prefix:String, postfix:String) {
        viewModelScope.launch {
            try {
                llm.load(pathToModel)
                this@MainViewModel.prefix =prefix
                this@MainViewModel.postfix = postfix
                messages += "Loaded $pathToModel"
                messages += "prefix $prefix, postfix: $postfix"
                messages += "system: ${llm.system_info()}"
                Log.e(tag, "loaded $pathToModel")
            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                messages += "load() failed" + exc.message!!
            }
        }
    }

    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    fun clear() {
        messages = listOf()
    }

    fun log(message: String) {
        messages += "**系统日志：** "+message
    }
}
