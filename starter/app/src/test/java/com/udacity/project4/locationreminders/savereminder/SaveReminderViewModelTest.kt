package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    //TODO: provide testing to the SaveReminderView and its live data objects
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Use a fake repository to be injected into the view model.
    private lateinit var fakeDataSource: FakeDataSource

    @Before
    fun setupSaveReminderViewModel() {
        // Initialise the repository with no tasks.
        fakeDataSource = FakeDataSource()

        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun saveReminder_setsNewReminder() {
        val reminder = ReminderDataItem(
            "golden_gate_bridge title",
            "golden_gate_bridge desc",
            "golden_gate_bridge",
            37.819927,
            -122.478256,
            "1"
        )
        saveReminderViewModel.saveReminder(
            reminder
        )

        assertThat(
            saveReminderViewModel.showToast.getOrAwaitValue(),
            `is`("Reminder Saved !")
        )
        assertThat(
            saveReminderViewModel.navigationCommand.getOrAwaitValue(),
            `is`<NavigationCommand>(NavigationCommand.Back)
        )
    }

    @Test
    fun validateEnteredData_validate() {
        val titleNull = ReminderDataItem(
            null,
            "golden_gate_bridge desc",
            "golden_gate_bridge",
            37.819927,
            -122.478256,
            "1"
        )

        val validateTitle = saveReminderViewModel.validateEnteredData(titleNull)
        assertThat(validateTitle, `is`(false))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )

        val locationNull = ReminderDataItem(
            "golden_gate_bridge title",
            "golden_gate_bridge desc",
            null,
            37.819927,
            -122.478256,
            "1"
        )

        val validateLocation = saveReminderViewModel.validateEnteredData(locationNull)
        assertThat(validateLocation, `is`(false))
    }

}