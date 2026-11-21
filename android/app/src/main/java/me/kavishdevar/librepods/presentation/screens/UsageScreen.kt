/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.UsageStats
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

private class UsagePalette(
    val background: Brush,
    val heroBackground: Brush,
    val heroBorder: Color,
    val heroContent: Color,
    val heroSecondary: Color,
    val bar: Color,
    val barToday: Color,
    val tile: Color,
    val accent: Color,
    val content: Color,
    val secondary: Color,
    val fontFamily: FontFamily?,
    val cornerRadius: Int,
)

@Composable
private fun rememberUsagePalette(): UsagePalette {
    val isDark = isSystemInDarkTheme()
    val colors = MaterialTheme.colorScheme
    return when (LocalDesignSystem.current) {
        DesignSystem.Apple -> {
            val sfPro = FontFamily(Font(R.font.sf_pro))
            UsagePalette(
                background = Brush.verticalGradient(
                    0f to if (isDark) Color(0xFF3A5530) else Color(0xFFBFD6AE),
                    0.45f to if (isDark) Color(0xFF1C2718) else Color(0xFFE3ECDB),
                    1f to colors.surfaceContainer,
                ),
                heroBackground = Brush.verticalGradient(
                    listOf(Color(0xFF3A4F30), Color(0xFF26341F))
                ),
                heroBorder = Color(0xFF7FA266).copy(alpha = 0.45f),
                heroContent = Color.White,
                heroSecondary = Color.White.copy(alpha = 0.72f),
                bar = Color.White.copy(alpha = 0.28f),
                barToday = Color(0xFFB8D8A4),
                tile = if (isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.72f),
                accent = if (isDark) Color(0xFFADCB9C) else Color(0xFF4C7339),
                content = if (isDark) Color.White else Color.Black,
                secondary = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.55f),
                fontFamily = sfPro,
                cornerRadius = 28,
            )
        }
        DesignSystem.Material -> UsagePalette(
            background = Brush.verticalGradient(listOf(colors.surfaceContainer, colors.surfaceContainer)),
            heroBackground = Brush.verticalGradient(listOf(colors.primaryContainer, colors.primaryContainer)),
            heroBorder = Color.Transparent,
            heroContent = colors.onPrimaryContainer,
            heroSecondary = colors.onPrimaryContainer.copy(alpha = 0.75f),
            bar = colors.onPrimaryContainer.copy(alpha = 0.25f),
            barToday = colors.primary,
            tile = colors.surfaceBright,
            accent = colors.primary,
            content = colors.onSurface,
            secondary = colors.onSurfaceVariant,
            fontFamily = null,
            cornerRadius = 24,
        )
    }
}

@Composable
fun UsageScreen(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var stats by remember { mutableStateOf(UsageStats()) }
    LaunchedEffect(viewModel.isReady) {
        if (!viewModel.isReady) return@LaunchedEffect
        while (true) {
            stats = viewModel.loadUsageStats()
            delay(30_000)
        }
    }

    val model = state.instance?.model
    UsageContent(
        stats = stats,
        deviceName = state.deviceName,
        modelName = model?.displayName ?: state.deviceName,
        manufacturer = model?.manufacturer ?: "Apple Inc.",
        modelNumber = state.instance?.actualModelNumber,
        caseRes = model?.caseRes ?: R.drawable.airpods_pro_2_case,
    )
}

@Composable
private fun UsageContent(
    stats: UsageStats,
    deviceName: String,
    modelName: String,
    manufacturer: String,
    modelNumber: String?,
    caseRes: Int,
) {
    val palette = rememberUsagePalette()
    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material
    val topPadding = if (m3eEnabled) 8.dp else WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

    val today = LocalDate.now()
    val weekStart = today.minusDays(6)
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            if (!m3eEnabled) {
                Text(
                    text = deviceName,
                    style = TextStyle(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.content,
                        fontFamily = palette.fontFamily
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            HeroCard(
                palette = palette,
                title = modelName,
                caseRes = caseRes,
                weekWornMs = stats.wornBetween(weekStart, today),
                days = (0..6).map { weekStart.plusDays(it.toLong()) },
                stats = stats,
                today = today,
            )

            Text(
                text = listOfNotNull(
                    "${stringResource(R.string.usage_last_7_days)} ${weekStart.format(shortDateFormatter)}–${today.format(shortDateFormatter)}",
                    manufacturer,
                    modelNumber?.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                style = TextStyle(fontSize = 13.sp, color = palette.secondary, fontFamily = palette.fontFamily),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
            )

            if (stats.sessionCount == 0) {
                UsageTile(palette, modifier = Modifier.fillMaxWidth()) {
                    TileLabel(palette, stringResource(R.string.usage_empty_title))
                    Spacer(modifier = Modifier.height(6.dp))
                    TileCaption(palette, stringResource(R.string.usage_empty_description))
                }
            } else {
                UsageTile(palette, modifier = Modifier.fillMaxWidth()) {
                    TileLabel(palette, stringResource(R.string.usage_total_wear_time), large = true)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = durationText(stats.totalWornMs, 52.sp, 24.sp),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            fontFamily = palette.fontFamily
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TileCaption(
                        palette,
                        listOfNotNull(
                            stats.firstWornAt?.let {
                                stringResource(R.string.usage_tracked_since, toLocalDate(it).format(dateFormatter))
                            },
                            pluralStringResource(R.plurals.usage_days_worn_count, stats.daysWorn, stats.daysWorn)
                        ).joinToString(" · ")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                TileRow {
                    StatTile(
                        palette, it,
                        label = stringResource(R.string.usage_times_worn),
                        value = AnnotatedString("%,d".format(stats.sessionCount)),
                        caption = stringResource(R.string.usage_times_unit)
                    )
                    StatTile(
                        palette, it,
                        label = stringResource(R.string.usage_average_wear),
                        value = durationText(stats.averageSessionMs, 34.sp, 16.sp),
                        caption = stringResource(R.string.usage_per_wear)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TileRow {
                    StatTile(
                        palette, it,
                        label = stringResource(R.string.usage_longest_wear),
                        value = durationText(stats.longestSessionMs, 34.sp, 16.sp),
                        caption = null
                    )
                    StatTile(
                        palette, it,
                        label = stringResource(R.string.usage_days_worn),
                        value = AnnotatedString(stats.daysWorn.toString()),
                        caption = stringResource(R.string.usage_days_unit)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TileRow {
                    StatTile(
                        palette, it,
                        label = stringResource(R.string.usage_first_worn),
                        value = AnnotatedString(stats.firstWornAt?.let { t -> toLocalDate(t).format(dateFormatter) } ?: "—"),
                        caption = null,
                        valueSize = 22.sp
                    )
                    StatTile(
                        palette, it,
                        label = stringResource(R.string.usage_last_worn),
                        value = AnnotatedString(
                            if (stats.currentSessionStart != null) stringResource(R.string.usage_now)
                            else stats.lastWornAt?.let { t -> toLocalDate(t).format(dateFormatter) } ?: "—"
                        ),
                        caption = if (stats.currentSessionStart != null) stringResource(R.string.usage_in_ears) else null,
                        valueSize = 22.sp
                    )
                }
            }

            Text(
                text = stringResource(R.string.usage_footer),
                style = TextStyle(fontSize = 12.sp, color = palette.secondary, fontFamily = palette.fontFamily),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun HeroCard(
    palette: UsagePalette,
    title: String,
    caseRes: Int,
    weekWornMs: Long,
    days: List<LocalDate>,
    stats: UsageStats,
    today: LocalDate,
) {
    val shape = RoundedCornerShape(palette.cornerRadius.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.heroBackground)
            .border(1.dp, palette.heroBorder, shape)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.heroContent,
                fontFamily = palette.fontFamily
            )
        )
        Image(
            bitmap = ImageBitmap.imageResource(caseRes),
            contentDescription = title,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            text = durationText(weekWornMs, 64.sp, 28.sp),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = palette.heroContent,
                fontFamily = palette.fontFamily
            )
        )
        Text(
            text = stringResource(R.string.usage_worn_last_7_days),
            style = TextStyle(fontSize = 15.sp, color = palette.heroSecondary, fontFamily = palette.fontFamily)
        )
        Spacer(modifier = Modifier.height(16.dp))
        WeekBars(palette, days.map { it to stats.wornOn(it) }, today)
    }
}

@Composable
private fun WeekBars(palette: UsagePalette, days: List<Pair<LocalDate, Long>>, today: LocalDate) {
    val max = days.maxOf { it.second }.coerceAtLeast(1L)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (day, ms) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(48.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((ms.toFloat() / max).coerceIn(0.06f, 1f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (day == today) palette.barToday else palette.bar)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day.dayOfWeek.getDisplayName(DateTextStyle.NARROW, Locale.getDefault()),
                    style = TextStyle(fontSize = 12.sp, color = palette.heroSecondary, fontFamily = palette.fontFamily)
                )
            }
        }
    }
}

@Composable
private fun TileRow(content: @Composable (Modifier) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content(Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun UsageTile(
    palette: UsagePalette,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(palette.cornerRadius.dp))
            .background(palette.tile)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        content()
    }
}

@Composable
private fun StatTile(
    palette: UsagePalette,
    modifier: Modifier,
    label: String,
    value: AnnotatedString,
    caption: String?,
    valueSize: TextUnit = 34.sp,
) {
    UsageTile(palette, modifier) {
        TileLabel(palette, label)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = TextStyle(
                fontSize = valueSize,
                fontWeight = FontWeight.Bold,
                color = palette.content,
                fontFamily = palette.fontFamily
            )
        )
        caption?.let {
            Spacer(modifier = Modifier.height(2.dp))
            TileCaption(palette, it)
        }
    }
}

@Composable
private fun TileLabel(palette: UsagePalette, text: String, large: Boolean = false) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = if (large) 20.sp else 15.sp,
            fontWeight = if (large) FontWeight.SemiBold else FontWeight.Normal,
            color = if (large) palette.content else palette.secondary,
            fontFamily = palette.fontFamily
        )
    )
}

@Composable
private fun TileCaption(palette: UsagePalette, text: String) {
    Text(
        text = text,
        style = TextStyle(fontSize = 13.sp, color = palette.secondary, fontFamily = palette.fontFamily)
    )
}

@Composable
private fun durationText(ms: Long, numberSize: TextUnit, unitSize: TextUnit): AnnotatedString {
    val h = stringResource(R.string.unit_hours_short)
    val min = stringResource(R.string.unit_minutes_short)
    val s = stringResource(R.string.unit_seconds_short)
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val parts = when {
        hours > 0 -> listOf(hours to h, minutes to min)
        minutes > 0 -> listOf(minutes to min)
        else -> listOf(totalSeconds to s)
    }
    return buildAnnotatedString {
        parts.forEach { (number, unit) ->
            withStyle(SpanStyle(fontSize = numberSize)) { append("%,d".format(number)) }
            withStyle(SpanStyle(fontSize = unitSize, fontWeight = FontWeight.SemiBold)) { append(unit) }
        }
    }
}

private fun toLocalDate(epochMs: Long): LocalDate =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
