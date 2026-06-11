package com.syspilot.viewer.service;

import com.syspilot.viewer.model.SessionData;
import com.syspilot.viewer.model.TurnData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Scans a logs directory for session/turn trajectory files on demand.
 */
public class LogsWatcherService {

    private final Path logsDir;
    private final Map<String, SessionData> sessions = new LinkedHashMap<>();

    public LogsWatcherService(Path logsDir) {
        this.logsDir = logsDir;
    }

    public Collection<SessionData> getSessions() { return sessions.values(); }

    /** Full rescan of the logs directory. Call from UI thread or manually. */
    public void refreshNow() {
        sessions.clear();

        File[] dirs = logsDir.toFile().listFiles(File::isDirectory);
        if (dirs == null) return;

        Arrays.sort(dirs, Comparator.comparing(File::getName));
        for (File dir : dirs) {
            SessionData session = scanSession(dir.toPath());
            if (session != null && !session.getTurns().isEmpty()) {
                sessions.put(session.getName(), session);
            }
        }
    }

    private SessionData scanSession(Path sessionDir) {
        String name = sessionDir.getFileName().toString();
        SessionData session = new SessionData(sessionDir, name);

        File[] subdirs = sessionDir.toFile().listFiles(File::isDirectory);
        if (subdirs == null) return session;

        // Runner mode: trajectory.json directly in session dir
        Path directTraj = sessionDir.resolve("trajectory.json");
        if (Files.exists(directTraj)) {
            session.addTurn(new TurnData(sessionDir, "run", directTraj));
            return session;
        }

        // CLI mode: turn_NNN subdirectories
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
}
