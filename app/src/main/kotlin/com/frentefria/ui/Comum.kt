package com.frentefria.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val PT = Locale("pt", "BR")

/** "qui, 14/11" */
fun LocalDate.curta(): String = dayOfWeek.getDisplayName(TextStyle.SHORT, PT).removeSuffix(".") + ", " + format(DateTimeFormatter.ofPattern("dd/MM"))

/** "14/11" */
fun LocalDate.diaMes(): String = format(DateTimeFormatter.ofPattern("dd/MM"))

/** "14 de novembro" */
fun LocalDate.porExtenso(): String = format(DateTimeFormatter.ofPattern("d 'de' MMMM", PT))

fun LocalDate.semana(): String = dayOfWeek.getDisplayName(TextStyle.SHORT, PT).removeSuffix(".").replaceFirstChar { it.uppercase() }

/** "hoje", "amanhã", "em 3 dias", "há 2 dias" */
fun emDias(n: Int): String = when {
    n == 0 -> "hoje"
    n == 1 -> "amanhã"
    n == -1 -> "ontem"
    n > 1 -> "em $n dias"
    else -> "há ${-n} dias"
}

fun plural(n: Int, um: String, varios: String) = if (n == 1) "1 $um" else "$n $varios"

@Composable
fun Secao(titulo: String, modifier: Modifier = Modifier) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    cor: Color = MaterialTheme.colorScheme.surfaceContainer,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cor),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = conteudo)
    }
}

/** Seletor de data do Material, em LocalDate. `ate` limita o último dia escolhível. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscolherData(
    inicial: LocalDate,
    aoEscolher: (LocalDate) -> Unit,
    aoFechar: () -> Unit,
    ate: LocalDate? = null,
) {
    val millis = inicial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val limite = ate?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = millis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = limite == null || utcTimeMillis <= limite
        },
    )
    DatePickerDialog(
        onDismissRequest = aoFechar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { aoEscolher(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                aoFechar()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    ) {
        DatePicker(estado, showModeToggle = false)
    }
}

/**
 * Esqueleto de toda tela: barra de título com voltar opcional. Nas abas, a
 * barra de navegação de baixo já cuida do espaço do sistema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tela(
    titulo: String,
    aoVoltar: (() -> Unit)? = null,
    comAbas: Boolean = aoVoltar == null,
    acoes: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    conteudo: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (aoVoltar != null) androidx.compose.material3.IconButton(onClick = aoVoltar) {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = acoes,
            )
        },
        floatingActionButton = fab,
        contentWindowInsets = if (comAbas) androidx.compose.foundation.layout.WindowInsets(0) else androidx.compose.foundation.layout.WindowInsets.navigationBars,
        content = conteudo,
    )
}

/** "dom, 20/09/2026" */
fun LocalDate.completa(): String = dayOfWeek.getDisplayName(TextStyle.SHORT, PT).removeSuffix(".") + ", " + format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
