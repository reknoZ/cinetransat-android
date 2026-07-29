package com.heewhack.cinetransat.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.ProductDetails
import com.heewhack.cinetransat.R
import com.heewhack.cinetransat.billing.SupportBillingRepository
import com.heewhack.cinetransat.billing.SupportProductIds
import com.heewhack.cinetransat.billing.formattedPrice
import com.heewhack.cinetransat.ui.LocalComponentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDeveloperSheet(
    onDismiss: () -> Unit,
) {
    val activity = LocalComponentActivity.current
    val repository = remember { SupportBillingRepository(activity) }
    val state by repository.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCustomDialog by remember { mutableStateOf(false) }
    var customAmount by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(repository) {
        repository.start()
        onDispose { repository.end() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.support_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.support_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            )

            Text(
                text = stringResource(R.string.support_onetime_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AmountChip(
                    label =
                        state.tipProducts.find { it.productId == SupportProductIds.TIP_199 }
                            ?.formattedPrice() ?: "1.99",
                    enabled = !state.isPurchasing,
                    onClick = {
                        repository.tipProduct(SupportProductIds.TIP_199)?.let {
                            repository.launchPurchase(activity, it)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                AmountChip(
                    label =
                        state.tipProducts.find { it.productId == SupportProductIds.TIP_499 }
                            ?.formattedPrice() ?: "4.99",
                    enabled = !state.isPurchasing,
                    onClick = {
                        repository.tipProduct(SupportProductIds.TIP_499)?.let {
                            repository.launchPurchase(activity, it)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                AmountChip(
                    label =
                        state.tipProducts.find { it.productId == SupportProductIds.TIP_999 }
                            ?.formattedPrice() ?: "9.99",
                    enabled = !state.isPurchasing,
                    onClick = {
                        repository.tipProduct(SupportProductIds.TIP_999)?.let {
                            repository.launchPurchase(activity, it)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            AmountChip(
                label = stringResource(R.string.support_custom),
                enabled = !state.isPurchasing,
                onClick = {
                    customAmount = ""
                    customError = null
                    showCustomDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                emphasized = false,
            )

            Text(
                text = stringResource(R.string.support_monthly_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AmountChip(
                    label =
                        monthlyLabel(
                            state.monthlyProducts.find { it.productId == SupportProductIds.MONTHLY_099 },
                            fallback = "0.99",
                        ),
                    enabled = !state.isPurchasing,
                    onClick = {
                        repository.monthlyProduct(SupportProductIds.MONTHLY_099)?.let {
                            repository.launchPurchase(activity, it)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                AmountChip(
                    label =
                        monthlyLabel(
                            state.monthlyProducts.find { it.productId == SupportProductIds.MONTHLY_199 },
                            fallback = "1.99",
                        ),
                    enabled = !state.isPurchasing,
                    onClick = {
                        repository.monthlyProduct(SupportProductIds.MONTHLY_199)?.let {
                            repository.launchPurchase(activity, it)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            state.activeMonthlyProductId?.let { activeId ->
                val price =
                    state.monthlyProducts.find { it.productId == activeId }?.formattedPrice() ?: activeId
                Text(
                    text = stringResource(R.string.support_monthly_active, price),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                )
            }

            OutlinedButton(
                onClick = { repository.restore() },
                enabled = !state.isPurchasing,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            ) {
                Text(stringResource(R.string.support_restore))
            }

            if (state.isConnecting || state.isPurchasing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.didSucceed) {
                Text(
                    text = stringResource(R.string.support_thanks),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (
                !state.isConnecting &&
                state.tipProducts.isEmpty() &&
                state.monthlyProducts.isEmpty()
            ) {
                Text(
                    text = stringResource(R.string.support_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(stringResource(R.string.support_custom_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.support_custom_help))
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.support_custom_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    customError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val product = repository.tipProductForNominalAmount(customAmount)
                        if (product == null) {
                            customError =
                                activity.getString(R.string.support_custom_unavailable)
                        } else {
                            showCustomDialog = false
                            repository.launchPurchase(activity, product)
                        }
                    },
                ) {
                    Text(stringResource(R.string.support_purchase))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(stringResource(R.string.support_cancel))
                }
            },
        )
    }
}

@Composable
private fun monthlyLabel(
    details: ProductDetails?,
    fallback: String,
): String {
    val price = details?.formattedPrice() ?: fallback
    return price + stringResource(R.string.support_per_month)
}

@Composable
private fun AmountChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = true,
) {
    val pink = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground,
                containerColor = pink.copy(alpha = if (emphasized) 0.35f else 0.18f),
            ),
        border = BorderStroke(1.dp, pink.copy(alpha = 0.45f)),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}
