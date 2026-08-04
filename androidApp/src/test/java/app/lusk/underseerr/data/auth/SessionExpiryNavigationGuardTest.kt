package app.lusk.underseerr.data.auth

import android.app.Application
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavType
import androidx.navigation.Navigator
import app.lusk.underseerr.navigation.SIGN_IN_DESTINATIONS
import app.lusk.underseerr.navigation.Screen
import app.lusk.underseerr.navigation.isSignInDestination
import app.lusk.underseerr.navigation.shouldRouteToSignIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * The nav host reacts to an involuntary sign-out by routing to [Screen.PlexAuth].
 * The whole of that decision is [shouldRouteToSignIn]; everything around it in
 * `UnderseerrNavHost` is the Compose plumbing that feeds it. Testing the predicate
 * against *real* [NavDestination]s — built through the same type-safe route
 * machinery the graph uses — exercises the `hasRoute` matching for real, without
 * needing a Compose runtime, a device, or a Koin graph.
 *
 * Runs under Robolectric (hence JUnit 4, via the vintage engine): building a
 * type-safe destination parses its generated deep link through `android.net.Uri`,
 * which the stub android.jar answers with null. A stock [Application] is pinned so
 * Robolectric does not boot the real one, whose `startKoin` is not idempotent
 * across the per-method environments Robolectric creates.
 *
 * The regressions this catches:
 *  - an expiry arriving while the user is mid-sign-in wiping the back stack;
 *  - a new pre-auth screen being added without being added to the guard;
 *  - a post-auth screen accidentally landing in the guard, which would silently
 *    disable the whole redirect and put us back to the original bug (user stuck
 *    on a screen where every request fails).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SessionExpiryNavigationGuardTest {

    @Navigator.Name("test")
    private class StubNavigator : Navigator<NavDestination>() {
        override fun createDestination(): NavDestination = NavDestination(this)
    }

    private fun destinationFor(route: KClass<out Screen>): NavDestination =
        NavDestinationBuilder(StubNavigator(), route, emptyMap<KType, NavType<*>>()).build()

    private val signInRoutes = listOf(
        Screen.PlexAuth::class,
        Screen.PlexAuthCallback::class,
        Screen.ServerConfig::class,
        Screen.Splash::class
    )

    private val postAuthRoutes = listOf(
        Screen.MainTabs::class,
        Screen.Home::class,
        Screen.Requests::class,
        Screen.Issues::class,
        Screen.Profile::class,
        Screen.Search::class,
        Screen.MediaDetails::class,
        Screen.RequestDetails::class,
        Screen.IssueDetails::class,
        Screen.Settings::class,
        Screen.ServerManagement::class,
        Screen.About::class,
        Screen.PersonDetails::class,
        Screen.CategoryResults::class,
        Screen.VibrantCustomization::class
    )

    // --- the guard set itself -------------------------------------------------

    @Test
    fun `the guard covers exactly the sign-in flow`() {
        assertEquals(signInRoutes.toSet(), SIGN_IN_DESTINATIONS.toSet())
    }

    @Test
    fun `no post-auth destination is treated as sign-in`() {
        postAuthRoutes.forEach { route ->
            assertFalse(
                "$route is in the guard, which would disable the expiry redirect there",
                SIGN_IN_DESTINATIONS.contains(route)
            )
        }
    }

    // --- destination matching -------------------------------------------------

    @Test
    fun `each sign-in destination is recognised`() {
        signInRoutes.forEach { route ->
            assertTrue(
                "$route should be recognised as part of the sign-in flow",
                isSignInDestination(destinationFor(route))
            )
        }
    }

    @Test
    fun `post-auth destinations are not recognised as sign-in`() {
        postAuthRoutes.forEach { route ->
            assertFalse(
                "$route should not be treated as part of the sign-in flow",
                isSignInDestination(destinationFor(route))
            )
        }
    }

    @Test
    fun `a null destination is not sign-in`() {
        assertFalse(isSignInDestination(null))
    }

    // --- the predicate --------------------------------------------------------

    @Test
    fun `no expiry means no navigation regardless of where the user is`() {
        (signInRoutes + postAuthRoutes).forEach { route ->
            assertFalse(
                "A null reason must never navigate (was at $route)",
                shouldRouteToSignIn(null, destinationFor(route))
            )
        }
        assertFalse(shouldRouteToSignIn(null, null))
    }

    @Test
    fun `an expiry on any post-auth screen routes to sign-in`() {
        SessionExpiryReason.entries.forEach { reason ->
            postAuthRoutes.forEach { route ->
                assertTrue(
                    "$reason on $route should route the user to sign-in",
                    shouldRouteToSignIn(reason, destinationFor(route))
                )
            }
        }
    }

    @Test
    fun `an expiry while already in the sign-in flow does not re-navigate`() {
        SessionExpiryReason.entries.forEach { reason ->
            signInRoutes.forEach { route ->
                assertFalse(
                    "$reason on $route must not restart the sign-in flow",
                    shouldRouteToSignIn(reason, destinationFor(route))
                )
            }
        }
    }

    /**
     * Pins current behaviour: an expiry that lands before the graph has resolved a
     * destination still routes rather than being dropped. If that is ever changed
     * to "wait", this test should change with it deliberately.
     */
    @Test
    fun `an expiry with no resolved destination yet still routes`() {
        SessionExpiryReason.entries.forEach { reason ->
            assertTrue(shouldRouteToSignIn(reason, null))
        }
    }
}
