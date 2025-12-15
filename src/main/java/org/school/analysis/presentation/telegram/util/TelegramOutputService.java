package org.school.analysis.presentation.telegram.util;

import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.domain.dto.ExpenditureStats;
import org.school.analysis.domain.dto.MathSchoolStats;

import java.util.List;

public class TelegramOutputService {

    public String formatExpenditureStatsForTelegram(List<ExpenditureStats> stats) {
        StringBuilder result = new StringBuilder();
        result.append("Средние расходы в Fresno, Contra Costa, El Dorado, Glenn\n");

        if (stats.isEmpty()) {
            result.append("Нет данных для указанных округов или расходы <= 10");
            return result.toString();
        }

        for (ExpenditureStats stat : stats) {
            result.append(String.format("Округ: %s\n", stat.getCountyName()));
            result.append(String.format("Школ: %d\n", stat.getSchoolCount()));
            result.append(String.format("Средние расходы: $%.2f\n", stat.getAvgExpenditure()));
            result.append(String.format("Минимум: $%.2f\n", stat.getMinExpenditure()));
            result.append(String.format("Максимум: $%.2f\n\n", stat.getMaxExpenditure()));
        }

        return result.toString();
    }

    public String formatMathSchoolStatsForTelegram(MathSchoolStats stats, int minStudents, int maxStudents) {
        if (stats == null) {
            return String.format("Нет школ с %d-%d студентов", minStudents, maxStudents);
        }

        return String.format("Лучшая школа:\n" +
                        "ID: %d\n" +
                        "Название: %s\n" +
                        "Округ: %s\n" +
                        "Студентов: %d\n" +
                        "Математика: %.2f\n" +
                        "Расходы: $%.2f",
                stats.getId(),
                stats.getSchoolName(),
                stats.getCountyName(),
                stats.getStudents(),
                stats.getMathScore(),
                stats.getExpenditure());
    }

    public String formatAllMathSchoolsStatsForTelegram(MathSchoolStats range1, MathSchoolStats range2) {
        StringBuilder result = new StringBuilder();
        result.append("Лучшие школы по математике по диапазонам студентов\n");

        result.append("Диапазон 1 (5000-7500 студентов):\n");
        result.append(formatMathSchoolStatsForTelegram(range1, 5000, 7500));

        result.append("\nДиапазон 2 (10000-11000 студентов):\n");
        result.append(formatMathSchoolStatsForTelegram(range2, 10000, 11000));

        return result.toString();
    }

    public String formatCountryStudentStatsForTelegram(List<CountryStudentStats> stats) {
        StringBuilder result = new StringBuilder();
        result.append("Статистика студентов по странам\n\n");
        for (int i = 0; i < stats.size(); i++) {
            CountryStudentStats stat = stats.get(i);
            result.append(String.format("%d. %s\n", i + 1, stat.getCountryName()));
            result.append(String.format("   Школ: %d\n", stat.getSchoolCount()));
            result.append(String.format("   Среднее студентов: <b>%.1f</b>\n", stat.getAvgStudents()));
            result.append(String.format("   Всего студентов: %d\n", stat.getTotalStudents()));
            result.append(String.format("   Диапазон: %d - %d студентов\n\n",
                    stat.getMinStudents(), stat.getMaxStudents()));
        }
        return result.toString();
    }
}