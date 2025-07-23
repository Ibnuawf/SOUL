package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.JournalEntry
import com.example.data.JournalAnswers
import com.example.data.QuestionAnswer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// dark theme palette — all the vibes
val DarkObsidian = Color(0xFF060606)
val ThemeBg = Color(0xFF060606)
val CardBg = Color(0xFF131313)
val SubBg = Color(0xFF191816)
val BorderColor = Color(0xFF2E2920) // Rich golden-bronze border hue
val BorderColorLight = Color(0xFF3B352B)
val GoldAccent = Color(0xFFD4B275) // Refined gold accent
val SoftGold = Color(0xFFE9D8B6)
val MutedText = Color(0xFF9E9585) // Warm muted text color
val AccentRed = Color(0xFFE57373)
val LightText = Color(0xFFF1EDE6) // Off-white ivory for a bookish look

val AmbientGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF050505),
        Color(0xFF14120F)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoulApp(
    viewModel: JournalViewModel = viewModel()
) {
    val navController = rememberNavController()
    val isRollback by viewModel.isRollbackDetected.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmbientGradient)
                .padding(innerPadding)
        ) {
            // ghost watermark in the bg
            Image(
                painter = painterResource(id = R.drawable.img_soul_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.12f)
            )

            if (isRollback) {
                // clock tamper detected — locked down
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .background(AccentRed.copy(alpha = 0.08f), CircleShape)
                            .border(1.dp, AccentRed.copy(alpha = 0.3f), CircleShape)
                            .padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppBad,
                            contentDescription = "Security rollback alert",
                            tint = AccentRed,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = "Device Clock Error",
                        color = LightText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Wait, something is off with your device's clock! To keep your locked entries safe and prevent early peeking, the vault is temporarily locked.\n\nPlease fix your device's date and time settings to restore normal access.",
                        color = MutedText,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Set your clock to the correct time to continue.",
                        color = GoldAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        fadeIn(animationSpec = tween(350)) + slideInHorizontally(animationSpec = tween(350), initialOffsetX = { it / 8 })
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(350)) + slideOutHorizontally(animationSpec = tween(350), targetOffsetX = { -it / 8 })
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(350)) + slideInHorizontally(animationSpec = tween(350), initialOffsetX = { -it / 8 })
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(350)) + slideOutHorizontally(animationSpec = tween(350), targetOffsetX = { it / 8 })
                    }
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                    composable("reflect") {
                        ReflectScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                    composable("review_one_year_ago") {
                        ReviewOneYearAgoScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                    composable("archive") {
                        ArchiveScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: JournalViewModel
) {
    val todayCompleted by viewModel.todayCompleted.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val oneYearAgoEntry by viewModel.oneYearAgoEntry.collectAsStateWithLifecycle()
    val context = LocalContext.current

        // poke for notification + mic perms on 13+
    var hasNotifyPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifyPermission = granted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifyPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.checkStatus()
    }

    val totalEntriesCount = allEntries.size
    val lockedEntriesCount = allEntries.count {
        it.isCompleted && System.currentTimeMillis() < it.unlockTimestamp
    }
    val unlockedEntriesCount = allEntries.count {
        it.isCompleted && System.currentTimeMillis() >= it.unlockTimestamp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // today's date header
        val todayStr = remember {
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        }
        Text(
            text = todayStr,
            color = GoldAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Soul",
            color = LightText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // the big reflection card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.85f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(BorderColor, GoldAccent.copy(alpha = 0.25f), BorderColor)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AnimatedContent(
                targetState = todayCompleted,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                label = "reflectionCardState"
            ) { isCompleted ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(GoldAccent.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, GoldAccent.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Safe Lock",
                                tint = GoldAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Today is Locked & Safe!",
                            color = LightText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "We've locked your entry in a digital time capsule. See you in a year!",
                            color = MutedText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Divider(
                            modifier = Modifier.width(40.dp),
                            color = BorderColor,
                            thickness = 1.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "UNLOCKS ON",
                            color = GoldAccent.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 365 days from now
                        val unlockDateStr = remember {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 365) }
                            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(cal.time)
                        }
                        Text(
                            text = unlockDateStr,
                            color = SoftGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.SansSerif
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(GoldAccent.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, GoldAccent.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (oneYearAgoEntry != null) Icons.Default.Drafts else Icons.Default.LockOpen,
                                contentDescription = "Lock open",
                                tint = GoldAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (oneYearAgoEntry != null) "A Message from Past You!" else "Ready to Journal?",
                            color = LightText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (oneYearAgoEntry != null) {
                                "Exactly one year ago today, you wrote a note to yourself. Open it now to see how much you've grown!"
                            } else {
                                "Take a quick moment to record your day! Your answers will be locked up and ready to read in exactly one year."
                            },
                            color = MutedText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (oneYearAgoEntry != null) {
                                    navController.navigate("review_one_year_ago")
                                } else {
                                    navController.navigate("reflect")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .height(48.dp)
                                .fillMaxWidth(0.85f)
                                .testTag("reflect_button")
                        ) {
                            Text(
                                text = if (oneYearAgoEntry != null) "Unlock & Read!" else "Write Today's Entry",
                                color = ThemeBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // reminder banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SubBg.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(GoldAccent.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Notifications active",
                    tint = GoldAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Daily Reminder",
                        color = LightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "9:00 AM",
                        color = SoftGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(GoldAccent.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, GoldAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "A gentle nudge every 2 hours until you're ready",
                    color = MutedText,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // stat counters row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(CardBg.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(BorderColor, Color.Transparent)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { navController.navigate("archive") }
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GoldAccent.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                    Text(text = "LOCKED", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = lockedEntriesCount.toString(), color = GoldAccent, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                Text(text = "Locked Entries", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(CardBg.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(BorderColor, Color.Transparent)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { navController.navigate("archive") }
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = SoftGold.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                    Text(text = "UNLOCKED", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = unlockedEntriesCount.toString(), color = LightText, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                Text(text = "Unlocked Entries", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // archive button
        OutlinedButton(
            onClick = { navController.navigate("archive") },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(GoldAccent, SoftGold))),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth()
                .testTag("archive_nav_button")
        ) {
            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Archive", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "Open Journal Archive", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp)
        }
    }
}

fun copyUriToTempFile(context: Context, uri: Uri, prefix: String, suffix: String): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile(prefix, suffix, context.cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.use { it.copyTo(outputStream) }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun ReflectScreen(
    navController: NavController,
    viewModel: JournalViewModel
) {
    val activeAnswers by viewModel.activeAnswers.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentAnswer = activeAnswers.answers[currentIndex]
    val question = JOURNAL_QUESTIONS[currentIndex]

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableStateOf(0) }
    val audioRecorderHelper = remember { AudioRecorderHelper(context) }

    var playingAudioPath by remember { mutableStateOf<String?>(null) }
    val reflectionAudioPlayer = remember { AudioPlayerHelper() }
    var activeDecryptedAudioFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            reflectionAudioPlayer.stop()
            activeDecryptedAudioFile?.let { viewModel.deleteDecryptedTempFile(it) }
        }
    }

    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var isAudioDecrypting by remember { mutableStateOf(false) }
    var isMediaProcessing by remember { mutableStateOf(false) }
    var decryptingPath by remember { mutableStateOf<String?>(null) }
    var previewingPhotoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val selectPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isMediaProcessing = true
                val tempFile = withContext(Dispatchers.IO) {
                    copyUriToTempFile(context, it, "photo_upload_", ".jpg")
                }
                if (tempFile != null) {
                    viewModel.addPhoto(currentIndex, tempFile)
                }
                isMediaProcessing = false
            }
        }
    }



    // camera capture launcher
    var photoUriForCapture by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let { file ->
                scope.launch {
                    isMediaProcessing = true
                    viewModel.addPhoto(currentIndex, file)
                    isMediaProcessing = false
                }
            }
        }
    }



    // mic permission gate
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    // tick the recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0
            while (isRecording) {
                delay(1000)
                recordingDurationSec++
            }
        }
    }

    // kill recording if composable goes poof
    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                audioRecorderHelper.stopRecording()
            }
        }
    }

    // lock confirmation flag
    var showLockConfirmDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
        // header row with back + lock
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    if (currentIndex > 0) {
                        viewModel.prevQuestion()
                    } else {
                        navController.popBackStack()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LightText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "QUESTION ${currentIndex + 1} OF 5",
                color = GoldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            val allAnswersFilled = activeAnswers.answers.all { it.text.isNotBlank() }
            IconButton(
                onClick = { showLockConfirmDialog = true },
                enabled = allAnswersFilled
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = if (allAnswersFilled) GoldAccent else MutedText.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5-step progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0 until 5) {
                val isActive = i <= currentIndex
                val barColor by animateColorAsState(
                    targetValue = if (isActive) GoldAccent else Color.White.copy(alpha = 0.08f),
                    animationSpec = tween(400),
                    label = "barColorAnim"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(barColor, RoundedCornerShape(50))
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // question category label
        val categoryLabel = when (currentIndex) {
            0 -> "1. DAILY HIGHLIGHT"
            1 -> "2. CHALLENGES"
            2 -> "3. GOOD VIBES"
            3 -> "4. BRAIN DUMP"
            else -> "5. FUTURE SELF"
        }
        Text(
            text = categoryLabel,
            color = GoldAccent.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // the actual question
        Text(
            text = question,
            color = LightText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 28.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // text input for the answer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            OutlinedTextField(
                value = currentAnswer.text,
                onValueChange = { viewModel.onAnswerTextChange(currentIndex, it) },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("answer_field_$currentIndex"),
                placeholder = {
                    Text(
                        text = "Write your heart out here...",
                        color = MutedText.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    color = LightText,
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LightText,
                    unfocusedTextColor = LightText,
                    focusedContainerColor = CardBg.copy(alpha = 0.4f),
                    unfocusedContainerColor = CardBg.copy(alpha = 0.15f),
                    focusedBorderColor = GoldAccent.copy(alpha = 0.5f),
                    unfocusedBorderColor = BorderColor,
                    cursorColor = GoldAccent
                ),
                shape = RoundedCornerShape(16.dp)
            )

            // char counter in the corner
            Text(
                text = "${currentAnswer.text.length} chars",
                color = MutedText.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // attached media row
        if (currentAnswer.audioPath != null || currentAnswer.photoPaths.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ATTACHED:",
                    color = GoldAccent.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                // audio note pill
                currentAnswer.audioPath?.let { audioPath ->
                    val isPlayingCurrent = playingAudioPath == audioPath
                    Box(
                        modifier = Modifier
                            .background(SubBg, RoundedCornerShape(50))
                            .border(1.dp, BorderColor, RoundedCornerShape(50))
                            .clickable(enabled = !isAudioDecrypting) {
                                if (isPlayingCurrent) {
                                    reflectionAudioPlayer.stop()
                                    playingAudioPath = null
                                    activeDecryptedAudioFile?.let { viewModel.deleteDecryptedTempFile(it) }
                                    activeDecryptedAudioFile = null
                                } else {
                                    scope.launch {
                                        isAudioDecrypting = true
                                        val decryptedFile = viewModel.decryptFileForPlayback(audioPath)
                                        isAudioDecrypting = false
                                        if (decryptedFile != null && decryptedFile.exists()) {
                                            activeDecryptedAudioFile = decryptedFile
                                            playingAudioPath = audioPath
                                            reflectionAudioPlayer.onCompletionListener = {
                                                playingAudioPath = null
                                                viewModel.deleteDecryptedTempFile(decryptedFile)
                                                if (activeDecryptedAudioFile == decryptedFile) {
                                                    activeDecryptedAudioFile = null
                                                }
                                            }
                                            reflectionAudioPlayer.play(decryptedFile) { duration -> }
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAudioDecrypting) {
                                CircularProgressIndicator(
                                    color = GoldAccent,
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(11.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlayingCurrent) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingCurrent) "Pause" else "Play",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAudioDecrypting) "Decrypting..." else if (isPlayingCurrent) "Playing..." else "Voice Note",
                                color = LightText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = AccentRed.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable {
                                        if (isPlayingCurrent) {
                                            reflectionAudioPlayer.stop()
                                            playingAudioPath = null
                                        }
                                        viewModel.deleteAttachment(currentIndex, "AUDIO", audioPath)
                                    }
                            )
                        }
                    }
                }

                // photo attachements
                currentAnswer.photoPaths.forEach { photoPath ->
                    val isDecryptingThis = decryptingPath == photoPath
                    Box(
                        modifier = Modifier
                            .background(SubBg, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = decryptingPath == null) {
                                scope.launch {
                                    decryptingPath = photoPath
                                    val decryptedFile = viewModel.decryptFileForPlayback(photoPath)
                                    if (decryptedFile != null && decryptedFile.exists()) {
                                        val bitmap = BitmapFactory.decodeFile(decryptedFile.absolutePath)
                                        if (bitmap != null) {
                                            previewingPhotoBitmap = bitmap
                                        }
                                        viewModel.deleteDecryptedTempFile(decryptedFile)
                                    }
                                    decryptingPath = null
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDecryptingThis) {
                                CircularProgressIndicator(
                                    color = GoldAccent,
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(11.dp)
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Photo, contentDescription = "Photo", tint = GoldAccent, modifier = Modifier.size(13.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isDecryptingThis) "Decrypting..." else "Photo", color = LightText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = AccentRed.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable { viewModel.deleteAttachment(currentIndex, "PHOTO", photoPath) }
                            )
                        }
                    }
                }


            }
        }

        // recording UI — pulsing bars + timer
        if (isRecording) {
            val recordWave = rememberInfiniteTransition(label = "recordingWave")
            val wave1 by recordWave.animateFloat(initialValue = 0.3f, targetValue = 1.0f, animationSpec = infiniteRepeatable(tween(450, easing = EaseInOutQuad), RepeatMode.Reverse), label = "wave1")
            val wave2 by recordWave.animateFloat(initialValue = 0.9f, targetValue = 0.2f, animationSpec = infiniteRepeatable(tween(350, easing = EaseInOutQuad), RepeatMode.Reverse), label = "wave2")
            val wave3 by recordWave.animateFloat(initialValue = 0.4f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(550, easing = EaseInOutQuad), RepeatMode.Reverse), label = "wave3")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AccentRed.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, AccentRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // red pulsing dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AccentRed, CircleShape)
                    )

                    // animated waveform bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(2.dp, (4.dp + 16.dp * wave1)).background(AccentRed, RoundedCornerShape(50)))
                        Box(modifier = Modifier.size(2.dp, (4.dp + 12.dp * wave2)).background(AccentRed, RoundedCornerShape(50)))
                        Box(modifier = Modifier.size(2.dp, (4.dp + 18.dp * wave3)).background(AccentRed, RoundedCornerShape(50)))
                        Box(modifier = Modifier.size(2.dp, (4.dp + 14.dp * wave1 * wave2)).background(AccentRed, RoundedCornerShape(50)))
                        Box(modifier = Modifier.size(2.dp, (4.dp + 18.dp * wave2)).background(AccentRed, RoundedCornerShape(50)))
                        Box(modifier = Modifier.size(2.dp, (4.dp + 12.dp * wave3)).background(AccentRed, RoundedCornerShape(50)))
                        Box(modifier = Modifier.size(2.dp, (4.dp + 16.dp * wave1)).background(AccentRed, RoundedCornerShape(50)))
                    }

                    Text(
                        text = "RECORDING VOICE NOTE...",
                        color = AccentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = String.format("%02d:%02d", recordingDurationSec / 60, recordingDurationSec % 60),
                    color = LightText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // bottom controls — media + navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // media capture buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // record / stop audio button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isRecording) AccentRed else CardBg,
                            CircleShape
                        )
                        .border(1.dp, if (isRecording) AccentRed else BorderColor, CircleShape)
                        .clickable {
                            if (!hasMicPermission) {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (isRecording) {
                                    isRecording = false
                                    val file = audioRecorderHelper.stopRecording()
                                    file?.let {
                                        scope.launch {
                                            isMediaProcessing = true
                                            viewModel.addAudio(currentIndex, it)
                                            isMediaProcessing = false
                                        }
                                    }
                                } else {
                                    val file = audioRecorderHelper.startRecording()
                                    if (file != null) {
                                        isRecording = true
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice note",
                        tint = if (isRecording) ThemeBg else GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // add photo button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CardBg, CircleShape)
                        .border(1.dp, BorderColor, CircleShape)
                        .clickable {
                            showPhotoSourceDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "Add photo", tint = GoldAccent, modifier = Modifier.size(18.dp))
                }
            }

            // next / lock navigation
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val canProceed = currentAnswer.text.isNotBlank() ||
                        !currentAnswer.audioPath.isNullOrBlank() ||
                        currentAnswer.photoPaths.isNotEmpty()
                if (currentIndex < 4) {
                    Button(
                        onClick = { viewModel.nextQuestion() },
                        enabled = canProceed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBg,
                            disabledContainerColor = CardBg.copy(alpha = 0.4f),
                            contentColor = LightText,
                            disabledContentColor = LightText.copy(alpha = 0.3f)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                listOf(
                                    if (canProceed) GoldAccent.copy(alpha = 0.25f) else GoldAccent.copy(alpha = 0.05f),
                                    if (canProceed) SoftGold.copy(alpha = 0.25f) else SoftGold.copy(alpha = 0.05f)
                                )
                            )
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(text = "Next", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = { showLockConfirmDialog = true },
                        enabled = canProceed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            disabledContainerColor = GoldAccent.copy(alpha = 0.3f),
                            contentColor = ThemeBg,
                            disabledContentColor = ThemeBg.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("lock_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Save answers",
                            tint = if (canProceed) ThemeBg else ThemeBg.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Save & Lock Entry", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (isMediaProcessing) {
            // encrypting media overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(enabled = false) {}, // eat all touches
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(32.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = GoldAccent, strokeWidth = 3.dp)
                        Text(
                            text = "Securing Memory...",
                            color = LightText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Your reflection is wrapped in AES-256-GCM. Locked tight for 365 days.",
                            color = MutedText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    // lock confirmation dialog
    if (showLockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLockConfirmDialog = false },
            title = {
                Text(
                    text = "Lock Today's Entry?",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
            },
            text = {
                Text(
                    text = "Once you lock today's entry, your answers will be encrypted and hidden. You won't be able to edit or read them until exactly one year from today.\n\nAre you ready to lock it up and see it next year?",
                    color = MutedText,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLockConfirmDialog = false
                        viewModel.lockAndSaveToday()
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text(text = "Yes, Lock It!", color = ThemeBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLockConfirmDialog = false }
                ) {
                    Text(text = "Keep Editing", color = SoftGold)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = {
                Text(
                    text = "Add Photo Reflection",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose how you would like to capture or select your reflection's visual image.",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            try {
                                val tempFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
                                tempPhotoFile = tempFile
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                photoUriForCapture = uri
                                takePictureLauncher.launch(uri)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = ThemeBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Capture with Camera", color = ThemeBg, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showPhotoSourceDialog = false
                            selectPhotoLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftGold),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(BorderColor, BorderColorLight)))
                    ) {
                        Icon(imageVector = Icons.Default.Photo, contentDescription = null, tint = SoftGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Upload from Gallery", color = SoftGold, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text(text = "Cancel", color = SoftGold)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        )
    }



    if (previewingPhotoBitmap != null) {
        Dialog(onDismissRequest = { previewingPhotoBitmap = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = previewingPhotoBitmap!!.asImageBitmap(),
                        contentDescription = "Photo Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { previewingPhotoBitmap = null },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text(text = "Close", color = ThemeBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }


}
}

@Composable
fun ReviewOneYearAgoScreen(
    navController: NavController,
    viewModel: JournalViewModel
) {
    val oneYearAgoAnswers by viewModel.oneYearAgoDecryptedAnswers.collectAsStateWithLifecycle()
    val entryOneYearAgo by viewModel.oneYearAgoEntry.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LightText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ONE YEAR AGO TODAY...",
                color = GoldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (entryOneYearAgo != null && oneYearAgoAnswers != null) {
            Text(
                text = viewModel.getFormattedDateWithDay(entryOneYearAgo!!.timestamp),
                color = LightText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // scrollable q&a cards
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(JOURNAL_QUESTIONS.zip(oneYearAgoAnswers!!.answers)) { (question, answer) ->
                    DecryptedAnswerCard(question = question, answer = answer, viewModel = viewModel)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.completeReview()
                    navController.navigate("reflect") {
                        popUpTo("review_one_year_ago") { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .testTag("complete_review_button")
            ) {
                Text(
                    text = "Write Today's Entry",
                    color = ThemeBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldAccent, strokeWidth = 3.dp)
            }
        }
    }
}

@Composable
fun ArchiveScreen(
    navController: NavController,
    viewModel: JournalViewModel
) {
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val lockedEntries = allEntries.filter { it.isCompleted && now < it.unlockTimestamp }
    val unlockedEntries = allEntries.filter { it.isCompleted && now >= it.unlockTimestamp }

    val filteredUnlockedEntries = remember(unlockedEntries, searchQuery) {
        if (searchQuery.isBlank()) {
            unlockedEntries
        } else {
            unlockedEntries.filter { entry ->
                val formattedDate = viewModel.getFormattedDate(entry.timestamp).lowercase()
                formattedDate.contains(searchQuery.lowercase())
            }
        }
    }

    var selectedLockedEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var selectedUnlockedEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var unlockedDecryptedAnswers by remember { mutableStateOf<JournalAnswers?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // top bar with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LightText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "VAULT ARCHIVE",
                color = LightText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // archive tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = GoldAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    height = 3.dp,
                    color = GoldAccent
                )
            },
            divider = {
                Divider(color = BorderColor, thickness = 1.dp)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Locked (${lockedEntries.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Decrypted (${unlockedEntries.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // search bar for unlocked entries
        AnimatedVisibility(
            visible = selectedTab == 1 && unlockedEntries.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search unlocked entries by date...",
                        color = MutedText.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GoldAccent.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textStyle = LocalTextStyle.current.copy(color = LightText, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent.copy(alpha = 0.5f),
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardBg.copy(alpha = 0.4f),
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = GoldAccent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // list render area
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                if (lockedEntries.isEmpty()) {
                    EmptyState(icon = Icons.Default.Lock, text = "No locked entries yet!")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(lockedEntries) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBg.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(BorderColor, Color.Transparent)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedLockedEntry = entry }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = viewModel.getFormattedDate(entry.timestamp),
                                        color = LightText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassEmpty,
                                            contentDescription = null,
                                            tint = GoldAccent.copy(alpha = 0.6f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Unlocks on: ${viewModel.getFormattedDate(entry.unlockTimestamp)}",
                                            color = MutedText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(GoldAccent.copy(alpha = 0.05f), CircleShape)
                                        .border(1.dp, GoldAccent.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (unlockedEntries.isEmpty()) {
                    EmptyState(icon = Icons.Default.CheckCircle, text = "No unlocked entries yet. They unlock after 1 year!")
                } else if (filteredUnlockedEntries.isEmpty()) {
                    EmptyState(icon = Icons.Default.Search, text = "No entries match your search.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredUnlockedEntries) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBg.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(BorderColor, Color.Transparent)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        selectedUnlockedEntry = entry
                                        unlockedDecryptedAnswers = viewModel.decryptEntry(entry)
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = viewModel.getFormattedDate(entry.timestamp),
                                        color = LightText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Unlocked on: ${viewModel.getFormattedDate(entry.unlockTimestamp)}",
                                            color = GoldAccent.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(GoldAccent.copy(alpha = 0.05f), CircleShape)
                                        .border(1.dp, GoldAccent.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = "Unlocked",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // locked entry dialog — fancy combo lock dial
    if (selectedLockedEntry != null) {
        val entry = selectedLockedEntry!!
        val remainingMillis = entry.unlockTimestamp - System.currentTimeMillis()
        val days = remainingMillis / (1000 * 60 * 60 * 24)

        Dialog(onDismissRequest = { selectedLockedEntry = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Entry",
                        tint = GoldAccent,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Entry Locked",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You wrote this entry on ${viewModel.getFormattedDate(entry.timestamp)}.",
                        color = MutedText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // dial ticks + remaining days
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        // tick marks on the dial face
                        Canvas(modifier = Modifier.size(170.dp)) {
                            val radius = size.minDimension / 2f
                            val center = size / 2f
                            val tickCount = 60
                            for (i in 0 until tickCount) {
                                val angleInRad = Math.toRadians((i * 360f / tickCount).toDouble())
                                val isMajor = i % 5 == 0
                                val tickLength = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                                val strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                                val tickColor = if (isMajor) GoldAccent.copy(alpha = 0.5f) else GoldAccent.copy(alpha = 0.15f)
                                
                                val startX = (center.width + (radius - tickLength - 6.dp.toPx()) * Math.cos(angleInRad)).toFloat()
                                val startY = (center.height + (radius - tickLength - 6.dp.toPx()) * Math.sin(angleInRad)).toFloat()
                                val endX = (center.width + (radius - 6.dp.toPx()) * Math.cos(angleInRad)).toFloat()
                                val endY = (center.height + (radius - 6.dp.toPx()) * Math.sin(angleInRad)).toFloat()
                                
                                drawLine(
                                    color = tickColor,
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }

                        // center of the dial — days count
                        Box(
                            modifier = Modifier
                                .size(124.dp)
                                .background(Color(0xFF0A0A0A), CircleShape)
                                .border(1.dp, GoldAccent.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$days",
                                    color = GoldAccent,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = "DAYS LEFT",
                                    color = MutedText,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Unlocks on ${viewModel.getFormattedDate(entry.unlockTimestamp)}",
                        color = SoftGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { selectedLockedEntry = null },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Got it", color = ThemeBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // unlocked entry reader
    if (selectedUnlockedEntry != null && unlockedDecryptedAnswers != null) {
        val entry = selectedUnlockedEntry!!
        val answers = unlockedDecryptedAnswers!!

        // gather entries sharing the same month+day across years
        val md = if (entry.dateString.length >= 10) entry.dateString.substring(5) else ""
        val relatedEntries = if (md.isNotEmpty()) {
            unlockedEntries.filter { it.dateString.endsWith(md) }.sortedBy { it.dateString }
        } else {
            listOf(entry)
        }

        Dialog(onDismissRequest = {
            selectedUnlockedEntry = null
            unlockedDecryptedAnswers = null
            viewModel.cleanupAllTempFiles()
        }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 16.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Memory Unlocked!",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = {
                                selectedUnlockedEntry = null
                                unlockedDecryptedAnswers = null
                                viewModel.cleanupAllTempFiles()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = LightText, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // date header + year switcher arrows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentIndex = relatedEntries.indexOfFirst { it.dateString == entry.dateString }
                        val hasPrev = currentIndex > 0
                        val hasNext = currentIndex >= 0 && currentIndex < relatedEntries.size - 1

                        IconButton(
                            onClick = {
                                if (hasPrev) {
                                    val prev = relatedEntries[currentIndex - 1]
                                    selectedUnlockedEntry = prev
                                    unlockedDecryptedAnswers = viewModel.decryptEntry(prev)
                                }
                            },
                            enabled = hasPrev
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Year",
                                tint = if (hasPrev) GoldAccent else MutedText.copy(alpha = 0.2f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = viewModel.getFormattedDateWithDay(entry.timestamp),
                            color = LightText,
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = {
                                if (hasNext) {
                                    val next = relatedEntries[currentIndex + 1]
                                    selectedUnlockedEntry = next
                                    unlockedDecryptedAnswers = viewModel.decryptEntry(next)
                                }
                            },
                            enabled = hasNext
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Year",
                                tint = if (hasNext) GoldAccent else MutedText.copy(alpha = 0.2f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // same day, different years — pill selector
                    if (relatedEntries.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SAME DAY FROM OTHER YEARS:",
                            color = GoldAccent.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(relatedEntries) { relEntry ->
                                val year = if (relEntry.dateString.length >= 4) relEntry.dateString.substring(0, 4) else ""
                                val isSelected = relEntry.dateString == entry.dateString
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) GoldAccent else SubBg,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) GoldAccent else BorderColor,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedUnlockedEntry = relEntry
                                            unlockedDecryptedAnswers = viewModel.decryptEntry(relEntry)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = year,
                                        color = if (isSelected) ThemeBg else LightText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(JOURNAL_QUESTIONS.zip(answers.answers)) { (question, ans) ->
                            DecryptedAnswerCard(question = question, answer = ans, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DecryptedAnswerCard(
    question: String,
    answer: QuestionAnswer,
    viewModel: JournalViewModel
) {
    var decryptedPhotoBitmaps by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    val context = LocalContext.current

    // decrypt photos to bitmaps in-memory on display
    LaunchedEffect(answer.photoPaths) {
        if (answer.photoPaths.isNotEmpty()) {
            val bitmaps = mutableListOf<android.graphics.Bitmap>()
            answer.photoPaths.forEach { path ->
                val decryptedFile = viewModel.decryptFileForPlayback(path)
                if (decryptedFile != null && decryptedFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(decryptedFile.absolutePath)
                    if (bitmap != null) {
                        bitmaps.add(bitmap)
                    }
                    viewModel.deleteDecryptedTempFile(decryptedFile)
                }
            }
            decryptedPhotoBitmaps = bitmaps
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = question,
                color = GoldAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (answer.text.isNotBlank()) {
                Text(
                    text = answer.text,
                    color = LightText,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            } else {
                Text(
                    text = "(No written response)",
                    color = MutedText.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // voice note player
            answer.audioPath?.let { audioPath ->
                Spacer(modifier = Modifier.height(12.dp))
                AudioPlayerPill(audioPath = audioPath, viewModel = viewModel)
            }

            // polaroid-style photo grid
            if (decryptedPhotoBitmaps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(decryptedPhotoBitmaps) { bitmap ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F4EE)), // Authentic Ivory Polaroid card background
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Decrypted attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(105.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "M E M O R Y",
                                    color = Color(0xFF5E5446), // Literary brown text
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }
                    }
                }
            }


        }
    }
}

@Composable
fun AudioPlayerPill(
    audioPath: String,
    viewModel: JournalViewModel
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isDecrypting by remember { mutableStateOf(false) }
    var playerHelper = remember { AudioPlayerHelper() }
    val scope = rememberCoroutineScope()
    var activeDecryptedFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            playerHelper.stop()
            activeDecryptedFile?.let { viewModel.deleteDecryptedTempFile(it) }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SubBg, RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isDecrypting) {
                if (isPlaying) {
                    playerHelper.stop()
                    isPlaying = false
                    activeDecryptedFile?.let { viewModel.deleteDecryptedTempFile(it) }
                    activeDecryptedFile = null
                } else {
                    scope.launch {
                        isDecrypting = true
                        val decryptedFile = viewModel.decryptFileForPlayback(audioPath)
                        isDecrypting = false
                        if (decryptedFile != null && decryptedFile.exists()) {
                            activeDecryptedFile = decryptedFile
                            isPlaying = true
                            playerHelper.onCompletionListener = {
                                isPlaying = false
                                activeDecryptedFile?.let { viewModel.deleteDecryptedTempFile(it) }
                                activeDecryptedFile = null
                            }
                            playerHelper.play(decryptedFile) { duration -> }
                        }
                    }
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(GoldAccent.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, GoldAccent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isDecrypting) {
                CircularProgressIndicator(
                    color = GoldAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = if (isDecrypting) "Decrypting Voice Note..." else if (isPlaying) "Playing Decrypted Audio..." else "Play Decrypted Audio",
                color = LightText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Voice note decrypted safely",
                color = MutedText,
                fontSize = 10.sp
            )
        }
    }
}



@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp)
        ) {
            // dotted ring in the bg
            Canvas(modifier = Modifier.size(100.dp)) {
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.08f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(10f, 10f), 0f
                        )
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(GoldAccent.copy(alpha = 0.03f), CircleShape)
                    .border(1.5.dp, GoldAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = text,
            color = MutedText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Serif,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
