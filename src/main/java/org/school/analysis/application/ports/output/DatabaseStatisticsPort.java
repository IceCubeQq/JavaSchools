package org.school.analysis.application.ports.output;

import java.sql.Connection;

public interface DatabaseStatisticsPort {
    Connection getConnection();
    String getDatabaseStatistics();
    boolean isDatabaseEmpty();
}