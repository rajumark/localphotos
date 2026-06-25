package com.localphotos.app.faceprocessing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FaceStats {
    private val _totalProcessed = MutableStateFlow(0)
    val totalProcessed: StateFlow<Int> = _totalProcessed.asStateFlow()

    private val _totalFacesFound = MutableStateFlow(0)
    val totalFacesFound: StateFlow<Int> = _totalFacesFound.asStateFlow()

    private val _totalErrors = MutableStateFlow(0)
    val totalErrors: StateFlow<Int> = _totalErrors.asStateFlow()

    private val _errorMessages = MutableStateFlow<List<String>>(emptyList())
    val errorMessages: StateFlow<List<String>> = _errorMessages.asStateFlow()

    fun recordSuccess(faceCount: Int) {
        _totalProcessed.value++
        _totalFacesFound.value += faceCount
    }

    fun recordError(uri: String, message: String) {
        _totalProcessed.value++
        _totalErrors.value++
        _errorMessages.value = _errorMessages.value + "[$uri] $message"
    }

    fun formatSummary(): String {
        val lines = mutableListOf(
            "Face Processing Stats",
            "=====================",
            "Images processed: ${_totalProcessed.value}",
            "Total faces found: ${_totalFacesFound.value}",
            "Total errors: ${_totalErrors.value}",
            ""
        )
        if (_errorMessages.value.isNotEmpty()) {
            lines.add("Errors:")
            _errorMessages.value.forEach { lines.add("  $it") }
        }
        return lines.joinToString("\n")
    }
}
