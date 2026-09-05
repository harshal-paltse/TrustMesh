package com.trustmesh.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trustmesh.designsystem.theme.TrustMeshTheme
import com.trustmesh.domain.model.LinkedAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedAccountsScreen(
    accounts: List<LinkedAccount>,
    totalAgentLimit: Double,
    paymentSuccessMessage: String? = null,
    onLinkBank: () -> Unit,
    onInitiateRazorpay: (Double) -> Unit,
    onClearPaymentSuccess: () -> Unit,
    onRefresh: () -> Unit
) {
    val colors = TrustMeshTheme.colors
    val typography = TrustMeshTheme.typography

    val totalBankBalance = accounts.sumOf { it.availableBalance }
    val remainingUnreserved = (totalBankBalance - totalAgentLimit).coerceAtLeast(0.0)

    var showTopUpDialog by remember { mutableStateOf(false) }
    var selectedAmount by remember { mutableStateOf(1000.0) }
    var customAmountText by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Caches", style = typography.headlineLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundBase,
                    titleContentColor = colors.textPrimary
                ),
                actions = {
                    IconButton(onClick = onLinkBank) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Link Plaid", tint = colors.primary)
                    }
                }
            )
        },
        containerColor = colors.backgroundBase
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Payment success notification banner
            if (paymentSuccessMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated1),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.success,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = paymentSuccessMessage,
                                    style = typography.caption,
                                    color = colors.textPrimary
                                )
                            }
                            IconButton(onClick = onClearPaymentSuccess) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = colors.textSecondary)
                            }
                        }
                    }
                }
            }

            // Reconciliation balance allocation card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated1),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = colors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Envelope Reserve Reconciliation", style = typography.caption, color = colors.textSecondary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Bank Funds", style = typography.bodyMedium, color = colors.textSecondary)
                            Text("₹${String.format("%.2f", totalBankBalance)}", style = typography.monetaryMedium, color = colors.textPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reserved by Agents", style = typography.bodyMedium, color = colors.textSecondary)
                            Text("₹${String.format("%.2f", totalAgentLimit)}", style = typography.monetaryMedium, color = colors.secondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = colors.divider, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Free Unreserved Funds", style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            Text("₹${String.format("%.2f", remainingUnreserved)}", style = typography.monetaryMedium, color = colors.success)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Razorpay Direct Deposit Button
                        Button(
                            onClick = { showTopUpDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = colors.backgroundBase
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Add Funds with Razorpay (UPI / Card)",
                                    style = typography.labelLarge.copy(color = colors.backgroundBase)
                                )
                            }
                        }
                    }
                }
            }

            // Linked accounts header
            item {
                Text(
                    "Connected Reserves & Accounts",
                    style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }

            if (accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No bank accounts linked.", style = typography.bodySmall, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onLinkBank,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Link via Plaid Sandbox", style = typography.labelLarge.copy(color = colors.backgroundBase))
                            }
                        }
                    }
                }
            } else {
                items(accounts) { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceElevated1)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(acc.institutionName, style = typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            Text("ID: ...${acc.plaidAccountId.takeLast(4)}", style = typography.caption, color = colors.textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${String.format("%.2f", acc.availableBalance)}", style = typography.monetaryMedium, color = colors.textPrimary)
                            Text("Available", style = typography.caption, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }

    // Top-Up Amount Selection Dialog
    if (showTopUpDialog) {
        val quickAmounts = listOf(500.0, 1000.0, 2500.0, 5000.0)

        AlertDialog(
            onDismissRequest = { showTopUpDialog = false },
            containerColor = colors.surfaceElevated1,
            title = {
                Text(
                    "Fund Reserve with Razorpay",
                    style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "Select an amount to deposit into your TrustMesh wallet via UPI, Debit/Credit Card, or NetBanking:",
                        style = typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickAmounts.forEach { amt ->
                            val isSelected = !isCustomSelected && selectedAmount == amt
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) colors.primary else colors.backgroundBase,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        isCustomSelected = false
                                        selectedAmount = amt
                                    }
                            ) {
                                Text(
                                    text = "₹${amt.toInt()}",
                                    style = typography.caption.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) colors.backgroundBase else colors.textPrimary,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = {
                            customAmountText = it
                            val parsed = it.toDoubleOrNull()
                            if (parsed != null && parsed > 0) {
                                isCustomSelected = true
                                selectedAmount = parsed
                            }
                        },
                        label = { Text("Custom Amount (₹)", style = typography.caption) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.divider,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTopUpDialog = false
                        val finalAmount = if (isCustomSelected) {
                            customAmountText.toDoubleOrNull() ?: selectedAmount
                        } else selectedAmount
                        if (finalAmount > 0.0) {
                            onInitiateRazorpay(finalAmount)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Proceed to Pay ₹${selectedAmount.toInt()}", color = colors.backgroundBase)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTopUpDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}
