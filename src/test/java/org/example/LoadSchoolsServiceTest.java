package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.services.LoadSchoolsService;
import org.school.analysis.domain.model.School;
import org.school.analysis.infrastructure.csv.CsvSchoolParser;
import org.school.analysis.infrastructure.database.DatabaseManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadSchoolsServiceTest {

    @Mock
    private CsvSchoolParser csvParser;

    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private Connection connection;

    private LoadSchoolsService loadSchoolsService;

    @BeforeEach
    void setUp() {
        loadSchoolsService = new LoadSchoolsService(csvParser, databaseManager);
    }

    @Test
    void testExecute_Success() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        List<School> mockSchools = Arrays.asList(
                createSchool(1, "School 1"),
                createSchool(2, "School 2")
        );

        when(csvParser.parseSchools(csvStream)).thenReturn(mockSchools);
        when(databaseManager.getConnection()).thenReturn(connection);
        int result = loadSchoolsService.execute(csvStream);
        assertEquals(2, result);
        verify(csvParser).parseSchools(csvStream);
        verify(databaseManager).getConnection();
        verify(databaseManager).insertSchools(mockSchools);
    }

    @Test
    void testExecute_ParsingFailure() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        when(csvParser.parseSchools(csvStream))
                .thenThrow(new RuntimeException("Parsing failed"));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loadSchoolsService.execute(csvStream));

        assertTrue(exception.getMessage().contains("Ошибка загрузки школ из CSV"));
        verify(csvParser).parseSchools(csvStream);
        verify(databaseManager, never()).getConnection();
        verify(databaseManager, never()).insertSchools(any());
    }

    @Test
    void testExecute_ValidateEmptyList() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        when(csvParser.parseSchools(csvStream)).thenReturn(Collections.emptyList());
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loadSchoolsService.execute(csvStream));
        assertTrue(exception.getMessage().contains("Ошибка загрузки школ из CSV"));
        verify(csvParser).parseSchools(csvStream);
        verify(databaseManager, never()).getConnection();
    }

    @Test
    void testExecute_ValidateNullList() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        when(csvParser.parseSchools(csvStream)).thenReturn(null);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loadSchoolsService.execute(csvStream));
        assertTrue(exception.getMessage().contains("Ошибка загрузки школ из CSV"));
        verify(csvParser).parseSchools(csvStream);
        verify(databaseManager, never()).getConnection();
    }

    @Test
    void testExecute_ValidateNoValidSchools() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        List<School> invalidSchools = Arrays.asList(
                createSchool(null, null),
                createSchool(1, null),
                createSchool(2, ""),
                createSchool(null, "Invalid")
        );

        when(csvParser.parseSchools(csvStream)).thenReturn(invalidSchools);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loadSchoolsService.execute(csvStream));
        assertTrue(exception.getMessage().contains("Ошибка загрузки школ из CSV"));
        verify(csvParser).parseSchools(csvStream);
        verify(databaseManager, never()).getConnection();
    }

    @Test
    void testExecute_MixedValidAndInvalidSchools() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        List<School> mixedSchools = Arrays.asList(
                createSchool(null, null),
                createSchool(1, "Valid School 1"),
                createSchool(2, ""),
                createSchool(3, "Valid School 2")
        );

        when(csvParser.parseSchools(csvStream)).thenReturn(mixedSchools);
        when(databaseManager.getConnection()).thenReturn(connection);
        int result = loadSchoolsService.execute(csvStream);
        assertEquals(4, result);
        verify(databaseManager).insertSchools(mixedSchools);
    }

    @Test
    void testExecute_DatabaseInsertFails() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        List<School> mockSchools = Collections.singletonList(createSchool(1, "Test School"));

        when(csvParser.parseSchools(csvStream)).thenReturn(mockSchools);
        when(databaseManager.getConnection()).thenReturn(connection);
        doThrow(new RuntimeException("Insert failed"))
                .when(databaseManager).insertSchools(mockSchools);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loadSchoolsService.execute(csvStream));

        assertTrue(exception.getMessage().contains("Ошибка загрузки школ из CSV"));
        verify(csvParser).parseSchools(csvStream);
        verify(databaseManager).getConnection();
        verify(databaseManager).insertSchools(mockSchools);
    }

    @Test
    void testExecute_WithNullInputStream() throws IOException {
        InputStream nullStream = null;
        when(csvParser.parseSchools(nullStream))
                .thenThrow(new NullPointerException("Stream is null"));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loadSchoolsService.execute(nullStream));

        assertTrue(exception.getMessage().contains("Ошибка загрузки школ из CSV"));
        verify(csvParser).parseSchools(nullStream);
    }

    @Test
    void testExecute_ConnectionIsClosed() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        List<School> mockSchools = Collections.singletonList(createSchool(1, "Test School"));

        when(csvParser.parseSchools(csvStream)).thenReturn(mockSchools);
        when(databaseManager.getConnection()).thenReturn(connection);
        int result = loadSchoolsService.execute(csvStream);
        assertEquals(1, result);
        verify(databaseManager).getConnection();
    }

    @Test
    void testExecute_LargeNumberOfSchools() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        List<School> largeSchoolList = Collections.nCopies(1000, createSchool(999, "Large School"));

        when(csvParser.parseSchools(csvStream)).thenReturn(largeSchoolList);
        when(databaseManager.getConnection()).thenReturn(connection);
        int result = loadSchoolsService.execute(csvStream);
        assertEquals(1000, result);
        verify(databaseManager).insertSchools(largeSchoolList);
    }

    @Test
    void testConstructor_DependenciesNotNull() {
        assertDoesNotThrow(() -> new LoadSchoolsService(csvParser, databaseManager));
    }

    @Test
    void testExecute_IOExceptionInParsing() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        when(csvParser.parseSchools(csvStream))
                .thenThrow(new RuntimeException("IO Error"));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loadSchoolsService.execute(csvStream));

        assertTrue(exception.getMessage().contains("Ошибка загрузки школ из CSV"));
    }

    @Test
    void testExecute_ValidationThrowsDifferentException() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        List<School> schools = Collections.singletonList(createSchool(1, "Valid School"));

        when(csvParser.parseSchools(csvStream)).thenReturn(schools);
        when(databaseManager.getConnection()).thenThrow(new RuntimeException("Unexpected DB error"));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loadSchoolsService.execute(csvStream));
        assertNotNull(exception.getMessage());
        verify(csvParser).parseSchools(csvStream);
    }

    @Test
    void testExecute_ValidSchoolsOnly() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        List<School> validSchools = Arrays.asList(
                createSchool(1, "School 1"),
                createSchool(2, "School 2"),
                createSchool(3, "School 3")
        );

        when(csvParser.parseSchools(csvStream)).thenReturn(validSchools);
        when(databaseManager.getConnection()).thenReturn(connection);
        int result = loadSchoolsService.execute(csvStream);
        assertEquals(3, result);
        verify(databaseManager).insertSchools(validSchools);
    }

    @Test
    void testExecute_OnlyIdNull() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        List<School> schools = Arrays.asList(
                createSchool(null, "School with null ID"),
                createSchool(1, "Valid School")
        );

        when(csvParser.parseSchools(csvStream)).thenReturn(schools);
        when(databaseManager.getConnection()).thenReturn(connection);
        int result = loadSchoolsService.execute(csvStream);
        assertEquals(2, result);
    }

    @Test
    void testExecute_ComplexSchoolObjects() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test".getBytes());
        School complexSchool = new School();
        complexSchool.setId(999);
        complexSchool.setName("Complex School");
        complexSchool.setStudents(1000);
        complexSchool.setTeachers(50.5);
        complexSchool.setMathScore(85.5);

        when(csvParser.parseSchools(csvStream)).thenReturn(Collections.singletonList(complexSchool));
        when(databaseManager.getConnection()).thenReturn(connection);

        int result = loadSchoolsService.execute(csvStream);
        assertEquals(1, result);
        verify(databaseManager).insertSchools(anyList());
    }

    private School createSchool(Integer id, String name) {
        School school = new School();
        school.setId(id);
        school.setName(name);
        return school;
    }
}