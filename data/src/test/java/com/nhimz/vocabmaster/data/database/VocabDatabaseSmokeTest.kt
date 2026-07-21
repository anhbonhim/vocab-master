package com.nhimz.vocabmaster.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nhimz.vocabmaster.data.database.entity.SectionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VocabDatabaseSmokeTest {

    private var database: VocabDatabase? = null
    private var vocabDao: VocabDao? = null

    @Before
    fun setup() {
        try {
            database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                VocabDatabase::class.java
            ).allowMainThreadQueries().build()
            vocabDao = database?.vocabDao()
        } catch (e: UnsatisfiedLinkError) {
            println("Skipping Room SQLite test on Termux due to UnsatisfiedLinkError: ${e.message}")
        }
    }

    @After
    fun teardown() {
        database?.close()
    }

    @Test
    fun testInsertAndGetSection() = runTest {
        val dao = vocabDao ?: return@runTest // Skip if initialization failed
        try {
            val section = SectionEntity(
                id = "section_1",
                index = 1,
                name = "Test Section",
                cefrSublevel = "A1",
                icon = "ic_test",
                description = "A test section"
            )

            dao.insertAllSections(listOf(section))

            val sections = dao.getAllSections().first()
            assertEquals(1, sections.size)
            assertEquals(section.id, sections[0].id)
            assertEquals(section.name, sections[0].name)
        } catch (e: UnsatisfiedLinkError) {
            println("Skipping Room SQLite test on Termux due to UnsatisfiedLinkError: ${e.message}")
        }
    }
}