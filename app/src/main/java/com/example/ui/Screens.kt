package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Folder
import com.example.data.SmartCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCardApp(
    viewModel: SmartCardViewModel,
    modifier: Modifier = Modifier
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationError by viewModel.generationError.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddCardSheet by remember { mutableStateOf(false) }
    
    // For editing
    var folderToEdit by remember { mutableStateOf<Folder?>(null) }
    var cardToEdit by remember { mutableStateOf<SmartCard?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to events
    LaunchedEffect(viewModel.eventFlow) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SmartCardViewModel.UiEvent.CardCreatedSuccess -> {
                    showAddCardSheet = false
                    snackbarHostState.showSnackbar("Card populated successfully!")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CharcoalBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (selectedFolderId == null) {
                // Dashboard View
                DashboardScreen(
                    folders = folders,
                    onFolderClick = { id -> viewModel.selectFolder(id) },
                    onAddFolderClick = { showAddFolderDialog = true },
                    onEditFolderClick = { folder -> folderToEdit = folder },
                    onDeleteFolderClick = { folder -> 
                        viewModel.deleteFolder(folder)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted folder: ${folder.name}")
                        }
                    },
                    onAddCardClick = { showAddCardSheet = true }
                )
            } else {
                // Folder Detail View
                val currentFolder = folders.find { it.id == selectedFolderId }
                FolderDetailScreen(
                    folder = currentFolder,
                    cards = cards,
                    onBackClick = { viewModel.selectFolder(null) },
                    onCardToggleComplete = { card -> viewModel.toggleCardCompleted(card) },
                    onCardDelete = { card -> 
                        viewModel.deleteCard(card)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted card: ${card.title}")
                        }
                    },
                    onCardEdit = { card -> cardToEdit = card },
                    onAddCardClick = { showAddCardSheet = true }
                )
            }

            // Dialogs & Sheets
            if (showAddFolderDialog) {
                AddFolderDialog(
                    onDismiss = { showAddFolderDialog = false },
                    onConfirm = { name, colorHex, icon ->
                        viewModel.addFolder(name, colorHex, icon)
                        showAddFolderDialog = false
                    }
                )
            }

            if (folderToEdit != null) {
                EditFolderDialog(
                    folder = folderToEdit!!,
                    onDismiss = { folderToEdit = null },
                    onConfirm = { updatedFolder ->
                        viewModel.updateFolder(updatedFolder)
                        folderToEdit = null
                    }
                )
            }

            if (cardToEdit != null) {
                EditCardDialog(
                    card = cardToEdit!!,
                    onDismiss = { cardToEdit = null },
                    onConfirm = { updatedCard ->
                        viewModel.updateCard(updatedCard)
                        cardToEdit = null
                    }
                )
            }

            if (showAddCardSheet) {
                AddCardSheet(
                    folders = folders,
                    preselectedFolderId = selectedFolderId,
                    isGenerating = isGenerating,
                    error = generationError,
                    onDismiss = {
                        showAddCardSheet = false
                        viewModel.clearGenerationError()
                    },
                    onGenerate = { name, folder ->
                        viewModel.addCardWithAI(name, folder)
                    },
                    onManualCreate = { folderId, title, addr, desc, price, alt ->
                        viewModel.addManualCard(folderId, title, addr, desc, price, alt)
                        showAddCardSheet = false
                    }
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(
    folders: List<Folder>,
    onFolderClick: (Int) -> Unit,
    onAddFolderClick: () -> Unit,
    onEditFolderClick: (Folder) -> Unit,
    onDeleteFolderClick: (Folder) -> Unit,
    onAddCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrightViolet, ElectricBlue)
                        )
                    )
            ) {
                Text(
                    text = "⚡",
                    fontSize = 22.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "SmartCard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "AI-Powered Personal Organizer",
                    fontSize = 12.sp,
                    color = OnDarkTextSecondary
                )
            }
        }

        Text(
            text = "Categories & Folders",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (folders.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                EmptyStateView(
                    iconText = "📂",
                    message = "No folders found.\nCreate folders to organize your smart cards!",
                    actionText = "Add First Folder",
                    onActionClick = onAddFolderClick
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(folders) { folder ->
                    FolderGridItem(
                        folder = folder,
                        onClick = { onFolderClick(folder.id) },
                        onEditClick = { onEditFolderClick(folder) },
                        onDeleteClick = { onDeleteFolderClick(folder) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large AI Generation trigger button (Prominent FAB with electric glow styling)
        Button(
            onClick = onAddCardClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("ai_fab_generate_card")
                .border(
                    BorderStroke(1.dp, Brush.linearGradient(listOf(BrightVioletLight, ElectricBlueLight))),
                    RoundedCornerShape(16.dp)
                )
                .background(
                    Brush.linearGradient(colors = listOf(BrightViolet, ElectricBlue)),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("✨", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Generate AI Smart Card",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Create Folder helper
        OutlinedButton(
            onClick = onAddFolderClick,
            border = BorderStroke(1.dp, BorderColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnDarkTextPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_folder_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Folder")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Custom Folder", fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridItem(
    folder: Folder,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val folderColor = remember(folder.color) {
        try {
            Color(android.graphics.Color.parseColor(folder.color))
        } catch (e: Exception) {
            BrightViolet
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onEditClick
            )
            .testTag("folder_item_${folder.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Folder icon/emoji
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(folderColor.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, folderColor.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                ) {
                    val emoji = when (folder.iconName) {
                        "travel" -> "✈️"
                        "project" -> "💻"
                        "food" -> "🍔"
                        "idea" -> "💡"
                        else -> "📁"
                    }
                    Text(text = emoji, fontSize = 20.sp)
                }

                // Controls row
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Folder",
                            tint = OnDarkTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Folder",
                            tint = CoralWarning.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = folder.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tap to open",
                    fontSize = 11.sp,
                    color = OnDarkTextSecondary
                )
            }
        }
    }
}

// ==========================================
// SCREEN 2: FOLDER DETAIL VIEW
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: Folder?,
    cards: List<SmartCard>,
    onBackClick: () -> Unit,
    onCardToggleComplete: (SmartCard) -> Unit,
    onCardDelete: (SmartCard) -> Unit,
    onCardEdit: (SmartCard) -> Unit,
    onAddCardClick: () -> Unit
) {
    if (folder == null) return

    val folderColor = remember(folder.color) {
        try {
            Color(android.graphics.Color.parseColor(folder.color))
        } catch (e: Exception) {
            BrightViolet
        }
    }

    val emoji = when (folder.iconName) {
        "travel" -> "✈️"
        "project" -> "💻"
        "food" -> "🍔"
        "idea" -> "💡"
        else -> "📁"
    }

    val suggestions = when (folder.iconName) {
        "travel" -> listOf("Eiffel Tower Paris", "Grand Canyon National Park", "Kyoto Temples Tour", "Sydney Opera House")
        "project" -> listOf("Build Android AI App", "Create Backyard Garden Plan", "Draft Sci-Fi Short Story", "Design Custom Desk")
        "food" -> listOf("Best Pasta in Rome", "Joe's Pizza NYC", "Top Tokyo Sushi Restaurant", "Authentic Paris Bakery")
        else -> listOf("Learn Kotlin Language", "Setup Workout Plan", "Brainstorm Product Name", "Daily Meditations")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CharcoalSurface)
                    .border(BorderStroke(1.dp, BorderColor), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Dashboard",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(folderColor.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, folderColor.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = folder.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${cards.size} Smart Cards",
                    fontSize = 11.sp,
                    color = OnDarkTextSecondary
                )
            }
        }

        if (cards.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎴",
                    fontSize = 54.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "This folder is empty",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Use the generator suggestions below or type your own idea using the bottom button!",
                    fontSize = 13.sp,
                    color = OnDarkTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "✨ Instant AI Suggestions:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = folderColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { query ->
                        SuggestionChip(
                            text = query,
                            color = folderColor,
                            onClick = onAddCardClick
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(cards) { card ->
                    SmartCardItem(
                        card = card,
                        folderColor = folderColor,
                        onToggleComplete = { onCardToggleComplete(card) },
                        onEditClick = { onCardEdit(card) },
                        onDeleteClick = { onCardDelete(card) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Action Button style add trigger
        Button(
            onClick = onAddCardClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = folderColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("folder_add_card_fab")
        ) {
            Text("✨ Add AI Card inside Folder", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun SuggestionChip(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = CharcoalSurface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SmartCardItem(
    card: SmartCard,
    folderColor: Color,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (card.isCompleted) CharcoalSurface.copy(alpha = 0.6f) else CharcoalSurface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (card.isCompleted) BorderColor.copy(alpha = 0.4f) else folderColor.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_card_item_${card.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Colored status left bar accent
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (card.isCompleted) OnDarkTextSecondary.copy(alpha = 0.4f) else folderColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Row header (Title & Action Controls)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (card.isCompleted) OnDarkTextSecondary else Color.White,
                        textDecoration = if (card.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Control actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleComplete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (card.isCompleted) EmeraldSuccess else Color.Transparent
                                    )
                                    .border(
                                        BorderStroke(1.dp, if (card.isCompleted) EmeraldSuccess else OnDarkTextSecondary),
                                        CircleShape
                                    )
                            ) {
                                if (card.isCompleted) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Card",
                                tint = OnDarkTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Card",
                                tint = CoralWarning.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location/Address
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Address",
                        tint = folderColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.address,
                        fontSize = 12.sp,
                        color = OnDarkTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = card.description,
                    fontSize = 13.sp,
                    color = if (card.isCompleted) OnDarkTextSecondary.copy(alpha = 0.8f) else OnDarkTextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                Spacer(modifier = Modifier.height(10.dp))

                // Price and Alternatives Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Estimated Prices/Cost",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = folderColor
                        )
                        Text(
                            text = card.estimatedPrices,
                            fontSize = 12.sp,
                            color = OnDarkTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "AI Alternatives / Steps",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlueLight
                        )
                        Text(
                            text = card.alternatives,
                            fontSize = 12.sp,
                            color = OnDarkTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: ADD CARD AI SHEET / MODAL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardSheet(
    folders: List<Folder>,
    preselectedFolderId: Int?,
    isGenerating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onGenerate: (String, Folder) -> Unit,
    onManualCreate: (Int, String, String, String, String, String) -> Unit
) {
    var cardName by remember { mutableStateOf("") }
    var selectedFolderIndex by remember {
        mutableStateOf(
            if (preselectedFolderId != null) {
                folders.indexOfFirst { it.id == preselectedFolderId }.coerceAtLeast(0)
            } else 0
        )
    }

    var isManualMode by remember { mutableStateOf(false) }

    // Manual Fields State
    var manualTitle by remember { mutableStateOf("") }
    var manualAddress by remember { mutableStateOf("") }
    var manualDescription by remember { mutableStateOf("") }
    var manualPrices by remember { mutableStateOf("") }
    var manualAlternatives by remember { mutableStateOf("") }

    val activeFolder = folders.getOrNull(selectedFolderIndex)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = if (isManualMode) "✍️ Create Card Manually" else "✨ Create Smart Card",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isGenerating) {
                    GeneratingLoadingView()
                } else {
                    if (folders.isEmpty()) {
                        Text(
                            text = "Please create a folder in the dashboard first before generating cards!",
                            color = CoralWarning,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else if (activeFolder != null) {
                        // Category Chip Selector
                        Text(
                            text = "Select Folder Target:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnDarkTextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            items(folders.size) { index ->
                                val f = folders[index]
                                val fColor = remember(f.color) {
                                    try { Color(android.graphics.Color.parseColor(f.color)) } catch (e: Exception) { BrightViolet }
                                }
                                val isSelected = index == selectedFolderIndex
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFolderIndex = index },
                                    label = { Text(f.name, color = Color.White) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = fColor,
                                        containerColor = CharcoalSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) fColor else BorderColor,
                                        selectedBorderColor = fColor,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }

                        // Error Display
                        if (error != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CoralWarning.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, CoralWarning.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = CoralWarning)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        fontSize = 12.sp,
                                        color = OnDarkTextPrimary
                                    )
                                }
                            }
                        }

                        if (!isManualMode) {
                            // AI Mode
                            Text(
                                text = "Name of Place, Restaurant or Idea:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnDarkTextSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = cardName,
                                onValueChange = { cardName = it },
                                placeholder = { Text("e.g. Eiffel Tower, Learn French...", color = OnDarkTextSecondary) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .testTag("ai_card_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrightViolet,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            // Generate Button
                            Button(
                                onClick = { onGenerate(cardName, activeFolder) },
                                enabled = cardName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = CharcoalSurfaceVariant
                                ),
                                contentPadding = PaddingValues(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("ai_generate_confirm_button")
                                    .background(
                                        if (cardName.isNotBlank()) Brush.linearGradient(
                                            listOf(
                                                BrightViolet,
                                                ElectricBlue
                                            )
                                        )
                                        else Brush.linearGradient(
                                            listOf(
                                                CharcoalSurfaceVariant,
                                                CharcoalSurfaceVariant
                                            )
                                        ),
                                        RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Text("✨ Fetch & Generate with AI", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Switch to manual trigger
                            TextButton(
                                onClick = { isManualMode = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Or write details manually", color = OnDarkTextSecondary, fontSize = 13.sp)
                            }

                        } else {
                            // Manual Mode Fields
                            OutlinedTextField(
                                value = manualTitle,
                                onValueChange = { manualTitle = it },
                                label = { Text("Title") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = manualAddress,
                                onValueChange = { manualAddress = it },
                                label = { Text("Address / Context Location") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = manualDescription,
                                onValueChange = { manualDescription = it },
                                label = { Text("Description") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = manualPrices,
                                onValueChange = { manualPrices = it },
                                label = { Text("Estimated Prices / Cost") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = manualAlternatives,
                                onValueChange = { manualAlternatives = it },
                                label = { Text("Alternatives / Next Steps") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            // Save Button
                            Button(
                                onClick = {
                                    onManualCreate(
                                        activeFolder.id,
                                        manualTitle,
                                        manualAddress,
                                        manualDescription,
                                        manualPrices,
                                        manualAlternatives
                                    )
                                },
                                enabled = manualTitle.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightViolet),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("manual_save_confirm_button")
                            ) {
                                Text("Save Card", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TextButton(
                                onClick = { isManualMode = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Back to AI Mode", color = ElectricBlueLight, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cancel row
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = OnDarkTextSecondary)
                }
            }
        }
    }
}

@Composable
fun GeneratingLoadingView() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        )
    )

    val progressMessages = listOf(
        "✨ Contacting Gemini-3.5-Flash...",
        "🔍 Accessing search grounding nodes...",
        "📍 Pinpointing coordinates via maps grounding...",
        "💵 Synthesizing ticket rates & budgets...",
        "💡 Composing similar travel options...",
        "🎨 Polishing the visual layout cards..."
    )

    var currentMessageIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            currentMessageIndex = (currentMessageIndex + 1) % progressMessages.size
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            CircularProgressIndicator(
                color = ElectricBlue,
                strokeWidth = 4.dp,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = "✨",
                fontSize = 28.sp,
                modifier = Modifier
                    .background(CharcoalSurface, CircleShape)
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "AI AUTO-COMPLETION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BrightViolet,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = progressMessages[currentMessageIndex],
            fontSize = 14.sp,
            color = OnDarkTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This takes standard network speeds to aggregate live data.",
            fontSize = 11.sp,
            color = OnDarkTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// COMPONENT: ADD FOLDER DIALOG
// ==========================================
@Composable
fun AddFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#8B5CF6") }
    var selectedIcon by remember { mutableStateOf("general") }

    val colors = listOf("#8B5CF6", "#3B82F6", "#EC4899", "#10B981", "#F59E0B", "#F43F5E")
    val icons = listOf("general" to "📁", "travel" to "✈️", "project" to "💻", "food" to "🍔", "idea" to "💡")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text("📁 Create New Folder", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_folder_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pick Accent Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnDarkTextSecondary)
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    BorderStroke(
                                        if (selectedColor == hex) 3.dp else 0.dp,
                                        Color.White
                                    ),
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pick Type & Icon:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnDarkTextSecondary)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    icons.forEach { (iconId, emoji) ->
                        val isSelected = selectedIcon == iconId
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(android.graphics.Color.parseColor(selectedColor)).copy(alpha = 0.2f)
                                    else CharcoalSurfaceVariant
                                )
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color(android.graphics.Color.parseColor(selectedColor)) else BorderColor
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = iconId }
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = OnDarkTextSecondary)
                    }
                    Button(
                        onClick = { onConfirm(name, selectedColor, selectedIcon) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrightViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_folder_confirm")
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: EDIT FOLDER DIALOG
// ==========================================
@Composable
fun EditFolderDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onConfirm: (Folder) -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }
    var selectedColor by remember { mutableStateOf(folder.color) }
    var selectedIcon by remember { mutableStateOf(folder.iconName) }

    val colors = listOf("#8B5CF6", "#3B82F6", "#EC4899", "#10B981", "#F59E0B", "#F43F5E")
    val icons = listOf("general" to "📁", "travel" to "✈️", "project" to "💻", "food" to "🍔", "idea" to "💡")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text("✏️ Edit Folder Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_folder_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pick Accent Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnDarkTextSecondary)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    BorderStroke(
                                        if (selectedColor == hex) 3.dp else 0.dp,
                                        Color.White
                                    ),
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pick Type & Icon:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnDarkTextSecondary)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    icons.forEach { (iconId, emoji) ->
                        val isSelected = selectedIcon == iconId
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(android.graphics.Color.parseColor(selectedColor)).copy(alpha = 0.2f)
                                    else CharcoalSurfaceVariant
                                )
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color(android.graphics.Color.parseColor(selectedColor)) else BorderColor
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = iconId }
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = OnDarkTextSecondary)
                    }
                    Button(
                        onClick = { onConfirm(folder.copy(name = name, color = selectedColor, iconName = selectedIcon)) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrightViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("edit_folder_confirm")
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: EDIT SMART CARD DIALOG
// ==========================================
@Composable
fun EditCardDialog(
    card: SmartCard,
    onDismiss: () -> Unit,
    onConfirm: (SmartCard) -> Unit
) {
    var title by remember { mutableStateOf(card.title) }
    var address by remember { mutableStateOf(card.address) }
    var description by remember { mutableStateOf(card.description) }
    var estimatedPrices by remember { mutableStateOf(card.estimatedPrices) }
    var alternatives by remember { mutableStateOf(card.alternatives) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("✏️ Edit Smart Card Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_card_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Execution Context") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_card_address_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_card_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = estimatedPrices,
                    onValueChange = { estimatedPrices = it },
                    label = { Text("Estimated Prices / Cost") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_card_price_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = alternatives,
                    onValueChange = { alternatives = it },
                    label = { Text("Alternatives / Next Steps") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_card_alt_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = OnDarkTextSecondary)
                    }
                    Button(
                        onClick = {
                            onConfirm(
                                card.copy(
                                    title = title,
                                    address = address,
                                    description = description,
                                    estimatedPrices = estimatedPrices,
                                    alternatives = alternatives
                                )
                            )
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrightViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("edit_card_confirm")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: UNIVERSAL EMPTY STATE VIEW
// ==========================================
@Composable
fun EmptyStateView(
    iconText: String,
    message: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp)
    ) {
        Text(text = iconText, fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 15.sp,
            color = OnDarkTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrightViolet)
            ) {
                Text(actionText, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
