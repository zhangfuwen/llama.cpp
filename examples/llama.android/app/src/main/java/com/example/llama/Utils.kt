package com.example.llama

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

fun showConfirmDialog(
    context: Context,
    title: String = "Confirmation",
    message: String = "Are you sure you want to proceed?",
    positiveButtonText: String = "OK",
    negativeButtonText: String = "Cancel",
    negativeAction: () -> Unit = {},
    positiveAction: () -> Unit = {}
) {
    val alertDialogBuilder = AlertDialog.Builder(context)

    alertDialogBuilder.setTitle(title)
    alertDialogBuilder.setMessage(message)

    alertDialogBuilder.setPositiveButton(positiveButtonText) { dialogInterface: DialogInterface, _: Int ->
        dialogInterface.dismiss()
        positiveAction.invoke()
    }

    alertDialogBuilder.setNegativeButton(negativeButtonText) { dialogInterface: DialogInterface, _: Int ->
        dialogInterface.dismiss()
        negativeAction.invoke()
    }

    val alertDialog = alertDialogBuilder.create()
    alertDialog.show()
}


class DirPickHelper(val activity: ComponentActivity, var block: (Uri?) -> Unit = {}) {
    private var folderUri: Uri? = null

    private val folderPickerLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        folderUri = uri
        block(uri)
    }

    fun launch() {
        folderPickerLauncher.launch(Uri.EMPTY)
    }

}

class FilePickHelper(val activity: ComponentActivity, var handler: (Uri?)->Unit = {}) {
    private val filePickerLauncher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handler(uri)
    }

    fun launch() {
        filePickerLauncher.launch("*/*")
    }
}
