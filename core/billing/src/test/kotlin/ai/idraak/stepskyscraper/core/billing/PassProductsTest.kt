package ai.idraak.stepskyscraper.core.billing

import ai.idraak.stepskyscraper.core.domain.model.PassPlan
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PassProductsTest {
    @Test
    fun playProductsMapToTheirPlans() {
        assertThat(PassProducts.planFor("architect_pass_lifetime", null)).isEqualTo(PassPlan.Lifetime)
        assertThat(PassProducts.planFor("architect_pass", "monthly")).isEqualTo(PassPlan.Monthly)
        assertThat(PassProducts.planFor("architect_pass:monthly", null)).isEqualTo(PassPlan.Monthly)
        assertThat(PassProducts.planFor("architect_pass", "annual")).isEqualTo(PassPlan.Annual)
    }

    @Test
    fun testStoreProductsMapToTheirPlans() {
        assertThat(PassProducts.planFor("pass_lifetime", null)).isEqualTo(PassPlan.Lifetime)
        assertThat(PassProducts.planFor("pass_monthly", null)).isEqualTo(PassPlan.Monthly)
        assertThat(PassProducts.planFor("pass_yearly", null)).isEqualTo(PassPlan.Annual)
    }
}
