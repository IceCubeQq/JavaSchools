package org.school.analysis.infrastructure.database;

import org.school.analysis.application.ports.output.DatabaseManager;
import org.school.analysis.domain.model.School;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

public class DatabaseManagerImpl implements DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManagerImpl.class);
    private Connection connection;

    @Override
    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:schools.db");
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось подключиться к базе данных", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createTables() {
        String[] createTableSQL = {
                "CREATE TABLE IF NOT EXISTS counties (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL)",

                "CREATE TABLE IF NOT EXISTS districts (id INTEGER PRIMARY KEY, name TEXT, county_id INTEGER, " +
                        "FOREIGN KEY (county_id) REFERENCES counties(id))",

                "CREATE TABLE IF NOT EXISTS schools (id INTEGER PRIMARY KEY, district_id INTEGER, name TEXT NOT NULL, " +
                        "county_id INTEGER, grades TEXT, students INTEGER, teachers REAL, FOREIGN KEY (district_id) REFERENCES districts(id), " +
                        "FOREIGN KEY (county_id) REFERENCES counties(id))",

                "CREATE TABLE IF NOT EXISTS school_financials (id INTEGER PRIMARY KEY AUTOINCREMENT, school_id INTEGER UNIQUE, " +
                        "calworks REAL, lunch REAL, expenditure REAL, income REAL, fiscal_year DATE DEFAULT CURRENT_DATE, " +
                        "FOREIGN KEY (school_id) REFERENCES schools(id))",

                "CREATE TABLE IF NOT EXISTS school_performance (id INTEGER PRIMARY KEY AUTOINCREMENT, school_id INTEGER UNIQUE, " +
                        "english_learners REAL, read_score REAL, math_score REAL, test_date DATE DEFAULT CURRENT_DATE, " +
                        "FOREIGN KEY (school_id) REFERENCES schools(id))",

                "CREATE INDEX IF NOT EXISTS idx_schools_district ON schools(district_id)",

                "CREATE INDEX IF NOT EXISTS idx_schools_county ON schools(county_id)",

                "CREATE INDEX IF NOT EXISTS idx_performance_math ON school_performance(math_score DESC)",

                "CREATE INDEX IF NOT EXISTS idx_performance_read ON school_performance(read_score DESC)",

                "CREATE INDEX IF NOT EXISTS idx_financials_expenditure ON school_financials(expenditure DESC)"
        };

        try (Statement statement = connection.createStatement()) {
            for (String sql : createTableSQL) {
                statement.execute(sql);
            }
            logger.info("Таблицы успешно созданы по 3-ей нормальной форме)");
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось создать таблицы", e);
        }
    }

    @Override
    public void insertSchools(List<School> schools) {
        try {
            connection.setAutoCommit(false);
            insertCounties(schools);
            insertDistricts(schools);
            insertSchoolsData(schools);
            insertFinancialsData(schools);
            insertPerformanceData(schools);
            connection.commit();
            logger.info("Все данные успешно сохранены в БД");

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Ошибка при откатке транзакции", rollbackEx);
            }
            throw new RuntimeException("Не удалось сохранить школы в базу данных", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.warn("Не удалось восстановить автокоммит режим", e);
            }
        }
    }

    @Override
    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Соединение с БД не установлено. Вызовите connect() перед использованием.");
        }
        return connection;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Соединение с базой данных закрыто");
            }
        } catch (SQLException e) {
            logger.error("Ошибка при закрытии соединения", e);
        }
    }

    private void insertCounties(List<School> schools) throws SQLException {
        String insertCountySQL = "INSERT OR IGNORE INTO counties (name) VALUES (?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertCountySQL)) {
            for (School school : schools) {
                if (school.getCountry() != null && !school.getCountry().trim().isEmpty()) {
                    pstmt.setString(1, school.getCountry().trim());
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
            logger.debug("Округа сохранены в БД");
        }
    }

    private void insertDistricts(List<School> schools) throws SQLException {
        String insertDistrictSQL = "INSERT OR IGNORE INTO districts (id, name, county_id) " +
                "VALUES (?, ?, (SELECT id FROM counties WHERE name = ?))";

        try (PreparedStatement pstmt = connection.prepareStatement(insertDistrictSQL)) {
            for (School school : schools) {
                pstmt.setInt(1, school.getDistrictId());
                pstmt.setString(2, "District " + school.getDistrictId());
                pstmt.setString(3, school.getCountry());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            logger.debug("Районы сохранены в БД");
        }
    }

    private void insertSchoolsData(List<School> schools) throws SQLException {
        String insertSchoolSQL = "INSERT OR REPLACE " +
                "INTO schools(id, district_id, name, county_id, grades, students, teachers) " +
                "VALUES (?, ?, ?, (SELECT id FROM counties WHERE name = ?), ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertSchoolSQL)) {
            for (School school : schools) {
                pstmt.setInt(1, school.getId());
                pstmt.setInt(2, school.getDistrictId());
                pstmt.setString(3, school.getName());
                pstmt.setString(4, school.getCountry());
                pstmt.setString(5, school.getGrades());
                pstmt.setObject(6, school.getStudents());
                pstmt.setObject(7, school.getTeachers());

                pstmt.addBatch();
            }
            pstmt.executeBatch();
            logger.debug("Основные данные школ сохранены в БД");
        }
    }

    private void insertFinancialsData(List<School> schools) throws SQLException {
        String insertFinancialSQL = " INSERT OR REPLACE " +
                "INTO school_financials (school_id, calworks, lunch, expenditure, income) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertFinancialSQL)) {
            for (School school : schools) {
                pstmt.setInt(1, school.getId());
                pstmt.setObject(2, school.getCalworks());
                pstmt.setObject(3, school.getLunch());
                pstmt.setObject(4, school.getExpenditure());
                pstmt.setObject(5, school.getIncome());

                pstmt.addBatch();
            }
            pstmt.executeBatch();
            logger.debug("Финансовые данные сохранены в БД");
        }
    }

    private void insertPerformanceData(List<School> schools) throws SQLException {
        String insertPerformanceSQL = "INSERT OR REPLACE " +
                "INTO school_performance (school_id, english_learners, read_score, math_score) " +
                "VALUES (?, ?, ?, ?) ";

        try (PreparedStatement pstmt = connection.prepareStatement(insertPerformanceSQL)) {
            for (School school : schools) {
                pstmt.setInt(1, school.getId());
                pstmt.setObject(2, school.getEnglish());
                pstmt.setObject(3, school.getReadScore());
                pstmt.setObject(4, school.getMathScore());

                pstmt.addBatch();
            }
            pstmt.executeBatch();
            logger.debug("Данные об успеваемости сохранены в БД");
        }
    }
}