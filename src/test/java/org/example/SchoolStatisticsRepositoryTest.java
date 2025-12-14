package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.exception.RepositoryException;
import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.domain.dto.ExpenditureStats;
import org.school.analysis.domain.dto.MathSchoolStats;
import org.school.analysis.infrastructure.database.SchoolStatisticsRepository;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolStatisticsRepositoryTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private Logger mockLogger;

    private SchoolStatisticsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SchoolStatisticsRepository(mockConnection);
    }

    @Test
    void testConstructor_NullConnection() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new SchoolStatisticsRepository(null));
        assertEquals("Соединение null", exception.getMessage());
    }

    @Test
    void testConstructor_ValidConnection() {
        SchoolStatisticsRepository repo = new SchoolStatisticsRepository(mockConnection);
        assertNotNull(repo);
        assertEquals(mockConnection, repo.getConnection());
    }

    @Test
    void testGetConnection() {
        Connection result = repository.getConnection();
        assertEquals(mockConnection, result);
    }

    @Test
    void testGetDatabaseStatistics_WithNullValues() throws Exception {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true);

        when(mockResultSet.getObject(1))
                .thenReturn(0)
                .thenReturn(0)
                .thenReturn(null)
                .thenReturn(70.0)
                .thenReturn(4500.0)
                .thenReturn(0)
                .thenReturn(0)
                .thenReturn(0);
        String stats = repository.getDatabaseStatistics();
        assertNotNull(stats);
        assertTrue(stats.contains("Всего школ в БД: <b>0</b>"));
        assertTrue(stats.contains("База данных пуста"));
    }

    @Test
    void testIsDatabaseEmpty_True() throws Exception {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery("SELECT COUNT(*) FROM schools")).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(0);
        boolean result = repository.isDatabaseEmpty();
        assertTrue(result);
    }

    @Test
    void testIsDatabaseEmpty_False() throws Exception {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery("SELECT COUNT(*) FROM schools")).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(10);
        boolean result = repository.isDatabaseEmpty();
        assertFalse(result);
    }

    @Test
    void testIsDatabaseEmpty_SQLException() throws Exception {
        when(mockConnection.createStatement()).thenThrow(new SQLException("Table not found"));

        boolean result = repository.isDatabaseEmpty();
        assertTrue(result);
    }

    @Test
    void testFindAverageExpenditureInCounties_Success() throws Exception {
        List<String> counties = Arrays.asList("Fresno", "Contra Costa", "El Dorado", "Glenn");
        double minExpenditure = 10.0;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        when(mockResultSet.getString("county_name"))
                .thenReturn("Fresno")
                .thenReturn("Contra Costa");

        when(mockResultSet.getInt("school_count"))
                .thenReturn(5)
                .thenReturn(3);

        when(mockResultSet.getDouble("avg_expenditure"))
                .thenReturn(15000.50)
                .thenReturn(12000.75);

        when(mockResultSet.getDouble("min_expenditure"))
                .thenReturn(10000.0)
                .thenReturn(11000.0);

        when(mockResultSet.getDouble("max_expenditure"))
                .thenReturn(20000.0)
                .thenReturn(13000.0);
        List<ExpenditureStats> result = repository.findAverageExpenditureInCounties(counties, minExpenditure);
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals("Fresno", result.get(0).getCountyName());
        assertEquals(5, result.get(0).getSchoolCount());
        assertEquals(15000.50, result.get(0).getAvgExpenditure(), 0.001);

        assertEquals("Contra Costa", result.get(1).getCountyName());
        assertEquals(3, result.get(1).getSchoolCount());
        assertEquals(12000.75, result.get(1).getAvgExpenditure(), 0.001);
        verify(mockPreparedStatement).setString(1, "Fresno");
        verify(mockPreparedStatement).setString(2, "Contra Costa");
        verify(mockPreparedStatement).setString(3, "El Dorado");
        verify(mockPreparedStatement).setString(4, "Glenn");
        verify(mockPreparedStatement).setDouble(5, 10.0);
    }

    @Test
    void testFindAverageExpenditureInCounties_EmptyResult() throws Exception {
        List<String> counties = Arrays.asList("Fresno", "Contra Costa", "El Dorado", "Glenn");
        double minExpenditure = 10.0;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        List<ExpenditureStats> result = repository.findAverageExpenditureInCounties(counties, minExpenditure);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAverageExpenditureInCounties_SQLException() throws Exception {
        List<String> counties = Arrays.asList("Fresno", "Contra Costa", "El Dorado", "Glenn");
        double minExpenditure = 10.0;

        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));
        RepositoryException exception = assertThrows(RepositoryException.class,
                () -> repository.findAverageExpenditureInCounties(counties, minExpenditure));

        assertTrue(exception.getMessage().contains("Ошибка при выполнении запроса средних расходов для округов"));
        assertTrue(exception.getCause() instanceof SQLException);
    }

    @Test
    void testFindAverageExpenditureInCounties_WithEmptyCountyList() throws Exception {
        List<String> counties = new ArrayList<>();
        double minExpenditure = 10.0;
        assertThrows(Exception.class,
                () -> repository.findAverageExpenditureInCounties(counties, minExpenditure));
    }

    @Test
    void testFindTopMathSchoolByStudentRange_Success() throws Exception {
        int minStudents = 5000;
        int maxStudents = 7500;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        when(mockResultSet.getInt("id")).thenReturn(123);
        when(mockResultSet.getString("school_name")).thenReturn("Test School");
        when(mockResultSet.getString("county_name")).thenReturn("Test County");
        when(mockResultSet.getInt("students")).thenReturn(6000);
        when(mockResultSet.getDouble("math_score")).thenReturn(95.5);
        when(mockResultSet.getDouble("expenditure")).thenReturn(8000.0);
        MathSchoolStats result = repository.findTopMathSchoolByStudentRange(minStudents, maxStudents);
        assertNotNull(result);
        assertEquals(123, result.getId());
        assertEquals("Test School", result.getSchoolName());
        assertEquals("Test County", result.getCountyName());
        assertEquals(6000, result.getStudents());
        assertEquals(95.5, result.getMathScore(), 0.001);
        assertEquals(8000.0, result.getExpenditure(), 0.001);

        verify(mockPreparedStatement).setInt(1, 5000);
        verify(mockPreparedStatement).setInt(2, 7500);
    }

    @Test
    void testFindTopMathSchoolByStudentRange_NoResults() throws Exception {
        int minStudents = 5000;
        int maxStudents = 7500;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        MathSchoolStats result = repository.findTopMathSchoolByStudentRange(minStudents, maxStudents);
        assertNull(result);
    }

    @Test
    void testFindTopMathSchoolByStudentRange_SQLException() throws Exception {
        int minStudents = 5000;
        int maxStudents = 7500;

        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));
        RepositoryException exception = assertThrows(RepositoryException.class,
                () -> repository.findTopMathSchoolByStudentRange(minStudents, maxStudents));

        assertEquals("Ошибка при поиске лучшей школы", exception.getMessage());
        assertTrue(exception.getCause() instanceof SQLException);
    }

    @Test
    void testFindTopMathSchoolByStudentRange_InvalidRange() throws Exception {
        int minStudents = 10000;
        int maxStudents = 5000;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        MathSchoolStats result = repository.findTopMathSchoolByStudentRange(minStudents, maxStudents);
        assertNull(result);
    }

    @Test
    void testFindAverageStudentsByCountries_Success() throws Exception {
        int limit = 5;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        when(mockResultSet.getString("country_name"))
                .thenReturn("Butte")
                .thenReturn("Fresno");

        when(mockResultSet.getInt("school_count"))
                .thenReturn(10)
                .thenReturn(8);

        when(mockResultSet.getDouble("avg_students"))
                .thenReturn(450.5)
                .thenReturn(320.75);

        when(mockResultSet.getInt("min_students"))
                .thenReturn(100)
                .thenReturn(150);

        when(mockResultSet.getInt("max_students"))
                .thenReturn(800)
                .thenReturn(500);

        when(mockResultSet.getInt("total_students"))
                .thenReturn(4505)
                .thenReturn(2566);
        List<CountryStudentStats> result = repository.findAverageStudentsByCountries(limit);
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals("Butte", result.get(0).getCountryName());
        assertEquals(10, result.get(0).getSchoolCount());
        assertEquals(450.5, result.get(0).getAvgStudents(), 0.001);
        assertEquals(100, result.get(0).getMinStudents());
        assertEquals(800, result.get(0).getMaxStudents());
        assertEquals(4505, result.get(0).getTotalStudents());

        assertEquals("Fresno", result.get(1).getCountryName());
        assertEquals(8, result.get(1).getSchoolCount());
        assertEquals(320.75, result.get(1).getAvgStudents(), 0.001);
        assertEquals(150, result.get(1).getMinStudents());
        assertEquals(500, result.get(1).getMaxStudents());
        assertEquals(2566, result.get(1).getTotalStudents());

        verify(mockPreparedStatement).setInt(1, 5);
    }

    @Test
    void testFindAverageStudentsByCountries_EmptyResult() throws Exception {
        int limit = 10;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        List<CountryStudentStats> result = repository.findAverageStudentsByCountries(limit);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAverageStudentsByCountries_SQLException() throws Exception {
        int limit = 5;

        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));
        RepositoryException exception = assertThrows(RepositoryException.class,
                () -> repository.findAverageStudentsByCountries(limit));

        assertEquals("Ошибка при получении статистики студентов", exception.getMessage());
        assertTrue(exception.getCause() instanceof SQLException);
    }

    @Test
    void testFindAverageStudentsByCountries_ZeroLimit() throws Exception {
        int limit = 0;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        List<CountryStudentStats> result = repository.findAverageStudentsByCountries(limit);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockPreparedStatement).setInt(1, 0);
    }

    @Test
    void testFindAverageStudentsByCountries_NegativeLimit() throws Exception {
        int limit = -5;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        List<CountryStudentStats> result = repository.findAverageStudentsByCountries(limit);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mockPreparedStatement).setInt(1, -5);
    }

}