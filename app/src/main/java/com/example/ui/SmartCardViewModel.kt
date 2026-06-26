package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.Folder
import com.example.data.SmartCard
import com.example.data.SmartCardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SmartCardViewModel(private val repository: SmartCardRepository) : ViewModel() {

    // Folders list StateFlow
    val folders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected folder ID
    private val _selectedFolderId = MutableStateFlow<Int?>(null)
    val selectedFolderId: StateFlow<Int?> = _selectedFolderId.asStateFlow()

    // Cards list for selected folder StateFlow
    val cards: StateFlow<List<SmartCard>> = _selectedFolderId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getCardsForFolder(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // AI Generation States
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    // Shared flow to emit events (like closing bottom sheets on success)
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    sealed interface UiEvent {
        object CardCreatedSuccess : UiEvent
    }

    init {
        // Ensure default folders exist in database
        viewModelScope.launch {
            repository.ensureDefaultFoldersExist()
        }
    }

    fun selectFolder(folderId: Int?) {
        _selectedFolderId.value = folderId
    }

    // Folder Operations
    fun addFolder(name: String, color: String, iconName: String) {
        viewModelScope.launch {
            repository.insertFolder(Folder(name = name, color = color, iconName = iconName))
        }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            repository.updateFolder(folder)
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            // If the deleted folder was selected, clear selection
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
        }
    }

    // Card Operations
    fun addCardWithAI(name: String, folder: Folder) {
        if (name.isBlank()) return
        
        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            
            val result = GeminiClient.generateCardDetails(name, folder.name)
            
            result.fold(
                onSuccess = { details ->
                    val newCard = SmartCard(
                        folderId = folder.id,
                        title = details.title,
                        address = details.address,
                        description = details.description,
                        estimatedPrices = details.estimatedPrices,
                        alternatives = details.alternatives
                    )
                    repository.insertCard(newCard)
                    _isGenerating.value = false
                    _eventFlow.emit(UiEvent.CardCreatedSuccess)
                },
                onFailure = { error ->
                    _isGenerating.value = false
                    _generationError.value = error.message ?: "An unexpected error occurred."
                }
            )
        }
    }

    fun addManualCard(
        folderId: Int,
        title: String,
        address: String,
        description: String,
        estimatedPrices: String,
        alternatives: String
    ) {
        viewModelScope.launch {
            val newCard = SmartCard(
                folderId = folderId,
                title = title,
                address = address,
                description = description,
                estimatedPrices = estimatedPrices,
                alternatives = alternatives
            )
            repository.insertCard(newCard)
            _eventFlow.emit(UiEvent.CardCreatedSuccess)
        }
    }

    fun updateCard(card: SmartCard) {
        viewModelScope.launch {
            repository.updateCard(card)
        }
    }

    fun deleteCard(card: SmartCard) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    fun toggleCardCompleted(card: SmartCard) {
        viewModelScope.launch {
            repository.updateCard(card.copy(isCompleted = !card.isCompleted))
        }
    }

    fun clearGenerationError() {
        _generationError.value = null
    }
}

class SmartCardViewModelFactory(private val repository: SmartCardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmartCardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SmartCardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
