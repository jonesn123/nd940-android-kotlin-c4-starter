package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.FakeDataSource.Companion.ERROR_MESSAGE
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects
    //TODO: Add testing implementation to the RemindersLocalRepository.kt
    private val reminder1 = ReminderDTO(
        "golden_gate_bridge title",
        "golden_gate_bridge desc",
        "golden_gate_bridge",
        37.819927,
        -122.478256
    )
    private val reminder2 = ReminderDTO(
        "ferry_building title",
        "ferry_building desc",
        "ferry_building",
        37.795490,
        -122.394276
    )
    private val reminder3 =
        ReminderDTO("pier_39 title", "pier_39 desc", "pier_39", 37.808674, -122.409821)

    private val reminders = listOf(reminder1, reminder2, reminder3).sortedBy { it.id }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var remindersListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the view model.
    private lateinit var fakeDataSource: FakeDataSource

    @Before
    fun setUpReminderListViewModel() {
        // Initialise the repository with no tasks.
        fakeDataSource = FakeDataSource(reminders as MutableList<ReminderDTO>)

        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadReminder_setReminderList() {
        remindersListViewModel.loadReminders()
        val items = reminders.map { reminder ->
            ReminderDataItem(
                reminder.title,
                reminder.description,
                reminder.location,
                reminder.latitude,
                reminder.longitude,
                reminder.id
            )
        }
        assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue(),
            `is`<List<ReminderDataItem>>(items)
        )
    }

    @Test
    fun loadReminders_loading() = mainCoroutineRule.runBlockingTest {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminder in the viewmodel.
        remindersListViewModel.loadReminders()

        // Then assert that the progress indicator is shown.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_ReturnException() = mainCoroutineRule.runBlockingTest {
        fakeDataSource.setReturnError(true)
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`(ERROR_MESSAGE))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun invalidateShowNoData_EmptyRemindersList() {
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun invalidateShowNoData_NonEmptyRemindersList() = mainCoroutineRule.runBlockingTest {
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }
}