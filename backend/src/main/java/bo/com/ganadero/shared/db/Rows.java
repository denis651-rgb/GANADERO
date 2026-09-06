package bo.com.ganadero.shared.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * SQLite guarda UUID/timestamps como TEXT; sqlite-jdbc no soporta
 * ResultSet.getObject(col, UUID.class) ni conversion automatica a Instant.
 * Helpers compartidos para leer esas columnas desde cualquier repo Jdbc*.
 */
public final class Rows {
    private Rows() {}

    public static UUID uuid(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        return raw == null ? null : UUID.fromString(raw);
    }

    public static Integer intOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** sqlite-jdbc's getObject() returns Integer (not Long) for values that fit in an int; nunca hagas (Long) rs.getObject(...). */
    public static Long longOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        return raw == null ? null : LocalDate.parse(raw);
    }

    public static LocalTime localTime(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        return raw == null ? null : LocalTime.parse(raw);
    }

    public static Instant instant(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        if (raw == null) return null;
        try {
            return Instant.parse(raw);
        } catch (java.time.format.DateTimeParseException ex) {
            // formato por defecto de SQLite (current_timestamp): "yyyy-MM-dd HH:mm:ss", sin 'T'/'Z'.
            String normalized = raw.contains("T") ? raw : raw.replace(' ', 'T') + "Z";
            return Instant.parse(normalized);
        }
    }
}
