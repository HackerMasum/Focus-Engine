package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EisenhowerQuadrant
import com.example.data.model.TaskEntity
import com.example.ui.theme.FocusCoral
import com.example.ui.theme.QuadrantImportantNotUrgent
import com.example.ui.theme.QuadrantNotUrgentNotImportant
import com.example.ui.theme.QuadrantUrgentImportant
import com.example.ui.theme.QuadrantUrgentNotImportant
import com.example.ui.viewmodel.ProductivityUiState
import com.example.ui.viewmodel.ProductivityViewModel
import com.example.ui.viewmodel.TaskFilter

@Composable
fun TasksScreen(
    viewModel: ProductivityViewModel,
    uiState: ProductivityUiState,
    onStartFocusOnTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var presetQuadrant by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Top Header with Title and View Switcher (Matrix vs List)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Eisenhower Matrix",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Prioritize by urgency and true importance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // View Switcher (Grid vs List)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            IconButton(
                                onClick = { viewModel.toggleMatrixView(true) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (uiState.isMatrixView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .testTag("view_matrix_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Matrix View",
                                    tint = if (uiState.isMatrixView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleMatrixView(false) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (!uiState.isMatrixView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .testTag("view_list_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewList,
                                    contentDescription = "List View",
                                    tint = if (!uiState.isMatrixView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isMatrixView) {
                // 2x2 Matrix Overview
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Q1 & Q2 Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MatrixQuadrantCard(
                                title = "Do First (Q1)",
                                subtitle = "Urgent & Important",
                                color = QuadrantUrgentImportant,
                                tasks = uiState.tasks.filter { it.quadrant == "Q1" && !it.isCompleted },
                                onAddClick = {
                                    presetQuadrant = "Q1"
                                    showAddTaskDialog = true
                                },
                                onToggleComplete = { viewModel.toggleTaskCompleted(it) },
                                onFocusClick = { onStartFocusOnTask(it) },
                                modifier = Modifier.weight(1f)
                            )
                            MatrixQuadrantCard(
                                title = "Schedule (Q2)",
                                subtitle = "High Impact Growth",
                                color = QuadrantImportantNotUrgent,
                                tasks = uiState.tasks.filter { it.quadrant == "Q2" && !it.isCompleted },
                                onAddClick = {
                                    presetQuadrant = "Q2"
                                    showAddTaskDialog = true
                                },
                                onToggleComplete = { viewModel.toggleTaskCompleted(it) },
                                onFocusClick = { onStartFocusOnTask(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Q3 & Q4 Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MatrixQuadrantCard(
                                title = "Delegate (Q3)",
                                subtitle = "Urgent, Not Vital",
                                color = QuadrantUrgentNotImportant,
                                tasks = uiState.tasks.filter { it.quadrant == "Q3" && !it.isCompleted },
                                onAddClick = {
                                    presetQuadrant = "Q3"
                                    showAddTaskDialog = true
                                },
                                onToggleComplete = { viewModel.toggleTaskCompleted(it) },
                                onFocusClick = { onStartFocusOnTask(it) },
                                modifier = Modifier.weight(1f)
                            )
                            MatrixQuadrantCard(
                                title = "Eliminate (Q4)",
                                subtitle = "Distractions / Backlog",
                                color = QuadrantNotUrgentNotImportant,
                                tasks = uiState.tasks.filter { it.quadrant == "Q4" && !it.isCompleted },
                                onAddClick = {
                                    presetQuadrant = "Q4"
                                    showAddTaskDialog = true
                                },
                                onToggleComplete = { viewModel.toggleTaskCompleted(it) },
                                onFocusClick = { onStartFocusOnTask(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Filter Chips for Detailed List
            item {
                Text(
                    text = if (uiState.isMatrixView) "All Tasks Breakdown" else "Filter Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TaskFilter.entries) { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            // Tasks List
            val filteredTasks = when (uiState.selectedFilter) {
                TaskFilter.ALL -> uiState.tasks
                TaskFilter.Q1 -> uiState.tasks.filter { it.quadrant == "Q1" }
                TaskFilter.Q2 -> uiState.tasks.filter { it.quadrant == "Q2" }
                TaskFilter.Q3 -> uiState.tasks.filter { it.quadrant == "Q3" }
                TaskFilter.Q4 -> uiState.tasks.filter { it.quadrant == "Q4" }
                TaskFilter.COMPLETED -> uiState.tasks.filter { it.isCompleted }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🎉", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tasks found in this view",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap the + button below to create a new task",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                        onFocusNow = { onStartFocusOnTask(task) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
            }
        }

        // Add Task Floating Action Button
        FloatingActionButton(
            onClick = {
                presetQuadrant = null
                showAddTaskDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_task_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            initialQuadrant = presetQuadrant ?: "Q2",
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, quadrant, pomodoros, category ->
                viewModel.addTask(title, desc, quadrant, pomodoros, category)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun MatrixQuadrantCard(
    title: String,
    subtitle: String,
    color: Color,
    tasks: List<TaskEntity>,
    onAddClick: () -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onFocusClick: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${tasks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            // Tasks preview (top 2 tasks)
            if (tasks.isEmpty()) {
                Text(
                    text = "No active tasks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                tasks.take(2).forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onFocusClick(task) }
                        )
                        IconButton(
                            onClick = { onToggleComplete(task) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Add Button inside quadrant
            OutlinedButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Text("+ Add", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onFocusNow: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quadrantColor = when (task.quadrant) {
        "Q1" -> QuadrantUrgentImportant
        "Q2" -> QuadrantImportantNotUrgent
        "Q3" -> QuadrantUrgentNotImportant
        else -> QuadrantNotUrgentNotImportant
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quadrant Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = quadrantColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.quadrant,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = quadrantColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Category Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Pomodoro Progress
                    Text(
                        text = "🍅 ${task.completedPomodoros}/${task.estimatedPomodoros}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (!task.isCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onFocusNow,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("focus_task_button_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Focus Now", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    initialQuadrant: String = "Q2",
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, quadrant: String, pomodoros: Int, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedQuadrant by remember { mutableStateOf(initialQuadrant) }
    var pomodoros by remember { mutableIntStateOf(2) }
    var selectedCategory by remember { mutableStateOf("Work") }

    val categories = listOf("Work", "Deep Work", "Study", "Health", "Personal")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Actionable Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_task_title")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Notes / Definition of Done") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Priority (Eisenhower Quadrant)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Column {
                    EisenhowerQuadrant.entries.forEach { q ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedQuadrant = q.code }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = selectedQuadrant == q.code,
                                onClick = { selectedQuadrant = q.code }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${q.code} • ${q.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = q.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Estimated Pomodoro blocks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimated Focus Blocks (Pomodoros):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (pomodoros > 1) pomodoros-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "$pomodoros 🍅",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = { if (pomodoros < 8) pomodoros++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Categories
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, desc, selectedQuadrant, pomodoros, selectedCategory)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("submit_task_button")
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
