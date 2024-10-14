package com.example.youtubeconverter

import android.Manifest
import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
//import com.example.youtubeconverter.MainActivity.Companion.ACTION_CONVERSION_COMPLETE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VideoInfo(val link: String, val title: String)


class ForegroundService : Service() {
    companion object {
        const val ACTION_CONVERT_MP4_TO_MP3 = "com.example.youtubeconverter.ACTION_CONVERT_MP4_TO_MP3"
        const val ACTION_CONVERT_YOUTUBE_TO_MP3 = "com.example.youtubeconverter.ACTION_CONVERT_YOUTUBE_TO_MP3"
        const val ACTION_CONVERT_PLAYLIST_TO_MP3 = "com.example.youtubeconverter.ACTION_CONVERT_PLAYLIST_TO_MP3"
        const val ACTION_CONVERT_PLAYLIST_TO_MP4 = "com.example.youtubeconverter.ACTION_CONVERT_PLAYLIST_TO_MP4"
        const val EXTRA_INPUT_PATH = "com.example.youtubeconverter.EXTRA_INPUT_PATH"
        const val EXTRA_OUTPUT_PATH = "com.example.youtubeconverter.EXTRA_OUTPUT_PATH"
        const val EXTRA_YOUTUBE_URL = "com.example.youtubeconverter.EXTRA_YOUTUBE_URL"
        const val EXTRA_PLAYLIST_URL = "com.example.youtubeconverter.EXTRA_PLAYLIST_URL"
        const val EXTRA_URI = "com.example.youtubeconverter.EXTRA_URI"
        const val ACTION_CONVERT_YOUTUBE_TO_MP4 = "com.example.youtubeconverter.ACTION_CONVERT_YOUTUBE_TO_MP4"
        const val EXTRA_RESOLUTION_CHOICE = "com.example.youtubeconverter.EXTRA_RESOLUTION_CHOICE"
        // ... other constants ...
    }

    // Other members...
    private val channelId = "ForegroundServiceChannel"
    private val notificationId = 1

    // List to store paths of temporary files
    private val tempFilePaths = mutableListOf<String>()


    /*override fun onCreate() {
        super.onCreate()
        //createNotificationChannel()
    }*/

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        // Start with an indeterminate progress notification
        updateNotificationProgress(0, indeterminate = true)
        // Determine what action to take based on the intent's extras
        when (intent?.action) {
            ACTION_CONVERT_MP4_TO_MP3 -> {
                val inputPath = intent.getStringExtra(EXTRA_INPUT_PATH) ?: return START_NOT_STICKY
                val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH) ?: return START_NOT_STICKY
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_URI)
                }
                //Log.e("InpuPathValueFromForegroundService", inputPath)
                // Start MP4 to MP3 conversion
                if (uri != null) {
                    tempFilePaths.add(inputPath)  // Add input file path to temp list
                    startMp4ToMp3Conversion(uri, inputPath, outputPath)
                }
            }
            ACTION_CONVERT_YOUTUBE_TO_MP3 -> {
                val youtubeUrl = intent.getStringExtra(EXTRA_YOUTUBE_URL) ?: return START_NOT_STICKY
                Log.d("YoutubeLinkConversion", "Starting startYoutubeToMp3Conversion(youtubeUrl)")
                // Start YouTube to MP3 conversion
                startYoutubeToMp3Conversion(youtubeUrl)
            }
            ACTION_CONVERT_PLAYLIST_TO_MP3 -> {
                val playlistUrl = intent.getStringExtra(EXTRA_PLAYLIST_URL) ?: return START_NOT_STICKY
                startPlaylistToMp3Conversion(playlistUrl)
            }
            ACTION_CONVERT_PLAYLIST_TO_MP4 -> {
                val playlistUrl = intent.getStringExtra(EXTRA_PLAYLIST_URL) ?: return START_NOT_STICKY
                startPlaylistToMp4Conversion(playlistUrl)
            }

            ACTION_CONVERT_YOUTUBE_TO_MP4 -> {
                val youtubeUrl = intent.getStringExtra(EXTRA_YOUTUBE_URL)
                val resolutionChoice = intent.getStringExtra(EXTRA_RESOLUTION_CHOICE)
                if (youtubeUrl != null && resolutionChoice != null) {
                    startYouTubeToMp4Conversion(youtubeUrl, resolutionChoice)
                }
            }

        }
        return START_NOT_STICKY
    }

    private fun startMp4ToMp3Conversion(uri: Uri, inputPath: String, outputPath: String) {
        // Handle MP4 to MP3 conversion here
        // Start conversion in a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext,"Conversion has started!")
                }
                //("InpuPathValueFromBeforeMP4..", inputPath)
                convertMp4ToMp3(applicationContext, inputPath, outputPath)
                // Extract the file name without the extension
                withContext(Dispatchers.Main) {
                    val fileName = getFileNameFromUri(uri, applicationContext)
                    //Log.e("FileName", fileName)
                    saveToDownloads(applicationContext, outputPath, fileName, "mp3")
                    tempFilePaths.add(outputPath) // Add output file path to temp list
                    cleanupTempFiles() // Call cleanup after saving
                    notifyConversionComplete("button1")
                    ToastUtil.showToast(applicationContext,"File $fileName.mp3 saved in Download/Converted Files")
                    // Send broadcast to indicate conversion completion
                    // After saving, update notification to show completion
                    completeNotification("File saved: $fileName.mp3")
                }

            } catch (e: Exception){
                //completeNotification("$e")
                ToastUtil.showToast(applicationContext,"Conversion Failed")
            }
        }
    }

    private fun notifyConversionComplete(taskId: String) {
        // Updated action string to match the new intent filter
        val broadcastIntent = Intent("com.example.youtubeconverter.TASK_COMPLETE")
        broadcastIntent.putExtra("taskId", taskId)
        sendBroadcast(broadcastIntent)
    }

    private fun startYoutubeToMp3Conversion(youtubeUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Conversion has started!")
                }
                val downloadPath = downloadYouTubeAudio(applicationContext, youtubeUrl)
                if (downloadPath.isNotEmpty() && File(downloadPath).exists()) {
                    val fileNameWithoutExtension = File(downloadPath).nameWithoutExtension.replace("_audio", "")
                    val outputPath = "${applicationContext.filesDir.absolutePath}/$fileNameWithoutExtension.mp3"

                    // Convert the audio file to MP3 using FFmpeg
                    convertAudioToMp3(applicationContext, downloadPath, outputPath)

                    withContext(Dispatchers.Main) {
                        val fileName = "$fileNameWithoutExtension.mp3"
                        saveToDownloads(applicationContext, outputPath, fileName, "mp3")
                        tempFilePaths.add(downloadPath)  // Add downloaded file path
                        tempFilePaths.add(outputPath)    // Add converted file path
                        cleanupTempFiles()               // Clean up temporary files
                        notifyConversionComplete("button2")
                        ToastUtil.showToast(applicationContext, "File $fileName saved in Download/Converted Files")
                        completeNotification("File saved: $fileName")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        ToastUtil.showToast(applicationContext, "Download failed")
                        notifyConversionComplete("button2")
                        completeNotification("Conversion failed")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Conversion failed: ${e.localizedMessage}")
                    completeNotification("Conversion failed")
                }
            }
        }
    }

    private suspend fun startYoutubeToMp3ConversionNoCoroutine(youtubeUrl: String, folderName: String = "Converted Files") {
        try {
            val downloadPath = downloadYouTubeAudio(applicationContext, youtubeUrl)
            if (downloadPath.isNotEmpty() && File(downloadPath).exists()) {
                val fileNameWithoutExtension = File(downloadPath).nameWithoutExtension.replace("_audio", "")
                val outputPath = "${applicationContext.filesDir.absolutePath}/$fileNameWithoutExtension.mp3"

                // Convert the audio file to MP3 using FFmpeg
                convertAudioToMp3(applicationContext, downloadPath, outputPath)

                val fileName = "$fileNameWithoutExtension.mp3"
                saveToDownloads(applicationContext, outputPath, fileName, "mp3", folderName)
                tempFilePaths.add(downloadPath)  // Add downloaded file path
                tempFilePaths.add(outputPath)    // Add converted file path
                cleanupTempFiles()               // Clean up temporary files
            }
        } catch (e: Exception) {
            // Handle exception
        }
    }

    private suspend fun startYoutubeToMp4ConversionNoCoroutine(
        youtubeUrl: String,
        folderName: String = "Converted Files"
    ) {
        try {
            val outputDir = applicationContext.filesDir.absolutePath
            val python = Python.getInstance()
            val pythonFile = python.getModule("download_youtube")

            // Get the highest available resolution
            val resolutionPy = pythonFile.callAttr("get_highest_resolution", youtubeUrl)
            val resolution = resolutionPy.toString()
            Log.d("PlaylistConversion", "Using resolution: $resolution")

            if (resolution.isEmpty() || resolution == "None") {
                Log.e("PlaylistConversion", "Failed to get resolution for video.")
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Failed to get resolution for video.")
                }
                return
            }

            // Download video without audio
            val videoFilePath = pythonFile.callAttr(
                "download_video_no_audio",
                youtubeUrl,
                resolution,
                outputDir
            ).toString()
            Log.d("PlaylistConversion", "Video file path: $videoFilePath")

            // Download audio separately
            val audioFilePath = pythonFile.callAttr("download_audio", youtubeUrl, outputDir).toString()
            Log.d("PlaylistConversion", "Audio file path: $audioFilePath")

            if (videoFilePath.isNotEmpty() && audioFilePath.isNotEmpty()) {
                val resolutionSuffix = "_${resolution}" // This will add the resolution, e.g., "_720p"
                val finalOutputPath = videoFilePath.replace("_video.mp4", "${resolutionSuffix}.mp4")
                Log.d("PlaylistConversion", "Final output path: $finalOutputPath")

                // Merge the video and audio files using FFmpeg
                mergeVideoAndAudio(applicationContext, videoFilePath, audioFilePath, finalOutputPath)
                Log.d("PlaylistConversion", "Merged video and audio")

                // Delete the temporary video and audio files after merging
                File(videoFilePath).delete()
                File(audioFilePath).delete()

                // Save the final merged file to Downloads/Converted Files
                val fileName = File(finalOutputPath).name
                saveToDownloads(applicationContext, finalOutputPath, fileName, "mp4", folderName)
                Log.d("PlaylistConversion", "Saved final file to Downloads/$folderName")
                tempFilePaths.add(finalOutputPath)
                cleanupTempFiles()
            } else {
                Log.e("PlaylistConversion", "Video or audio file path is invalid.")
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Failed to download video or audio.")
                }
            }
        } catch (e: Exception) {
            Log.e("PlaylistConversion", "Failed to process video: ${e.localizedMessage}", e)
            withContext(Dispatchers.Main) {
                ToastUtil.showToast(applicationContext, "Error during conversion: ${e.localizedMessage}")
            }
        }
    }




    private fun startPlaylistToMp3Conversion(playlistUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Conversion has started!")
                }
                val python = Python.getInstance()
                val pythonFile = python.getModule("download_youtube")
                // Extract video links and titles from the playlist
                val videoInfoListPy = pythonFile.callAttr("extract_playlist_links_and_titles", playlistUrl)
                val videoInfoList = videoInfoListPy.asList()
                val videoInfoListKotlin = videoInfoList.map { itemPy ->
                    val itemDict = itemPy.asMap() as Map<*, *>

                    val link = itemDict["link"]?.toString() ?: ""
                    val title = itemDict["title"]?.toString() ?: ""

                    VideoInfo(link, title)
                }

                val uniqueFolderName =
                    "Playlist_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                        Date()
                    )

                // Initialize a StringBuilder to collect the playlist info
                val playlistInfoBuilder = StringBuilder()

                for (videoInfo in videoInfoListKotlin) {
                    val link = videoInfo.link
                    val title = videoInfo.title

                    // Append to the playlist info
                    playlistInfoBuilder.append("$link - $title\n")

                    startYoutubeToMp3ConversionNoCoroutine(link, uniqueFolderName)
                }

                // After processing all videos, save the playlist info to a text file
                val playlistInfoText = playlistInfoBuilder.toString()
                val fileName = "playlist_info.txt"

                saveTextToDownloads(applicationContext, playlistInfoText, fileName, "text/plain", uniqueFolderName)

                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Playlist conversion completed")
                    notifyConversionComplete("button3")
                    ToastUtil.showToast(applicationContext,"Playlist files saved in Download/$uniqueFolderName")
                    completeNotification("Playlist saved in Download/$uniqueFolderName")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Failed to process playlist: ${e.localizedMessage}")
                    completeNotification("Failed to process playlist")
                }
            }
        }
    }

    private fun startPlaylistToMp4Conversion(playlistUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Conversion has started!")
                }
                val python = Python.getInstance()
                val pythonFile = python.getModule("download_youtube")
                // Extract video links and titles from the playlist
                val videoInfoListPy = pythonFile.callAttr("extract_playlist_links_and_titles", playlistUrl)
                val videoInfoList = videoInfoListPy.asList()
                // Properly cast the map to avoid type inference issues
                val videoInfoListKotlin = videoInfoList.map { itemPy ->
                    val itemDict = itemPy.asMap() as Map<*, *>
                    val link = itemDict["link"]?.toString() ?: ""
                    val title = itemDict["title"]?.toString() ?: ""
                    VideoInfo(link, title)
                }

                val uniqueFolderName =
                    "Playlist_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                        Date()
                    )

                // Initialize a StringBuilder to collect the playlist info
                val playlistInfoBuilder = StringBuilder()

                for (videoInfo in videoInfoListKotlin) {
                    val link = videoInfo.link
                    val title = videoInfo.title

                    // Append to the playlist info
                    playlistInfoBuilder.append("$link - $title\n")

                    startYoutubeToMp4ConversionNoCoroutine(link, uniqueFolderName)
                }

                // After processing all videos, save the playlist info to a text file
                val playlistInfoText = playlistInfoBuilder.toString()
                val fileName = "playlist_info.txt"

                saveTextToDownloads(applicationContext, playlistInfoText, fileName, "text/plain", uniqueFolderName)

                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Playlist conversion completed")
                    notifyConversionComplete("button3")
                    ToastUtil.showToast(applicationContext, "Playlist files saved in Download/$uniqueFolderName")
                    completeNotification("Playlist saved in Download/$uniqueFolderName")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Failed to process playlist: ${e.localizedMessage}")
                    completeNotification("Failed to process playlist")
                }
            }
        }
    }

    private fun saveTextToDownloads(context: Context, textContent: String, fileName: String, mimeType: String, folderName: String = "Converted Files") {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}${File.separator}$folderName"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri).use { outputStream ->
                    outputStream?.bufferedWriter().use { writer ->
                        writer?.write(textContent)
                    }
                }
                // Optionally, show a toast message or log
                // Toast.makeText(context, "Playlist info saved as $fileName in $relativePath", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                // Handle the IOException
                // Toast.makeText(context, "Error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Handle the error case when uri is null
            // Toast.makeText(context, "Failed to save file", Toast.LENGTH_LONG).show()
        }
    }


    private fun startYouTubeToMp4Conversion(youtubeUrl: String, resolution: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                ToastUtil.showToast(applicationContext, "Conversion has started!")
            }
            val python = Python.getInstance()
            val pythonFile = python.getModule("download_youtube")
            val outputDir = filesDir.absolutePath

            try {
                // Download video without audio
                val videoFilePath = pythonFile.callAttr("download_video_no_audio", youtubeUrl, resolution, outputDir).toString()

                // Download audio separately
                val audioFilePath = pythonFile.callAttr("download_video_audio", youtubeUrl, outputDir).toString()

                // If both video and audio are downloaded successfully
                if (videoFilePath.endsWith("_video.mp4") && audioFilePath.contains("_audio")) {
                    val resolutionSuffix = "_${resolution}" // This will add the resolution, e.g., "_720p"
                    val finalOutputPath = videoFilePath.replace("_video.mp4", "${resolutionSuffix}.mp4")

                    // Merge the video and audio files using FFmpeg
                    mergeVideoAndAudio(applicationContext, videoFilePath, audioFilePath, finalOutputPath)

                    // Delete the temporary video and audio files after merging
                    File(videoFilePath).delete()
                    File(audioFilePath).delete()

                    // Save the final merged file to Downloads/Converted Files
                    withContext(Dispatchers.Main) {
                        val fileName = File(finalOutputPath).name
                        saveToDownloads(applicationContext, finalOutputPath, fileName, "mp4") // Passing "mp4" for the file extension
                        tempFilePaths.add(finalOutputPath)
                        cleanupTempFiles()
                        notifyConversionComplete("button4")
                        ToastUtil.showToast(applicationContext, "Video saved to Downloads/Converted Files")
                        completeNotification("File saved: $fileName")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        ToastUtil.showToast(applicationContext, "Failed to download video or audio.")
                        notifyConversionComplete("button4")
                        stopForeground(true)
                        stopSelf()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showToast(applicationContext, "Failed to convert video: ${e.localizedMessage}")
                    notifyConversionComplete("button4")
                    completeNotification("Conversion failed")
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
    }





    private fun cleanupTempFiles() {
        for (path in tempFilePaths) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        tempFilePaths.clear() // Clear the list after cleanup
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name =
                    getString(R.string.channel_name) // Define this string in your strings.xml
                val descriptionText =
                    getString(R.string.channel_description) // Define this string in your strings.xml
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, name, importance).apply {
                    description = descriptionText
                }
                // Get the NotificationManager from the system
                val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // Create the notification channel
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            ToastUtil.showToast(applicationContext, "Error creating notification channel: ${e.message}")
        }
    }

    private fun updateNotificationProgress(progress: Int, indeterminate: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    // Log the lack of permission; consider requesting this permission at a suitable point in your app
                    //Log.e("NotificationProgress", "POST_NOTIFICATIONS permission not granted")
                    return
                }
            }
            val notification = createNotification(
                contentText = "Conversion in progress...",
                showProgress = true,
                progress = progress,
                indeterminate = indeterminate
            )
            startForeground(notificationId, notification)
        } catch (e: Exception) {
            //Log.e("NotificationProgress", "Error updating notification progress: ${e.message}")
        }
    }

    private fun completeNotification(contentText: String) {
        try {
            // Create an intent that opens the Downloads folder
            // Intent to open the system's Downloads app
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)

            // Create a PendingIntent from the intent
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build a new notification with a different icon to indicate completion
            val completedNotification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Conversion Completed")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_download_done) // Icon for completed download
                .setContentIntent(pendingIntent)  // Attach the pending intent to the notification
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                // Add any other properties for the completed notification here
                .build()
            stopForeground(false)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(this).notify(notificationId, completedNotification)
            } else {
                // Log or handle the lack of permission here
                ToastUtil.showToast(applicationContext, "POST_NOTIFICATIONS permission not granted")
            }
        } catch (e: Exception) {
            // Handle any exceptions that occur while attempting to post the notification
            ToastUtil.showToast(applicationContext, "Failed to post notification: ${e.localizedMessage}")
            // Consider notifying the user via another mechanism that doesn't require permissions
        }
    }

    private fun createNotification(
        contentText: String,
        showProgress: Boolean,
        progress: Int,
        indeterminate: Boolean
    ): Notification {
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Youtube Converter")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download) // System icon for download in progress
            .setOngoing(showProgress)
            .setAutoCancel(!showProgress)

        if (showProgress) {
            builder.setProgress(100, progress, indeterminate)
        }

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

