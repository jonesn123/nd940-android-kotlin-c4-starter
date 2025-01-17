package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private var reminders: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

    //    TODO: Create a fake data source to act as a double to the real data source
    private var shouldReturnError = false

    companion object {
        val ERROR_MESSAGE = "Could not get reminders"
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if(shouldReturnError) {
            return Result.Error(ERROR_MESSAGE)
        }
        reminders?.let {
            return Result.Success(ArrayList(it))
        }
        return Result.Error(
            "reminders is not found"
        )
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error(ERROR_MESSAGE)
        }
        reminders?.find { it.id == id }?.let { return Result.Success(it) }
        return Result.Error(
            "reminder is not found"
        )

    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }


    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }
}