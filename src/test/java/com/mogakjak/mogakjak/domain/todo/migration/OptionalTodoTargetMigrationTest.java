package com.mogakjak.mogakjak.domain.todo.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "ISSUE98_MYSQL_URL", matches = ".+")
class OptionalTodoTargetMigrationTest {

    @Test
    void migrationPreservesValuesAllowsNullAndCanRollbackWithoutNullRows() throws Exception {
        try (Connection connection = DriverManager.getConnection(System.getenv("ISSUE98_MYSQL_URL"), "root", "")) {
            // Dedicated disposable database only; never run against an application database.
            assertEquals("issue98_verify", connection.getCatalog());
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE todo (id INT PRIMARY KEY, target_time_in_seconds INT NOT NULL, actual_time_in_seconds INT NOT NULL)");
                try {
                    statement.executeUpdate("INSERT INTO todo VALUES (1, 3600, 600), (2, 7200, 1200)");
                    assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO todo VALUES (3, NULL, 0)"));
                    applySql(statement, "scripts/sql/98-optional-todo-target-time.sql");
                    applySql(statement, "scripts/sql/98-optional-todo-target-time.sql");
                    try (ResultSet rows = statement.executeQuery("SELECT * FROM todo ORDER BY id")) {
                        assertTrue(rows.next());
                        assertEquals(3600, rows.getInt("target_time_in_seconds"));
                        assertEquals(600, rows.getInt("actual_time_in_seconds"));
                        assertTrue(rows.next());
                        assertEquals(7200, rows.getInt("target_time_in_seconds"));
                        assertEquals(1200, rows.getInt("actual_time_in_seconds"));
                        assertFalse(rows.next());
                    }
                    assertNullability(statement, "YES");
                    statement.executeUpdate("INSERT INTO todo VALUES (3, NULL, 0)");
                    statement.executeUpdate("UPDATE todo SET target_time_in_seconds = NULL WHERE id = 1");
                    try (ResultSet row = statement.executeQuery("SELECT * FROM todo WHERE id = 1")) {
                        assertTrue(row.next());
                        assertNull(row.getObject("target_time_in_seconds"));
                        assertEquals(600, row.getInt("actual_time_in_seconds"));
                    }
                    assertThrows(SQLException.class,
                            () -> applySql(statement, "scripts/sql/98-optional-todo-target-time-rollback.sql"));
                    assertNullability(statement, "YES");
                    // Restore test fixtures to their known targets, then verify the documented rollback.
                    statement.executeUpdate("DELETE FROM todo WHERE id = 3");
                    statement.executeUpdate("UPDATE todo SET target_time_in_seconds = 3600 WHERE id = 1");
                    applySql(statement, "scripts/sql/98-optional-todo-target-time-rollback.sql");
                    assertNullability(statement, "NO");
                } finally {
                    statement.execute("DROP TABLE todo");
                }
            }
        }
    }

    private void applySql(Statement statement, String file) throws Exception {
        String sql = Files.readString(Path.of(file)).replaceAll("(?m)^--.*$", "");
        for (String command : sql.split(";")) {
            if (!command.isBlank()) statement.execute(command);
        }
    }

    private void assertNullability(Statement statement, String expected) throws SQLException {
        try (ResultSet row = statement.executeQuery("SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'todo' AND COLUMN_NAME = 'target_time_in_seconds'")) {
            assertTrue(row.next());
            assertEquals(expected, row.getString(1));
        }
    }
}
