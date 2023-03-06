package com.highestpeak.gist.mess.util;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.highestpeak.gist.common.conf.Hconf;
import com.highestpeak.gist.common.conf.Hconfs;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 组合几种实现的 StopWatch，基本都相当于这里的 Watcher 对象 <br/>
 * <p>
 * 1. org.springframework.util.StopWatch 参考 api <br/>
 * {@link SessionWatcher#start()}、{@link SessionWatcher#stop()} <br/>
 * {@link SessionWatcher#currWatchTotalCost()} <br/>
 * {@link SessionWatcher#getWatcherTaskCount()} <br/>
 * <p>
 * 2. com.google.common.base.Stopwatch todo 这个里面的 ticker 并不是经常用. 所以要不要整合进来呢
 * <p>
 * 3. 树形耗时记录. 堆栈跨函数的 context 类型的耗时记录. thread context 类型的耗时记录. <br/>
 * 4. 很多分布式 trace 实现. 可以在多系统中实现这种效果.但是本类是一个单机简易实现. <br/>
 * 组合成树形结构来看就懂了 {@link Session}、{@link LogDataNode}、{@link SessionWatcher} <br/>
 * <p>
 * 4. 还有一个 org.junit.rules.Stopwatch
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-08
 */
@SuppressWarnings("unused")
@Slf4j
public class StopWatch {

    public static final Hconf<Long> MIN_COST = Hconfs.ofLong("com.highestpeak.stopwatch-min-time", 1000).build();

    private static final ThreadLocal<Session> SESSION_WATCHER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 耗时小于这个时间,则不去打印日志
     */
    private static final long IGNORE_FAST_THAN = 2;

    /**
     * watcher 对象. watcher 不是 LogDataNode 上的节点. 只是一个类似游标的对象 <br/>
     */
    public static final class SessionWatcher {
        private final Session session;
        private LogDataNode currentLogData;

        private SessionWatcher(LogDataNode parent, Session session) {
            this.currentLogData = parent.createChild();

            this.session = session;
            session.updateCurrent(this.currentLogData);
        }

        // ======================== split 操作方式的 api ======================== //
        // todo split 方式是 先执行 task 再去 split 需要写 test 方法来完善 example

        private void cut(String title, String comment) {
            // 更新本次 log 节点的值
            currentLogData.logNodeEnd();
            currentLogData.setLogName(title);
            currentLogData.setComment(comment);

            // 仅当当前 watcher 中 data 的 size 大于 1 时，才删除当前节点；避免删除为空后，后续记录 split 时出现为空的问题
            List<LogDataNode> parentChildren = currentLogData.getParent().getChildren();
            if (currentLogData.getCost() < IGNORE_FAST_THAN && parentChildren.size() > 1) {
                parentChildren.remove(parentChildren.size() - 1);
            }
        }

        public void split(String title, String comment) {
            cut(title, comment);

            // 下一个节点
            currentLogData = currentLogData.getParent().createChild();
            session.updateCurrent(currentLogData);
        }

        public void split(String title) {
            split(title, "");
        }

        /**
         * 停止 log 树上. 本层节点的记时. 更新树指针到父目录
         */
        public void splitAndStop(String title, String comment) {
            cut(title, comment);
            session.updateCurrent(currentLogData.getParent());
        }

        public void splitAndStop(String title) {
            splitAndStop(title, "");
        }

        // ======================== start stop 操作方式的 api ======================== //
        // todo start stop 方式是 start、然后执行task、然后stop 需要写 test 方法来完善 example

        public void start() {
            start("");
        }

        public void start(String taskName) {
            if (this.currentLogData.getLogName() != null) {
                throw new IllegalStateException("Can't start watcher new watch: it's already running");
            }
            this.currentLogData.setLogName(taskName);
            this.currentLogData.setStart(System.currentTimeMillis());
        }

        public void stop() {
            if (this.currentLogData.getLogName() != null) {
                throw new IllegalStateException("Can't stop StopWatch: it's not running");
            }
            split(this.currentLogData.getLogName(), this.currentLogData.getComment());
        }

        // ======================== 其他 api ======================== //

        public long currWatchTotalCost() {
            List<LogDataNode> brotherNodes = this.currentLogData.getParent().getChildren();
            return brotherNodes.stream()
                    .filter(LogDataNode::end)
                    .mapToLong(LogDataNode::getCost)
                    .sum();
        }

        /**
         * alias for {@link #currWatchTotalCost()}
         */
        public long totalTimeMillis() {
            return currWatchTotalCost();
        }

        public int getWatcherTaskCount() {
            List<LogDataNode> brotherNodes = this.currentLogData.getParent().getChildren();
            return (int) brotherNodes.stream()
                    .filter(LogDataNode::end)
                    .count();
        }

        /**
         * alias for {@link #getWatcherTaskCount()}
         */
        public int getTaskCount() {
            return getWatcherTaskCount();
        }

    }

    @Data
    private static class Session {
        /**
         * Session 过期时间. 创建时刻生成的 expireTime
         */
        private final long expireTime;

        /**
         * 日志链路的根节点
         */
        private final LogDataNode root;
        /**
         * 当前在日志链路上的位置
         */
        private LogDataNode currentNode;

        public Session() {
            expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
            root = new LogDataNode(null);
            currentNode = root;
        }

        /**
         * 在当前 session 上创建一个 watcher
         * 需要传递 currentNode 以及更新 session 的几个信息
         */
        public SessionWatcher createWatcher() {
            return new SessionWatcher(currentNode, this);
        }

        /**
         * 更新日志链路 current 指针位置
         */
        private void updateCurrent(LogDataNode nextNode) {
            this.currentNode = nextNode;
        }

        /**
         * 打印当前 session 的日志树结构
         */
        private void log() {
            if (root.getChildren().isEmpty()) {
                return;
            }
            LogDataNode d = root.getChildren().get(0);
            if (d != null && d.getCost() >= MIN_COST.get()) {
                log.warn("log session cost. info: {}", JsonUtil.toJson(d));
            }
        }
    }

    /**
     * 调用链上一个 logData 节点
     */
    @Data
    private static final class LogDataNode {
        /**
         * basic 字段 <br/>
         * log 的主题
         */
        private String logName;
        /**
         * log 的一些额外描述
         */
        private String comment;
        /**
         * 节点开始记时的时间
         * 不使用 final 是因为有另一种操作 api 需要重设 start
         */
        private long start;
        /**
         * 这次 log 的 耗时信息
         */
        private long cost;

        /**
         * log 结构信息
         */
        private final int level;
        /**
         * 父节点
         * JsonIgnore 防止递归打印 log
         */
        @JsonIgnore
        private final LogDataNode parent;
        /**
         * 子节点
         */
        private List<LogDataNode> children;

        public LogDataNode(LogDataNode parent) {
            level = parent == null ? 0 : parent.level + 1;
            start = System.currentTimeMillis();
            this.parent = parent;
        }

        public LogDataNode createChild() {
            LogDataNode child = new LogDataNode(this);

            if (children == null) {
                children = Lists.newArrayList();
            }
            children.add(child);

            return child;
        }

        public void logNodeEnd() {
            this.cost = System.currentTimeMillis() - this.start;
        }

        public boolean end() {
            return this.cost != 0;
        }
    }

    public static SessionWatcher createSessionWatcher() {
        Session session = SESSION_WATCHER_THREAD_LOCAL.get();
        if (session != null && session.expireTime < System.currentTimeMillis()) {
            log.warn("createWatcher found timeout session, last request not execute method #logAndStop"); // 当前线程的前一个请求为调用 logAndStop
            logAndStop();
            session = null;
        }
        if (session == null) {
            session = new Session();
            SESSION_WATCHER_THREAD_LOCAL.set(session);
        }
        return session.createWatcher();
    }

    public static void sessionLogAndStop() {
        Session session = SESSION_WATCHER_THREAD_LOCAL.get();
        SESSION_WATCHER_THREAD_LOCAL.remove();
        if (session != null) {
            session.log();
        }
    }

    /**
     * alias for {@link #sessionLogAndStop()}
     */
    public static void logAndStop() {
        sessionLogAndStop();
    }

    public static void debugPrintSession() {
        Session session = SESSION_WATCHER_THREAD_LOCAL.get();
        if (session != null) {
            log.info("root = {}", JsonUtil.toJson(session.getRoot()));
        }
    }


}
