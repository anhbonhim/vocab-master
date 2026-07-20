# Testing Patterns

**Analysis Date:** 2026-07-20

## Test Framework

**Runner:**
- JUnit 4 is used for both unit tests (`testImplementation(libs.junit)`) and instrumented tests (`androidTestImplementation(libs.androidx.test.ext.junit)`).
- AndroidX Test Runner for instrumented tests.

**Assertion Library:**
- Likely standard JUnit assertions (based on typical setups).

**Run Commands:**
```bash
./gradlew test              # Run all unit tests
./gradlew connectedAndroidTest # Run all instrumented tests
```

## Test File Organization

**Location:**
- Unit tests are located in `app/src/test/java/...` mimicking the source package structure.
- Instrumented tests (if any) would be in `app/src/androidTest/java/...`.

**Naming:**
- Test classes append `Test` to the class being tested (e.g., `ScrambledWordMapperTest.kt`).
- Functions within the test class use the `@Test` annotation.

**Structure:**
```
app/src/test/java/com/nhimz/vocabmaster/
├── ui/
│   └── components/
│       └── quiz/
│           └── ScrambledWordMapperTest.kt
```

## Test Structure

**Suite Organization:**
```kotlin
package com.nhimz.vocabmaster.ui.components.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScrambledWordMapperTest {

    @Test
    fun testGenerateScrambledOptions_createsValidOptions() {
        // Arrange
        val word = "apple"
        // Act
        val options = ScrambledWordMapper.generateScrambledOptions(word)
        // Assert
        assertEquals(word.length, options.size)
        // ... more assertions
    }
}
```

**Patterns:**
- Standard Arrange-Act-Assert (AAA) pattern within test functions.
- Uses `@Test` annotation from `org.junit.Test`.

## Mocking

**Framework:**
- The provided code snippets don't explicitly show a mocking framework (like Mockito or MockK) in use for the existing small number of tests. However, standard Android testing often incorporates these. Wait for further implementation to confirm if one is standardized.

**Patterns:**
- Currently, tests seem to be testing pure functions (like a Mapper) that don't require heavy mocking.

## Fixtures and Factories

**Test Data:**
- Simple test data is created inline within the test functions (e.g., `val word = "apple"`).

**Location:**
- No dedicated fixture or factory files are currently evident in the small test suite.

## Coverage

**Requirements:** None explicitly enforced in the Gradle files provided.

**View Coverage:**
```bash
./gradlew testDebugUnitTestCoverage # (Standard gradle task if jacoco is configured, though not explicitly seen in build.gradle.kts)
```

## Test Types

**Unit Tests:**
- Scope: Testing individual utility classes or pure functions (e.g., `ScrambledWordMapperTest`).

**Instrumented / UI Tests:**
- Dependencies are present (`androidx.compose.ui.test.junit4`, `androidx.test.espresso.core`), indicating readiness for Compose UI tests and Espresso tests, though few/none may currently exist based on the file count.

## Common Patterns

**Coroutines Testing:**
- `testImplementation(libs.kotlinx.coroutines.test)` is present, indicating Coroutine testing is supported, likely using `runTest` from the `kotlinx-coroutines-test` library for ViewModels or Repository layers.

---

*Testing analysis: 2026-07-20*