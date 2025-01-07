package com.pacvue.segment.event.store;

import cn.hutool.core.util.StrUtil;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ZookeeperMasterElection implements MasterElection, Watcher {
    private static final String NODE_PREFIX = "/node-";
    private final String zkAddress;
    private final String electionPath;
    private ZooKeeper zooKeeper;
    private String currentNode;
    private String masterNode;

    public ZookeeperMasterElection(String zkAddress, String electionPath) throws IOException {
        this.zkAddress = zkAddress;
        this.zooKeeper = new ZooKeeper(zkAddress, 10000, this);
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
        try {
            // Watch for the previous node to be deleted to trigger re-election
            if (event.getType() == Event.EventType.NodeDeleted && event.getPath().equals(currentNode)) {
                this.currentNode = null;
                log.info("current node deleted is {}", currentNode);
            }
        } catch (Exception e) {
            log.warn("try to check master failed", e);
        }
    }

    /**
     * 尝试创建currentNode并且监听
     */
    public void startElection() {
        // Create a temporary sequential node for master election
        try {
            ensureParentPathExists(electionPath);
            currentNode = zooKeeper.create(electionPath + NODE_PREFIX, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            watchNode(currentNode);
            log.info("created current node: {}", currentNode);
            checkMaster();
        } catch (KeeperException.SessionExpiredException | KeeperException.ConnectionLossException ex) {
            handleSessionExpired(this::startElection);
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

    /**
     * 获取当前最新的masterNode,尝试监听当前节点前面的节点
     */
    private void checkMaster() {
        try {
            masterNode = null;
            List<String> nodes = zooKeeper.getChildren(electionPath, false);
            Collections.sort(nodes); // Sort nodes lexicographically

            // The first node in the sorted list is the master
            masterNode = nodes.get(0);

            if (currentNode.endsWith(masterNode)) {
                // This is the smallest node, so we're the master
                log.info("I am the master: {}", currentNode);
            } else {
                int currentIndex = nodes.indexOf(currentNode.substring(currentNode.lastIndexOf("/") + 1));
                // Watch for the node before us to be deleted, triggering re-election
                if (currentIndex > 0) {
                    String prevNode = nodes.get(currentIndex - 1);
                    watchNode(prevNode);
                }
            }
        } catch (KeeperException | InterruptedException e) {
            log.warn("try to check master failed", e);
        }
    }

    private void handleSessionExpired(Runnable callback) {
        try {
            // 关闭当前的 ZooKeeper 客户端实例
            if (zooKeeper != null) {
                zooKeeper.close();
            }
            // 重新建立连接
            zooKeeper = new ZooKeeper(zkAddress, 10000, this);
            log.info("Reconnected to ZooKeeper with a new session.");
            callback.run();
        } catch (Exception e) {
            log.warn("Reconnected to ZooKeeper with a new session failed", e);
        }
    }

    public void watchNode(String nodePath) throws KeeperException, InterruptedException {
        // Register a watcher to listen to changes on this node
        Stat stat = zooKeeper.exists(nodePath, true);
        if (stat != null) {
            log.info("Watcher set on node: {}", nodePath);
        } else {
            log.warn("Node does not exist, cannot set watcher on: {}", nodePath);
        }
    }

    public void stop() throws InterruptedException {
        if (isMaster()) {
            log.info("I am stopping the master election process.");
        }
        zooKeeper.close();
    }
}
