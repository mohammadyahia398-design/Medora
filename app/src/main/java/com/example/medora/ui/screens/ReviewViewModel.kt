package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.data.Card
import com.example.medora.data.CardRepository
import com.example.medora.data.MedoraDatabase
import kotlinx.coroutines.launch

// One review session over all cards. Lives in the ViewModel so rotation does not reset it.
class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CardRepository(MedoraDatabase.get(app).cardDao())

    var loaded by mutableStateOf(false)
        private set
    var deck by mutableStateOf<List<Card>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var revealed by mutableStateOf(false)
        private set
    var knew by mutableIntStateOf(0)
        private set

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            deck = repo.getAll().shuffled()
            index = 0
            knew = 0
            revealed = false
            loaded = true
        }
    }

    fun reveal() {
        revealed = true
    }

    fun answer(knewIt: Boolean) {
        if (knewIt) knew += 1
        revealed = false
        index += 1
    }

    fun restart() {
        loaded = false
        load()
    }
}
