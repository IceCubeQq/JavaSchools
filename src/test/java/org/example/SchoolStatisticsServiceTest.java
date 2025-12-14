package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.exception.DatabaseExceptionHandler;
import org.school.analysis.application.exception.RepositoryException;
import org.school.analysis.application.ports.output.SchoolRepository;
import org.school.analysis.application.services.SchoolStatisticsService;
import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.domain.dto.ExpenditureStats;
import org.school.analysis.domain.dto.MathSchoolStats;
import org.school.analysis.presentation.TelegramOutputService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolStatisticsServiceTest {

    private SchoolStatisticsService statisticsService;

    @Mock
    private SchoolRepository repository;

    @Mock
    private TelegramOutputService telegramOutputService;

    @Mock
    private DatabaseExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        statisticsService = new SchoolStatisticsService(
                repository,
                telegramOutputService,
                exceptionHandler
        );
    }

    @Test
    void getExpenditureReportForTelegram_ShouldReturnFormattedReport_WhenDataExists() {
        List<ExpenditureStats> mockStats = Arrays.asList(
                createExpenditureStat("Fresno", 5, 15000.50, 10000.00, 20000.00),
                createExpenditureStat("Contra Costa", 3, 12000.75, 11000.00, 13000.00)
        );

        String expectedReport = "Средние расходы...";

        when(repository.findAverageExpenditureInCounties(
                anyList(),
                eq(10.0)
        )).thenReturn(mockStats);

        when(telegramOutputService.formatExpenditureStatsForTelegram(mockStats))
                .thenReturn(expectedReport);
        String result = statisticsService.getExpenditureReportForTelegram();
        assertNotNull(result);
        assertEquals(expectedReport, result);
        verify(repository).findAverageExpenditureInCounties(
                argThat(list ->
                        list.containsAll(Arrays.asList("Fresno", "Contra Costa", "El Dorado", "Glenn")) &&
                                list.size() == 4
                ),
                eq(10.0)
        );
        verify(telegramOutputService).formatExpenditureStatsForTelegram(mockStats);
        verifyNoInteractions(exceptionHandler);
    }

    @Test
    void getExpenditureReportForTelegram_ShouldHandleRepositoryException() {
        RepositoryException repositoryException = new RepositoryException("DB error");
        String errorMessage = "Ошибка базы данных при получении расходов: DB error";

        when(repository.findAverageExpenditureInCounties(anyList(), anyDouble()))
                .thenThrow(repositoryException);

        when(exceptionHandler.handleExpenditureQueryError(repositoryException))
                .thenReturn(errorMessage);
        String result = statisticsService.getExpenditureReportForTelegram();
        assertNotNull(result);
        assertEquals(errorMessage, result);

        verify(repository).findAverageExpenditureInCounties(anyList(), eq(10.0));
        verify(exceptionHandler).handleExpenditureQueryError(repositoryException);
        verifyNoInteractions(telegramOutputService);
    }

    @Test
    void getExpenditureReportForTelegram_ShouldHandleGenericException() {
        RuntimeException runtimeException = new RuntimeException("Network error");
        String errorMessage = "Ошибка при выполнении запроса расходов: Network error";

        when(repository.findAverageExpenditureInCounties(anyList(), anyDouble()))
                .thenThrow(runtimeException);

        when(exceptionHandler.handleExpenditureQueryError(runtimeException))
                .thenReturn(errorMessage);
        String result = statisticsService.getExpenditureReportForTelegram();
        assertNotNull(result);
        assertEquals(errorMessage, result);

        verify(exceptionHandler).handleExpenditureQueryError(runtimeException);
    }

    @Test
    void getMathSchoolsReportForTelegram_ShouldReturnFormattedReport_WhenBothRangesHaveData() {
        MathSchoolStats range1Stats = createMathSchoolStats(1, "School A", "Fresno", 6000, 95.5, 12000.0);
        MathSchoolStats range2Stats = createMathSchoolStats(2, "School B", "Contra Costa", 10500, 98.2, 15000.0);

        String expectedReport = "Лучшие школы по математике...";

        when(repository.findTopMathSchoolByStudentRange(5000, 7500))
                .thenReturn(range1Stats);
        when(repository.findTopMathSchoolByStudentRange(10000, 11000))
                .thenReturn(range2Stats);

        when(telegramOutputService.formatAllMathSchoolsStatsForTelegram(range1Stats, range2Stats))
                .thenReturn(expectedReport);
        String result = statisticsService.getMathSchoolsReportForTelegram();
        assertNotNull(result);
        assertEquals(expectedReport, result);

        verify(repository).findTopMathSchoolByStudentRange(5000, 7500);
        verify(repository).findTopMathSchoolByStudentRange(10000, 11000);
        verify(telegramOutputService).formatAllMathSchoolsStatsForTelegram(range1Stats, range2Stats);
        verifyNoInteractions(exceptionHandler);
    }

    @Test
    void getMathSchoolsReportForTelegram_ShouldHandleNullResults() {
        String expectedReport = "Лучшие школы по математике...";

        when(repository.findTopMathSchoolByStudentRange(5000, 7500))
                .thenReturn(null);
        when(repository.findTopMathSchoolByStudentRange(10000, 11000))
                .thenReturn(null);

        when(telegramOutputService.formatAllMathSchoolsStatsForTelegram(null, null))
                .thenReturn(expectedReport);
        String result = statisticsService.getMathSchoolsReportForTelegram();
        assertNotNull(result);
        assertEquals(expectedReport, result);

        verify(telegramOutputService).formatAllMathSchoolsStatsForTelegram(null, null);
    }

    @Test
    void getMathSchoolsReportForTelegram_ShouldHandleException() {
        RepositoryException repositoryException = new RepositoryException("Query failed");
        String errorMessage = "Ошибка базы данных для диапазона 5000-7500: Query failed";

        when(repository.findTopMathSchoolByStudentRange(5000, 7500))
                .thenThrow(repositoryException);

        when(exceptionHandler.handleMathSchoolsQueryError(repositoryException, 5000, 7500))
                .thenReturn(errorMessage);
        String result = statisticsService.getMathSchoolsReportForTelegram();
        assertNotNull(result);
        assertEquals(errorMessage, result);

        verify(repository).findTopMathSchoolByStudentRange(5000, 7500);
        verify(exceptionHandler).handleMathSchoolsQueryError(repositoryException, 5000, 7500);
        verify(repository, never()).findTopMathSchoolByStudentRange(10000, 11000);
        verifyNoInteractions(telegramOutputService);
    }

    @Test
    void getStudentStatistics_ShouldReturnList_WhenDataExists() {
        List<CountryStudentStats> expectedStats = Arrays.asList(
                createCountryStudentStats("Fresno", 10, 500.5, 100, 1000, 5005),
                createCountryStudentStats("Contra Costa", 8, 450.25, 200, 800, 3602)
        );

        when(repository.findAverageStudentsByCountries(10))
                .thenReturn(expectedStats);
        List<CountryStudentStats> result = statisticsService.getStudentStatistics(10);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Fresno", result.get(0).getCountryName());
        assertEquals(10, result.get(0).getSchoolCount());

        verify(repository).findAverageStudentsByCountries(10);
    }

    @Test
    void getStudentStatistics_ShouldThrowException_WhenRepositoryFails() {
        RepositoryException repositoryException = new RepositoryException("Connection lost");

        when(repository.findAverageStudentsByCountries(5))
                .thenThrow(repositoryException);
        RepositoryException thrownException = assertThrows(
                RepositoryException.class,
                () -> statisticsService.getStudentStatistics(5)
        );

        assertEquals("Connection lost", thrownException.getMessage());
        verify(repository).findAverageStudentsByCountries(5);
    }

    @Test
    void getStudentStatistics_ShouldReturnEmptyList_WhenNoData() {
        when(repository.findAverageStudentsByCountries(anyInt()))
                .thenReturn(Collections.emptyList());
        List<CountryStudentStats> result = statisticsService.getStudentStatistics(15);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findAverageStudentsByCountries(15);
    }



    @Test
    void constructor_ShouldThrowException_WhenNullRepository() {
        NullPointerException thrownException = assertThrows(
                NullPointerException.class,
                () -> new SchoolStatisticsService(
                        null,
                        telegramOutputService,
                        exceptionHandler
                )
        );

        assertNotNull(thrownException);
        assertTrue(thrownException.getMessage().contains("SchoolRepository"));
    }

    @Test
    void constructor_ShouldThrowException_WhenNullTelegramOutputService() {
        NullPointerException thrownException = assertThrows(
                NullPointerException.class,
                () -> new SchoolStatisticsService(
                        repository,
                        null,
                        exceptionHandler
                )
        );

        assertNotNull(thrownException);
        assertTrue(thrownException.getMessage().contains("TelegramOutputService"));
    }

    @Test
    void constructor_ShouldThrowException_WhenNullExceptionHandler() {
        NullPointerException thrownException = assertThrows(
                NullPointerException.class,
                () -> new SchoolStatisticsService(
                        repository,
                        telegramOutputService,
                        null
                )
        );

        assertNotNull(thrownException);
        assertTrue(thrownException.getMessage().contains("DatabaseExceptionHandler"));
    }

    private ExpenditureStats createExpenditureStat(String county, int count, double avg, double min, double max) {
        ExpenditureStats stat = new ExpenditureStats();
        stat.setCountyName(county);
        stat.setSchoolCount(count);
        stat.setAvgExpenditure(avg);
        stat.setMinExpenditure(min);
        stat.setMaxExpenditure(max);
        return stat;
    }

    private MathSchoolStats createMathSchoolStats(int id, String name, String county,
                                                  int students, double mathScore, double expenditure) {
        MathSchoolStats stats = new MathSchoolStats();
        stats.setId(id);
        stats.setSchoolName(name);
        stats.setCountyName(county);
        stats.setStudents(students);
        stats.setMathScore(mathScore);
        stats.setExpenditure(expenditure);
        return stats;
    }

    private CountryStudentStats createCountryStudentStats(String countryName, int schoolCount,
                                                          double avgStudents, int minStudents,
                                                          int maxStudents, int totalStudents) {
        CountryStudentStats stats = new CountryStudentStats();
        stats.setCountryName(countryName);
        stats.setSchoolCount(schoolCount);
        stats.setAvgStudents(avgStudents);
        stats.setMinStudents(minStudents);
        stats.setMaxStudents(maxStudents);
        stats.setTotalStudents(totalStudents);
        return stats;
    }
}