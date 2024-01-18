package dk.itu.moapd.scootersharing.mhas

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dk.itu.moapd.scootersharing.mhas.fragments.MainFragment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class RecyclerViewInstrumentedTest {


    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("dk.itu.moapd.scootersharing.mhas", appContext.packageName)
    }

    @Test
    fun checkRecyclerViewDisplayedOnMainFragment() {

        launchFragmentInContainer<MainFragment>()
        onView(withId(R.id.list_rides_view)).check(matches(isDisplayed()))

    }

}
