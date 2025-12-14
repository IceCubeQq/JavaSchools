package org.school.analysis.application.services;

import org.school.analysis.application.ports.output.SchoolRepository;
import org.school.analysis.application.exception.DatabaseExceptionHandler;
import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.domain.dto.ExpenditureStats;
import org.school.analysis.domain.dto.MathSchoolStats;
import org.school.analysis.presentation.TelegramOutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class SchoolStatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(SchoolStatisticsService.class);
    private final SchoolRepository repository;
    private final TelegramOutputService telegramOutputService;
    private final DatabaseExceptionHandler exceptionHandler;

    public SchoolStatisticsService(SchoolRepository repository, TelegramOutputService telegramOutputService,
                                   DatabaseExceptionHandler exceptionHandler) {
        this.repository = repository;
        this.telegramOutputService = telegramOutputService;
        this.exceptionHandler = exceptionHandler;
    }

    public String getExpenditureReportForTelegram() {
        try {
            List<String> counties = Arrays.asList("Fresno", "Contra Costa", "El Dorado", "Glenn");
            List<ExpenditureStats> stats = repository.findAverageExpenditureInCounties(counties, 10.0);
            return telegramOutputService.formatExpenditureStatsForTelegram(stats);
        } catch (Exception e) {
            return exceptionHandler.handleExpenditureQueryError(e);
        }
    }

    public String getMathSchoolsReportForTelegram() {
        try {
            MathSchoolStats range1 = repository.findTopMathSchoolByStudentRange(5000, 7500);
            MathSchoolStats range2 = repository.findTopMathSchoolByStudentRange(10000, 11000);
            return telegramOutputService.formatAllMathSchoolsStatsForTelegram(range1, range2);
        } catch (Exception e) {
            return exceptionHandler.handleMathSchoolsQueryError(e, 5000, 7500);
        }
    }

    public List<CountryStudentStats> getStudentStatistics(int limit) {
        try {
            return repository.findAverageStudentsByCountries(limit);
        } catch (Exception e) {
            logger.error("Ошибка получения статистики студентов", e);
            throw e;
        }
    }
}