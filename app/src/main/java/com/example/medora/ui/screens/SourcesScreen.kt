package com.example.medora.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medora.R
import com.example.medora.sources.SourceCache
import com.example.medora.sources.SourceItem
import com.example.medora.ui.components.MedoraButton
import com.example.medora.ui.components.MedoraCard
import kotlinx.coroutines.launch
import java.net.URLEncoder

private const val MIN_QUERY_LENGTH = 3

// Search medical papers (PubMed via Europe PMC). Each result shows its source link
// and the date it was accessed (the date of the search).
class SourcesViewModel(app: Application) : AndroidViewModel(app) {
    var query by mutableStateOf("")
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var results by mutableStateOf<List<SourceItem>>(emptyList())
        private set
    var accessed by mutableStateOf("")
        private set
    var searched by mutableStateOf(false)
        private set
    var fromCache by mutableStateOf(false)
        private set

    fun search() {
        val q = query.trim()
        if (q.length < MIN_QUERY_LENGTH || loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val found = SourceCache.search(getApplication<Application>(), q)
                accessed = found.accessed
                fromCache = found.fromCache
                results = found.items
                searched = true
            } catch (e: Exception) {
                results = emptyList()
                error = e.message ?: e.javaClass.simpleName
            } finally {
                loading = false
            }
        }
    }
}

private fun citationOf(item: SourceItem, accessed: String): String {
    val parts = mutableListOf<String>()
    if (item.authors.isNotBlank()) parts += item.authors + "."
    parts += item.title + "."
    if (item.journal.isNotBlank()) parts += item.journal + "."
    if (item.year.isNotBlank()) parts += item.year + "."
    if (item.pmid.isNotBlank()) parts += "PMID: " + item.pmid + "."
    if (item.doi.isNotBlank()) parts += "DOI: " + item.doi + "."
    parts += item.url
    parts += "(Accessed: $accessed)"
    return parts.joinToString(" ")
}

@Composable
fun SourcesScreen(vm: SourcesViewModel = viewModel()) {
    val results = vm.results
    val error = vm.error
    val loading = vm.loading

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.sources_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                label = { Text(stringResource(R.string.sources_query)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            MedoraButton(
                text = stringResource(R.string.sources_search),
                onClick = { vm.search() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { TrustedSitesCard(vm.query.trim()) }
        if (loading) {
            item { CircularProgressIndicator() }
        }
        if (vm.fromCache && results.isNotEmpty() && !loading) {
            item {
                Text(
                    text = stringResource(R.string.sources_cached, vm.accessed),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (error != null) {
            item {
                Text(
                    text = stringResource(R.string.sources_error, error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (vm.searched && results.isEmpty() && !loading && error == null) {
            item {
                Text(
                    text = stringResource(R.string.sources_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(results) { item ->
            SourceCard(item, vm.accessed)
        }
    }
}

@Composable
private fun SourceCard(item: SourceItem, accessed: String) {
    val context = LocalContext.current
    MedoraCard(modifier = Modifier.fillMaxWidth(), spaced = false) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (item.authors.isNotBlank()) {
            Text(text = item.authors, style = MaterialTheme.typography.bodySmall)
        }
        val where = listOf(item.journal, item.year).filter { it.isNotBlank() }.joinToString(" - ")
        if (where.isNotBlank()) {
            Text(text = where, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.sources_by),
            style = MaterialTheme.typography.bodySmall,
        )
        if (item.pmid.isNotBlank()) {
            Text(
                text = stringResource(R.string.sources_pmid, item.pmid),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = stringResource(R.string.sources_accessed, accessed),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.sources_open_failed, Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(stringResource(R.string.sources_open))
            }
            TextButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("citation", citationOf(item, accessed)))
                Toast.makeText(context, R.string.sources_copied, Toast.LENGTH_SHORT).show()
            }) {
                Text(stringResource(R.string.sources_copy))
            }
        }
    }
}

// Official health sites. We only open their own search page in the browser:
// no copying of their content into the app.
private val trustedSites = listOf(
    "WHO" to "https://www.who.int/home/search-results?indexCatalogue=genericsearchindex1&searchQuery=",
    "CDC" to "https://search.cdc.gov/search/?query=",
    "MedlinePlus" to "https://vsearch.nlm.nih.gov/vivisimo/cgi-bin/query-meta?v%3Aproject=medlineplus&query=",
)

@Composable
private fun TrustedSitesCard(query: String) {
    val context = LocalContext.current
    MedoraCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.sources_trusted_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.sources_trusted_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            trustedSites.forEach { (name, base) ->
                TextButton(onClick = {
                    val url = base + URLEncoder.encode(query, "UTF-8")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(context, R.string.sources_open_failed, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(name)
                }
            }
        }
    }
}
