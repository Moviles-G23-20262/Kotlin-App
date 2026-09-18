package com.campusswap.app.screens.search

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.CampusHeader
import com.campusswap.app.components.CampusIconButton
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.CampusSlider
import com.campusswap.app.components.CampusTextField
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.Pill
import com.campusswap.app.components.ProductCard
import com.campusswap.app.components.formatPrice
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.Category
import com.campusswap.app.data.Condition
import com.campusswap.app.data.SortOption
import com.campusswap.app.ui.theme.CampusSwapTheme

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

    val results = remember(query, selectedCategory, selectedCondition, maxPrice, sortOption, vm.allProducts.size) {
        vm.allProducts
            .filter { selectedCategory == Category.ALL || it.category == selectedCategory }
            .filter { selectedCondition == null || it.condition == selectedCondition }
            .filter { it.price <= maxPrice.toDouble() }
            .filter {
                query.isBlank() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.category.label.contains(query, ignoreCase = true) ||
                    (it.course?.code?.contains(query, ignoreCase = true) == true)
            }
            .let { list ->
                when (sortOption) {
                    SortOption.RELEVANCE -> list
                    SortOption.PRICE_LOW -> list.sortedBy { it.price }
                    SortOption.PRICE_HIGH -> list.sortedByDescending { it.price }
                    SortOption.RATING -> list.sortedByDescending { it.rating ?: 0.0 }
                }
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
                    onValueChange = { query = it },
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
