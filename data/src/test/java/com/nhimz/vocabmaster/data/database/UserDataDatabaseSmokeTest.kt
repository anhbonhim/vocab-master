package com.nhimz.vocabmaster.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Ignore("Robolectric Conscrypt native library is unavailable on this Termux aarch64 environment.")
class UserDataDatabaseSmokeTest {

    private var database: UserDataDatabase? = null
    private var userDataDao: UserDataDao? = null

    @Before
    fun setup() {
        try {
            database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                UserDataDatabase::class.java
            ).allowMainThreadQueries().build()
            userDataDao = database?.userDataDao()
        } catch (e: UnsatisfiedLinkError) {
            println("Skipping Room SQLite test on Termux due to UnsatisfiedLinkError: ${e.message}")
        }
    }

    @After
    fun teardown() {
        database?.close()
    }

    @Test
    fun testInsertAndGetCard() = runTest {
        val dao = userDataDao ?: return@runTest
        try {
            val card = FsrsCardEntity(
                questionId = "q1",
                due = 0L,
                stability = null,
                difficulty = null,
                step = 0,
                state = State.New.value,
                lastReview = null,
                reps = 0,
                lapses = 0
            )
            dao.insertCard(card)
            val read = dao.getCardByQuestionId("q1")
            assertNotNull(read)
            assertEquals("q1", read?.questionId)
        } catch (e: UnsatisfiedLinkError) {
            println("Skipping Room SQLite test on Termux due to UnsatisfiedLinkError: ${e.message}")
        }
    }
}
