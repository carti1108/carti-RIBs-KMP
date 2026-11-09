package com.carti.ribs

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SmokeTest {
    @Test
    fun coroutineTestRunsOnEveryTarget() = runTest {
        assertTrue(Ribs.VERSION.isNotEmpty())
    }
}
