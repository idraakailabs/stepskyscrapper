package ai.idraak.stepskyscraper.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The Material Symbols Rounded "sync" glyph (U+E627) as a vector, outline taken from the bundled font.
 *
 * Used when the icon spins: a font glyph sits inside a text line box whose metrics (ascent 1056 / descent 96
 * on a 960 em) never centre it exactly in its square, so rotating it wobbles. The vector's 960×960 viewport is
 * the glyph's em square, and the outline (160..800 on both axes) is centred in it by construction.
 */
internal val SyncVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "sync",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).addPath(
        pathData = PathParser().parsePathString(SYNC_PATH).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

private const val SYNC_PATH =
    "M240 482Q240 527 257 569.5Q274 612 310 648L320 658V600Q320 583 331.5 571.5Q343 560 360 560" +
        "Q377 560 388.5 571.5Q400 583 400 600V760Q400 777 388.5 788.5Q377 800 360 800H200" +
        "Q183 800 171.5 788.5Q160 777 160 760Q160 743 171.5 731.5Q183 720 200 720H270L254 706" +
        "Q202 660 181 601Q160 542 160 482Q160 388 208 311.5Q256 235 337 194Q351 186 366.5 193" +
        "Q382 200 387 216Q392 231 386.5 246Q381 261 367 269Q309 301 274.5 357.5Q240 414 240 482Z" +
        "M720 478Q720 433 703 390.5Q686 348 650 312L640 302V360Q640 377 628.5 388.5Q617 400 600 400" +
        "Q583 400 571.5 388.5Q560 377 560 360V200Q560 183 571.5 171.5Q583 160 600 160H760" +
        "Q777 160 788.5 171.5Q800 183 800 200Q800 217 788.5 228.5Q777 240 760 240H690L706 254" +
        "Q755 303 777.5 360.5Q800 418 800 478Q800 572 752 648.5Q704 725 623 766Q609 774 593.5 767" +
        "Q578 760 573 744Q568 729 573.5 714Q579 699 593 691Q651 659 685.5 602.5Q720 546 720 478Z"
