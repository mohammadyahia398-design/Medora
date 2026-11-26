package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.ai.AiClient
import com.example.medora.ai.AiSettings
import com.example.medora.ai.NoAiKeyException
import com.example.medora.data.AiQuestion
import com.example.medora.data.LectureRepository
import com.example.medora.data.MedoraDatabase
import com.example.medora.domain.CardSuggester
import kotlinx.coroutines.launch

// Runs the single AI request for a lecture and saves summary, slides and questions.
// Cards are not saved directly: the user picks them first.
class AiViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)
    private val lectures = LectureRepository(db.lectureDao())
    private val questions = db.aiQuestionDao()
    private val client = AiClient(AiSettings(app))

    var busy by mutableStateOf(false)
        private set
    var noKey by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var newCards by mutableStateOf<List<CardSuggester.Suggestion>?>(null)
        private set

    fun generate(lectureId: Long, title: String, content: String, existingFronts: Set<String>) {
        if (busy || content.isBlank()) return
        busy = true
        viewModelScope.launch {
            try {
                val pack = client.generate(title, content)
                lectures.saveAi(lectureId, pack.summary, pack.slidesJson)
                if (pack.questions.isNotEmpty()) {
                    questions.deleteByLecture(lectureId)
                    questions.insertAll(
                        pack.questions.map {
                            AiQuestion(
                                lectureId = lectureId,
                                question = it.question,
                                options = it.options.joinToString("\n"),
                                correctIndex = it.correctIndex,
                                source = it.source,
                            )
                        }
                    )
                }
                newCards = pack.cards.filter { it.front !in existingFronts }
            } catch (e: NoAiKeyException) {
                noKey = true
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                busy = false
            }
        }
    }

    fun clearCards() {
        newCards = null
    }

    fun clearError() {
        error = null
        noKey = false
    }
}
