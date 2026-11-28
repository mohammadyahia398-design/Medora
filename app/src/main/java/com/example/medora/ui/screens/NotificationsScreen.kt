package com.example.medora.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.data.AppNotification
import com.example.medora.data.MedoraDatabase
import com.example.medora.ui.components.MedoraOutlinedButton
import com.example.medora.ui.icons.MedoraIcons
import com.example.medora.ui.icons.themed
import com.example.medora.ui.theme.Dimens
import com.example.medora.ui.theme.MedoraPastel
import com.example.medora.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class NotificationsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = MedoraDatabase.get(app).notificationDao()

    val items: StateFlow<List<AppNotification>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unread: StateFlow<Int> = dao.observeUnread()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markRead(id: Long) { viewModelScope.launch { dao.markRead(id) } }
    fun markAllRead() { viewModelScope.launch { dao.markAllRead() } }
    fun clear() { viewModelScope.launch { dao.clear() } }
}

private const val DAY_MS = 86_400_000L

private fun notifDayStart(t: Long): Long = Calendar.getInstance().apply {
    timeInMillis = t
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

// Time only for today; date and time for older lines.
private fun notifTimeLabel(at: Long, todayStart: Long): String {
    val format = if (at >= todayStart) {
        DateFormat.getTimeInstance(DateFormat.SHORT)
    } else {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    }
    return format.format(Date(at))
}

// Notifications center. Same idea as the reference (big title with a count, groups by time,
// an icon badge and a dot for unread), in MEDORA's look: fully round rows, pastel for unread.
@Composable
fun NotificationsScreen(vm: NotificationsViewModel = viewModel()) {
    val items by vm.items.collectAsState()
    val unread by vm.unread.collectAsState()
    val todayStart = remember { notifDayStart(System.currentTimeMillis()) }
    val weekStart = todayStart - 6 * DAY_MS

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text(
                text = stringResource(R.string.b10_notif_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (unread > 0) CountBadge(unread)
        }

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.b10_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxl),
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (unread > 0) {
                    MedoraOutlinedButton(
                        text = stringResource(R.string.b10_mark_read),
                        onClick = { vm.markAllRead() },
                        modifier = Modifier.weight(1f),
                    )
                }
                MedoraOutlinedButton(
                    text = stringResource(R.string.b10_clear),
                    onClick = { vm.clear() },
                    modifier = Modifier.weight(1f),
                )
            }
            NotifGroup(stringResource(R.string.b9_today), items.filter { it.at >= todayStart }, todayStart) { vm.markRead(it) }
            NotifGroup(
                stringResource(R.string.b10_week),
                items.filter { it.at >= weekStart && it.at < todayStart },
                todayStart,
            ) { vm.markRead(it) }
            NotifGroup(stringResource(R.string.b10_earlier), items.filter { it.at < weekStart }, todayStart) { vm.markRead(it) }
        }
    }
}

@Composable
private fun NotifGroup(title: String, list: List<AppNotification>, todayStart: Long, onTap: (Long) -> Unit) {
    if (list.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.xs),
        )
        list.forEach { NotifRow(it, todayStart, onTap) }
    }
}

@Composable
private fun NotifRow(n: AppNotification, todayStart: Long, onTap: (Long) -> Unit) {
    val isReview = n.kind == AppNotification.KIND_REVIEW
    val tone = MedoraPastel.tone(if (isReview) 3 else 2)
    val surface = MaterialTheme.colorScheme.surface
    val text = if (isReview) {
        stringResource(R.string.b10_notif_review, n.value)
    } else {
        stringResource(R.string.b10_notif_task, n.value)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(if (n.read) surface else tone.container)
            .clickable(enabled = !n.read) { onTap(n.id) }
            .padding(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(Dimens.touch).clip(CircleShape).background(if (n.read) tone.container else surface),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                imageVector = (if (isReview) MedoraIcons.Study else MedoraIcons.Planner).themed(),
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconM),
            )
        }
        Spacer(Modifier.width(Spacing.m))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (n.read) FontWeight.Normal else FontWeight.SemiBold,
                color = if (n.read) MaterialTheme.colorScheme.onSurface else tone.content,
            )
            Text(
                text = notifTimeLabel(n.at, todayStart),
                style = MaterialTheme.typography.bodySmall,
                color = if (n.read) MaterialTheme.colorScheme.onSurfaceVariant else tone.content.copy(alpha = 0.7f),
            )
        }
        if (!n.read) {
            Spacer(Modifier.width(Spacing.m))
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

// Small round number (unread count).
@Composable
internal fun CountBadge(count: Int) {
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99" else count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

// Entry row inside Settings: opens the center and shows the unread count.
@Composable
internal fun SettingsNotificationsGroup(onOpen: () -> Unit, vm: NotificationsViewModel = viewModel()) {
    val unread by vm.unread.collectAsState()
    SettingsGroup(title = stringResource(R.string.b10_notif_title)) {
        SettingsRow(
            icon = MedoraIcons.Bell,
            title = stringResource(R.string.b10_notif_open),
            toneIndex = 3,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onOpen),
            trailing = { if (unread > 0) CountBadge(unread) },
        )
    }
}
