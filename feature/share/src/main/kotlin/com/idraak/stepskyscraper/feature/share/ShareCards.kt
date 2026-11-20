package com.idraak.stepskyscraper.feature.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idraak.stepskyscraper.core.designsystem.component.BrandMark
import com.idraak.stepskyscraper.core.designsystem.theme.SkyTokens
import com.idraak.stepskyscraper.core.designsystem.theme.SkyTone
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import com.idraak.stepskyscraper.core.designsystem.tower.SkylineCanvas
import com.idraak.stepskyscraper.core.designsystem.tower.SkylineSpec
import com.idraak.stepskyscraper.core.designsystem.tower.SkylineTower
import com.idraak.stepskyscraper.core.designsystem.tower.TowerCanvas
import com.idraak.stepskyscraper.core.designsystem.tower.TowerSpec
import com.idraak.stepskyscraper.core.designsystem.tower.TowerTime
import com.idraak.stepskyscraper.core.ui.format.Formats

enum class ShareSubject { Tower, Skyline }

enum class ShareFormat { Story, Square }

@Immutable
data class TowerCardData(
    val name: String,
    val floors: Int,
    val kitName: String,
    val month: String,
    val steps: Long,
    val buildDays: Int,
    val spec: TowerSpec,
)

@Immutable
data class SkylineCardData(val name: String, val towers: List<SkylineTower>, val floors: Int, val steps: Long, val since: String)

/** Card geometry in "design dp": stories are laid out at 270 × 480, squares at 300 × 300 (as on the board). */
object ShareCardSize {
    val Story = 270.dp to 480.dp
    val Square = 300.dp to 300.dp
    const val EXPORT_STORY_W = 1_080
    const val EXPORT_STORY_H = 1_920
    const val EXPORT_SQUARE = 1_080
}

/**
 * Renders a share card at its design size; [scale] lets the composer show it smaller (8a) and the exporter
 * render it at exactly 1080 px wide by overriding density. Text sits on solid sky, never on the tower;
 * the top 14% and bottom 10% of stories stay clear of the Stories UI.
 */
@Composable
fun ShareCard(
    subject: ShareSubject,
    format: ShareFormat,
    showStats: Boolean,
    tower: TowerCardData?,
    skyline: SkylineCardData?,
    modifier: Modifier = Modifier,
) {
    val (w, h) = if (format == ShareFormat.Story) ShareCardSize.Story else ShareCardSize.Square
    BoxWithConstraints(modifier) {
        val scale = maxWidth / w
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, 1f)) {
            Box(Modifier.size(w, h)) {
                when {
                    subject == ShareSubject.Tower && tower != null && format == ShareFormat.Story -> StoryTower(tower, showStats)
                    subject == ShareSubject.Tower && tower != null -> SquareTower(tower, showStats)
                    skyline != null && format == ShareFormat.Story -> StorySkyline(skyline, showStats)
                    skyline != null -> SquareSkyline(skyline, showStats)
                }
            }
        }
    }
}

private val Light = Color(0xFFFBFAF7)
private val NightInk = Color(0xFFF1ECE5)
private val NightSub = Color(0xFFC3CADB)
private val DayInk = Color(0xFF24211D)

@Composable
private fun names(size: Int): TextStyle = SsTheme.type.towerNameHeader.copy(fontSize = size.sp, lineHeight = size.sp)

@Composable
private fun meta(): TextStyle = SsTheme.type.microStrong.copy(letterSpacing = 0.6.sp)

@Composable
private fun Mark(color: Color, oneLine: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        BrandMark(color)
        Text(
            stringResource(if (oneLine) R.string.card_brand_one_line else R.string.card_brand),
            style = SsTheme.type.micro.copy(fontSize = 9.sp, lineHeight = 10.8.sp),
            color = color,
        )
    }
}

@Composable
private fun StoryTower(t: TowerCardData, stats: Boolean) {
    Box(Modifier.fillMaxSize().background(SkyTokens.gradient(SkyTone.Dusk).brush())) {
        Column(Modifier.padding(start = 20.dp, top = 26.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(t.name.uppercase(), style = names(34), color = Light, maxLines = 1)
            Text(stringResource(R.string.card_floors_style_month, t.floors, t.kitName, t.month).uppercase(), style = meta(), color = Light)
        }
        TowerCanvas(t.spec.copy(time = TowerTime.Dusk), t.name, Modifier.offset(30.dp, 92.dp).size(210.dp, 310.dp))
        Row(Modifier.align(Alignment.BottomStart).padding(20.dp).width(230.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (stats) {
                    Text(
                        stringResource(R.string.card_steps, Formats.steps(t.steps)),
                        style = SsTheme.type.numberXS.copy(fontSize = 20.sp),
                        color = Light,
                        maxLines = 1,
                    )
                    Text(stringResource(R.string.card_in_days, t.buildDays), style = SsTheme.type.micro, color = Light)
                }
            }
            Mark(Light, oneLine = false)
        }
    }
}

@Composable
private fun StorySkyline(s: SkylineCardData, stats: Boolean) {
    Box(Modifier.fillMaxSize().background(SkyTokens.gradient(SkyTone.Night).brush())) {
        Column(Modifier.padding(start = 20.dp, top = 26.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.card_my_district).uppercase(), style = meta(), color = NightSub)
            Text(s.name.uppercase(), style = names(34), color = NightInk, maxLines = 1)
            Text(stringResource(R.string.card_district_meta, s.towers.size, s.floors).uppercase(), style = meta(), color = NightSub)
        }
        SkylineCanvas(SkylineSpec(s.towers, time = TowerTime.Night), s.name, Modifier.offset(8.dp, 150.dp).size(254.dp, 230.dp))
        Row(Modifier.align(Alignment.BottomStart).padding(20.dp).width(230.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (stats) {
                    Text(
                        stringResource(R.string.card_steps, Formats.steps(s.steps)),
                        style = SsTheme.type.numberXS.copy(fontSize = 20.sp),
                        color = NightInk,
                        maxLines = 1,
                    )
                    Text(stringResource(R.string.card_since, s.since), style = SsTheme.type.micro, color = NightSub)
                }
            }
            Mark(NightInk, oneLine = false)
        }
    }
}

@Composable
private fun SquareTower(t: TowerCardData, stats: Boolean) {
    val sub = Color(0xFF3E4A55)
    Box(Modifier.fillMaxSize().background(SkyTokens.gradient(SkyTone.Day).brush())) {
        TowerCanvas(t.spec.copy(time = TowerTime.Day), t.name, Modifier.offset(150.dp, 14.dp).size(136.dp, 272.dp))
        Column(Modifier.offset(20.dp, 22.dp).width(140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.name.uppercase(), style = names(32).copy(lineHeight = 30.sp), color = DayInk)
            Text(stringResource(R.string.card_floors_style, t.floors, t.kitName).uppercase(), style = meta(), color = sub)
        }
        if (stats) {
            Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 56.dp)) {
                Text(Formats.steps(t.steps), style = SsTheme.type.numberXS, color = DayInk)
                Text(stringResource(R.string.card_steps_days, t.buildDays), style = SsTheme.type.micro, color = sub)
            }
        }
        Mark(DayInk, oneLine = true, Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 18.dp))
    }
}

@Composable
private fun SquareSkyline(s: SkylineCardData, stats: Boolean) {
    val sub = Color(0xFF6B4A3A)
    Box(Modifier.fillMaxSize().background(SkyTokens.gradient(SkyTone.Dawn).brush())) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(s.name.uppercase(), style = names(28), color = DayInk, maxLines = 1)
            Text(
                stringResource(R.string.card_towers, s.towers.size).uppercase() +
                    if (stats) " · " + stringResource(R.string.card_steps, Formats.steps(s.steps)).uppercase() else "",
                style = meta(),
                color = sub,
            )
        }
        SkylineCanvas(SkylineSpec(s.towers, time = TowerTime.Dawn), s.name, Modifier.offset(10.dp, 78.dp).size(280.dp, 184.dp))
        Mark(DayInk, oneLine = true, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 14.dp))
    }
}
