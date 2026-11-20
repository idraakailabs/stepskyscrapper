package ai.idraak.stepskyscraper.widget

import ai.idraak.stepskyscraper.core.designsystem.tower.GroundStyle
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerFit
import ai.idraak.stepskyscraper.core.designsystem.tower.renderTowerBitmap
import ai.idraak.stepskyscraper.core.domain.repository.SyncListener
import ai.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.SkyTime
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.tower.toSpec
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun progress(): GetTowerProgressUseCase
}

/** What both widgets show; computed on the device, nothing leaves it. */
internal data class WidgetModel(
    val name: String,
    val nextFloor: Int,
    val stepsToNext: Int,
    val segments: Int,
    val floorsToday: Int,
    val week: Int,
    val weekGoal: Int,
    val tower: Bitmap,
)

private object WidgetColors {
    val surface = ColorProvider(Color(0xFFFBFAF7), Color(0xFF201D1A))
    val ink = ColorProvider(Color(0xFF24211D), Color(0xFFF1ECE5))
    val sub = ColorProvider(Color(0xFF6A635A), Color(0xFFADA59B))
    val body = ColorProvider(Color(0xFF4A443D), Color(0xFFADA59B))
    val fill = ColorProvider(Color(0xFFE8A23A), Color(0xFFF0B455))
    val empty = ColorProvider(Color(0xFFECE7E0), Color(0xFF3A3530))
}

private fun Context.isNight(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

private suspend fun load(context: Context, small: Boolean): WidgetModel? {
    val progress = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).progress()
    val s = progress().first()
    if (!s.settings.onboardingComplete) return null
    val t = s.city.activeTower
    val night = context.isNight()
    val density = context.resources.displayMetrics.density
    val spec = t.toSpec(
        if (night && !small) SkyTime.Night else SkyTime.Day,
        crane = !small,
        progress = s.todayProgress.stepsIntoFloor / 1_000f,
        showGround = false,
    ).let { if (small) it.copy(mono = if (night) 0xCFC7BC else 0x3A3530, ground = GroundStyle.None) else it }
    val (w, h) = if (small) 62 to 96 else 104 to 138
    val bitmap = renderTowerBitmap(spec, (w * density).toInt(), (h * density).toInt(), density, TowerFit(pad = 2f)).asAndroidBitmap()
    return WidgetModel(
        t.name,
        t.floorsBuilt + 1,
        s.todayProgress.stepsToNextFloor,
        s.todayProgress.segmentsFilled,
        s.todayProgress.floors,
        s.week.floors,
        s.week.goalFloors,
        bitmap,
    )
}

private fun openApp(context: Context) = context.packageManager.getLaunchIntentForPackage(context.packageName)

@Composable
private fun Segments(filled: Int) {
    Row(GlanceModifier.fillMaxWidth().height(6.dp)) {
        repeat(10) { i ->
            Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(end = if (i < 9) 2.dp else 0.dp)) {
                Box(
                    GlanceModifier.fillMaxSize().cornerRadius(2.dp).background(
                        if (i <
                            filled
                        ) {
                            WidgetColors.fill
                        } else {
                            WidgetColors.empty
                        },
                    ),
                ) {}
            }
        }
    }
}

/** Small 2×2: single-colour silhouette so it sits quietly on any wallpaper. */
class SmallTowerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = load(context, small = true)
        val launch = openApp(context)
        provideContent {
            val root = GlanceModifier.fillMaxSize().cornerRadius(
                24.dp,
            ).background(WidgetColors.surface).padding(horizontal = 14.dp, vertical = 12.dp)
                .let { m -> launch?.let { m.clickable(actionStartActivity(it)) } ?: m }
            if (model == null) {
                Box(root, contentAlignment = Alignment.Center) {
                    Text(context.getString(R.string.widget_setup), style = TextStyle(color = WidgetColors.ink, fontSize = 13.sp))
                }
                return@provideContent
            }
            Box(
                root.semantics {
                    contentDescription =
                        context.getString(R.string.widget_small_cd, model.name, Formats.steps(model.stepsToNext))
                },
            ) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                    Image(ImageProvider(model.tower), null, GlanceModifier.width(62.dp).height(96.dp))
                }
                Column(GlanceModifier.fillMaxSize()) {
                    Text(
                        context.getString(R.string.widget_floor, model.nextFloor),
                        style = TextStyle(color = WidgetColors.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        Formats.steps(model.stepsToNext),
                        style = TextStyle(color = WidgetColors.ink, fontSize = 36.sp, fontWeight = FontWeight.Medium),
                    )
                    Text(context.getString(R.string.widget_to_next), style = TextStyle(color = WidgetColors.body, fontSize = 12.sp))
                    Spacer(GlanceModifier.height(8.dp))
                    Segments(model.segments)
                }
            }
        }
    }
}

/** Medium 4×2: sky panel with the tower and crane, name, next-floor number, today and week. */
class MediumTowerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = load(context, small = false)
        val launch = openApp(context)
        val night = context.isNight()
        provideContent {
            val root = GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(WidgetColors.surface)
                .let { m -> launch?.let { m.clickable(actionStartActivity(it)) } ?: m }
            if (model == null) {
                Box(root, contentAlignment = Alignment.Center) {
                    Text(context.getString(R.string.widget_setup), style = TextStyle(color = WidgetColors.ink, fontSize = 13.sp))
                }
                return@provideContent
            }
            Row(
                root.semantics {
                    contentDescription =
                        context.getString(
                            R.string.widget_medium_cd,
                            model.name,
                            model.floorsToday,
                            model.week,
                            model.weekGoal,
                            Formats.steps(model.stepsToNext),
                        )
                },
            ) {
                Box(
                    GlanceModifier.width(
                        116.dp,
                    ).fillMaxHeight().background(
                        ImageProvider(if (night) R.drawable.widget_sky_night else R.drawable.widget_sky_day),
                    ).padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) { Image(ImageProvider(model.tower), null, GlanceModifier.width(104.dp).height(138.dp)) }
                Column(GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        model.name.uppercase(),
                        style = TextStyle(color = WidgetColors.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            Formats.steps(model.stepsToNext),
                            style = TextStyle(color = WidgetColors.ink, fontSize = 30.sp, fontWeight = FontWeight.Medium),
                        )
                        Text(
                            " " + context.getString(R.string.widget_to_next),
                            style = TextStyle(color = WidgetColors.body, fontSize = 12.sp),
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Row(GlanceModifier.fillMaxWidth()) {
                        Text(
                            context.resources.getQuantityString(R.plurals.widget_floors_today, model.floorsToday, model.floorsToday),
                            style = TextStyle(color = WidgetColors.body, fontSize = 12.sp),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Text(
                            context.getString(R.string.widget_week, model.week, model.weekGoal),
                            style = TextStyle(color = WidgetColors.body, fontSize = 12.sp),
                        )
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    WeekBar(if (model.weekGoal == 0) 0f else model.week.toFloat() / model.weekGoal)
                }
            }
        }
    }
}

@Composable
private fun WeekBar(fraction: Float) {
    // Glance has no fractional widths: the bar is ten equal cells, filled to the nearest tenth.
    val filled = (fraction.coerceIn(0f, 1f) * 10).toInt()
    Row(GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp).background(WidgetColors.empty)) {
        repeat(10) { i ->
            Box(GlanceModifier.defaultWeight().fillMaxHeight().background(if (i < filled) WidgetColors.fill else WidgetColors.empty)) {}
        }
    }
}

class SmallTowerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallTowerWidget()
}

class MediumTowerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumTowerWidget()
}

/** Redraws both widgets after every sync (and so at most every 15–30 minutes in the background). */
@Singleton
class WidgetSyncListener @Inject constructor(@ApplicationContext private val context: Context) : SyncListener {
    override suspend fun onSynced() {
        SmallTowerWidget().updateAll(context)
        MediumTowerWidget().updateAll(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds @IntoSet
    abstract fun widgets(impl: WidgetSyncListener): SyncListener
}
