package app.lusk.underseerr.domain.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SubscriptionStatusTest : StringSpec({

    "every tier except FREE can use the hosted relay" {
        SubscriptionStatus(SubscriptionTier.FREE).hasHostedAccess shouldBe false
        SubscriptionStatus(SubscriptionTier.TRIAL).hasHostedAccess shouldBe true
        SubscriptionStatus(SubscriptionTier.PREMIUM).hasHostedAccess shouldBe true
        SubscriptionStatus(SubscriptionTier.COMPLIMENTARY).hasHostedAccess shouldBe true
    }

    "complimentary access is not premium" {
        SubscriptionStatus(SubscriptionTier.COMPLIMENTARY).isPremium shouldBe false
    }
})
