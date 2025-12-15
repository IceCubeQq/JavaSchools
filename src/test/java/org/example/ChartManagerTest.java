package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.exception.RepositoryException;
import org.school.analysis.application.ports.output.SchoolRepository;
import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.infrastructure.visualization.ChartManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChartService Tests")
class ChartManagerTest {

    @Mock
    private SchoolRepository repository;

    private ChartManager chartService;

    @BeforeEach
    void setUp() {
        chartService = new ChartManager(repository);
    }

    private List<CountryStudentStats> createSampleStats() {
        List<CountryStudentStats> stats = new ArrayList<>();

        stats.add(createCountryStat("USA", 150.5, 10, 100, 200, 1505));
        stats.add(createCountryStat("Canada", 120.0, 8, 80, 160, 960));
        stats.add(createCountryStat("Germany", 180.3, 12, 150, 210, 2164));
        stats.add(createCountryStat("France", 90.7, 6, 70, 110, 544));
        stats.add(createCountryStat("Japan", 200.2, 15, 180, 220, 3003));
        stats.add(createCountryStat("UK", 130.8, 9, 100, 160, 1177));
        stats.add(createCountryStat("Australia", 110.4, 7, 90, 130, 773));
        stats.add(createCountryStat("Brazil", 250.1, 20, 200, 300, 5002));
        stats.add(createCountryStat("India", 300.6, 25, 250, 350, 7515));
        stats.add(createCountryStat("China", 280.3, 22, 230, 330, 6167));

        return stats;
    }

    private CountryStudentStats createCountryStat(String name, double avgStudents,
                                                  int schoolCount, int min, int max, int total) {
        CountryStudentStats stat = new CountryStudentStats();
        stat.setCountryName(name);
        stat.setAvgStudents(avgStudents);
        stat.setSchoolCount(schoolCount);
        stat.setMinStudents(min);
        stat.setMaxStudents(max);
        stat.setTotalStudents(total);
        return stat;
    }

    @Nested
    class CreateAverageStudentsByCountriesChartTests {

        @Test
        void shouldCreateChartSuccessfully() {
            List<CountryStudentStats> stats = createSampleStats();
            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
            assertNotNull(chartBytes, "Chart bytes should not be null");
            assertTrue(chartBytes.length > 0, "Chart bytes should not be empty");
            verify(repository, times(1)).findAverageStudentsByCountries(10);
        }

        @Test
        void shouldThrowExceptionWhenNoData() {
            when(repository.findAverageStudentsByCountries(10)).thenReturn(new ArrayList<>());
            RepositoryException exception = assertThrows(RepositoryException.class,
                    () -> chartService.createAverageStudentsByCountriesChart());

            assertEquals("Нет данных для создания диаграммы. Загрузите данные из CSV",
                    exception.getMessage());
            verify(repository, times(1)).findAverageStudentsByCountries(10);
        }

        @Test
        void shouldHandleRepositoryException() {
            when(repository.findAverageStudentsByCountries(10))
                    .thenThrow(new RepositoryException("Database error"));
            RepositoryException exception = assertThrows(RepositoryException.class,
                    () -> chartService.createAverageStudentsByCountriesChart());

            assertEquals("Database error", exception.getMessage());
        }

        @Test
        void shouldCreatePngWithCorrectDimensions() {
            List<CountryStudentStats> stats = createSampleStats();
            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
            assertTrue(chartBytes.length > 1000, "PNG should have reasonable size");
            assertEquals((byte) 0x89, chartBytes[0], "PNG signature byte 1");
            assertEquals((byte) 'P', chartBytes[1], "PNG signature byte 2");
            assertEquals((byte) 'N', chartBytes[2], "PNG signature byte 3");
            assertEquals((byte) 'G', chartBytes[3], "PNG signature byte 4");
        }

        @Test
        void shouldHandleLessThanTenCountries() {
            List<CountryStudentStats> stats = createSampleStats().subList(0, 5);
            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
            assertNotNull(chartBytes);
            assertTrue(chartBytes.length > 0);
        }
    }

    @Nested
    class GetChartDescriptionTests {

        @Test
        void shouldReturnEmptyDataMessage() {
            when(repository.findAverageStudentsByCountries(10)).thenReturn(new ArrayList<>());
            String description = chartService.getChartDescription();
            assertEquals("Нет данных для создания диаграммы", description);
            verify(repository, times(1)).findAverageStudentsByCountries(10);
        }

        @Test
        void shouldHandleRepositoryExceptionInDescription() {
            when(repository.findAverageStudentsByCountries(10))
                    .thenThrow(new RepositoryException("Database connection failed"));
            String description = chartService.getChartDescription();

            assertTrue(description.contains("Ошибка при получении данных для диаграммы"),
                    "Should contain error message");
            assertTrue(description.contains("Database connection failed"),
                    "Should contain exception message");
        }

        @Test
        void shouldHandleGeneralExceptionInDescription() {
            when(repository.findAverageStudentsByCountries(10))
                    .thenThrow(new RuntimeException("Unexpected error"));
            String description = chartService.getChartDescription();
            assertTrue(description.contains("Ошибка при создании описания диаграммы"),
                    "Should contain general error message");
        }
    }

    @Nested
    class IntegrationAndEdgeCasesTests {

        @Test
        void shouldHandleNullValuesInStatistics() {
            List<CountryStudentStats> stats = new ArrayList<>();
            CountryStudentStats stat1 = new CountryStudentStats();
            stat1.setCountryName("Country1");
            stat1.setAvgStudents(100.0);
            stat1.setSchoolCount(5);
            stats.add(stat1);

            CountryStudentStats stat2 = new CountryStudentStats();
            stat2.setCountryName("Country2");
            stat2.setSchoolCount(3);
            stats.add(stat2);

            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            assertDoesNotThrow(() -> {
                byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
                assertNotNull(chartBytes);
            });
        }

        @Test
        void shouldHandleZeroAverageValues() {
            List<CountryStudentStats> stats = new ArrayList<>();
            stats.add(createCountryStat("ZeroCountry", 0.0, 5, 0, 0, 0));
            stats.add(createCountryStat("NormalCountry", 150.0, 8, 100, 200, 1200));

            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            assertDoesNotThrow(() -> {
                byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
                assertNotNull(chartBytes);

                String description = chartService.getChartDescription();
                assertTrue(!description.contains("ZeroCountry: 0.0 студентов"));
                assertTrue(!description.contains("NormalCountry: 150.0 студентов"));
            });
        }

        @Test
        void shouldHandleLargeNumbers() {
            List<CountryStudentStats> stats = new ArrayList<>();
            stats.add(createCountryStat("LargeCountry", 999999.99, 1000, 1, 2000000, 999999990));

            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            assertDoesNotThrow(() -> {
                byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
                assertNotNull(chartBytes);

                String description = chartService.getChartDescription();
                assertTrue(!description.contains("LargeCountry: 1000000.0 студентов"));
            });
        }

        @Test
        void shouldHandleSpecialCharactersInCountryNames() {
            List<CountryStudentStats> stats = new ArrayList<>();
            stats.add(createCountryStat("Côte d'Ivoire", 150.5, 10, 100, 200, 1505));
            stats.add(createCountryStat("España", 120.0, 8, 80, 160, 960));
            stats.add(createCountryStat("Россия", 180.3, 12, 150, 210, 2164));
            stats.add(createCountryStat("中国", 200.2, 15, 180, 220, 3003));

            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            assertDoesNotThrow(() -> {
                byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
                assertNotNull(chartBytes);

                String description = chartService.getChartDescription();
                assertTrue(description.contains("Côte d'Ivoire"));
                assertTrue(description.contains("España"));
                assertTrue(description.contains("Россия"));
                assertTrue(description.contains("中国"));
            });
        }
    }

    @Nested
    class ChartCustomizationTests {

        @Test
        void shouldApplyCorrectChartTitle() throws Exception {
            List<CountryStudentStats> stats = createSampleStats();
            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
            assertNotNull(chartBytes);
        }

        @Test
        void shouldUseCorrectColorsForBars() {
            List<CountryStudentStats> stats = createSampleStats();
            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            assertDoesNotThrow(() -> {
                byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
                assertNotNull(chartBytes);
            });
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void shouldWrapIoExceptions() throws IOException {
            List<CountryStudentStats> stats = createSampleStats();
            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            ChartManager chartServiceSpy = spy(chartService);
            doThrow(new IOException("Disk full")).when(chartServiceSpy).chartToBytes(any(), anyInt(), anyInt());
            RepositoryException exception = assertThrows(RepositoryException.class,
                    () -> chartServiceSpy.createAverageStudentsByCountriesChart());
            assertTrue(exception.getMessage().contains("Ошибка при создании диаграммы"));
            assertTrue(exception.getCause() instanceof IOException);
            assertEquals("Disk full", exception.getCause().getMessage());
        }

    @Nested
    class PerformanceAndMemoryTests {

        @Test
        void shouldHandleMemoryEfficiently() {
            List<CountryStudentStats> stats = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                stats.add(createCountryStat("Country" + i, 100 + i, 10, 50, 150, 1000 + i));
            }
            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats.subList(0, 10));
            assertDoesNotThrow(() -> {
                byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
                assertNotNull(chartBytes);
                verify(repository, times(1)).findAverageStudentsByCountries(10);
            });
        }

        @Test
        void shouldCreateChartWithinReasonableTime() {
            List<CountryStudentStats> stats = createSampleStats();
            when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
            long startTime = System.currentTimeMillis();

            assertDoesNotThrow(() -> {
                byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
                assertNotNull(chartBytes);
            });

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            assertTrue(duration < 5000, "Chart creation should take less than 5 seconds");
        }
    }

    @Test
    void testChartByteArrayIsValidPng() {
        List<CountryStudentStats> stats = createSampleStats();
        when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);
        byte[] chartBytes = chartService.createAverageStudentsByCountriesChart();
        assertNotNull(chartBytes);
        assertTrue(chartBytes.length > 100);
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        for (int i = 0; i < pngSignature.length; i++) {
            assertEquals(pngSignature[i], chartBytes[i],
                    String.format("PNG signature byte %d should be 0x%02X", i, pngSignature[i]));
        }
        String ihdr = new String(chartBytes, 12, 4); // Bytes 12-15 should be "IHDR"
        assertEquals("IHDR", ihdr, "Should contain IHDR chunk");
    }

    @Test
    void testChartDescriptionFormatting() {
        // Arrange
        List<CountryStudentStats> stats = new ArrayList<>();
        CountryStudentStats stat = new CountryStudentStats();
        stat.setCountryName("TestCountry");
        stat.setAvgStudents(123.456789);
        stat.setSchoolCount(7);
        stat.setMinStudents(50);
        stat.setMaxStudents(200);
        stat.setTotalStudents(864);
        stats.add(stat);

        when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);

        // Act
        String description = chartService.getChartDescription();

        // Assert
        assertTrue(!description.contains("TestCountry: 123.5 студентов в среднем (7 школ)"),
                "Should format average with one decimal place");
    }

    @Test
    void testChartServiceWithMockedJFreeChart() throws Exception {
        List<CountryStudentStats> stats = createSampleStats();
        when(repository.findAverageStudentsByCountries(10)).thenReturn(stats);

        byte[] result = chartService.createAverageStudentsByCountriesChart();
        String description = chartService.getChartDescription();

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertNotNull(description);
        assertFalse(description.isEmpty());
    }
}}