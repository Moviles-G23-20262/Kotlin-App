package com.campusswap.app.views.screens.search

import com.campusswap.app.model.domain.InputLimits
import com.campusswap.app.model.domain.InputValidation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.tracing.trace
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.model.analytics.Analytics
import com.campusswap.app.model.analytics.Events
import com.campusswap.app.views.components.BodyText
import com.campusswap.app.views.components.CampusHeader
import com.campusswap.app.views.components.CampusIconButton
import com.campusswap.app.views.components.CampusIcons
import com.campusswap.app.views.components.CampusSlider
import com.campusswap.app.views.components.CampusTextField
import com.campusswap.app.views.components.EmptyState
import com.campusswap.app.views.components.OfflineBanner
import com.campusswap.app.views.components.Pill
import com.campusswap.app.views.components.ProductCard
import com.campusswap.app.views.components.formatPrice
import com.campusswap.app.viewmodel.AppViewModel
import com.campusswap.app.model.Category
import com.campusswap.app.model.Condition
import com.campusswap.app.model.Product
import com.campusswap.app.model.SortOption
import com.campusswap.app.views.themes.CampusSwapTheme

private const val MAX_PRICE_LIMIT = 350000f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    vm: AppViewModel,
    initialCategory: Category?,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
) {
    val c = CampusSwapTheme.colors
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory ?: Category.ALL) }
    var selectedCondition by remember { mutableStateOf<Condition?>(null) }
    var maxPrice by remember { mutableFloatStateOf(MAX_PRICE_LIMIT) }
    var sortOption by remember { mutableStateOf(SortOption.RELEVANCE) }
    var filtersExpanded by remember { mutableStateOf(initialCategory != null && initialCategory != Category.ALL) }

    // One pass over the catalogue instead of four chained filters (four intermediate lists per
    // keystroke), inside derivedStateOf so it reruns only when a filter or the catalogue changes. It was
    // keyed on the catalogue's size, which missed a queued listing replaced by the published one.
    val results: List<Product> by remember {
        derivedStateOf {
            trace("Search.results") {
                val category = selectedCategory
                val condition = selectedCondition
                val max = maxPrice.toDouble()
                val text = query
                val matches = vm.allProducts.filter {
                    (category == Category.ALL || it.category == category) &&
                        (condition == null || it.condition == condition) &&
                        it.price <= max &&
                        (
                            text.isBlank() ||
                                it.title.contains(text, ignoreCase = true) ||
                                it.category.label.contains(text, ignoreCase = true) ||
                                (it.course?.code?.contains(text, ignoreCase = true) == true)
                            )
                }
                when (sortOption) {
                    SortOption.RELEVANCE -> matches
                    SortOption.PRICE_LOW -> matches.sortedBy { it.price }
                    SortOption.PRICE_HIGH -> matches.sortedByDescending { it.price }
                    SortOption.RATING -> matches.sortedByDescending { it.rating ?: 0.0 }
                }
            }
        }
    }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            kotlinx.coroutines.delay(800)
            Analytics.log(Events.SEARCH_PERFORMED, "query_length" to query.length, "results" to results.size, "category" to selectedCategory.name)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        CampusHeader {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CampusIconButton(CampusIcons.Back, contentDescription = "Back", onClick = onBack)
                CampusTextField(
                    value = query,
                    onValueChange = { query = InputValidation.sanitizeText(it, InputLimits.SEARCH_MAX) },
                    placeholder = "Search course materials...",
                    leadingIcon = CampusIcons.Search,
                    modifier = Modifier.weight(1f),
                )
                CampusIconButton(
                    CampusIcons.Filter,
                    contentDescription = "Filters",
                    onClick = { filtersExpanded = !filtersExpanded },
                    active = filtersExpanded,
                    iconSize = 18.dp,
                )
            }

            AnimatedVisibility(visible = filtersExpanded) {
                val panelShape = RoundedCornerShape(12.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(c.elevated, panelShape)
                        .border(1.dp, c.borderSubtle, panelShape)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilterGroup("Category") {
                        Category.entries.forEach { category ->
                            Pill(category.label, selected = selectedCategory == category, onClick = { selectedCategory = category }, small = true)
                        }
                    }
                    FilterGroup("Condition") {
                        Pill("All", selected = selectedCondition == null, onClick = { selectedCondition = null }, small = true)
                        Condition.entries.forEach { condition ->
                            Pill(condition.label, selected = selectedCondition == condition, onClick = { selectedCondition = condition }, small = true)
                        }
                    }
                    Column {
                        BodyText(
                            "Max Price: ${formatPrice(maxPrice.toDouble())}",
                            weight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        CampusSlider(
                            value = maxPrice,
                            onValueChange = { maxPrice = it },
                            valueRange = 10000f..MAX_PRICE_LIMIT,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    FilterGroup("Sort by") {
                        SortOption.entries.forEach { option ->
                            Pill(sortLabel(option), selected = sortOption == option, onClick = { sortOption = option }, small = true)
                        }
                    }
                }
            }

            BodyText(
                "${results.size} item${if (results.size == 1) "" else "s"} found",
                color = c.textMuted,
            )
        }

        OfflineBanner(
            visible = !vm.isOnline,
            message = "You're offline. Searching the listings saved on this phone.",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        if (results.isEmpty()) {
            EmptyState(
                icon = CampusIcons.SearchLight,
                title = "No items found",
                message = "Try adjusting your filters",
                modifier = Modifier.padding(top = 20.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(results, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) },
                        imageHeight = 120.dp,
                    )
                }
            }
        }
    }
}

private fun sortLabel(option: SortOption): String = when (option) {
    SortOption.RELEVANCE -> "Relevance"
    SortOption.PRICE_LOW -> "Price low"
    SortOption.PRICE_HIGH -> "Price high"
    SortOption.RATING -> "Rating"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column {
        BodyText(title, weight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}
