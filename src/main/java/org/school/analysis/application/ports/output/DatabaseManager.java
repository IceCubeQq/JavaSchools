package org.school.analysis.application.ports.output;

import org.school.analysis.domain.model.School;

import java.sql.Connection;
import java.util.List;


public interface DatabaseManager {
    void connect();
    void createTables();
    void insertSchools(List<School> schools);
    Connection getConnection();
    void close();
}