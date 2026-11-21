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

package me.kavishdevar.librepods.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus

private const val LOW_BATTERY = 20

/** Material 3 battery status: a hero card for the earbuds and a tile per component. */
@Composable
fun MaterialBatteryStatus(
    batteryList: List<Battery>,
    budsRes: Int,
    leftRes: Int,
    rightRes: Int,
    caseRes: Int,
) {
    val left = batteryList.find { it.component == BatteryComponent.LEFT }
    val right = batteryList.find { it.component == BatteryComponent.RIGHT }
    val case = batteryList.find { it.component == BatteryComponent.CASE }
    val connectedBuds = listOfNotNull(left, right).filter { it.isAvailable() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BatteryHero(
            budsRes = budsRes,
            level = connectedBuds.minOfOrNull { it.level },
            charging = connectedBuds.isNotEmpty() && connectedBuds.all { it.isCharging() },
            left = left?.takeIf { it.isAvailable() }?.level,
            right = right?.takeIf { it.isAvailable() }?.level,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val tileModifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            BatteryTile(tileModifier, stringResource(R.string.left), leftRes, left)
            BatteryTile(tileModifier, stringResource(R.string.right), rightRes, right)
            BatteryTile(tileModifier, stringResource(R.string.case_alt), caseRes, case)
        }
    }
}

@Composable
private fun BatteryHero(
    budsRes: Int,
    level: Int?,
    charging: Boolean,
    left: Int?,
    right: Int?,
) {
    val colors = MaterialTheme.colorScheme
    val cookie = remember { MaterialShapes.Cookie9Sided.normalized().toShape() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(colors.primaryContainer)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.buds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            if (charging) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceContainerLowest)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(BoltIcon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.charging),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .background(colors.primary.copy(alpha = 0.16f), cookie)
            )
            Image(
                bitmap = ImageBitmap.imageResource(budsRes),
                contentDescription = stringResource(R.string.buds),
                modifier = Modifier.size(144.dp)
            )
        }
        Text(
            text = percentText(level, 80.sp, 32.sp),
            color = colors.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 84.sp
        )
        if (left != null && right != null) {
            Text(
                text = stringResource(R.string.battery_left_right, left, right),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun BatteryTile(modifier: Modifier, label: String, imageRes: Int, battery: Battery?) {
    val colors = MaterialTheme.colorScheme
    val available = battery?.isAvailable() == true
    val level = battery?.level ?: 0
    val charging = available && battery.isCharging()
    val low = available && !charging && level <= LOW_BATTERY

    val container = when {
        charging -> colors.tertiaryContainer
        low -> colors.errorContainer
        else -> colors.surfaceBright
    }
    val content = when {
        charging -> colors.onTertiaryContainer
        low -> colors.onErrorContainer
        else -> colors.onSurface
    }
    val muted = if (charging || low) content else colors.onSurfaceVariant
    val bar = when {
        charging -> colors.onTertiaryContainer
        low -> colors.error
        else -> colors.primary
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = ImageBitmap.imageResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                alpha = if (available) 1f else 0.38f
            )
            Spacer(modifier = Modifier.weight(1f))
            if (charging) {
                Icon(
                    BoltIcon,
                    contentDescription = stringResource(R.string.charging),
                    tint = content,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = muted
        )
        Text(
            text = percentText(if (available) level else null, 30.sp, 15.sp),
            color = if (available) content else colors.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        LinearProgressIndicator(
            progress = { if (available) level / 100f else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = bar,
            trackColor = if (charging || low) content.copy(alpha = 0.24f) else colors.outlineVariant,
        )
    }
}

private fun Battery.isAvailable() = status != BatteryStatus.DISCONNECTED && level > 0

private fun Battery.isCharging() =
    status == BatteryStatus.CHARGING || status == BatteryStatus.OPTIMIZED_CHARGING

private fun percentText(level: Int?, numberSize: TextUnit, unitSize: TextUnit) = buildAnnotatedString {
    withStyle(SpanStyle(fontSize = numberSize)) { append(level?.toString() ?: "—") }
    if (level != null) withStyle(SpanStyle(fontSize = unitSize)) { append("%") }
}

private val BoltIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Bolt",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(13f, 2f)
            lineTo(4f, 14f)
            horizontalLineTo(11f)
            lineTo(10f, 22f)
            lineTo(19f, 10f)
            horizontalLineTo(12f)
            close()
        }
    }.build()
}
