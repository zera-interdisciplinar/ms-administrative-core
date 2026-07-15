package com.zera.ms_administrative_core.core.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class StatusTest {

    @Test
    void activeStatusShouldAllowSuspensionAndInactivation() {
        assertTrue(Status.ACTIVE.canTransitionTo(Status.INACTIVE));
        assertTrue(Status.ACTIVE.canTransitionTo(Status.SUSPENDED));
        assertFalse(Status.ACTIVE.canTransitionTo(Status.ACTIVE));
    }

    @Test
    void inactiveStatusShouldOnlyAllowActivation() {
        assertTrue(Status.INACTIVE.canTransitionTo(Status.ACTIVE));
        assertFalse(Status.INACTIVE.canTransitionTo(Status.INACTIVE));
        assertFalse(Status.INACTIVE.canTransitionTo(Status.SUSPENDED));
    }

    @Test
    void suspendedStatusShouldAllowActivationAndInactivation() {
        assertTrue(Status.SUSPENDED.canTransitionTo(Status.ACTIVE));
        assertTrue(Status.SUSPENDED.canTransitionTo(Status.INACTIVE));
        assertFalse(Status.SUSPENDED.canTransitionTo(Status.SUSPENDED));
    }

    @Test
    void isActiveShouldOnlyReturnTrueForActive() {
        assertTrue(Status.ACTIVE.isActive());
        assertFalse(Status.INACTIVE.isActive());
        assertFalse(Status.SUSPENDED.isActive());
    }
}
