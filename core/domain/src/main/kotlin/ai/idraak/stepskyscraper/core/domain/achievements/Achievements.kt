package ai.idraak.stepskyscraper.core.domain.achievements

import java.time.LocalDate

/** Aggregated, derived numbers that achievements and unlock rules are evaluated against. */
data class CityStats(
    val totalFloors: Int,
    val towersFinished: Int,
    val buildDays: Int,
    val architectLevel: Int,
    val maxFloorsInOneDay: Int,
    val nightShiftDays: Int,
    val earlyRiserDays: Int,
    val weeksGoalReached: Int,
    val finishedKits: Int,
    val largestFinishedTower: Int,
    val fullestDistrictTowers: Int,
    val districtsFounded: Int,
    val shares: Int,
    val makeovers: Int,
    val pauseReturns: Int,
) {
    companion object {
        val Empty = CityStats(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0)
    }
}

sealed interface AchievementReward {
    data class Blueprints(val amount: Int) : AchievementReward

    data class Cosmetic(val itemId: String, val label: String) : AchievementReward
}

/** Every achievement is cumulative. None depends on consecutive days. */
data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val target: Int,
    val reward: AchievementReward,
    val measure: (CityStats) -> Int,
)

data class AchievementProgress(
    val definition: AchievementDefinition,
    val progress: Int,
    val earnedOn: LocalDate?,
) {
    val isEarned: Boolean get() = earnedOn != null
    val fraction: Float get() = (progress.toFloat() / definition.target).coerceIn(0f, 1f)
}

object AchievementCatalog {
    val all: List<AchievementDefinition> = listOf(
        def("first_floor", "First Floor", "Build your first floor", "foundation", 1, bp(25)) { it.totalFloors },
        def("topping_out", "Topping Out", "Finish your first tower", "flag", 1, bp(50)) { it.towersFinished },
        def("five_towers", "Five Towers", "Finish 5 towers", "location_city", 5, cos("ground_plaque", "District name plaque")) {
            it.towersFinished
        },
        def("century", "Century", "Build 100 floors", "counter_1", 100, bp(100)) { it.totalFloors },
        def("double_century", "Double Century", "Build 200 floors", "apartment", 200, cos("facade_copper_deco", "Copper Deco facade")) {
            it.totalFloors
        },
        def(
            "night_shift",
            "Night Shift",
            "Finish a floor after 9 pm on 10 days",
            "dark_mode",
            10,
            cos("lights_moonlit", "Moonlit windows"),
        ) {
            it.nightShiftDays
        },
        def(
            "full_district",
            "Full District",
            "Finish 8 towers in one district",
            "holiday_village",
            8,
            cos("ground_tram", "Tram stop + park"),
        ) {
            it.fullestDistrictTowers
        },
        def(
            "rest_is_building",
            "Rest Is Building Too",
            "Paused goals, then came back",
            "self_improvement",
            1,
            cos("crown_terrace", "Hammock roof terrace"),
        ) { it.pauseReturns },
        def("early_riser", "Early Riser", "Finish a floor before 8 am on 5 days", "wb_twilight", 5, cos("palette_dawn", "Dawn palette")) {
            it.earlyRiserDays
        },
        def("ten_floor_day", "Ten-Floor Day", "Build 10 floors in one day", "stairs", 10, bp(50)) { it.maxFloorsInOneDay },
        def("fifteen_floor_day", "High Rise Day", "Build 15 floors in one day", "directions_run", 15, bp(100)) { it.maxFloorsInOneDay },
        def("build_days_10", "Ten Build Days", "Build on 10 different days", "event_available", 10, bp(30)) { it.buildDays },
        def("build_days_30", "Thirty Build Days", "Build on 30 different days", "calendar_month", 30, bp(60)) { it.buildDays },
        def("build_days_100", "Hundred Build Days", "Build on 100 different days", "history", 100, bp(150)) { it.buildDays },
        def("week_goal_1", "Good Week", "Reach your weekly goal", "emoji_events", 1, bp(40)) { it.weeksGoalReached },
        def("week_goal_10", "Ten Good Weeks", "Reach your weekly goal in 10 weeks", "star", 10, bp(120)) { it.weeksGoalReached },
        def("first_makeover", "Makeover", "Customise a tower", "palette", 1, bp(20)) { it.makeovers },
        def("show_off", "Proud Builder", "Share a tower or skyline", "ios_share", 1, bp(20)) { it.shares },
        def("ten_towers", "Ten Towers", "Finish 10 towers", "domain", 10, bp(150)) { it.towersFinished },
        def("five_hundred", "Five Hundred", "Build 500 floors", "trending_up", 500, bp(200)) { it.totalFloors },
        def("thousand", "Thousand Floors", "Build 1,000 floors", "bolt", 1_000, bp(300)) { it.totalFloors },
        def("all_styles", "Every Style", "Finish a tower in each of the 5 styles", "explore", 5, bp(150)) { it.finishedKits },
        def("landmark", "Landmark", "Finish a 100-floor tower", "landscape", 100, bp(200)) { it.largestFinishedTower },
        def("second_district", "New District", "Found a second district", "park", 2, bp(100)) { it.districtsFounded },
    )

    private val byId = all.associateBy { it.id }

    fun definition(id: String): AchievementDefinition? = byId[id]

    private fun bp(amount: Int) = AchievementReward.Blueprints(amount)

    private fun cos(id: String, label: String) = AchievementReward.Cosmetic(id, label)

    private fun def(
        id: String,
        title: String,
        description: String,
        icon: String,
        target: Int,
        reward: AchievementReward,
        measure: (CityStats) -> Int,
    ) = AchievementDefinition(id, title, description, icon, target, reward, measure)
}
