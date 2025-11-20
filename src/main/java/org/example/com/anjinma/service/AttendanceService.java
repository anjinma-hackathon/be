package org.example.com.anjinma.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.example.com.anjinma.dto.StudentJoinMessage;
import org.springframework.stereotype.Service;

@Service
public class AttendanceService {

    // roomId -> (studentId -> Presence)
    private final Map<Long, Map<String, Presence>> presence = new ConcurrentHashMap<>();
    // sessionId -> Key
    private final Map<String, Key> sessionIndex = new ConcurrentHashMap<>();

    public void join(Long roomId, StudentJoinMessage student, String sessionId) {
        Presence p = new Presence(student.getStudentId(), student.getStudentName(), sessionId, Instant.now().toEpochMilli());
        presence.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
            .put(student.getStudentId(), p);
        if (sessionId != null) {
            sessionIndex.put(sessionId, new Key(roomId, student.getStudentId()));
        }
    }

    public void leave(Long roomId, String studentId) {
        Map<String, Presence> room = presence.get(roomId);
        if (room != null) {
            Presence removed = room.remove(studentId);
            if (removed != null && removed.sessionId != null) {
                sessionIndex.remove(removed.sessionId);
            }
            if (room.isEmpty()) {
                presence.remove(roomId);
            }
        }
    }

    public Long leaveBySession(String sessionId) {
        Key key = sessionIndex.remove(sessionId);
        if (key == null) return null;
        leave(key.roomId, key.studentId);
        return key.roomId;
    }

    public void touch(String sessionId) {
        Key key = sessionIndex.get(sessionId);
        if (key == null) return;
        Map<String, Presence> room = presence.get(key.roomId);
        if (room == null) return;
        Presence p = room.get(key.studentId);
        if (p != null) p.lastSeen = Instant.now().toEpochMilli();
    }

    public List<StudentJoinMessage> list(Long roomId) {
        Map<String, Presence> room = presence.get(roomId);
        if (room == null) return List.of();
        Collection<Presence> values = room.values();
        List<StudentJoinMessage> result = new ArrayList<>(values.size());
        for (Presence p : values) {
            result.add(new StudentJoinMessage(p.studentId, p.studentName, p.langCode));
        }
        return result;
    }

    public Set<Long> evictIdle(long maxIdleMillis) {
        long now = Instant.now().toEpochMilli();
        Set<Long> affectedRooms = new HashSet<>();
        for (Map.Entry<Long, Map<String, Presence>> e : presence.entrySet()) {
            Long roomId = e.getKey();
            Map<String, Presence> room = e.getValue();
            room.values().removeIf(p -> {
                boolean idle = (now - p.lastSeen) > maxIdleMillis;
                if (idle) {
                    if (p.sessionId != null) sessionIndex.remove(p.sessionId);
                    affectedRooms.add(roomId);
                }
                return idle;
            });
            if (room.isEmpty()) presence.remove(roomId);
        }
        return affectedRooms;
    }

    private record Key(Long roomId, String studentId) {}

    private static class Presence {
        final String studentId;
        final String studentName;
        final String sessionId;
        volatile long lastSeen;
        volatile String langCode; // e.g., en, ja, ...

        Presence(String studentId, String studentName, String sessionId, long lastSeen) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.sessionId = sessionId;
            this.lastSeen = lastSeen;
        }
    }

    public void setLanguage(Long roomId, String studentId, String langCode) {
        Map<String, Presence> room = presence.get(roomId);
        if (room == null) return;
        Presence p = room.get(studentId);
        if (p != null) p.langCode = langCode;
    }

    public String getLanguage(Long roomId, String studentId) {
        Map<String, Presence> room = presence.get(roomId);
        if (room == null) return null;
        Presence p = room.get(studentId);
        return p == null ? null : p.langCode;
    }
}
