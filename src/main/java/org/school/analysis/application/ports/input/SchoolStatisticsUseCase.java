package org.school.analysis.application.ports.input;

import org.school.analysis.domain.dto.CountryStudentStats;

import java.util.List;

public interface SchoolStatisticsUseCase {
    String getExpenditureReportForTelegram();
    String getMathSchoolsReportForTelegram();
    List<CountryStudentStats> getStudentStatistics(int limit);
}
