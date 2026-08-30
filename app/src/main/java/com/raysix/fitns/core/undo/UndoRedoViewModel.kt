package com.raysix.fitns.core.undo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UndoRedoViewModel @Inject constructor(
    private val manager: AppUndoRedoManager
) : ViewModel() {
    val state: StateFlow<UndoRedoState> = manager.state

    fun undo() {
        viewModelScope.launch { manager.undo() }
    }

    fun redo() {
        viewModelScope.launch { manager.redo() }
    }

    fun consumeMessage() {
        manager.consumeMessage()
    }
}
