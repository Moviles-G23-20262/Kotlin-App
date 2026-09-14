package com.campusswap.app.screens.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.ProductPlaceholderImage
import com.campusswap.app.components.formatPrice
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.CartLine
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.ErrorRed

@Composable
fun CartScreen(
    vm: AppViewModel,
    onBrowse: () -> Unit,
) {
    var showCheckoutConfirm by remember { mutableStateOf(false) }

    if (vm.cart.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            com.campusswap.app.components.EmptyState(
                icon = Icons.Outlined.ShoppingCart,
                title = "Cart is empty",
                message = "Items you add will show up here.",
                actionLabel = "Browse Items",
                onAction = onBrowse,
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(vm.cart, key = { it.product.id }) { line ->
                CartLineRow(
                    line = line,
                    onIncrease = { vm.setQuantity(line.product.id, line.quantity + 1) },
                    onDecrease = { vm.setQuantity(line.product.id, line.quantity - 1) },
                    onRemove = { vm.removeFromCart(line.product.id) },
                )
            }
        }

        Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatPrice(vm.cartTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Button(
                    onClick = { showCheckoutConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                ) {
                    Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null)
                    Text("  Checkout")
                }
            }
        }
    }

    if (showCheckoutConfirm) {
        AlertDialog(
            onDismissRequest = { showCheckoutConfirm = false },
            title = { Text("Checkout simulated") },
            text = { Text("This prototype doesn't process real payments. In the full app you'd coordinate a safe on-campus meeting point with each seller next.") },
            confirmButton = {
                Button(onClick = { showCheckoutConfirm = false; vm.cart.clear() }) {
                    Text("Got it")
                }
            },
        )
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductPlaceholderImage(
                category = line.product.category,
                seed = line.product.imageSeed,
                modifier = Modifier.size(72.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(line.product.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(
                    formatPrice(line.product.price),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedIconButton(onClick = onDecrease, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(14.dp))
                    }
                    Text("${line.quantity}", modifier = Modifier.padding(horizontal = 12.dp))
                    OutlinedIconButton(onClick = onIncrease, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase quantity", modifier = Modifier.size(14.dp))
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = ErrorRed)
            }
        }
    }
}
