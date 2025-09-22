package com.egan.core

import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.assertTrue

class AndroidCoreTest {
    private lateinit var core: ICore

    @Before
    fun setup() {
        core = mock()
    }

    @Test
    fun coreOperationShouldReturnFalse() = runTest {
        assertTrue { core.operation("test") }
    }
}