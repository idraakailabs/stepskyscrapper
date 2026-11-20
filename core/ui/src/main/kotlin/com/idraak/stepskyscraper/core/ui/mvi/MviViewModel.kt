package com.idraak.stepskyscraper.core.ui.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * MVI contract shared by every screen: an immutable [S] exposed as a StateFlow, a single [onIntent]
 * entry point, and one-off [E] effects delivered over a Channel. Reducers are pure functions owned by
 * each feature; this class only holds the plumbing.
 */
abstract class MviViewModel<S, I, E>(initial: S) : ViewModel() {
    private val mutableState = MutableStateFlow(initial)
    val state: StateFlow<S> = mutableState.asStateFlow()

    private val effectChannel = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = effectChannel.receiveAsFlow()

    abstract fun onIntent(intent: I)

    protected val currentState: S get() = mutableState.value

    protected fun reduce(reducer: (S) -> S) = mutableState.update(reducer)

    protected fun emit(effect: E) {
        effectChannel.trySend(effect)
    }
}

/** Loading / content / error wrapper every screen state uses so all three are always handled. */
sealed interface Lce<out T> {
    data object Loading : Lce<Nothing>

    data class Content<T>(val value: T) : Lce<T>

    data class Error(val message: UiText) : Lce<Nothing>
}
