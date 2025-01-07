package com.pacvue.segment.event.store;

import org.apache.zookeeper.KeeperException;

@FunctionalInterface
public interface MasterElection {
    boolean isMaster();
}
