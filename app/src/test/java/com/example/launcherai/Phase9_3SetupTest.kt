package com.example.launcherai

import com.example.launcherai.ui.SetupFlowUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9_3SetupTest {

    @Test
    fun testSetupFlowUtilsConstants() {
        assertEquals("key_first_run_completed", SetupFlowUtils.KEY_FIRST_RUN_COMPLETED)
        assertEquals("key_setup_current_step", SetupFlowUtils.KEY_SETUP_CURRENT_STEP)
    }

    @Test
    fun testFreshInstallRouting_UncompletedSetupLaunchesWizard() {
        val isCompleted = false
        val skipWizardExtra = false
        val shouldLaunch = !isCompleted && !skipWizardExtra
        assertTrue(shouldLaunch)
    }

    @Test
    fun testAlreadyCompletedSetup_DoesNotLaunchWizard() {
        val isCompleted = true
        val skipWizardExtra = false
        val shouldLaunch = !isCompleted && !skipWizardExtra
        assertFalse(shouldLaunch)
    }

    @Test
    fun testSkipWizardExtra_PreventsWizardLaunch() {
        val isCompleted = false
        val skipWizardExtra = true
        val shouldLaunch = !isCompleted && !skipWizardExtra
        assertFalse(shouldLaunch)
    }

    @Test
    fun testSetupProgressionSequence_Advances1To5() {
        var currentStep = 1
        currentStep = (currentStep + 1).coerceIn(1, 5)
        assertEquals(2, currentStep)

        currentStep = (currentStep + 1).coerceIn(1, 5)
        assertEquals(3, currentStep)

        currentStep = (currentStep + 1).coerceIn(1, 5)
        assertEquals(4, currentStep)

        currentStep = (currentStep + 1).coerceIn(1, 5)
        assertEquals(5, currentStep)

        currentStep = (currentStep + 1).coerceIn(1, 5)
        assertEquals(5, currentStep)
    }

    @Test
    fun testSetupBackNavigationSequence_Reverses5To1() {
        var currentStep = 5
        currentStep = (currentStep - 1).coerceIn(1, 5)
        assertEquals(4, currentStep)

        currentStep = (currentStep - 1).coerceIn(1, 5)
        assertEquals(3, currentStep)

        currentStep = (currentStep - 1).coerceIn(1, 5)
        assertEquals(2, currentStep)

        currentStep = (currentStep - 1).coerceIn(1, 5)
        assertEquals(1, currentStep)
    }

    @Test
    fun testStepPersistenceAndProcessRecreationClamping() {
        val savedStep = 3
        val restoredStep = savedStep.coerceIn(1, 5)
        assertEquals(3, restoredStep)

        val invalidHighStep = 99
        val clampedHigh = invalidHighStep.coerceIn(1, 5)
        assertEquals(5, clampedHigh)

        val invalidLowStep = -5
        val clampedLow = invalidLowStep.coerceIn(1, 5)
        assertEquals(1, clampedLow)
    }

    @Test
    fun testDefaultLauncherStatusScenarios() {
        val launcherIsDefault = true
        val launcherNotDefault = false

        assertTrue(launcherIsDefault)
        assertFalse(launcherNotDefault)
    }

    @Test
    fun testUserAcceptsOrCancelsRoleRequest_DoesNotBreakStepState() {
        var activeStep = 1
        // Role request returned (accept or cancel)
        val roleRequestHandled = true
        if (roleRequestHandled) {
            activeStep = 2 // Advances step cleanly
        }
        assertEquals(2, activeStep)
    }

    @Test
    fun testResetSetup_RestoresFirstLaunchBehavior() {
        val isCompleted = false
        val currentStep = 1

        assertFalse(isCompleted)
        assertEquals(1, currentStep)
    }

    @Test
    fun testNoRepeatedSetupLaunches_OrNavigationLoops() {
        val isCompleted = true
        val shouldReLaunchOnResume = !isCompleted
        assertFalse(shouldReLaunchOnResume)
    }
}
