package com.campusswap.app.screens.sell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.CampusSwapChip
import com.campusswap.app.components.ConditionBadge
import com.campusswap.app.components.ProductPlaceholderImage
import com.campusswap.app.components.formatPrice
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.components.OfflineBanner
import com.campusswap.app.data.Product
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import com.campusswap.app.components.NoClipboard
import com.campusswap.app.components.PriceVisualTransformation
import com.campusswap.app.domain.InputLimits
import com.campusswap.app.domain.InputValidation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.Category
import com.campusswap.app.data.Condition
import com.campusswap.app.data.Course
import com.campusswap.app.data.SampleData
import com.campusswap.app.data.SellDraft
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.campusswap.app.components.CampusHeader
import com.campusswap.app.components.CampusIconButton
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.HeadingText
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.SecondaryBlue
import com.campusswap.app.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SellStep(val title: String) {
    PRODUCT_INFO("Photos & Info"),
    COURSE_CONDITION("Course & Condition"),
    REVIEW("Review"),
    CONFIRMATION("Done"),
}

@Composable
fun SellScreen(
    vm: AppViewModel,
    onExit: () -> Unit,
    onPublished: (String) -> Unit,
) {
    var step by remember { mutableStateOf(SellStep.PRODUCT_INFO) }
    var draft by remember { mutableStateOf(SellDraft()) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var publishedProduct by remember { mutableStateOf<Product?>(null) }
    var draftRestored by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val draftStore = (LocalContext.current.applicationContext as CampusSwapApplication).container.draftStore

    val isDirty = draft.isDirty

    // Local storage: the draft lives in DataStore, so leaving the screen, closing the app or
    // losing signal mid-form never loses what the seller typed.
    LaunchedEffect(Unit) {
        val saved = draftStore.read()
        if (saved != null && saved.isDirty && !draft.isDirty) {
            draft = saved
            draftRestored = true
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { draft }.drop(1).collectLatest { current ->
            delay(400) // write once the user pauses typing, not on every key
            if (current.isDirty) draftStore.save(current) else draftStore.clear()
        }
    }

    fun requestExit() {
        if (isDirty) showDiscardDialog = true else onExit()
    }

    BackHandler(enabled = true) {
        when (step) {
            SellStep.PRODUCT_INFO -> requestExit()
            SellStep.COURSE_CONDITION -> step = SellStep.PRODUCT_INFO
            SellStep.REVIEW -> step = SellStep.COURSE_CONDITION
            SellStep.CONFIRMATION -> onExit()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CampusSwapTheme.colors.bg)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        CampusHeader {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CampusIconButton(
                    icon = CampusIcons.Close,
                    contentDescription = "Close",
                    onClick = { if (step == SellStep.CONFIRMATION) onExit() else requestExit() },
                    iconSize = 18.dp,
                )
                Icon(CampusIcons.Tag, contentDescription = null, tint = CampusSwapTheme.colors.accentHi, modifier = Modifier.size(16.dp))
                HeadingText(
                    text = if (step == SellStep.CONFIRMATION) "Listing published" else "List New Item",
                    size = CampusType.sizeMd,
                )
            }
        }

        if (step != SellStep.CONFIRMATION) {
            StepIndicator(step = step, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                SellStep.PRODUCT_INFO -> ProductInfoStep(
                    draft = draft,
                    draftRestored = draftRestored,
                    isOnline = vm.isOnline,
                    onDraftChange = { draft = it },
                    onContinue = { step = SellStep.COURSE_CONDITION },
                )
                SellStep.COURSE_CONDITION -> CourseConditionStep(
                    draft = draft,
                    onDraftChange = { draft = it },
                    onBack = { step = SellStep.PRODUCT_INFO },
                    onContinue = { step = SellStep.REVIEW },
                )
                SellStep.REVIEW -> ReviewStep(
                    draft = draft,
                    isPublishing = isPublishing,
                    onEditInfo = { step = SellStep.PRODUCT_INFO },
                    onEditCourse = { step = SellStep.COURSE_CONDITION },
                    onBack = { step = SellStep.COURSE_CONDITION },
                    onPublish = {
                        isPublishing = true
                        scope.launch {
                            publishedProduct = vm.publishListing(draft)
                            draft = SellDraft()
                            draftStore.clear()
                            isPublishing = false
                            step = SellStep.CONFIRMATION
                        }
                    },
                )
                SellStep.CONFIRMATION -> ConfirmationStep(
                    queued = publishedProduct?.pendingSync != null,
                    isOnline = vm.isOnline,
                    onViewListing = { publishedProduct?.id?.let(onPublished) },
                    onBackToHome = onExit,
                )
            }
        }
    }

    if (showDiscardDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Leave this listing?") },
            text = { Text("Your draft is saved on this phone, so you can finish it later. Or discard it now.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onExit() }) { Text("Keep draft") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        draft = SellDraft()
                        step = SellStep.PRODUCT_INFO
                        scope.launch { draftStore.clear() }
                        onExit()
                    },
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}

@Composable
private fun StepIndicator(step: SellStep, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SellStep.entries.filter { it != SellStep.CONFIRMATION }.forEach { s ->
            val active = s.ordinal <= step.ordinal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(if (active) AccentBlue else SecondaryBlue.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun ProductInfoStep(
    draft: SellDraft,
    draftRestored: Boolean,
    isOnline: Boolean,
    onDraftChange: (SellDraft) -> Unit,
    onContinue: () -> Unit,
) {
    // Errors appear once the user has typed in a field, not on an untouched form.
    val showTitleError = draft.title.isNotEmpty() && InputValidation.titleError(draft.title) != null
    val showDescriptionError = draft.description.isNotEmpty() && InputValidation.descriptionError(draft.description) != null
    val showPriceError = draft.price.isNotEmpty() && InputValidation.priceError(draft.price) != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        OfflineBanner(
            visible = !isOnline,
            message = "You're offline. You can still publish: the listing is sent when you reconnect.",
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (draftRestored) {
            Text(
                "We restored the draft you left unfinished.",
                style = MaterialTheme.typography.bodySmall,
                color = AccentBlue,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Text("Add photos", style = MaterialTheme.typography.titleMedium)
        Text(
            "The first photo is used as the cover.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(draft.photoCount) { index ->
                Box(modifier = Modifier.size(84.dp)) {
                    ProductPlaceholderImage(
                        category = draft.category,
                        seed = index,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (index == 0) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Cover photo",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .size(16.dp),
                        )
                    }
                    IconButton(
                        onClick = { onDraftChange(draft.copy(photoCount = draft.photoCount - 1)) },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove photo",
                            tint = Color.White,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .padding(2.dp),
                        )
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(SecondaryBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clickable(enabled = draft.photoCount < 6) {
                            onDraftChange(draft.copy(photoCount = draft.photoCount + 1))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo", tint = AccentBlue)
                }
            }
        }

        NoClipboard {
            OutlinedTextField(
                value = draft.title,
                onValueChange = {
                    onDraftChange(draft.copy(title = InputValidation.sanitizeText(it, InputLimits.TITLE_MAX).replace('\n', ' ')))
                },
                label = { Text("Title") },
                singleLine = true,
                isError = showTitleError,
                supportingText = {
                    FieldSupport(
                        error = if (showTitleError) InputValidation.titleError(draft.title) else null,
                        counter = "${draft.title.length}/${InputLimits.TITLE_MAX}",
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = { onDraftChange(draft.copy(description = InputValidation.sanitizeText(it, InputLimits.DESCRIPTION_MAX))) },
                label = { Text("Description") },
                minLines = 3,
                maxLines = 8,
                isError = showDescriptionError,
                supportingText = {
                    FieldSupport(
                        error = if (showDescriptionError) InputValidation.descriptionError(draft.description) else null,
                        counter = "${draft.description.length}/${InputLimits.DESCRIPTION_MAX}",
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }

        Text("Category", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 18.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Category.entries.filter { it != Category.ALL }) { category ->
                CampusSwapChip(
                    label = category.label,
                    selected = draft.category == category,
                    onClick = { onDraftChange(draft.copy(category = category)) },
                )
            }
        }

        NoClipboard {
            OutlinedTextField(
                value = draft.price,
                onValueChange = { onDraftChange(draft.copy(price = InputValidation.sanitizePrice(it))) },
                label = { Text("Price") },
                singleLine = true,
                leadingIcon = { Text("$", modifier = Modifier.padding(start = 12.dp)) },
                suffix = { Text("COP") },
                isError = showPriceError,
                supportingText = {
                    FieldSupport(
                        error = if (showPriceError) InputValidation.priceError(draft.price) else null,
                        hint = "Between ${formatPrice(InputLimits.PRICE_MIN.toDouble())} and ${formatPrice(InputLimits.PRICE_MAX.toDouble())}",
                    )
                },
                visualTransformation = PriceVisualTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            )
        }

        Button(
            onClick = onContinue,
            enabled = draft.isProductInfoValid,
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun FieldSupport(error: String?, hint: String? = null, counter: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(error ?: hint.orEmpty(), color = if (error != null) MaterialTheme.colorScheme.error else Color.Unspecified, modifier = Modifier.weight(1f))
        if (counter != null) Text(counter, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CourseConditionStep(
    draft: SellDraft,
    onDraftChange: (SellDraft) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    var courseQuery by remember { mutableStateOf(draft.course?.let { "${it.code} — ${it.name}" } ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Course association", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = courseQuery,
            onValueChange = {
                courseQuery = InputValidation.sanitizeText(it, InputLimits.SEARCH_MAX)
                if (draft.course != null) onDraftChange(draft.copy(course = null))
            },
            label = { Text("Search course code or name") },
            singleLine = true,
            enabled = !draft.notAssociatedWithCourse,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )

        if (!draft.notAssociatedWithCourse && draft.course == null && courseQuery.isNotBlank()) {
            val matches = SampleData.courses.filter {
                it.code.contains(courseQuery, ignoreCase = true) || it.name.contains(courseQuery, ignoreCase = true)
            }
            Column(modifier = Modifier.padding(top = 4.dp)) {
                matches.forEach { course ->
                    CourseRow(course) {
                        onDraftChange(draft.copy(course = course))
                        courseQuery = "${course.code} — ${course.name}"
                    }
                }
                if (matches.isEmpty()) {
                    Text(
                        "No matching course.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = draft.notAssociatedWithCourse,
                onCheckedChange = {
                    onDraftChange(draft.copy(notAssociatedWithCourse = it, course = if (it) null else draft.course))
                    if (it) courseQuery = ""
                },
            )
            Text("Not associated with a course")
        }

        Text("Condition", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Condition.entries.forEach { condition ->
                CampusSwapChip(
                    label = condition.label,
                    selected = draft.condition == condition,
                    onClick = { onDraftChange(draft.copy(condition = condition)) },
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Back")
            }
            Button(
                onClick = onContinue,
                enabled = draft.isCourseConditionValid,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            ) {
                Text("Review listing")
            }
        }
    }
}

@Composable
private fun CourseRow(course: Course, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(course.code, style = MaterialTheme.typography.labelLarge, color = AccentBlue)
            Text(course.name, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReviewStep(
    draft: SellDraft,
    isPublishing: Boolean,
    onEditInfo: () -> Unit,
    onEditCourse: () -> Unit,
    onBack: () -> Unit,
    onPublish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Review your listing", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onEditInfo) { Text("Edit") }
        }

        if (draft.photoCount > 0) {
            ProductPlaceholderImage(
                category = draft.category,
                seed = 0,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 8.dp),
            )
        }
        Text(draft.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 14.dp))
        Text(
            text = formatPrice(draft.price.toDoubleOrNull() ?: 0.0),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            draft.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        CampusSwapChip(
            label = draft.category.label,
            selected = false,
            onClick = {},
            modifier = Modifier.padding(top = 10.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Course & condition", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onEditCourse) { Text("Edit") }
        }
        Text(
            text = if (draft.notAssociatedWithCourse || draft.course == null) "Not associated with a course" else "${draft.course.code} — ${draft.course.name}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        draft.condition?.let {
            ConditionBadge(it, modifier = Modifier.padding(top = 8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, enabled = !isPublishing, modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Back")
            }
            Button(
                onClick = onPublish,
                enabled = !isPublishing,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Publish listing")
                }
            }
        }
    }
}

@Composable
private fun ConfirmationStep(
    queued: Boolean,
    isOnline: Boolean,
    onViewListing: () -> Unit,
    onBackToHome: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val (title, message) = when {
            queued && !isOnline -> "Saved on your phone" to
                "You're offline. We'll publish it automatically as soon as you reconnect, even if you close the app."
            queued -> "Publishing your listing" to "It'll be visible to other students in a moment."
            else -> "Your listing is live!" to "Verified students can now find it in Search and Home."
        }
        Icon(
            if (queued && !isOnline) Icons.Outlined.CloudOff else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (queued && !isOnline) AccentBlue else SuccessGreen,
            modifier = Modifier.size(72.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Button(
            onClick = onViewListing,
            modifier = Modifier.padding(top = 28.dp).fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        ) {
            Text("View listing")
        }
        OutlinedButton(
            onClick = onBackToHome,
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth().height(52.dp),
        ) {
            Text("Back to Home")
        }
    }
}
