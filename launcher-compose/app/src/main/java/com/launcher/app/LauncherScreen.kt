package com.launcher.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.wrapContentSize
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap,
)

private suspend fun loadApps(context: Context): List<LaunchableApp> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(launcherIntent, 0)
        .asSequence()
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { info ->
            LaunchableApp(
                label = info.loadLabel(pm).toString(),
                packageName = info.activityInfo.packageName,
                icon = info.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
            )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    try {
        if (intent == null) throw ActivityNotFoundException()
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.launch_failed, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<LaunchableApp>?>(null) }
    var reloadCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(reloadCount) { apps = loadApps(context) }

    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }

    val filtered = remember(apps, query) {
        val q = query.trim()
        apps.orEmpty().filter { it.label.contains(q, ignoreCase = true) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.menu_description))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.refresh_apps)) },
                                onClick = {
                                    menuOpen = false
                                    apps = null
                                    reloadCount++
                                },
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { moreOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_description))
                        }
                        DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.system_settings)) },
                                onClick = {
                                    moreOpen = false
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    } catch (e: ActivityNotFoundException) {
                                        Toast.makeText(context, R.string.launch_failed, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_description))
                                }
                            }
                        },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {}

            val current = apps
            when {
                current == null -> Box(Modifier.fillMaxSize().wrapContentSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                current.isEmpty() -> EmptyText(stringResource(R.string.no_apps))
                filtered.isEmpty() -> EmptyText(stringResource(R.string.no_match))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppRow(app = app) {
                            launchApp(context, app.packageName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRow(app: LaunchableApp, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "pressScale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
