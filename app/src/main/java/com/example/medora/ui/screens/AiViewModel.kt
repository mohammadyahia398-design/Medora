package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medora.R
import com.example.medora.ai.AiClient
import com.example.medora.ai.AiFailKind
import com.example.medora.ai.AiFailure
import com.example.medora.ai.AiSettings
import com.example.medora.ai.ProviderGuard
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
    private val client = AiClient(AiSettings(app), ProviderGuard(app))

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
                lectures.saveAi(lectureId, pack.summary, pack.slidesJson, pack.insightsJson)
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
            } catch (e: AiFailure) {
                error = getApplication<Application>().getString(failText(e.kind)) + "\n\n" + e.message.orEmpty()
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                busy = false
            }
        }
    }

    private fun failText(kind: AiFailKind): Int = when (kind) {
        AiFailKind.NO_INTERNET -> R.string.ai_fail_no_internet
        AiFailKind.INVALID_KEY -> R.string.ai_fail_invalid_key
        AiFailKind.RATE_LIMIT -> R.string.ai_fail_rate_limit
        AiFailKind.DAILY_LIMIT -> R.string.ai_fail_daily_limit
        AiFailKind.SERVER -> R.string.ai_fail_server
        AiFailKind.BAD_ANSWER -> R.string.ai_fail_bad_answer
        AiFailKind.OTHER -> R.string.ai_fail_other
    }

    fun clearCards() {
        newCards = null
    }

    fun clearError() {
        error = null
        noKey = false
    }
}
