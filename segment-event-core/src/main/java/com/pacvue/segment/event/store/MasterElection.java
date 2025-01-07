package com.pacvue.segment.event.store;

@FunctionalInterface
public interface MasterElection {
    boolean isMaster();
}
