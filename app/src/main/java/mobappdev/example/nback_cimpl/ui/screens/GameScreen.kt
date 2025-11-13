package mobappdev.example.nback_cimpl.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.FeedbackState
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    vm: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val gameState by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()
    val nBack by vm.nBack.collectAsState()
    
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                vm.setTextToSpeech(tts)
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }
    
    val buttonScale by animateFloatAsState(
        targetValue = when (gameState.feedbackState) {
            FeedbackState.CORRECT -> 1.2f
            FeedbackState.INCORRECT -> 0.8f
            FeedbackState.NONE -> 1f
        },
        animationSpec = tween(400)
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("N-Back Game (N=$nBack)") },
                navigationIcon = {
                    if (!gameState.isGameRunning) {
                        TextButton(onClick = onNavigateBack) {
                            Text("← Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Score section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Score: $score",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (gameState.isGameRunning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Event ${gameState.currentEventIndex} / ${gameState.totalEvents}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Correct Matches: ${gameState.correctAnswers}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Game display area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (gameState.gameType) {
                    GameType.Visual -> {
                        VisualGrid(eventValue = gameState.eventValue)
                    }
                    GameType.Audio -> {
                        AudioDisplay(
                            eventValue = gameState.eventValue,
                            isRunning = gameState.isGameRunning
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedVisibility(visible = gameState.feedbackState != FeedbackState.NONE) {
                    Text(
                        text = when (gameState.feedbackState) {
                            FeedbackState.CORRECT -> "✓ Correct!"
                            FeedbackState.INCORRECT -> "Try again!"
                            FeedbackState.NONE -> ""
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (gameState.feedbackState) {
                            FeedbackState.CORRECT -> Color(0xFF4CAF50)
                            FeedbackState.INCORRECT -> Color(0xFFFF9800)
                            FeedbackState.NONE -> Color.Transparent
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Button(
                    onClick = { vm.checkMatch() },
                    enabled = gameState.isGameRunning && gameState.currentEventIndex >= nBack,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(buttonScale),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (gameState.feedbackState) {
                            FeedbackState.CORRECT -> Color(0xFF4CAF50)
                            FeedbackState.INCORRECT -> Color(0xFFFF9800)
                            FeedbackState.NONE -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        text = "MATCH",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (gameState.isGameRunning && gameState.currentEventIndex < nBack) {
                    Text(
                        text = "Wait for ${nBack - gameState.currentEventIndex} more events",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Game over popup
            if (!gameState.isGameRunning && gameState.totalEvents > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Game Over!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Score: $score",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun VisualGrid(eventValue: Int) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(0.8f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0..2) {
                    val position = row * 3 + col
                    val isActive = position == eventValue
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun AudioDisplay(eventValue: Int, isRunning: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.sound_on),
            contentDescription = "Audio",
            modifier = Modifier.size(120.dp),
        )
    }
}
