package com.campusswap.app.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.CampusSwapChip
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.ProductCard
import com.campusswap.app.components.formatPrice
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.Category
import com.campusswap.app.data.Condition
import com.campusswap.app.data.SortOption

@Composable
fun SearchScreen(
    vm: AppViewModel,
    initialCategory: Category?,
    onProductClick: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory ?: Category.ALL) }
    var selectedCondition by remember { mutableStateOf<Condition?>(null) }
    var maxPrice by remember { mutableStateOf(350000f) }
    var sortOption by remember { mutableStateOf(SortOption.RELEVANCE) }
    var filtersExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val results = remember(query, selectedCategory, selectedCondition, maxPrice, sortOption, vm.allProducts.size) {
        vm.allProducts
            .filter { selectedCategory == Category.ALL || it.category == selectedCategory }
            .filter { selectedCondition == null || it.condition == selectedCondition }
            .filter { it.price <= maxPrice.toDouble() }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || (it.course?.code?.contains(query, ignoreCase = true) == true) }
            .let { list ->
                when (sortOption) {
                    SortOption.RELEVANCE -> list
                    SortOption.PRICE_LOW -> list.sortedBy { it.price }
                    SortOption.PRICE_HIGH -> list.sortedByDescending { it.price }
                    SortOption.RATING -> list.sortedByDescending { it.rating ?: 0.0 }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search items or course codes") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    )
                    IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filters",
                            tint = if (filtersExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                AnimatedVisibility(visible = filtersExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text("Category", style = MaterialTheme.typography.titleSmall)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
                        ) {
                            items(Category.entries.toList()) { category ->
                                CampusSwapChip(
                                    label = category.label,
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                )
                            }
                        }

                        Text("Condition", style = MaterialTheme.typography.titleSmall)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
                        ) {
                            item {
                                CampusSwapChip(
                                    label = "All",
                                    selected = selectedCondition == null,
                                    onClick = { selectedCondition = null },
                                )
                            }
                            items(Condition.entries.toList()) { condition ->
                                CampusSwapChip(
                                    label = condition.label,
                                    selected = selectedCondition == condition,
                                    onClick = { selectedCondition = condition },
                                )
                            }
                        }

                        Text("Max price: ${formatPrice(maxPrice.toDouble())}", style = MaterialTheme.typography.titleSmall)
                        Slider(
                            value = maxPrice,
                            onValueChange = { maxPrice = it },
                            valueRange = 10000f..350000f,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Sort by", style = MaterialTheme.typography.titleSmall)
                            Box {
                                TextButton(onClick = { sortMenuExpanded = true }) {
                                    Text(sortOption.label)
                                }
                                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                    SortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = { sortOption = option; sortMenuExpanded = false },
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                    }
                }

                Text(
                    text = "${results.size} result${if (results.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        if (results.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "No matches yet",
                message = "Try a different keyword, or widen your filters.",
                modifier = Modifier.padding(top = 32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { product ->
                            ProductCard(
                                product = product,
                                isWishlisted = vm.isWishlisted(product.id),
                                onClick = { onProductClick(product.id) },
                                onToggleWishlist = { vm.toggleWishlist(product.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
