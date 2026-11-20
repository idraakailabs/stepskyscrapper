package ai.idraak.stepskyscraper.core.domain.model

import java.time.Instant

/** Architect Pass plans. Annual carries the 7-day free trial. */
enum class PassPlan { Monthly, Annual, Lifetime }

data class PlanOffer(
    val plan: PassPlan,
    val formattedPrice: String,
    val priceMicros: Long,
    val currencyCode: String,
    val freeTrialDays: Int,
)

data class Entitlement(
    val isPremium: Boolean,
    val plan: PassPlan?,
    /** When the cached entitlement was last confirmed with Google Play (offline cache). */
    val verifiedAt: Instant?,
) {
    companion object {
        val Free = Entitlement(isPremium = false, plan = null, verifiedAt = null)
    }
}

/** Marker for the UI host a purchase / ad flow needs (implemented by the Activity in :app). */
interface UiHost

enum class AdAvailability { Ready, Loading, Unavailable, ConsentRequired }

data class WalletEntry(
    val key: String,
    val amount: Int,
    val reason: WalletReason,
    val createdAt: Instant,
)

enum class WalletReason { ToppingOut, PremiumBonus, RewardedAd, Achievement, Purchase }
