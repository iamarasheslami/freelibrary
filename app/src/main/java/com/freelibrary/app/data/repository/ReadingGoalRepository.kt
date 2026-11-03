package com.freelibrary.app.data.repository

import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.dao.BookShelfStateDao
import com.freelibrary.app.data.local.dao.ReadingDayDao
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.domain.model.ReadingSummary
import com.freelibrary.app.domain.model.localDay
import com.freelibrary.app.domain.model.readingStreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/** The reader's year on the Profile page: books finished this year and month, the yearly goal and the reading streak. */
@Singleton
class ReadingGoalRepository
    @Inject
    constructor(
        private val shelfStateDao: BookShelfStateDao,
        private val readingDayDao: ReadingDayDao,
        private val settings: SettingsRepository,
        private val time: TimeProvider,
    ) {
        /** Follows every change. The year, month and today are fixed when collection starts, on the phone's own time zone. */
        val summary: Flow<ReadingSummary> =
            flow {
                val zone = TimeZone.getDefault()
                val now = time.nowMillis()
                val periods = readingPeriods(now, zone)
                val today = localDay(now, zone)
                emitAll(
                    combine(
                        shelfStateDao.observeFinishedBetween(ShelfState.FINISHED, periods.yearStart, periods.nextYearStart),
                        shelfStateDao.observeFinishedBetween(ShelfState.FINISHED, periods.monthStart, periods.nextMonthStart),
                        settings.yearlyGoal,
                        readingDayDao.observeDays(),
                    ) { thisYear, thisMonth, goal, days ->
                        ReadingSummary(periods.year, thisYear, thisMonth, goal, readingStreak(days, today))
                    },
                )
            }

        /** Saves the yearly goal, or removes it when [goal] is null. */
        suspend fun setGoal(goal: Int?) {
            settings.setYearlyGoal(goal)
        }
    }
