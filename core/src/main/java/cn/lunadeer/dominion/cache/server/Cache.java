package cn.lunadeer.dominion.cache.server;

import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static cn.lunadeer.dominion.cache.CacheManager.UPDATE_INTERVAL;

public abstract class Cache {

    public void load() {
        if (getLastTaskTimeStamp() + UPDATE_INTERVAL < System.currentTimeMillis()) {
            XLogger.debug("run loadExecution immediately");
            resetLastTaskTimeStamp();
            try {
                loadExecution();
            } catch (Exception e) {
                XLogger.error(e);
            }
        } else {
            scheduleLoad();
        }
    }

    public void delete(Integer idToDelete) {
        if (getLastTaskTimeStamp() + UPDATE_INTERVAL < System.currentTimeMillis()) {
            XLogger.debug("run loadExecution immediately");
            resetLastTaskTimeStamp();
            try {
                deleteExecution(idToDelete);
            } catch (Exception e) {
                XLogger.error(e);
            }
        } else {
            scheduleLoad();
        }
    }

    private void scheduleLoad() {
        if (isTaskScheduled()) return;
        XLogger.debug("schedule loadExecution");
        setTaskScheduled();
        Scheduler.runTaskLaterAsync(() -> {
                    XLogger.debug("scheduled loadExecution run");
                    try {
                        resetLastTaskTimeStamp();
                        loadExecution();
                    } catch (Exception e) {
                        XLogger.error(e);
                    } finally {
                        unsetTaskScheduled();
                    }
                },
                getTaskScheduledDelayTick());
    }

    public void load(Integer idToLoad) {
        if (getLastTaskTimeStamp() + UPDATE_INTERVAL < System.currentTimeMillis()) {
            resetLastTaskTimeStamp();
            try {
                loadExecution(idToLoad);
            } catch (Exception e) {
                XLogger.error(e);
            }
        } else {
            if (isTaskScheduled()) return;
            setTaskScheduled();
            Scheduler.runTaskLaterAsync(() -> {
                        try {
                            resetLastTaskTimeStamp();
                            loadExecution();
                        } catch (Exception e) {
                            XLogger.error(e);
                        } finally {
                            unsetTaskScheduled();
                        }
                    },
                    getTaskScheduledDelayTick());
        }
    }

    abstract void loadExecution() throws Exception;

    abstract void loadExecution(Integer idToLoad) throws Exception;

    abstract void deleteExecution(Integer idToDelete) throws Exception;


    private final AtomicLong lastTask = new AtomicLong(0);
    private final AtomicBoolean taskScheduled = new AtomicBoolean(false);

    private Long getLastTaskTimeStamp() {
        return lastTask.get();
    }

    private void resetLastTaskTimeStamp() {
        lastTask.set(System.currentTimeMillis());
    }

    private Boolean isTaskScheduled() {
        return taskScheduled.get();
    }

    private void setTaskScheduled() {
        taskScheduled.set(true);
    }

    private void unsetTaskScheduled() {
        taskScheduled.set(false);
    }

    private long getTaskScheduledDelayTick() {
        return (UPDATE_INTERVAL - (System.currentTimeMillis() - getLastTaskTimeStamp())) / 1000 * 20L;
    }

}
