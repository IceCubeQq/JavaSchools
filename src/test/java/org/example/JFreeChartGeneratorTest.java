package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.exception.RepositoryException;
import org.school.analysis.infrastructure.adapters.JFreeChartGenerator;
import org.school.analysis.infrastructure.visualization.ChartService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JFreeChartGeneratorTest {

    private JFreeChartGenerator chartGenerator;

    @Mock
    private ChartService chartService;

    @BeforeEach
    void setUp() {
        chartGenerator = new JFreeChartGenerator(chartService);
    }

    @Test
    void createAverageStudentsChart_ShouldReturnByteArray_WhenChartServiceSucceeds() {
        byte[] expectedChartData = new byte[]{1, 2, 3, 4, 5};
        when(chartService.createAverageStudentsByCountriesChart()).thenReturn(expectedChartData);
        byte[] result = chartGenerator.createAverageStudentsChart();
        assertNotNull(result);
        assertArrayEquals(expectedChartData, result);
        verify(chartService).createAverageStudentsByCountriesChart();
    }

    @Test
    void createAverageStudentsChart_ShouldThrowRuntimeException_WhenChartServiceThrowsException() {
        String errorMessage = "Chart creation failed";
        when(chartService.createAverageStudentsByCountriesChart())
                .thenThrow(new RuntimeException(errorMessage));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chartGenerator.createAverageStudentsChart());

        assertEquals("Не удалось создать диаграмму", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals(errorMessage, exception.getCause().getMessage());
        verify(chartService).createAverageStudentsByCountriesChart();
    }

    @Test
    void createAverageStudentsChart_ShouldWrapRepositoryException_WhenChartServiceThrowsIt() {
        String errorMessage = "Database error";
        when(chartService.createAverageStudentsByCountriesChart())
                .thenThrow(new RepositoryException(errorMessage));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chartGenerator.createAverageStudentsChart());

        assertEquals("Не удалось создать диаграмму", exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(RepositoryException.class, exception.getCause());
        assertEquals(errorMessage, exception.getCause().getMessage());
    }

    @Test
    void createAverageStudentsChart_ShouldWrapNullPointerException_WhenChartServiceThrowsIt() {
        when(chartService.createAverageStudentsByCountriesChart())
                .thenThrow(new NullPointerException("Null data"));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chartGenerator.createAverageStudentsChart());

        assertEquals("Не удалось создать диаграмму", exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(NullPointerException.class, exception.getCause());
        assertEquals("Null data", exception.getCause().getMessage());
    }

    @Test
    void getChartDescription_ShouldReturnDescription_WhenChartServiceSucceeds() {
        String expectedDescription = "Диаграмма: Среднее количество студентов по странам";
        when(chartService.getChartDescription()).thenReturn(expectedDescription);
        String result = chartGenerator.getChartDescription();
        assertNotNull(result);
        assertEquals(expectedDescription, result);
        verify(chartService).getChartDescription();
    }

    @Test
    void getChartDescription_ShouldReturnRepositoryErrorMessage_WhenRepositoryExceptionIsThrown() {
        String errorMessage = "Database connection failed";
        when(chartService.getChartDescription())
                .thenThrow(new RepositoryException(errorMessage));
        String result = chartGenerator.getChartDescription();
        assertNotNull(result);
        assertTrue(result.contains("Ошибка получения данных для диаграммы"));
        assertTrue(result.contains(errorMessage));
        verify(chartService).getChartDescription();
    }

    @Test
    void getChartDescription_ShouldReturnGenericErrorMessage_WhenGenericExceptionIsThrown() {
        when(chartService.getChartDescription())
                .thenThrow(new RuntimeException("Unexpected error"));
        String result = chartGenerator.getChartDescription();
        assertNotNull(result);
        assertEquals("Ошибка создания диаграммы", result);
        verify(chartService).getChartDescription();
    }

    @Test
    void getChartDescription_ShouldReturnGenericErrorMessage_WhenNullPointerExceptionIsThrown() {
        when(chartService.getChartDescription())
                .thenThrow(new NullPointerException());
        String result = chartGenerator.getChartDescription();
        assertNotNull(result);
        assertEquals("Ошибка создания диаграммы", result);
        verify(chartService).getChartDescription();
    }

    @Test
    void getChartDescription_ShouldReturnGenericErrorMessage_WhenIllegalArgumentExceptionIsThrown() {
        when(chartService.getChartDescription())
                .thenThrow(new IllegalArgumentException("Invalid argument"));
        String result = chartGenerator.getChartDescription();
        assertNotNull(result);
        assertEquals("Ошибка создания диаграммы", result);
        verify(chartService).getChartDescription();
    }

    @Test
    void getChartDescription_ShouldHandleEmptyString_WhenChartServiceReturnsIt() {
        when(chartService.getChartDescription()).thenReturn("");
        String result = chartGenerator.getChartDescription();
        assertNotNull(result);
        assertEquals("", result);
        verify(chartService).getChartDescription();
    }

    @Test
    void getChartDescription_ShouldHandleNull_WhenChartServiceReturnsIt() {
        when(chartService.getChartDescription()).thenReturn(null);
        String result = chartGenerator.getChartDescription();
        assertNull(result);
        verify(chartService).getChartDescription();
    }

    @Test
    void createAverageStudentsChart_ShouldHandleNullReturn_WhenChartServiceReturnsNull() {
        when(chartService.createAverageStudentsByCountriesChart()).thenReturn(null);
        byte[] result = chartGenerator.createAverageStudentsChart();
        assertNull(result);
        verify(chartService).createAverageStudentsByCountriesChart();
    }

    @Test
    void createAverageStudentsChart_ShouldHandleEmptyByteArray_WhenChartServiceReturnsIt() {
        byte[] emptyArray = new byte[0];
        when(chartService.createAverageStudentsByCountriesChart()).thenReturn(emptyArray);
        byte[] result = chartGenerator.createAverageStudentsChart();
        assertNotNull(result);
        assertEquals(0, result.length);
        verify(chartService).createAverageStudentsByCountriesChart();
    }

    @Test
    void getChartDescription_ShouldPreserveFormatting_WhenChartServiceReturnsFormattedText() {
        String formattedDescription = """
            Диаграмма: Среднее количество студентов по странам
            
            Статистика по странам:
            • Fresno: 450.5 студентов в среднем (15 школ)
            • Contra Costa: 380.2 студентов в среднем (12 школ)
            
            Диаграмма построена на основе данных из CSV файла
            """;

        when(chartService.getChartDescription()).thenReturn(formattedDescription);
        String result = chartGenerator.getChartDescription();
        assertNotNull(result);
        assertEquals(formattedDescription, result);
        verify(chartService).getChartDescription();
    }

    @Test
    void getChartDescription_ShouldHandleSpecialCharacters() {
        String specialDescription = "Диаграмма с спецсимволами: © ® ™ € £ ¥ ± ≠ ≤ ≥";
        when(chartService.getChartDescription()).thenReturn(specialDescription);
        String result = chartGenerator.getChartDescription();
        assertNotNull(result);
        assertEquals(specialDescription, result);
        verify(chartService).getChartDescription();
    }

    @Test
    void createAverageStudentsChart_ShouldPropagateInterruptedException() {
        when(chartService.createAverageStudentsByCountriesChart())
                .thenThrow(new RuntimeException(new InterruptedException("Thread interrupted")));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chartGenerator.createAverageStudentsChart());

        assertEquals("Не удалось создать диаграмму", exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }

    @Test
    void sameInstance_ShouldUseSameChartService_ForMultipleCalls() {
        byte[] chartData = new byte[]{1, 2, 3};
        String description = "Test description";

        when(chartService.createAverageStudentsByCountriesChart()).thenReturn(chartData);
        when(chartService.getChartDescription()).thenReturn(description);
        byte[] result1 = chartGenerator.createAverageStudentsChart();
        String result2 = chartGenerator.getChartDescription();
        byte[] result3 = chartGenerator.createAverageStudentsChart();
        assertArrayEquals(chartData, result1);
        assertEquals(description, result2);
        assertArrayEquals(chartData, result3);

        verify(chartService, times(2)).createAverageStudentsByCountriesChart();
        verify(chartService, times(1)).getChartDescription();
    }

    @Test
    void getChartDescription_ShouldReturnRepositoryExceptionMessageWithDetails() {
        RepositoryException repoException = new RepositoryException(
                "Database error: Connection timeout after 30 seconds. Retry count: 3",
                new SQLException("Connection timed out")
        );

        when(chartService.getChartDescription()).thenThrow(repoException);
        String result = chartGenerator.getChartDescription();
        assertNotNull(result);
        assertTrue(result.contains("Ошибка получения данных для диаграммы"));
        assertTrue(result.contains("Database error: Connection timeout after 30 seconds. Retry count: 3"));
        verify(chartService).getChartDescription();
    }
    private static class SQLException extends Exception {
        public SQLException(String message) {
            super(message);
        }
    }
}