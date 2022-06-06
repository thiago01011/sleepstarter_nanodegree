/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    private var _showSnackbasEvent = MutableLiveData<Boolean>()
    val showSnackBarEvent: LiveData<Boolean> = _showSnackbasEvent

    fun doneShowingSnackbar() {
        _showSnackbasEvent.value = false
    }

    private var tonight = MutableLiveData<SleepNight?>()
    val nights = database.getAllNights()

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight> = _navigateToSleepQuality

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    val startButtonVisible = Transformations.map(tonight) { nights ->
        nights == null
    }

    val stopButtonVisible = Transformations.map(tonight) { nights ->
        nights != null
    }

    val clearButtonVisible = Transformations.map(nights) { nights ->
        nights?.isNotEmpty()
    }

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        viewModelScope.launch(Dispatchers.IO) {
            tonight.postValue(getTonightFromDatabase())
        }
    }

    private fun getTonightFromDatabase(): SleepNight? {
        var night = database.getTonight()

        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }

    fun onStartTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val newNight = SleepNight()
            insert(newNight)
            tonight.postValue(getTonightFromDatabase())
        }
    }

    private suspend fun insert(night: SleepNight) {
        database.insert(night)
    }

    fun onStopTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            update(oldNight)
            Log.i("GALOG", "SleepTrackerViewModel: oldNight= ${oldNight.nightId}")
            try {
                _navigateToSleepQuality.postValue(oldNight)
            }catch (e: Exception) {
                Log.i("GALOG", "SleepTrackerViewModel: ERROR")
                e.printStackTrace()
            }
        }
    }

    private fun update(night: SleepNight) {
        database.update(night)
    }

    fun onClear() {
        viewModelScope.launch(Dispatchers.IO) {
            clear()
            tonight.postValue(null)
            _showSnackbasEvent.postValue(true)
        }
    }

    private fun clear() {
        database.clear()
    }

    private val _navigateToSleepDataQuality = MutableLiveData<Long>()
    val navigateToSleepDataQuality
        get() = _navigateToSleepDataQuality

    fun onSleepNightClicked(id: Long) {
        _navigateToSleepDataQuality.value = id
    }
    fun onSleepDataQualityNavigated() {
        _navigateToSleepDataQuality.value = null
    }
}
