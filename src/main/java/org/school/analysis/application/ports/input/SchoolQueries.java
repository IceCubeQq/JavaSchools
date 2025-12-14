package org.school.analysis.application.ports.input;

import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.domain.dto.ExpenditureStats;
import org.school.analysis.domain.dto.MathSchoolStats;

import java.util.List;

public interface SchoolQueries {
    String getExpenditureReport();
    String getMathSchoolsReport();
    List<CountryStudentStats> getStudentStatistics(int limit);
    List<ExpenditureStats> findAverageExpenditureInCounties(List<String> counties, double minExpenditure);
    MathSchoolStats findTopMathSchoolByStudentRange(int minStudents, int maxStudents);
}