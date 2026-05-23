package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BankViewModel
import com.example.data.CardTransaction
import com.example.data.TransferStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDashboardScreen(
    viewModel: BankViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current

    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val isIncomeActive by viewModel.isIncomeActive.collectAsStateWithLifecycle()
    val targetCardNumber by viewModel.targetCardNumber.collectAsStateWithLifecycle()
    val transferAmount by viewModel.transferAmount.collectAsStateWithLifecycle()
    val transferStatus by viewModel.transferStatus.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var showHeaderInfo by remember { mutableStateOf(false) }

    // Intercept transfer status messages
    LaunchedEffect(transferStatus) {
        when (transferStatus) {
            is TransferStatus.Success -> {
                val successMsg = transferStatus as TransferStatus.Success
                Toast.makeText(
                    context,
                    "Успешный перевод: ${successMsg.amount} ₴ отправлено на карту ${successMsg.card}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.dismissStatus()
            }
            is TransferStatus.Error -> {
                val errorMsg = transferStatus as TransferStatus.Error
                Toast.makeText(context, errorMsg.message, Toast.LENGTH_LONG).show()
                viewModel.dismissStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE3F2FD),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Logo",
                                tint = Color(0xFF1E88E5),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize()
                            )
                        }
                        Text(
                            text = "Дрим Банк",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Color(0xFF0F172A)
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHeaderInfo = !showHeaderInfo }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = Color(0xFF64748B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // General status hint if dynamic info toggled
            AnimatedVisibility(
                visible = showHeaderInfo,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "О приложении",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Виртуальный симулятор банковского счета с ежесекундным начислением и поддержкой мгновенных переводов на другие карты.",
                            color = Color(0xFF475569),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Real-looking credit card
            Spacer(modifier = Modifier.height(4.dp))
            CardWidget(
                balance = balance,
                cardNumber = "1022 4014 2513 2329",
                onCopyClick = {
                    clipboard.setText(AnnotatedString("1022401425132329"))
                    Toast.makeText(context, "Номер карты скопирован!", Toast.LENGTH_SHORT).show()
                }
            )

            // THE BLUE SETTINGS BUTTON
            // It toggles accumulation on and off, but ALWAYS displays just "Настройки" to keep it secret.
            Button(
                onClick = {
                    viewModel.toggleIncome()
                    // Stealthy confirmation toast
                    Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)), // Strictly blue button
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("settings_button"),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 6.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Настройки",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            // Transfer money section
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Transfer icon",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Денежный перевод",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                    }

                    // Recipient's card input
                    OutlinedTextField(
                        value = targetCardNumber,
                        onValueChange = { viewModel.onCardNumberChanged(it) },
                        label = { Text("Номер карты получателя") },
                        placeholder = { Text("#### #### #### ####") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Card security icon",
                                tint = Color(0xFF94A3B8)
                            )
                        },
                        trailingIcon = {
                            if (targetCardNumber.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onCardNumberChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear text"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_card_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E88E5),
                            cursorColor = Color(0xFF1E88E5)
                        )
                    )

                    // Transfer amount input
                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { viewModel.onAmountChanged(it) },
                        label = { Text("Сумма перевода") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        leadingIcon = {
                            Text(
                                text = "₴",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        suffix = {
                            Text("грн", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        },
                        trailingIcon = {
                            if (transferAmount.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onAmountChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear amount"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E88E5),
                            cursorColor = Color(0xFF1E88E5)
                        )
                    )

                    // Transfer Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.executeTransfer()
                        },
                        enabled = targetCardNumber.isNotEmpty() && transferAmount.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F172A),
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("transfer_submit_button")
                    ) {
                        Text(
                            text = "Подтвердить перевод",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Operational history
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "История переводов",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "${transactions.size} операций",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (transactions.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Empty log Icon",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Не зафиксировано ни одного перевода",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Введите номер любой карты в форму выше и отправьте монеты, чтобы увидеть их в этом списке.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        transactions.forEach { tx ->
                            TransactionRow(transaction = tx)
                        }
                    }
                }
            }
        }
    }
}

// Gorgeous physical bank card UI
@Composable
fun CardWidget(
    balance: Long,
    cardNumber: String,
    onCopyClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // Standard Credit Card aspect ratio
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B1424),
                            Color(0xFF14243B),
                            Color(0xFF1B3354)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            // Subtle abstract glow in the background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF2196F3).copy(alpha = 0.15f),
                    radius = size.width * 0.35f,
                    center = Offset(size.width * 0.9f, size.height * 0.2f)
                )
            }

            // Elements layout inside the card
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top card header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ukrainian flag accents + Platinum Card Label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tiny Elegant Ukrainian flag drawing
                        Canvas(modifier = Modifier.size(width = 24.dp, height = 16.dp)) {
                            drawRoundRect(
                                color = Color(0xFF0057B7),
                                size = Size(size.width, size.height / 2f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = Color(0xFFFFD700),
                                size = Size(size.width, size.height / 2f),
                                topLeft = Offset(0f, size.height / 2f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                        
                        Text(
                            text = "ДРИМ ПЛАТИНУМ",
                            color = Color(0xFFB4C6E7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    // Contactless chip sign + logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Custom contactless lines drawing
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.7f),
                                radius = 12f,
                                center = Offset(size.width, size.height / 2f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.7f),
                                radius = 24f,
                                center = Offset(size.width, size.height / 2f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                            )
                        }
                    }
                }

                // Balance display (middle)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Баланс карты",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatBalance(balance),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("card_balance_text")
                        )
                        Text(
                            text = "₴",
                            color = Color(0xFFFFD700), // Beautiful yellow/gold currency sign
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Bottom card rows (Card number & Cardholder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onCopyClick() }
                        ) {
                            Text(
                                text = cardNumber,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.testTag("card_number_text")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Draw minimalist tiny duplicate square card copy indicator
                            Canvas(modifier = Modifier.size(10.dp)) {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.4f),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(18f, 18f),
                                    cornerRadius = CornerRadius(3f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                )
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.4f),
                                    topLeft = Offset(8f, 8f),
                                    size = Size(18f, 18f),
                                    cornerRadius = CornerRadius(3f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                )
                            }
                        }
                        Text(
                            text = "IVAN DRAGOMIROV",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp
                        )
                    }

                    // Gold Card Chip drawing
                    Box(
                        modifier = Modifier
                            .size(34.dp, 26.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFE2B04E))
                            .drawBehind {
                                // Draw lines of card chip
                                val strokeWidth = 3f
                                drawLine(
                                    color = Color(0xFF6B4E1A),
                                    start = Offset(size.width * 0.33f, 0f),
                                    end = Offset(size.width * 0.33f, size.height),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    color = Color(0xFF6B4E1A),
                                    start = Offset(size.width * 0.66f, 0f),
                                    end = Offset(size.width * 0.66f, size.height),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    color = Color(0xFF6B4E1A),
                                    start = Offset(0f, size.height * 0.5f),
                                    end = Offset(size.width, size.height * 0.5f),
                                    strokeWidth = strokeWidth
                                )
                            }
                    )
                }
            }
        }
    }
}

// Transaction list row implementation
@Composable
fun TransactionRow(
    transaction: CardTransaction,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Transfer icon",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize()
                    )
                }
                Column {
                    Text(
                        text = "Перевод на карту",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Карта: ${transaction.targetCard}",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatTimestamp(transaction.timestamp),
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "-${formatBalance(transaction.amount)} ₴",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

fun formatBalance(amount: Long): String {
    return String.format(Locale.getDefault(), "%,d", amount).replace(",", " ")
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
