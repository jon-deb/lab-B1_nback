package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.*

/**
 * This is the Home screen composable
 *
 * Currently this screen shows the saved highscore
 * It also contains a button which can be used to show that the C-integration works
 * Furthermore it contains two buttons that you can use to start a game
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: GameViewModel,
    onNavigateToGame: () -> Unit = {}
) {
    val highscore by vm.highscore.collectAsState()
    val nBack by vm.nBack.collectAsState()
    val eventInterval by vm.eventInterval.collectAsState()
    val numberOfEvents by vm.numberOfEvents.collectAsState()

    var selectedGameType by remember { mutableStateOf(GameType.Visual) }

    Scaffold(
        topBar = {
            TopAppBar (
                title = {
                    Text(
                        "N-Back Game",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High Score Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "High Score",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$highscore",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider()

            Text(
                text = "Game Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            // Game Type Selection
            GenerateGameTypeCard(
                selectedGameType = selectedGameType,
                onGameTypeSelected = { type ->
                    selectedGameType = type
                    vm.setGameType(type)
                },
                iconResVisual = R.drawable.visual,
                iconResAudio = R.drawable.sound_on
            )

            // N-Back Value Setting
            SettingsSummaryCard(
                nBack = nBack,
                eventInterval = eventInterval,
                numberOfEvents = numberOfEvents
            )

            // Start Game Button
            Button(
                onClick = {
                    vm.setGameType(selectedGameType)
                    vm.startGame()
                    onNavigateToGame()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "START GAME",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    Surface {
        HomeScreen(FakeVM())
    }
}
