package com.raysix.fitns.core.undo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class UndoRedoState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoLabel: String? = null,
    val redoLabel: String? = null,
    val busy: Boolean = false,
    val message: String? = null
)

class UndoRedoAction(
    val label: String,
    val undo: suspend () -> Unit,
    val redo: suspend () -> Unit
)

@Singleton
class AppUndoRedoManager @Inject constructor() {
    private val undoStack = ArrayDeque<UndoRedoAction>()
    private val redoStack = ArrayDeque<UndoRedoAction>()
    private val _state = MutableStateFlow(UndoRedoState())

    val state: StateFlow<UndoRedoState> = _state.asStateFlow()
    var isApplying: Boolean = false
        private set

    fun record(action: UndoRedoAction) {
        if (isApplying) return
        undoStack.addLast(action)
        redoStack.clear()
        publish(message = "${action.label} can be undone.")
    }

    suspend fun undo() {
        val action = undoStack.removeLastOrNull() ?: return
        runAction(action, undoing = true)
    }

    suspend fun redo() {
        val action = redoStack.removeLastOrNull() ?: return
        runAction(action, undoing = false)
    }

    fun clear(message: String? = null) {
        undoStack.clear()
        redoStack.clear()
        publish(message = message)
    }

    fun consumeMessage() {
        publish(message = null)
    }

    private suspend fun runAction(action: UndoRedoAction, undoing: Boolean) {
        isApplying = true
        publish(busy = true, message = null)
        try {
            if (undoing) {
                action.undo()
                redoStack.addLast(action)
                publish(message = "Undid ${action.label}.")
            } else {
                action.redo()
                undoStack.addLast(action)
                publish(message = "Redid ${action.label}.")
            }
        } finally {
            isApplying = false
            publish(busy = false)
        }
    }

    private fun publish(
        busy: Boolean = _state.value.busy,
        message: String? = _state.value.message
    ) {
        _state.value = UndoRedoState(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            undoLabel = undoStack.lastOrNull()?.label,
            redoLabel = redoStack.lastOrNull()?.label,
            busy = busy,
            message = message
        )
    }
}
