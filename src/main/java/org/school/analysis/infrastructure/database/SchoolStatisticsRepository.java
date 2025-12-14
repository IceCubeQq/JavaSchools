package org.school.analysis.infrastructure.database;

import org.school.analysis.application.ports.output.DatabaseStatisticsPort;
import org.school.analysis.application.ports.output.SchoolRepository;
import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.domain.dto.ExpenditureStats;
import org.school.analysis.domain.dto.MathSchoolStats;
import org.school.analysis.application.exception.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchoolStatisticsRepository implements SchoolRepository, DatabaseStatisticsPort {
    private static final Logger logger = LoggerFactory.getLogger(SchoolStatisticsRepository.class);
    private final Connection connection;

    public SchoolStatisticsRepository(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Соединение null");
        }
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public String getDatabaseStatistics() {
        StringBuilder stats = new StringBuilder();

        try {
            String[] statisticsQueries = {
                    "SELECT COUNT(*) as total_schools FROM schools",
                    "SELECT COUNT(DISTINCT county_id) as total_counties FROM schools",
                    "SELECT AVG(students) as avg_students FROM schools WHERE students IS NOT NULL",
                    "SELECT AVG(math_score) as avg_math FROM school_performance WHERE math_score IS NOT NULL",
                    "SELECT AVG(read_score) as avg_read FROM school_performance WHERE read_score IS NOT NULL",
                    "SELECT AVG(expenditure) as avg_expenditure FROM school_financials WHERE expenditure IS NOT NULL",
                    "SELECT SUM(students) as total_students FROM schools WHERE students IS NOT NULL",
                    "SELECT MIN(students) as min_students FROM schools WHERE students IS NOT NULL",
                    "SELECT MAX(students) as max_students FROM schools WHERE students IS NOT NULL"
            };

            String[] descriptions = {
                    "Всего школ в БД",
                    "Всего округов",
                    "Среднее количество студентов",
                    "Средний балл по математике",
                    "Средний балл по чтению",
                    "Средние расходы на студента",
                    "Общее количество студентов",
                    "Минимальное количество студентов в школе",
                    "Максимальное количество студентов в школе"
            };

            for (int i = 0; i < statisticsQueries.length; i++) {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(statisticsQueries[i])) {

                    if (rs.next()) {
                        Object value = rs.getObject(1);
                        if (value instanceof Number) {
                            Number num = (Number) value;
                            if (descriptions[i].contains("расходы") || descriptions[i].contains("балл") ||
                                    descriptions[i].contains("Среднее")) {
                                stats.append(String.format("%s: <b>%.2f</b>\n", descriptions[i], num.doubleValue()));
                            } else {
                                stats.append(String.format("%s: <b>%d</b>\n", descriptions[i], num.intValue()));
                            }
                        } else if (value != null) {
                            stats.append(String.format("%s: <b>%s</b>\n", descriptions[i], value));
                        }
                    }
                } catch (SQLException e) {
                    stats.append(String.format("%s: <b>Ошибка</b>\n", descriptions[i]));
                }
            }
            if (isDatabaseEmpty()) {
                stats.append("\nБаза данных пуста. Используйте 'Загрузить данные' для импорта");
            }

        } catch (Exception e) {
            stats.append("Ошибка получения статистики ").append(e.getMessage());
            stats.append("\nВозможно, база данных не инициализирована. Используйте 'Загрузить данные'");
        }

        return stats.toString();
    }

    @Override
    public boolean isDatabaseEmpty() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM schools")) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            logger.error("Ошибка проверки", e);
            return true;
        }
    }

    @Override
    public List<ExpenditureStats> findAverageExpenditureInCounties(List<String> counties, double minExpenditure) {
        logger.debug("Запрос средних расходов для округов: {} с minExpenditure: {}", counties, minExpenditure);

        String sql = "SELECT c.name as county_name, COUNT(*) as school_count, " +
                "AVG(f.expenditure) as avg_expenditure, MIN(f.expenditure) as min_expenditure, " +
                "MAX(f.expenditure) as max_expenditure FROM school_financials f JOIN schools s " +
                "ON f.school_id = s.id JOIN counties c ON s.county_id = c.id " +
                "WHERE c.name IN (?, ?, ?, ?) " +
                "AND f.expenditure > ? GROUP BY c.name ORDER BY avg_expenditure DESC";

        List<ExpenditureStats> stats = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < counties.size(); i++) {
                pstmt.setString(i + 1, counties.get(i));
            }
            pstmt.setDouble(5, minExpenditure);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ExpenditureStats stat = new ExpenditureStats();
                    stat.setCountyName(rs.getString("county_name"));
                    stat.setSchoolCount(rs.getInt("school_count"));
                    stat.setAvgExpenditure(rs.getDouble("avg_expenditure"));
                    stat.setMinExpenditure(rs.getDouble("min_expenditure"));
                    stat.setMaxExpenditure(rs.getDouble("max_expenditure"));
                    stats.add(stat);
                }
            }
        } catch (SQLException e) {
            String errorMessage = String.format("Ошибка при выполнении запроса средних расходов для округов: %s",
                    String.join(", ", counties));
            logger.error(errorMessage, e);
            throw new RepositoryException(errorMessage, e);
        }

        logger.info("Найдено {} записей о расходах", stats.size());
        return stats;
    }

    @Override
    public MathSchoolStats findTopMathSchoolByStudentRange(int minStudents, int maxStudents) {
        logger.debug("Поиск лучшей школы по математике для диапазона студентов: {}-{}", minStudents, maxStudents);

        String sql = "SELECT s.id, s.name as school_name, c.name as county_name, s.students, " +
                "p.math_score, f.expenditure FROM schools s JOIN school_performance p ON s.id = p.school_id " +
                "JOIN school_financials f ON s.id = f.school_id JOIN counties c ON s.county_id = c.id " +
                "WHERE s.students BETWEEN ? AND ? AND p.math_score IS NOT NULL " +
                "ORDER BY p.math_score DESC LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, minStudents);
            pstmt.setInt(2, maxStudents);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    MathSchoolStats stats = new MathSchoolStats();
                    stats.setId(rs.getInt("id"));
                    stats.setSchoolName(rs.getString("school_name"));
                    stats.setCountyName(rs.getString("county_name"));
                    stats.setStudents(rs.getInt("students"));
                    stats.setMathScore(rs.getDouble("math_score"));
                    stats.setExpenditure(rs.getDouble("expenditure"));
                    logger.info("Найдена школа: {} (ID: {}) с баллом по математике: {}",
                            stats.getSchoolName(), stats.getId(), stats.getMathScore());
                    return stats;
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Ошибка при поиске лучшей школы", e);
        }
        logger.warn("Не найдено школ с количеством студентов в диапазоне {}-{}", minStudents, maxStudents);
        return null;
    }

    @Override
    public List<CountryStudentStats> findAverageStudentsByCountries(int limit) {
        logger.debug("Запрос статистики студентов по странам, лимит: {}", limit);
        String sql = "SELECT " +
                "c.name as country_name, " +
                "COUNT(*) as school_count, " +
                "AVG(s.students) as avg_students, " +
                "MIN(s.students) as min_students, " +
                "MAX(s.students) as max_students, " +
                "SUM(s.students) as total_students " +
                "FROM schools s " +
                "JOIN counties c ON s.county_id = c.id " +
                "WHERE s.students IS NOT NULL " +
                "GROUP BY c.name " +
                "HAVING COUNT(*) >= 3 " +
                "ORDER BY school_count DESC " +
                "LIMIT ?";

        List<CountryStudentStats> stats = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CountryStudentStats stat = new CountryStudentStats();
                    stat.setCountryName(rs.getString("country_name"));
                    stat.setSchoolCount(rs.getInt("school_count"));
                    stat.setAvgStudents(rs.getDouble("avg_students"));
                    stat.setMinStudents(rs.getInt("min_students"));
                    stat.setMaxStudents(rs.getInt("max_students"));
                    stat.setTotalStudents(rs.getInt("total_students"));
                    stats.add(stat);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Ошибка при получении статистики студентов", e);
        }
        return stats;
    }
}