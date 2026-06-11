package com.syspilot.viewer.service;

import com.syspilot.viewer.model.SessionData;
import com.syspilot.viewer.model.TurnData;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

/**
 * Watches a logs directory for new/modified trajectory files.
 * Registers watches on session subdirectories so turn creation is caught in real time.
 */
public class LogsWatcherService {

    private final Path logsDir;
    private final WatchService watchService;
    private final ScheduledExecutorService debouncer =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "logs-watcher-debounce");
                t.setDaemon(true);
                return t;
            });
    private final ScheduledExecutorService poller =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "logs-watcher-poller");
                t.setDaemon(true);
                return t;
            });

    private final Map<String, SessionData> sessions = new LinkedHashMap<>();
    private final Map<String, Integer> sessionTurnCounts = new ConcurrentHashMap<>();
    private final Map<WatchKey, Path> watchKeyToDir = new ConcurrentHashMap<>();
    private final ObjectProperty<TurnData> newTurnEvent = new SimpleObjectProperty<>();

    public LogsWatcherService(Path logsDir) throws IOException {
        this.logsDir = logsDir;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    public ObjectProperty<TurnData> newTurnProperty() { return newTurnEvent; }
    public Collection<SessionData> getSessions() { return sessions.values(); }

    public void start() {
        scanExistingSessions();
        startWatching();
        startPolling();
    }

    public void stop() {
        try { watchService.close(); } catch (IOException ignored) {}
        debouncer.shutdownNow();
        poller.shutdownNow();
    }

    /** Force an immediate rescan of all sessions. */
    public void refreshNow() {
        pollCycle();
    }

    // ---- Polling fallback (for WSL / network drives where inotify is unreliable) ----

    private void startPolling() {
        poller.scheduleWithFixedDelay(() -> {
            try {
                pollCycle();
            } catch (Exception e) {
                System.err.println("Logs poller error: " + e.getMessage());
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

    private void pollCycle() {
        // Check for new session directories
        File[] dirs = logsDir.toFile().listFiles(File::isDirectory);
        if (dirs == null) return;

        for (File dir : dirs) {
            String name = dir.getName();
            if (!matchesSessionPattern(name)) continue;

            Path sessionDir = dir.toPath();
            // Rescan if new or if turn count changed
            SessionData existing = sessions.get(name);
            SessionData fresh = scanSession(sessionDir);
            if (fresh == null) continue;

            int freshCount = fresh.getTurns().size();
            int oldCount = existing != null ? existing.getTurns().size() : 0;

            if (existing == null || freshCount != oldCount) {
                Platform.runLater(() -> {
                    sessions.put(name, fresh);
                    sessionTurnCounts.put(name, freshCount);
                    if (freshCount > oldCount) {
                        for (int i = oldCount; i < freshCount; i++) {
                            newTurnEvent.set(fresh.getTurns().get(i));
                        }
                    }
                });

                // Register watch on new session dir
                if (existing == null) {
                    try { registerWatch(sessionDir); } catch (IOException ignored) {}
                }
            }
        }
    }

    // ---- Registration ----

    private void registerWatch(Path dir) throws IOException {
        WatchKey key = dir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        watchKeyToDir.put(key, dir);
    }

    // ---- Scanning ----

    private void scanExistingSessions() {
        File[] dirs = logsDir.toFile().listFiles(File::isDirectory);
        if (dirs == null) return;

        Arrays.sort(dirs, Comparator.comparing(File::getName));
        for (File dir : dirs) {
            SessionData session = scanSession(dir.toPath());
            if (session != null && !session.getTurns().isEmpty()) {
                sessions.put(session.getName(), session);
                sessionTurnCounts.put(session.getName(), session.getTurns().size());
            }
        }
    }

    private SessionData scanSession(Path sessionDir) {
        String name = sessionDir.getFileName().toString();
        SessionData session = new SessionData(sessionDir, name);

        File[] subdirs = sessionDir.toFile().listFiles(File::isDirectory);
        if (subdirs == null) return session;

        // Check for runner mode (trajectory.json directly in session dir)
        Path directTraj = sessionDir.resolve("trajectory.json");
        if (Files.exists(directTraj)) {
            session.addTurn(new TurnData(sessionDir, "run", directTraj));
            return session;
        }

        // Check for turn_NNN subdirectories
        List<File> turnDirs = new ArrayList<>();
        for (File sub : subdirs) {
            if (sub.getName().startsWith("turn_")) {
                turnDirs.add(sub);
            }
        }
        turnDirs.sort(Comparator.comparing(File::getName));

        for (File turnDir : turnDirs) {
            Path trajFile = turnDir.toPath().resolve("trajectory.json");
            if (Files.exists(trajFile)) {
                TurnData turn = new TurnData(turnDir.toPath(), turnDir.getName(), trajFile);
                try {
                    String content = Files.readString(trajFile);
                    int idx = content.indexOf("\"problem\"");
                    if (idx > 0) {
                        int valStart = content.indexOf("\"", content.indexOf(":", idx) + 1);
                        if (valStart > 0) {
                            int valEnd = content.indexOf("\"", valStart + 1);
                            if (valEnd > 0) {
                                String preview = content.substring(valStart + 1, valEnd);
                                if (preview.length() > 80) preview = preview.substring(0, 80) + "...";
                                turn.setPreview(preview);
                            }
                        }
                    }
                } catch (IOException ignored) {}
                session.addTurn(turn);
            }
        }
        return session;
    }

    // ---- Watching ----

    private void startWatching() {
        Thread watcher = new Thread(() -> {
            try {
                // Register logs dir and all existing session dirs
                registerWatch(logsDir);
                for (SessionData s : sessions.values()) {
                    registerWatch(s.getDir());
                }

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.poll(2, TimeUnit.SECONDS);
                    if (key == null) continue;

                    Path watchedDir = watchKeyToDir.get(key);
                    if (watchedDir == null) {
                        key.reset();
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                        Path relative = (Path) event.context();
                        Path absolute = watchedDir.resolve(relative);
                        String name = absolute.getFileName().toString();

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE
                                && Files.isDirectory(absolute)) {
                            if (matchesSessionPattern(name)) {
                                // New session directory
                                try { registerWatch(absolute); } catch (IOException e) {
                                    System.err.println("Failed to watch session dir: " + absolute);
                                }
                                scheduleScan(absolute, 500);
                            } else if (name.startsWith("turn_")) {
                                // New turn directory inside a session
                                scheduleScan(watchedDir, 300);
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            // A directory or file was modified — re-scan the owning session.
                            // This catches: trajectory.json written, turn dir contents changed, etc.
                            Path sessionDir = resolveOwningSession(watchedDir, absolute);
                            if (sessionDir != null) {
                                scheduleScan(sessionDir, 500);
                            }
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException ignored) {
            } catch (IOException e) {
                System.err.println("Logs watcher error: " + e.getMessage());
            }
        }, "logs-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    /**
     * Determine which session directory owns the changed path.
     */
    private Path resolveOwningSession(Path watchedDir, Path absolute) {
        String watchedName = watchedDir.getFileName().toString();
        // If we're watching a session dir directly, it's the owner
        if (matchesSessionPattern(watchedName)) {
            return watchedDir;
        }
        // If we're watching logsDir and the path is a session dir
        if (watchedDir.equals(logsDir)) {
            if (Files.isDirectory(absolute) && matchesSessionPattern(absolute.getFileName().toString())) {
                return absolute;
            }
        }
        return null;
    }

    private void scheduleScan(Path sessionDir, long delayMs) {
        String sessionName = sessionDir.getFileName().toString();
        debouncer.schedule(() -> {
            SessionData fresh = scanSession(sessionDir);
            if (fresh == null) return;
            Platform.runLater(() -> {
                int oldCount = sessionTurnCounts.getOrDefault(sessionName, 0);
                int newCount = fresh.getTurns().size();
                sessions.put(sessionName, fresh);
                sessionTurnCounts.put(sessionName, newCount);

                // Fire newTurnEvent for each genuinely new turn
                if (newCount > oldCount) {
                    for (int i = oldCount; i < newCount; i++) {
                        newTurnEvent.set(fresh.getTurns().get(i));
                    }
                }
            });
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private static boolean matchesSessionPattern(String name) {
        return name.matches("\\d{8}_\\d{6}_(cli|runner)");
    }
}
