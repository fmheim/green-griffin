package com.felix.greengriffin.board.presentation

import com.felix.greengriffin.board.data.repository.GameStateRepository
import com.felix.greengriffin.board.domain.model.SavedGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/** How long the board has to stand still before the game is written. */
const val SAVE_DEBOUNCE_MILLIS = 250L

/**
 * The only writer of the saved game.
 *
 * Every change of the played game is collected by a single coroutine, so one write always
 * finishes before the next one starts and a stale snapshot can never land on top of a
 * newer one. Changes closer together than [debounceMillis] are conflated into one write,
 * so a stone drop costs one row update instead of four.
 *
 * Because this is the only writer, [persistedGame] is exactly what the row holds, which is
 * what makes the two skips below safe: the game restored on start-up is not written back,
 * and a change that leaves the *saved* part of the state untouched (a hover highlight, an
 * error message) is not written at all.
 *
 * There is deliberately no "delete the row" operation. Clearing a game resets the played
 * state and is written like any other change, so nothing can be resurrected by a write
 * that was already in flight when the player cleared.
 */
class GameStateAutoSaver(
    private val gameStateRepository: GameStateRepository,
    private val debounceMillis: Long = SAVE_DEBOUNCE_MILLIS,
) {

    /**
     * The game the row holds. Only this class writes it, so only this class changes it.
     *
     * Volatile because `debounce` runs its own producer coroutine, so the two `onEach`
     * blocks below are not guaranteed to share a thread once [start] is given a scope
     * that is not confined to one.
     */
    @Volatile
    private var persistedGame: SavedGame? = null

    /** The newest game that has not been written yet, or `null` when nothing is due. */
    @Volatile
    private var pendingGame: SavedGame? = null

    private var job: Job? = null

    /**
     * Starts writing [games] in [scope], which must be the scope that dies with the
     * screen: when it is cancelled the last change is still flushed.
     *
     * [alreadyPersisted] is the game that was restored from the row, or `null` for a game
     * that has never been saved. Passing it keeps the restored state from being written
     * straight back, while a brand new game is written as soon as it is dealt.
     */
    fun start(
        games: Flow<SavedGame>,
        alreadyPersisted: SavedGame?,
        scope: CoroutineScope,
    ): Job {
        check(job == null) { "The auto saver is already running" }
        persistedGame = alreadyPersisted
        pendingGame = null
        return games
            .distinctUntilChanged()
            .onEach { pendingGame = it }
            .debounce(debounceMillis)
            .onEach { persist(it) }
            // `debounce` drops the value it is still holding when the scope is cancelled -
            // hitting back right after placing a stone would otherwise lose that stone.
            .onCompletion { flush() }
            .launchIn(scope)
            .also { job = it }
    }

    private suspend fun persist(game: SavedGame) {
        if (game != persistedGame) {
            gameStateRepository.saveGameState(game)
            persistedGame = game
        }
        // Anything newer that arrived while this write ran stays pending.
        if (pendingGame == game) pendingGame = null
    }

    /** Writes the change the debounce window was still holding, if there is one. */
    private suspend fun flush() {
        val pending = pendingGame ?: return
        // The scope is already cancelled by the time this runs on teardown, so the write
        // has to opt out of cancellation to reach the database at all.
        withContext(NonCancellable) { persist(pending) }
    }
}
