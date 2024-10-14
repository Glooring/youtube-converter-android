package com.example.youtubeconverter

import androidx.activity.viewModels
import android.Manifest
import android.R
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import com.arthenica.mobileffmpeg.FFmpeg
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.RadioButton

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.youtubeconverter.ui.theme.MP4ToMP3ConverterTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaquo.python.PyObject

class MainActivity : ComponentActivity() {
    private val channelId = "download_channel" // Unique ID for the notification channel
    // Obtain the ViewModel
    private lateinit var taskCompleteReceiver: BroadcastReceiver
    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 101
        //const val ACTION_CONVERSION_COMPLETE = "com.example.youtubeconverter.ACTION_CONVERSION_COMPLETE"
    }

    private val requestPostNotificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission was granted
            // You can proceed with posting notifications
        } else {
            // Permission was denied
            // Handle the denial appropriately, perhaps by informing the user
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                // You can proceed with posting notifications
            } else {
                // Permission was denied
                // Handle the denial appropriately, perhaps by informing the user
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
                // Request the permission
                requestPostNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        super.onCreate(savedInstanceState)
        val viewModel: MainViewModel by viewModels()
        // Start Chaquopy
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        val python = Python.getInstance()
        val pythonFile = python.getModule("download_youtube")
        setContent {
            MP4ToMP3ConverterTheme {
                // Use the AppNavigation composable here
                AppNavigation(pythonFile)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Directly set the navigation bar color without changing icon/text appearance
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        } else {
            // For older versions, continue setting the navigation bar color
            // No need to adjust systemUiVisibility for icon/text colors
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        }

        // Initialize and register the receiver
        taskCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.youtubeconverter.TASK_COMPLETE") {
                    val taskId = intent.getStringExtra("taskId")
                    when (taskId) {
                        "button1" -> viewModel.setLoadingButtonOne(false)
                        "button2" -> viewModel.setLoadingButtonTwo(false)
                        "button3" -> viewModel.setLoadingButtonThree(false)
                        "button4" -> viewModel.setLoadingButtonFour(false)
                    }
                }
            }
        }
        registerReceiver(taskCompleteReceiver, IntentFilter("com.example.youtubeconverter.TASK_COMPLETE"),
            RECEIVER_EXPORTED
        )
        //val serviceIntent = Intent(this, ForegroundService::class.java)
        //startService(serviceIntent)
    }

    // Unregister the receiver to avoid memory leaks
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(taskCompleteReceiver)
    }


}

@Composable
fun AppNavigation(pythonFile: PyObject) {
    val viewModel: MainViewModel = viewModel() // Obtain ViewModel instance here
    val navController = rememberNavController()
    Surface {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                ConverterUI(
                    viewModel = viewModel,
                    onNavigateToYouTube = { navController.navigate("youtubeToMP3") },
                    onNavigateToPlaylist = { navController.navigate("playlistDownload") },
                    onNavigateToYouTubeToMP4 = { navController.navigate("youtubeToMP4") } // Navigate to the YouTube to MP4 screen
                )
            }
            composable("youtubeToMP3") {
                YoutubeToMP3Screen(viewModel = viewModel, pythonFile, onNavigateBack = { navController.popBackStack() })
            }
            composable("playlistDownload") {
                PlaylistDownloadScreen(viewModel = viewModel, pythonFile, onNavigateBack = { navController.popBackStack() })
            }
            composable("youtubeToMP4") {
                YoutubeToMP4Screen(viewModel = viewModel, pythonFile, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}


@Composable
fun ConverterUI(
    viewModel: MainViewModel,
    onNavigateToYouTube: () -> Unit,
    onNavigateToPlaylist: () -> Unit,
    onNavigateToYouTubeToMP4: () -> Unit // Add this parameter
) {
    //var fileSelected by remember { mutableStateOf(false) } // State to track file selection
    // Access MainViewModel instance directly

    val context = LocalContext.current // Get the current context
    val scope = rememberCoroutineScope() // Create a CoroutineScope*/

    // Remember a launcher for the result
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        // Update isLoading to true right before starting conversion
                        viewModel.setLoadingButtonOne(true)
                        // Log the isLoading value on the main thread
                        withContext(Dispatchers.Main) {
                            viewModel.isLoadingButtonOne.value?.let { isLoading ->
                                //Log.e("ConverterUI", "isLoading after setting to true: $isLoading")
                            }
                        }
                        val inputPath = uriToPath(uri, context) // Convert URI to file path
                        val outputPath = context.filesDir.absolutePath + "/output.mp3"
                        // Start the ForegroundService for conversion
                        startMp4ToMp3ConversionService(context, uri, inputPath, outputPath)
                    } catch (e: Exception) {
                        //Log.e("ConverterUI", "Error during file conversion: ${e.message}", e)
                        // Optionally, update the UI or show a message to the user indicating an error occurred.
                    }
                }
                // Note: Consider running the conversion in a background thread or coroutine
            }
            // Handle the file URI here
            // e.g., display it or prepare for conversion
        }
    )
    // Use Surface to enforce background color
    Surface {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            MainScreenButtons(viewModel = viewModel, filePickerLauncher, onNavigateToYouTube, onNavigateToPlaylist, onNavigateToYouTubeToMP4)
        }
    }
}

// This function is used to start the ForegroundService with the given paths
fun startMp4ToMp3ConversionService(context: Context, uri: Uri, inputPath: String, outputPath: String) {
    val intent = Intent(context, ForegroundService::class.java).apply {
        action = ForegroundService.ACTION_CONVERT_MP4_TO_MP3
        putExtra(ForegroundService.EXTRA_URI, uri as Parcelable)
        putExtra(ForegroundService.EXTRA_INPUT_PATH, inputPath)
        putExtra(ForegroundService.EXTRA_OUTPUT_PATH, outputPath)
    }
    //Log.e("InpuPathValueFromStartService", inputPath)
    context.startForegroundService(intent)
}

// This function is used to start the ForegroundService with the given paths
fun startYouTubeToMP3ConversionService(context: Context, youtubeUrl: String) {
    val intent = Intent(context, ForegroundService::class.java).apply {
        action = ForegroundService.ACTION_CONVERT_YOUTUBE_TO_MP3
        putExtra(ForegroundService.EXTRA_YOUTUBE_URL, youtubeUrl)
    }
    context.startForegroundService(intent)
}

// This function is used to start the ForegroundService for playlist conversion
fun startPlaylistToMp3ConversionService(context: Context, youtubeUrl: String) {
    val intent = Intent(context, ForegroundService::class.java).apply {
        action = ForegroundService.ACTION_CONVERT_PLAYLIST_TO_MP3
        putExtra(ForegroundService.EXTRA_PLAYLIST_URL, youtubeUrl)
    }
    context.startForegroundService(intent)
}

// This function is used to start the ForegroundService for YouTube to MP4 conversion
fun startYouTubeToMP4ConversionService(
    context: Context,
    youtubeUrl: String,
    resolutionChoice: String
) {
    val intent = Intent(context, ForegroundService::class.java).apply {
        action = ForegroundService.ACTION_CONVERT_YOUTUBE_TO_MP4
        putExtra(ForegroundService.EXTRA_YOUTUBE_URL, youtubeUrl)
        putExtra(ForegroundService.EXTRA_RESOLUTION_CHOICE, resolutionChoice)
    }
    context.startForegroundService(intent)
}




@Composable
fun MainScreenButtons(
    viewModel: MainViewModel,
    filePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    onNavigateToYouTube: () -> Unit,
    onNavigateToPlaylist: () -> Unit,
    onNavigateToYouTubeToMP4: () -> Unit // Add this parameter
) {
    // Access MainViewModel instance directly
    // Observe isLoading here
    // Use a Column to stack buttons vertically
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, // Center the Column content horizontally
        verticalArrangement = Arrangement.spacedBy(35.dp), // Add space between the Column children
        modifier = Modifier.padding(16.dp) // Add padding around the Column
    ) {

        // First button
        Button(
            onClick = {
                // Launch the file picker for "video/*" MIME type to select MP4 files
                if (!viewModel.isLoadingButtonOne.value) { // Prevent launching the picker again if already loading
                    filePickerLauncher.launch("video/*")
                }
            },
            colors = buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72),
                disabledContainerColor = Color(0xFFD0BCFF),
                disabledContentColor = Color(0xFF381E72),
            ),
            enabled = !(viewModel.isLoadingButtonOne.value), // Disable button based on conditions
            modifier = Modifier
                .size(width = 270.dp, height = 100.dp)
        ) {
            if (viewModel.isLoadingButtonOne.value) {
                // Show loading indicator within the button
                CircularProgressIndicator(
                    modifier = Modifier.size(43.dp), // Adjust the size as needed
                    color = Color(0xFF381E72), // Explicitly set the color,
                    strokeWidth = 4.5.dp
                )
            } else {
                // Show the button text
                //Log.e("MainScreenButtons", "is showing text on button")
                Text(
                    text = "MP4 To MP3",
                    fontSize = 26.5.sp
                )
            }
        }
        Button(
            onClick = onNavigateToYouTube,
            modifier = Modifier
                .size(width = 270.dp, height = 100.dp)
        ) {
            Text(
                text = "YouTube To MP3",
                fontSize = 26.5.sp
            )
        }
        Button(
            onClick = onNavigateToYouTubeToMP4,
            modifier = Modifier.size(width = 270.dp, height = 100.dp)
        ) {
            Text(
                text = "YouTube To MP4",
                fontSize = 26.5.sp
            )
        }
        Button(
            onClick = onNavigateToPlaylist,
            modifier = Modifier
                .size(width = 270.dp, height = 100.dp)
        ) {
            Text(
                text = "Playlist to MP3/MP4",
                fontSize = 21.5.sp
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeToMP3Screen(viewModel: MainViewModel, pythonFile: PyObject, onNavigateBack: () -> Unit, context: Context = LocalContext.current) {
    val youtubeLink = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube to MP3") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                YoutubeToMp3MainContent(viewModel = viewModel, youtubeLink, scope, pythonFile, context)
            }
        }
    }
}

@Composable
fun YoutubeToMp3MainContent(
    viewModel: MainViewModel,
    youtubeLink: MutableState<String>,
    scope: CoroutineScope,
    pythonFile: PyObject,
    context: Context
) {
    var isChecking by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding() // Add this line for navigation bar padding
            .imePadding() // Add this line for IME padding to push content above the keyboard
            .padding(bottom = 67.dp), // Additional bottom padding to raise the column content
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Insert the YouTube Link", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = youtubeLink.value,
            onValueChange = { youtubeLink.value = it },
            label = { Text("YouTube link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if(viewModel.isLoadingButtonTwo.value || isChecking) return@Button
                isChecking = true
                // Indicate that a check is in progress
                if (youtubeLink.value.isNotEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        var is_link_valid =
                            pythonFile.callAttr("is_youtube_link_valid", youtubeLink.value)
                                .toString()
                        withContext(Dispatchers.Main) {
                            if (is_link_valid == "valid") {
                                // Update isLoading to true right before starting conversion
                                viewModel.setLoadingButtonTwo(true)
                                startYouTubeToMP3ConversionService(context, youtubeLink.value)
                                youtubeLink.value = "" // Reset the YouTube link text field

                            } else {
                                // Handle download failure
                                withContext(Dispatchers.Main) {
                                    if (is_link_valid == "invalid_regex")
                                        ToastUtil.showToast(
                                            context,
                                            "Please enter a valid YouTube link."
                                        )
                                    else
                                        ToastUtil.showToast(context, "Link access failed")
                                }
                            }
                            // Reset checking state to allow new operations
                            isChecking = false
                        }
                    }
                } else {
                    // Handle empty input
                    ToastUtil.showToast(context, "Please enter a YouTube link.")
                    // Don't forget to reset isChecking since the operation did not proceed
                    isChecking = false
                }
            },
            colors = buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72),
                disabledContainerColor = Color(0xFFD0BCFF),
                disabledContentColor = Color(0xFF381E72),
            ),
            enabled = !(viewModel.isLoadingButtonTwo.value || isChecking), // Disable button based on conditions
            modifier = Modifier.size(width = 200.dp, height = 60.dp)
        ) {
            if (viewModel.isLoadingButtonTwo.value) {
                // Show loading indicator within the button
                CircularProgressIndicator(
                    modifier = Modifier.size(35.dp), // Adjust the size as needed
                    color = Color(0xFF381E72), // Explicitly set the color,
                    strokeWidth = 4.3.dp
                )
            } else {
                // Show the button text
                //Log.e("MainScreenButtons", "is showing text on button")
                Text(text = "Convert to MP3", fontSize = 19.sp)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDownloadScreen(
    viewModel: MainViewModel,
    pythonFile: PyObject,
    onNavigateBack: () -> Unit,
    context: Context = LocalContext.current
) {
    val playlistLink = remember { mutableStateOf("") }
    val downloadOption = remember { mutableStateOf("audio") } // "audio" or "video"
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Playlist Downloader") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(bottom = 67.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Enter the YouTube playlist link", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = playlistLink.value,
                        onValueChange = { playlistLink.value = it },
                        label = { Text("Playlist link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Radio buttons for download options
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = downloadOption.value == "audio",
                                onClick = { downloadOption.value = "audio" }
                            )
                            Text(text = "Download audio as .mp3")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = downloadOption.value == "video",
                                onClick = { downloadOption.value = "video" }
                            )
                            Text(text = "Download video at the highest resolution")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (viewModel.isLoadingButtonThree.value) return@Button

                            if (playlistLink.value.isNotEmpty()) {
                                viewModel.setLoadingButtonThree(true)
                                scope.launch(Dispatchers.IO) {
                                    val isLinkValid = pythonFile.callAttr(
                                        "is_youtube_playlist_link_valid",
                                        playlistLink.value
                                    ).toString()

                                    if (isLinkValid == "valid") {
                                        val actionType = downloadOption.value
                                        val intent = Intent(context, ForegroundService::class.java).apply {
                                            action = when (actionType) {
                                                "audio" -> ForegroundService.ACTION_CONVERT_PLAYLIST_TO_MP3
                                                "video" -> ForegroundService.ACTION_CONVERT_PLAYLIST_TO_MP4
                                                else -> null
                                            }
                                            putExtra(ForegroundService.EXTRA_PLAYLIST_URL, playlistLink.value)
                                        }
                                        if (intent.action != null) {
                                            context.startForegroundService(intent)
                                            playlistLink.value = "";
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                ToastUtil.showToast(
                                                    context,
                                                    "Please select a download option."
                                                )
                                            }
                                            viewModel.setLoadingButtonThree(false)
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            ToastUtil.showToast(
                                                context,
                                                "Please enter a valid YouTube playlist link."
                                            )
                                        }
                                        viewModel.setLoadingButtonThree(false)
                                    }
                                }
                            } else {
                                ToastUtil.showToast(context, "Please enter a playlist link.")
                            }
                        },
                        colors = buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72),
                            disabledContainerColor = Color(0xFFD0BCFF),
                            disabledContentColor = Color(0xFF381E72),
                        ),
                        enabled = !viewModel.isLoadingButtonThree.value,
                        modifier = Modifier.size(width = 200.dp, height = 60.dp)
                    ) {
                        if (viewModel.isLoadingButtonThree.value) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(35.dp),
                                color = Color(0xFF381E72),
                                strokeWidth = 4.3.dp
                            )
                        } else {
                            Text(text = "Start Download", fontSize = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistContent(
    viewModel: MainViewModel,
    youtubeLink: MutableState<String>,
    scope: CoroutineScope,
    pythonFile: PyObject,
    context: Context
) {
    var isChecking by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding() // Add this line for navigation bar padding
            .imePadding() // Add this line for IME padding to push content above the keyboard
            .padding(bottom = 67.dp), // Additional bottom padding to raise the column content
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Insert the YouTube Playlist Link", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = youtubeLink.value,
            onValueChange = { youtubeLink.value = it },
            label = { Text("YouTube link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if(viewModel.isLoadingButtonThree.value || isChecking) return@Button
                isChecking = true
                // Indicate that a check is in progress
                if (youtubeLink.value.isNotEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        var is_link_valid =
                            pythonFile.callAttr("is_youtube_playlist_link_valid", youtubeLink.value)
                                .toString()
                        withContext(Dispatchers.Main) {
                            if (is_link_valid == "valid") {
                                // Update isLoading to true right before starting conversion
                                viewModel.setLoadingButtonThree(true)
                                startPlaylistToMp3ConversionService(context, youtubeLink.value)
                                youtubeLink.value = "" // Reset the YouTube link text field

                            } else {
                                // Handle download failure
                                withContext(Dispatchers.Main) {
                                    if (is_link_valid == "invalid_regex")
                                        ToastUtil.showToast(
                                            context,
                                            "Please enter a valid YouTube link."
                                        )
                                    else
                                        ToastUtil.showToast(context, "Link access failed")
                                }
                            }
                            // Reset checking state to allow new operations
                            isChecking = false
                        }
                    }
                } else {
                    // Handle empty input
                    ToastUtil.showToast(context, "Please enter a YouTube link.")
                    // Don't forget to reset isChecking since the operation did not proceed
                    isChecking = false
                }
            },
            colors = buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72),
                disabledContainerColor = Color(0xFFD0BCFF),
                disabledContentColor = Color(0xFF381E72),
            ),
            enabled = !(viewModel.isLoadingButtonThree.value || isChecking), // Disable button based on conditions
            modifier = Modifier.size(width = 200.dp, height = 60.dp)
        ) {
            if (viewModel.isLoadingButtonThree.value) {
                // Show loading indicator within the button
                CircularProgressIndicator(
                    modifier = Modifier.size(35.dp), // Adjust the size as needed
                    color = Color(0xFF381E72), // Explicitly set the color,
                    strokeWidth = 4.3.dp
                )
            } else {
                // Show the button text
                //Log.e("MainScreenButtons", "is showing text on button")
                Text(text = "Convert to MP3", fontSize = 19.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeToMP4Screen(
    viewModel: MainViewModel,
    pythonFile: PyObject,
    onNavigateBack: () -> Unit,
    context: Context = LocalContext.current
) {
    val youtubeLink = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val resolutions = remember { mutableStateOf(listOf<String>()) }
    var isChecking by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }  // State to manage dropdown expansion
    val selectedResolution = remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube to MP4") },
                navigationIcon = {
                    IconButton(onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(bottom = 67.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Hide "Insert the YouTube Link" text and the text box when resolutions are displayed
                    if (resolutions.value.isEmpty()) {
                        Text(text = "Insert the YouTube Link", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = youtubeLink.value,
                        onValueChange = { youtubeLink.value = it },
                        label = { Text("YouTube link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = resolutions.value.isEmpty() // Lock the text box if resolutions are displayed
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Only show "Convert to MP4" button if no resolutions are displayed
                    if (resolutions.value.isEmpty()) {
                        Button(
                            onClick = {
                                if (viewModel.isLoadingButtonFour.value || isChecking) return@Button
                                isChecking = true

                                if (youtubeLink.value.isNotEmpty()) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val isLinkValid = pythonFile.callAttr(
                                                "is_youtube_link_valid",
                                                youtubeLink.value
                                            ).toString()
                                            withContext(Dispatchers.Main) {
                                                if (isLinkValid == "valid") {
                                                    // Fetch available resolutions
                                                    val resolutionList = pythonFile.callAttr(
                                                        "get_available_resolutions",
                                                        youtubeLink.value
                                                    ).asList().map { it.toString() }

                                                    if (resolutionList.isNotEmpty()) {
                                                        resolutions.value = resolutionList
                                                    } else {
                                                        ToastUtil.showToast(
                                                            context,
                                                            "No available resolutions found."
                                                        )
                                                    }
                                                } else {
                                                    if (isLinkValid == "invalid_regex")
                                                        ToastUtil.showToast(
                                                            context,
                                                            "Please enter a valid YouTube link."
                                                        )
                                                    else
                                                        ToastUtil.showToast(
                                                            context,
                                                            "Link access failed"
                                                        )
                                                }
                                                isChecking = false
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                ToastUtil.showToast(
                                                    context,
                                                    "Failed to get resolutions: ${e.localizedMessage}"
                                                )
                                                isChecking = false
                                            }
                                        }
                                    }
                                } else {
                                    ToastUtil.showToast(context, "Please enter a YouTube link.")
                                    isChecking = false
                                }
                            },
                            colors = buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72),
                                disabledContainerColor = Color(0xFFD0BCFF),
                                disabledContentColor = Color(0xFF381E72),
                            ),
                            enabled = !(viewModel.isLoadingButtonFour.value || isChecking), // Disable button based on conditions
                            modifier = Modifier.size(width = 200.dp, height = 60.dp)
                        ) {
                            if (viewModel.isLoadingButtonFour.value) {
                                // Show loading indicator within the button
                                CircularProgressIndicator(
                                    modifier = Modifier.size(35.dp), // Adjust the size as needed
                                    color = Color(0xFF381E72), // Explicitly set the color,
                                    strokeWidth = 4.3.dp
                                )
                            } else {
                                // Show the button text
                                Text(text = "Convert to MP4", fontSize = 19.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (resolutions.value.isNotEmpty()) {
                        Text(text = "Choose Resolution", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Dropdown menu for resolution selection
                        Box {
                            OutlinedTextField(
                                value = selectedResolution.value ?: "Select resolution",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { expanded = !expanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select resolution")
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                resolutions.value.forEach { resolution ->
                                    DropdownMenuItem(onClick = {
                                        selectedResolution.value = resolution
                                        expanded = false
                                    }) {
                                        Text(text = resolution)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Show "Download" button if resolution is selected
                        selectedResolution.value?.let {
                            Button(
                                onClick = {
                                    viewModel.setLoadingButtonFour(true)
                                    startYouTubeToMP4ConversionService(
                                        context,
                                        youtubeLink.value,
                                        it
                                    )
                                    youtubeLink.value = ""
                                    resolutions.value = emptyList()
                                    selectedResolution.value = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Download", fontSize = 16.sp)
                            }
                        }

                        // Add "Cancel" button to clear resolutions, reset YouTube link, and unlock the text box
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                resolutions.value = emptyList()
                                youtubeLink.value = ""
                                selectedResolution.value = null
                            },
                            colors = buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72),
                                disabledContainerColor = Color(0xFFD0BCFF),
                                disabledContentColor = Color(0xFF381E72),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(text = "Cancel", fontSize = 16.sp)
                        }

                    }
                }
            }
        }
    }
}



fun downloadYouTubeAudio(context: Context, youtubeLink: String): String {
    val message = mutableStateOf("")
    // Make sure Python is started
    if (!Python.isStarted()) {
        Python.start(AndroidPlatform(context))
    }
    val python = Python.getInstance()
    val pythonFile = python.getModule("download_youtube")
    // Use the passed context to get the files directory for the output
    val outputDir = context.filesDir.absolutePath
    try {
        val result = pythonFile.callAttr("download_audio", youtubeLink, outputDir)
        message.value = result.toString()
        //Toast.makeText(context, "Audio downloaded to: ${result.toString()}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        //message.value = e.toString()
        message.value = e.toString()
        //throw e // Rethrow the exception if you want to handle it elsewhere (e.g., showing an error message in UI)
    }
    return message.value
}



suspend fun convertMp4ToMp3(context: Context, inputPath: String, outputPath: String) {
    val command = arrayOf("-i", inputPath, "-q:a", "0", "-map", "a", outputPath)
    FFmpeg.execute(command)
}

suspend fun convertAudioToMp3(context: Context, inputPath: String, outputPath: String) {
    val command = arrayOf(
        "-y",
        "-i", inputPath,
        "-vn",
        "-ar", "44100",
        "-ac", "2",
        "-b:a", "192k",
        outputPath
    )
    FFmpeg.execute(command)
}

suspend fun mergeVideoAndAudio(context: Context, videoPath: String, audioPath: String, outputPath: String) {
    val command = arrayOf(
        "-i", videoPath,
        "-i", audioPath,
        "-c:v", "copy",
        "-c:a", "aac",
        outputPath
    )
    FFmpeg.execute(command)
}


fun uriToPath(uri: Uri, context: Context): String {
    val contentResolver = context.contentResolver
    val fileDescriptor = contentResolver.openFileDescriptor(uri, "r", null) ?: return ""

    //val fileName = getFileNameFromUri(uri, context).substringBeforeLast(".")
    // Create a file in your app's private storage directory with a .mp4 extension
    val destinationFile = File(context.filesDir, "selectedFile.mp4")

    FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
        FileOutputStream(destinationFile).use { outputStream ->
            // Copy the contents from the input stream to the output stream
            inputStream.copyTo(outputStream)
        }
    }
    // Return the path to the newly created file in your app's private storage
    return destinationFile.absolutePath
}

fun saveToDownloads(context: Context, outputPath: String, fileName: String, fileExtension: String, folderName: String = "Converted Files") {
    val resolver = context.contentResolver
    val inputUri = Uri.fromFile(File(outputPath))
    //Log.e("inputUriSaveDownloads", inputUri.toString())

    // Obtain the input file's name to use for the output file
    val inputFileCursor = resolver.query(inputUri, null, null, null, null)
    inputFileCursor?.close()

    val outputFileName = fileName?.substringBeforeLast(".") + ".$fileExtension"
    val relativePath = "${Environment.DIRECTORY_DOWNLOADS}${File.separator}$folderName"

    // Determine the correct MIME type based on the file extension
    val mimeType = when (fileExtension.lowercase()) {
        "mp3" -> "audio/mpeg"
        "mp4" -> "video/mp4"
        else -> "application/octet-stream" // Fallback for unknown file types
    }

    // Create a new file in the Downloads directory with the MP3 extension
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
    }

    val outputUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    val file = File(outputPath)

    try {
        outputUri?.let { uri ->
            resolver.openOutputStream(uri).use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    // Copy the content from input stream to output stream
                    inputStream.copyTo(outputStream!!)
                }
                // Show a Toast message that the file save was successful
                //Toast.makeText(context, "File saved as $outputFileName in 'Downloads/Converted Files'", Toast.LENGTH_LONG).show()
                //Log.e("LastUriToPath", uriToPath(uri, context))
                // After saving to downloads, delete the temporary files
                deleteTemporaryFiles(context, outputPath, uriToPath(uri, context))
                // Call this method to show the notification
                //showDownloadNotification(context, outputFileName)
            }
        } ?: run {
            // Handle the error case when uri is null
            //Toast.makeText(context, "Failed to save file", Toast.LENGTH_LONG).show()
        }
    } catch (e: IOException) {
        // Handle the IOException
        //Toast.makeText(context, "Error occurred: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// New function to delete temporary files
fun deleteTemporaryFiles(context: Context, outputPath: String, inputPath: String) {

    val outputMp3File = File(outputPath)
    //Log.e("DeleteFile", outputMp3File.toString())
    if (outputMp3File.exists()) {
        outputMp3File.delete()
    }

    //("deleteUriPath", inputPath)
    val selectedMp4File = File(inputPath)
    //Log.e("DeleteFile", selectedMp4File.toString())
    if (selectedMp4File.exists()) {
        selectedMp4File.delete()
    }
}
// Use the getFileNameFromUri function you have to get the original file name.


fun getFileNameFromUri(uri: Uri, context: Context): String {
    var fileName = "Unknown"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName.substringBeforeLast(".")
}


fun showDownloadNotification(context: Context, outputFileName: String) {
    val notificationId = 1 // Identifier for the notification
    val channelId = "download_channel" // Identifier for the notification channel

    // Intent to open the system's Downloads app
    val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)

    // PendingIntent that will start the intent when the notification is tapped
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Build the notification
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.stat_sys_download_done) // System icon for download done
        .setContentTitle("Conversion Completed")
        .setContentText(outputFileName)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    // Display the notification
    // Display the notification
    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(
                context, // Use context instead of this
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, handle accordingly
            return@with
        }
        notify(notificationId, notification)
    }

}




