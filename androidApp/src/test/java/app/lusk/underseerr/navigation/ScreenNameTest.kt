package app.lusk.underseerr.navigation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ScreenNameTest : StringSpec({
    val prefix = "app.lusk.underseerr.navigation.Screen"

    "objects map to a snake_case name" {
        screenNameFor("$prefix.Home") shouldBe "home"
        screenNameFor("$prefix.ServerManagement") shouldBe "server_management"
        screenNameFor("$prefix.VibrantCustomization") shouldBe "vibrant_customization"
    }

    "route arguments are dropped so IDs never reach analytics" {
        screenNameFor("$prefix.MediaDetails/{mediaType}/{mediaId}?openRequest={openRequest}") shouldBe "media_details"
        screenNameFor("$prefix.PlexAuthCallback/{token}") shouldBe "plex_auth_callback"
        screenNameFor("$prefix.RequestDetails/42") shouldBe "request_details"
    }

    "a missing or blank route yields no screen" {
        screenNameFor(null) shouldBe null
        screenNameFor("") shouldBe null
    }
})
