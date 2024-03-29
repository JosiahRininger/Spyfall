package com.dangerfield.spyfall

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /*

    Similar to unit tests. They test android funcationality. These need to be run on an emulator ussually.
     libraries are the same for unit tests.


     */
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.dangerfield.spyfall", appContext.packageName)
    }


    /*
    UI Tests: tests user interation. expresso is the popular library here
     */
}
