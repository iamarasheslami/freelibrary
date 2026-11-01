package com.freelibrary.app.data.repository

import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.dao.BookShelfStateDao
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.domain.model.ReadingSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/** The reader's year on the Profile page: books finished this year and month, and the yearly goal. */
@Singleton
class ReadingGoalRepository
    @Inject
    constructor(
        private val shelfStateDao: BookShelfStateDao,
        private val settings: SettingsRepository,
        private val time: TimeProvider,
    ) {
        /** Follows every change. The year and month are fixed when collection starts, on the phone's own time zone. */
        val summary: Flow<ReadingSummary> =
            flow {
                val periods = readingPeriods(time.nowMillis(), TimeZone.getDefault())
                emitAll(
                    combine(
                        shelfStateDao.observeFinishedBetween(ShelfState.FINISHED, periods.yearStart, periods.nextYearStart),
                        shelfStateDao.observeFinishedBetween(ShelfState.FINISHED, periods.monthStart, periods.nextMonthStart),
                        settings.yearlyGoal,
                    ) { thisYear, thisMonth, goal ->
                        ReadingSummary(periods.year, thisYear, thisMonth, goal)
                    },
                )
            }

        /** Saves the yearly goal, or removes it when [goal] is null. */
        suspend fun setGoal(goal: Int?) {
            settings.setYearlyGoal(goal)
        }
    }
