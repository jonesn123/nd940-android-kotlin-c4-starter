package com.udacity.project4

import android.app.Application
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @get:Rule
    var activityRule = object : ActivityTestRule<RemindersActivity>(RemindersActivity::class.java) {
        override fun beforeActivityLaunched() {
            super.beforeActivityLaunched()
            Log.d(RemindersActivityTest::class.java.simpleName, "beforeActivityLaunched")
            stopKoin()
            val appContext = getApplicationContext<Application>()
            val myModule = module {
                viewModel {
                    RemindersListViewModel(
                        appContext,
                        get() as ReminderDataSource
                    )
                }
                single {
                    SaveReminderViewModel(
                        appContext,
                        get() as ReminderDataSource
                    )
                }
                single { RemindersLocalRepository(get()) as ReminderDataSource }
                single { LocalDB.createRemindersDao(appContext) }
            }
            startKoin {
                modules(myModule)
            }

            repository = get()
            runBlocking {
                repository.deleteAllReminders()
            }
        }
    }

    //    TODO: add End to End testing to the app
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    private fun getReminder(): ReminderDTO {
        return ReminderDTO(
            "golden_gate_bridge title",
            "golden_gate_bridge desc",
            "golden_gate_bridge",
            37.819927,
            -122.478256
        )
    }

    @Test
    fun createReminder_checkRemindersList() = runBlocking {
        // Create a reminder.
        val reminder = getReminder()
        repository.saveReminder(reminder)

        // Start a Reminder screen.
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)

        // Check the List of added reminders.
        onView(withText(reminder.title)).check(matches(isDisplayed()))
        onView(withText(reminder.description)).check(matches(isDisplayed()))
        onView(withText(reminder.location)).check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun saveReminder_showToast() {
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)
        val activity = activityRule.activity

        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        val saveReminderViewModel: SaveReminderViewModel = get()
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("Walking Around"), closeSoftKeyboard())
        onView(withId(R.id.reminderDescription)).perform(
            typeText("Buy Cool drinks"),
            closeSoftKeyboard()
        )
        saveReminderViewModel.latitude.postValue(20.0)
        saveReminderViewModel.longitude.postValue(20.0)
        saveReminderViewModel.reminderSelectedLocationStr.postValue("Giant Supermarket")

        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(`is`(activity.window.decorView))))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun createReminder_missingTitle() {
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)
        val saveReminderViewModel: SaveReminderViewModel = get()

        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderDescription)).perform(
            typeText("Buy Cool drinks"),
            closeSoftKeyboard()
        )
        saveReminderViewModel.reminderSelectedLocationStr.postValue("Giant Supermarket")
        saveReminderViewModel.latitude.postValue(20.0)
        saveReminderViewModel.longitude.postValue(20.0)

        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("Please enter title")))

        scenario.close()
    }

    @Test
    fun createReminder_missingLocation() {
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)

        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("Walking Around"), closeSoftKeyboard())
        onView(withId(R.id.reminderDescription)).perform(
            typeText("Buy Cool drinks"),
            closeSoftKeyboard()
        )

        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("Please select location")))

        scenario.close()
    }
}