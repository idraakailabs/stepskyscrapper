package ai.idraak.stepskyscraper.core.datastore

import ai.idraak.stepskyscraper.core.domain.model.PassPlan
import ai.idraak.stepskyscraper.core.domain.model.PlanOffer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The paywall's cached plans survive a round trip and never throw on bad data. */
class OffersCacheTest {
    private val offers = listOf(
        PlanOffer(PassPlan.Annual, "$14.99", 14_990_000, "USD", 7),
        PlanOffer(PassPlan.Monthly, "Rs 1,099.00", 1_099_000_000, "PKR", 0),
        PlanOffer(PassPlan.Lifetime, "29,99 €", 29_990_000, "EUR", 0),
    )

    @Test
    fun roundTrip() {
        assertThat(EntitlementCacheStore.decodeOffers(EntitlementCacheStore.encodeOffers(offers))).isEqualTo(offers)
    }

    @Test
    fun emptyOrMalformedDecodesToNothing() {
        assertThat(EntitlementCacheStore.decodeOffers(null)).isEmpty()
        assertThat(EntitlementCacheStore.decodeOffers("")).isEmpty()
        assertThat(EntitlementCacheStore.decodeOffers("Weekly\u001F$1\u001F1\u001FUSD\u001F0")).isEmpty()
        assertThat(EntitlementCacheStore.decodeOffers("garbage")).isEmpty()
    }
}
