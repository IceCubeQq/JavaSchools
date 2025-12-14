package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.school.analysis.domain.model.School;
import org.school.analysis.infrastructure.database.DatabaseManagerImpl;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatabaseManagerImplTest {

    private DatabaseManagerImpl databaseManager;
    private Connection mockConnection;
    private Statement mockStatement;
    private PreparedStatement mockPreparedStatement;
    private DatabaseMetaData mockMetaData;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        databaseManager = new DatabaseManagerImpl();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (databaseManager != null) {
            try {
                databaseManager.close();
            } catch (Exception ignored) {
            }
        }
    }


    @Test
    void testCreateTables_Success() throws Exception {
        setupMocks();

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        databaseManager.createTables();
        verify(mockStatement, atLeast(11)).execute(anyString());
    }

    @Test
    void testCreateTables_SQLException() throws Exception {
        setupMocks();

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        doThrow(new SQLException("Table creation failed")).when(mockStatement).execute(anyString());
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> databaseManager.createTables());

        assertTrue(exception.getMessage().contains("Не удалось создать таблицы"));
    }

    @Test
    void testInsertSchools_Success() throws Exception {
        setupMocks();
        List<School> schools = createTestSchools(2);

        when(mockConnection.prepareStatement(anyString()))
                .thenReturn(mockPreparedStatement);
        PreparedStatement mockCountyPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockConnection.prepareStatement(eq("INSERT OR IGNORE INTO counties (name) VALUES (?)")))
                .thenReturn(mockCountyPreparedStatement);
        when(mockConnection.prepareStatement(contains("SELECT id FROM counties")))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);
        databaseManager.insertSchools(schools);
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection).commit();
        verify(mockConnection).setAutoCommit(true);
    }

    @Test
    void testInsertSchools_RollbackOnError() throws Exception {
        setupMocks();
        List<School> schools = createTestSchools(2);

        when(mockConnection.prepareStatement(anyString()))
                .thenThrow(new SQLException("Batch execution failed"));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> databaseManager.insertSchools(schools));

        assertTrue(exception.getMessage().contains("Не удалось сохранить школы в базу данных"));
        verify(mockConnection).rollback();
        verify(mockConnection).setAutoCommit(true);
    }

    @Test
    void testInsertSchools_NullList() throws Exception {
        setupMocks();
        assertThrows(NullPointerException.class,
                () -> databaseManager.insertSchools(null));
    }

    @Test
    void testInsertCounties_Success() throws Exception {
        setupMocks();
        List<School> schools = createTestSchools(3);
        PreparedStatement mockCountyStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("INSERT OR IGNORE INTO counties (name) VALUES (?)")))
                .thenReturn(mockCountyStmt);
        var method = DatabaseManagerImpl.class.getDeclaredMethod("insertCounties", List.class);
        method.setAccessible(true);
        method.invoke(databaseManager, schools);
        verify(mockCountyStmt, times(3)).addBatch();
        verify(mockCountyStmt).executeBatch();
    }

    @Test
    void testInsertCounties_WithNullCountry() throws Exception {
        setupMocks();
        List<School> schools = new ArrayList<>();
        School school1 = new School();
        school1.setCountry(null);

        School school2 = new School();
        school2.setCountry("");

        School school3 = new School();
        school3.setCountry("  ");

        School school4 = new School();
        school4.setCountry("Valid County");

        schools.addAll(Arrays.asList(school1, school2, school3, school4));

        PreparedStatement mockCountyStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("INSERT OR IGNORE INTO counties (name) VALUES (?)")))
                .thenReturn(mockCountyStmt);
        var method = DatabaseManagerImpl.class.getDeclaredMethod("insertCounties", List.class);
        method.setAccessible(true);
        method.invoke(databaseManager, schools);
        verify(mockCountyStmt, times(1)).addBatch();
        verify(mockCountyStmt).executeBatch();
    }

    @Test
    void testInsertFinancialsData_Success() throws Exception {
        setupMocks();
        List<School> schools = createTestSchools(1);

        PreparedStatement mockFinancialStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO school_financials")))
                .thenReturn(mockFinancialStmt);
        var method = DatabaseManagerImpl.class.getDeclaredMethod("insertFinancialsData", List.class);
        method.setAccessible(true);
        method.invoke(databaseManager, schools);
        verify(mockFinancialStmt, times(1)).addBatch();
        verify(mockFinancialStmt).executeBatch();
        verify(mockFinancialStmt).setInt(1, 1);
        verify(mockFinancialStmt).setObject(2, 10.5);
        verify(mockFinancialStmt).setObject(3, 20.5);
    }

    @Test
    void testInsertPerformanceData_Success() throws Exception {
        setupMocks();
        List<School> schools = createTestSchools(1);

        PreparedStatement mockPerformanceStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO school_performance")))
                .thenReturn(mockPerformanceStmt);
        var method = DatabaseManagerImpl.class.getDeclaredMethod("insertPerformanceData", List.class);
        method.setAccessible(true);
        method.invoke(databaseManager, schools);
        verify(mockPerformanceStmt, times(1)).addBatch();
        verify(mockPerformanceStmt).executeBatch();

        verify(mockPerformanceStmt).setInt(1, 1);
        verify(mockPerformanceStmt).setObject(2, 30.5);
        verify(mockPerformanceStmt).setObject(3, 700.0);
    }

    @Test
    void testGetConnection_Success() throws Exception {
        setupMocks();
        Connection connection = databaseManager.getConnection();

        assertNotNull(connection);
        assertEquals(mockConnection, connection);
    }

    @Test
    void testGetConnection_NotConnected() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> databaseManager.getConnection());

        assertTrue(exception.getMessage().contains("Соединение с БД не установлено"));
    }

    @Test
    void testClose_Success() throws Exception {
        setupMocks();
        when(mockConnection.isClosed()).thenReturn(false);
        databaseManager.close();
        verify(mockConnection).close();
    }

    @Test
    void testClose_NullConnection() {
        assertDoesNotThrow(() -> databaseManager.close());
    }

    @Test
    void testClose_AlreadyClosed() throws Exception {
        setupMocks();
        when(mockConnection.isClosed()).thenReturn(true);
        databaseManager.close();

        verify(mockConnection, never()).close();
    }

    @Test
    void testClose_SQLException() throws Exception {
        setupMocks();
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();

        assertDoesNotThrow(() -> databaseManager.close());

        verify(mockConnection).close();
    }

    @Test
    void testSetAutoCommit_RestoreOnException() throws Exception {
        setupMocks();
        List<School> schools = createTestSchools(1);

        when(mockConnection.prepareStatement(anyString()))
                .thenThrow(new SQLException("Test exception"));

        doThrow(new SQLException("Cannot set auto-commit")).when(mockConnection).setAutoCommit(true);

        assertThrows(RuntimeException.class,
                () -> databaseManager.insertSchools(schools));

        verify(mockConnection).setAutoCommit(true);
    }

    @Test
    void testInsertSchools_BatchExecutionReturnsNoUpdates() throws Exception {
        setupMocks();
        List<School> schools = createTestSchools(2);

        PreparedStatement mockCountyStmt = mock(PreparedStatement.class);
        PreparedStatement mockDistrictStmt = mock(PreparedStatement.class);
        PreparedStatement mockSchoolsStmt = mock(PreparedStatement.class);
        PreparedStatement mockFinancialStmt = mock(PreparedStatement.class);
        PreparedStatement mockPerformanceStmt = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement(eq("INSERT OR IGNORE INTO counties (name) VALUES (?)")))
                .thenReturn(mockCountyStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR IGNORE INTO districts")))
                .thenReturn(mockDistrictStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO schools")))
                .thenReturn(mockSchoolsStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO school_financials")))
                .thenReturn(mockFinancialStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO school_performance")))
                .thenReturn(mockPerformanceStmt);
        when(mockConnection.prepareStatement(contains("SELECT id FROM counties")))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        assertDoesNotThrow(() -> databaseManager.insertSchools(schools));

        verify(mockConnection).commit();
    }

    @Test
    void testInsertSchools_DuplicateData() throws Exception {
        setupMocks();
        List<School> schools = createTestSchools(2);
        schools.add(schools.get(0));

        PreparedStatement mockCountyStmt = mock(PreparedStatement.class);
        PreparedStatement mockDistrictStmt = mock(PreparedStatement.class);
        PreparedStatement mockSchoolsStmt = mock(PreparedStatement.class);
        PreparedStatement mockFinancialStmt = mock(PreparedStatement.class);
        PreparedStatement mockPerformanceStmt = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement(eq("INSERT OR IGNORE INTO counties (name) VALUES (?)")))
                .thenReturn(mockCountyStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR IGNORE INTO districts")))
                .thenReturn(mockDistrictStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO schools")))
                .thenReturn(mockSchoolsStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO school_financials")))
                .thenReturn(mockFinancialStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO school_performance")))
                .thenReturn(mockPerformanceStmt);
        when(mockConnection.prepareStatement(contains("SELECT id FROM counties")))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        assertDoesNotThrow(() -> databaseManager.insertSchools(schools));

        verify(mockConnection).commit();
    }

    @Test
    void testInsertSchools_MixedValidAndInvalidData() throws Exception {
        setupMocks();
        List<School> schools = new ArrayList<>();
        School validSchool = createTestSchool(1);
        School nullSchool = new School();
        nullSchool.setId(2);
        nullSchool.setDistrictId(23456);
        nullSchool.setName(null);
        nullSchool.setCountry(null);
        nullSchool.setGrades(null);

        School emptySchool = new School();
        emptySchool.setId(3);
        emptySchool.setDistrictId(34567);
        emptySchool.setName("");
        emptySchool.setCountry("");
        emptySchool.setGrades("");

        schools.addAll(Arrays.asList(validSchool, nullSchool, emptySchool));

        PreparedStatement mockCountyStmt = mock(PreparedStatement.class);
        PreparedStatement mockDistrictStmt = mock(PreparedStatement.class);
        PreparedStatement mockSchoolsStmt = mock(PreparedStatement.class);
        PreparedStatement mockFinancialStmt = mock(PreparedStatement.class);
        PreparedStatement mockPerformanceStmt = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement(eq("INSERT OR IGNORE INTO counties (name) VALUES (?)")))
                .thenReturn(mockCountyStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR IGNORE INTO districts")))
                .thenReturn(mockDistrictStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO schools")))
                .thenReturn(mockSchoolsStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO school_financials")))
                .thenReturn(mockFinancialStmt);
        when(mockConnection.prepareStatement(contains("INSERT OR REPLACE INTO school_performance")))
                .thenReturn(mockPerformanceStmt);
        when(mockConnection.prepareStatement(contains("SELECT id FROM counties")))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        assertDoesNotThrow(() -> databaseManager.insertSchools(schools));

        verify(mockConnection).commit();
        verify(mockCountyStmt, times(1)).addBatch();
    }

    @Test
    void testInsertSchools_TransactionIsolation() throws Exception {
        setupMocks();
        List<School> schools = createTestSchools(3);
        PreparedStatement mockCountyStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(eq("INSERT OR IGNORE INTO counties (name) VALUES (?)")))
                .thenReturn(mockCountyStmt);

        PreparedStatement mockDistrictStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(contains("INSERT OR IGNORE INTO districts")))
                .thenReturn(mockDistrictStmt);
        doThrow(new SQLException("Third batch failed")).when(mockDistrictStmt).executeBatch();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> databaseManager.insertSchools(schools));

        verify(mockConnection).rollback();
        verify(mockConnection, never()).commit();
    }

    private void setupMocks() throws Exception {
        mockConnection = mock(Connection.class);
        mockStatement = mock(Statement.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockMetaData = mock(DatabaseMetaData.class);
        var connectionField = DatabaseManagerImpl.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseManager, mockConnection);

        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getDatabaseProductName()).thenReturn("SQLite");
    }

    private List<School> createTestSchools(int count) {
        List<School> schools = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            schools.add(createTestSchool(i));
        }
        return schools;
    }

    private School createTestSchool(int id) {
        School school = new School();
        school.setId(id);
        school.setDistrictId(12345);
        school.setName("Test School " + id);
        school.setCountry("Test County");
        school.setGrades("KK-08");
        school.setStudents(100);
        school.setTeachers(10.5);
        school.setCalworks(10.5);
        school.setLunch(20.5);
        school.setComputers(30);
        school.setExpenditure(5000.0);
        school.setIncome(25.0);
        school.setEnglish(30.5);
        school.setReadScore(700.0);
        school.setMathScore(650.0);
        return school;
    }
}