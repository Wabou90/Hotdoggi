package com.flowautomation.app.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.flowautomation.app.storage.AppPreferences

class DownloadHandler(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun download(url: String, filename: String, folder: String, mimeType: String = "video/mp4"): Long {
        val sanitizedFolder = folder.trim().replace("/", "").replace("..", "")
        val relativePath = if (sanitizedFolder.isNotEmpty()) {
            "${Environment.DIRECTORY_DOWNLOADS}/$sanitizedFolder"
        } else {
            Environment.DIRECTORY_DOWNLOADS
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setDescription("Downloading from Google Flow")
            .setMimeType(mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(relativePath, filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        return downloadManager.enqueue(request)
    }

    fun cancel(downloadId: Long) {
        downloadManager.remove(downloadId)
    }

    companion object {
        fun suggestFilename(url: String, fallback: String = "flow_output.mp4"): String {
            val fromUrl = url.substringAfterLast("/").substringBefore("?")
            return fromUrl.ifEmpty { fallback }
        }
    }
}
