package com.zera.ms_administrative_core.core.domain.valueobject;

public enum Status {

      ACTIVE {
            @Override
            public boolean canTransitionTo(Status target) {
                return target == INACTIVE || target == SUSPENDED;
            }
        },
        INACTIVE {
            @Override
            public boolean canTransitionTo(Status target) {
                return target == ACTIVE;
            }
        },
        SUSPENDED {
            @Override
            public boolean canTransitionTo(Status target) {
                return target == ACTIVE || target == INACTIVE;
            }
        };
    
        public abstract boolean canTransitionTo(Status target);
    
        public boolean isActive() {
            return this == ACTIVE;
        }
}
