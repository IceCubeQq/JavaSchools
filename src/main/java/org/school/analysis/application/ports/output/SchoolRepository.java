package org.school.analysis.application.ports.output;

import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.domain.dto.ExpenditureStats;
import org.school.analysis.domain.dto.MathSchoolStats;

import java.util.List;


public interface SchoolRepository {
    List<ExpenditureStats> findAverageExpenditureInCounties(List<String> counties, double minExpenditure);
    MathSchoolStats findTopMathSchoolByStudentRange(int minStudents, int maxStudents);
    List<CountryStudentStats> findAverageStudentsByCountries(int limit);
}