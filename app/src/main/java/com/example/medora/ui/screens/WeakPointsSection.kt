package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.MedoraDatabase
import com.example.medora.ui.components.MedoraCard
import kotlinx.coroutines.launch

class WeakLecture(val lectureId: Long, val title: String, val again: Int, val total: Int)

class WeakCard(
    val front: String,
    val lectureId: Long,
    val lectureTitle: String,
    val again: Int,
    val total: Int,
)

private const val MIN_LOGS_PER_LECTURE = 3
private const val TOP_N = 5

// Weak points come from the review history only (rating 1 = "Again" = forgotten).
// No new database tables: it reads the existing cards, lectures and review logs.
class WeakPointsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MedoraDatabase.get(app)

    var lectures by mutableStateOf<List<WeakLecture>>(emptyList())
        private set
    var cards by mutableStateOf<List<WeakCard>>(emptyList())
        private set

    fun load() {
        viewModelScope.launch {
            val logs = db.statsDao().getAllLogs()
            val allCards = db.cardDao().getAll()
            val titles = db.lectureDao().getAll().associate { it.id to it.title }
            val cardById = allCards.associateBy { it.id }

            cards = logs.groupBy { it.cardId }
                .mapNotNull { (cardId, list) ->
                    val card = cardById[cardId]
                    val again = list.count { it.rating == 1 }
                    if (card == null || again == 0) {
                        null
                    } else {
                        WeakCard(card.front, card.lectureId, titles[card.lectureId].orEmpty(), again, list.size)
                    }
                }
                .sortedWith(
                    compareByDescending<WeakCard> { it.again }
                        .thenByDescending { it.again * 100 / it.total },
                )
                .take(TOP_N)

            lectures = logs
                .mapNotNull { log -> cardById[log.cardId]?.let { it.lectureId to log.rating } }
                .groupBy({ it.first }, { it.second })
                .mapNotNull { (lectureId, ratings) ->
                    val again = ratings.count { it == 1 }
                    if (ratings.size < MIN_LOGS_PER_LECTURE || again == 0) {
                        null
                    } else {
                        WeakLecture(lectureId, titles[lectureId].orEmpty(), again, ratings.size)
                    }
                }
                .sortedByDescending { it.again * 100 / it.total }
                .take(TOP_N)
        }
    }
}

@Composable
fun WeakPointsSection(onOpenLecture: (Long) -> Unit, vm: WeakPointsViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    val weakLectures = vm.lectures
    val weakCards = vm.cards

    MedoraCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.weak_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (weakLectures.isEmpty() && weakCards.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.weak_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (weakLectures.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.weak_lectures),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            weakLectures.forEach { l ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = l.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.weak_forgot, l.again, l.total),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { onOpenLecture(l.lectureId) }) {
                        Text(stringResource(R.string.open_lecture))
                    }
                }
            }
        }
        if (weakCards.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.weak_cards),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            weakCards.forEach { c ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = c.front,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.weak_forgot, c.again, c.total) +
                                if (c.lectureTitle.isNotBlank()) " - " + c.lectureTitle else "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = { onOpenLecture(c.lectureId) }) {
                        Text(stringResource(R.string.open_lecture))
                    }
                }
            }
        }
    }
}
