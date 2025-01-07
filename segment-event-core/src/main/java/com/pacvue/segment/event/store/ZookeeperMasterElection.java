package com.pacvue.segment.event.store;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ZookeeperMasterElection implements MasterElection, Watcher {
    private static final String NODE_PREFIX = "/node-";
    private final ZooKeeper zooKeeper;
    private final String electionPath;
    private String currentNode;
    private String masterNode;

    public ZookeeperMasterElection(String zkAddress, String electionPath) throws IOException {
        this.zooKeeper = new ZooKeeper(zkAddress, 6000, this);
        this.electionPath = electionPath;
    }

    @Override
    public boolean isMaster() {
        if (StrUtil.isBlank(currentNode)) {
            startElection();
        }
        return StrUtil.isNotBlank(masterNode) && StrUtil.equals(masterNode, currentNode);
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.None) {
            return;
        }

        try {
            // Watch for the previous node to be deleted to trigger re-election
            if (event.getType() == Event.EventType.NodeDeleted) {
                checkMaster();
            }
        } catch (Exception e) {
            log.warn("try to check master failed", e);
        }
    }

    public void startElection() {
        // Create a temporary sequential node for master election
        try {
            ensureParentPathExists(electionPath);
            currentNode = zooKeeper.create(electionPath + NODE_PREFIX, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.info("Created node: {}", currentNode);
            checkMaster();
        } catch (KeeperException | InterruptedException e) {
            log.warn("try to check master failed", e);
        }
    }

    public void ensureParentPathExists(String electionPath) throws KeeperException, InterruptedException {
        String[] pathParts = electionPath.split("/");  // 拆分路径
        StringBuilder currentPath = new StringBuilder();

        // 遍历路径的各部分
        for (String part : pathParts) {
            if (part.isEmpty()) continue;  // 跳过空部分（路径的前导斜杠）

            // 拼接当前路径
            currentPath.append("/").append(part);

            // 如果当前路径不存在，创建它
            if (zooKeeper.exists(currentPath.toString(), false) == null) {
                zooKeeper.create(currentPath.toString(), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                log.info("Created parent path: {}", currentPath.toString());
            }
        }
    }

    private void checkMaster() throws KeeperException, InterruptedException {
        List<String> nodes = zooKeeper.getChildren(electionPath, false);
        Collections.sort(nodes); // Sort nodes lexicographically

        // The first node in the sorted list is the master
        String firstNode = nodes.get(0);

        if (currentNode.endsWith(firstNode)) {
            // This is the smallest node, so we're the master
            masterNode = currentNode;
            log.info("I am the master: {}", currentNode);
        } else {
            // Watch for the node before us to be deleted, triggering re-election
            String prevNode = nodes.get(nodes.indexOf(currentNode.substring(electionPath.length())) - 1);
            Stat stat = zooKeeper.exists(electionPath + "/" + prevNode, true);
            if (stat != null) {
                log.info("I am not the master. Watching node: {}", prevNode);
            }
        }
    }

    public void stop() throws InterruptedException {
        if (isMaster()) {
            log.info("I am stopping the master election process.");
        }
        zooKeeper.close();
    }

}
