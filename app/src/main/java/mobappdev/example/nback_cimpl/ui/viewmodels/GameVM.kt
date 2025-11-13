package mobappdev.example.nback_cimpl.ui.viewmodels

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */

interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: StateFlow<Int>
    val eventInterval: StateFlow<Long>
    val numberOfEvents: StateFlow<Int>

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch()
    fun setTextToSpeech(tts: TextToSpeech?)
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
): GameViewModel, ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    private val _nBack = MutableStateFlow(2)
    override val nBack: StateFlow<Int>
        get() = _nBack

    private val _eventInterval = MutableStateFlow(2500L) // 2,5s
    override val eventInterval: StateFlow<Long>
        get() = _eventInterval

    private val _numberOfEvents = MutableStateFlow(10)
    override val numberOfEvents: StateFlow<Int>
        get() = _numberOfEvents

    private var job: Job? = null // coroutine job for the game event
    private val nBackHelper = NBackHelper() // Helper that generate the event array
    private var events = emptyArray<Int>() // Array with all events
    private var currentEventIndex = 0
    private var matchedEvents = mutableSetOf<Int>() // Track which events have been matched
    private var textToSpeech: TextToSpeech? = null

    private val audioLetters = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I")

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun setTextToSpeech(tts: TextToSpeech?) {
        textToSpeech = tts
    }

    override fun startGame() {
        job?.cancel() // Cancel any existing game loop

        // Reset game state
        currentEventIndex = 0
        matchedEvents.clear()
        _score.value = 0

        events = nBackHelper.generateNBackString(
            _numberOfEvents.value, 9, 30, _nBack.value
        ).toList().toTypedArray()

        Log.d("GameVM", "Generated sequence: ${events.contentToString()}")

        _gameState.value = _gameState.value.copy(
            isGameRunning = true,
            eventValue = -1,
            currentEventIndex = 0,
            totalEvents = events.size,
            feedbackState = FeedbackState.NONE
        )

        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame()
                GameType.Visual -> runVisualGame(events)
            }

            endGame()
        }
    }

    override fun checkMatch() {
        if (!_gameState.value.isGameRunning || currentEventIndex < _nBack.value) {
            return
        }

        if (matchedEvents.contains(currentEventIndex)) {
            return
        }

        matchedEvents.add(currentEventIndex)

        val isCorrect = events[currentEventIndex] == events[currentEventIndex - _nBack.value]

        if (isCorrect) {
            _score.value++
            _gameState.value = _gameState.value.copy(
                correctAnswers = _score.value,
                feedbackState = FeedbackState.CORRECT
            )
            Log.d("GameVM", "Correct match! Score: ${_score.value}")
        } else {
            _gameState.value = _gameState.value.copy(
                feedbackState = FeedbackState.INCORRECT
            )
            Log.d("GameVM", "Incorrect match")
        }

        viewModelScope.launch {
            delay(300)
            _gameState.value = _gameState.value.copy(feedbackState = FeedbackState.NONE)
        }
    }

    private suspend fun runVisualGame(events: Array<Int>) {
        for ((index, value) in events.withIndex()) {
            currentEventIndex = index
            _gameState.value = _gameState.value.copy(
                eventValue = value,
                currentEventIndex = index + 1
            )
            delay(_eventInterval.value)
        }

        _gameState.value = _gameState.value.copy(eventValue = -1)
    }

    private suspend fun runAudioGame() {
        for ((index, value) in events.withIndex()) {
            currentEventIndex = index

            val letter = audioLetters[value]
            textToSpeech?.speak(letter, TextToSpeech.QUEUE_FLUSH, null, null)

            _gameState.value = _gameState.value.copy(
                eventValue = value,
                currentEventIndex = index + 1
            )

            delay(_eventInterval.value)
        }

        _gameState.value = _gameState.value.copy(eventValue = -1)
    }

    private fun endGame() {
        _gameState.value = _gameState.value.copy(isGameRunning = false)

        if (_score.value > _highscore.value) {
            viewModelScope.launch {
                userPreferencesRepository.saveHighScore(_score.value)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository)
            }
        }
    }

    init {
        // Code that runs during creation of the vm
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
}

enum class GameType {
    Audio,
    Visual,
}

enum class FeedbackState {
    NONE,
    CORRECT,
    INCORRECT
}

data class GameState(
    val gameType: GameType = GameType.Visual,
    val eventValue: Int = -1,
    val isGameRunning: Boolean = false,
    val currentEventIndex: Int = 0,
    val totalEvents: Int = 0,
    val correctAnswers: Int = 0,
    val feedbackState: FeedbackState = FeedbackState.NONE
)

class FakeVM: GameViewModel {
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val nBack: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val eventInterval: StateFlow<Long>
        get() = MutableStateFlow(2000L).asStateFlow()
    override val numberOfEvents: StateFlow<Int>
        get() = MutableStateFlow(20).asStateFlow()

    override fun setGameType(gameType: GameType) {}
    override fun startGame() {}
    override fun checkMatch() {}
    override fun setTextToSpeech(tts: TextToSpeech?) {}
}