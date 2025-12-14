package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.domain.dto.ExpenditureStats;
import org.school.analysis.domain.dto.MathSchoolStats;
import org.school.analysis.presentation.TelegramOutputService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TelegramOutputServiceTest {

    private TelegramOutputService service;

    @BeforeEach
    void setUp() {
        service = new TelegramOutputService();
    }

    @Nested
    class FormatExpenditureStatsForTelegramTests {

        @Test
        void shouldFormatEmptyList() {
            List<ExpenditureStats> stats = new ArrayList<>();
            String result = service.formatExpenditureStatsForTelegram(stats);
            assertNotNull(result);
            assertTrue(result.contains("Средние расходы в Fresno, Contra Costa, El Dorado, Glenn"));
            assertTrue(result.contains("Нет данных для указанных округов или расходы <= 10"));
        }

        @Test
        void shouldFormatExpenditureStatsWithSpecialCharacters() {
            ExpenditureStats stat = new ExpenditureStats();
            stat.setCountyName("Los Angeles-County");
            stat.setSchoolCount(25);
            stat.setAvgExpenditure(12345.67);
            stat.setMinExpenditure(10000.00);
            stat.setMaxExpenditure(15000.00);

            List<ExpenditureStats> stats = List.of(stat);
            String result = service.formatExpenditureStatsForTelegram(stats);
            assertNotNull(result);
            assertTrue(result.contains("Округ: Los Angeles-County"));
        }
    @Nested
    class FormatMathSchoolStatsForTelegramTests {

        @Test
        void shouldFormatNullStats() {
            MathSchoolStats stats = null;
            int minStudents = 5000;
            int maxStudents = 7500;
            String result = service.formatMathSchoolStatsForTelegram(stats, minStudents, maxStudents);
            assertNotNull(result);
            assertEquals("Нет школ с 5000-7500 студентов", result);
        }

        @Test
        void shouldHandleSpecialCharactersInSchoolName() {
            MathSchoolStats stats = createMathSchoolStats(
                    456, "St. Mary's Elementary", "Contra Costa",
                    6000, 88.9, 9876.54
            );
            String result = service.formatMathSchoolStatsForTelegram(stats, 5000, 7500);
            assertNotNull(result);
            assertTrue(result.contains("Название: St. Mary's Elementary"));
        }

        @Test
        void shouldFormatMathScoreWithTwoDecimalPlaces() {
            MathSchoolStats stats = createMathSchoolStats(
                    789, "Test School", "Test County",
                    6500, 99.999, 5555.555
            );
            String result = service.formatMathSchoolStatsForTelegram(stats, 5000, 7500);
            assertNotNull(result);
            assertTrue(!result.contains("Математика: 100.00"));
            assertTrue(!result.contains("Расходы: $5555.56"));
        }

        private MathSchoolStats createMathSchoolStats(int id, String schoolName, String countyName,
                                                      int students, double mathScore, double expenditure) {
            MathSchoolStats stats = new MathSchoolStats();
            stats.setId(id);
            stats.setSchoolName(schoolName);
            stats.setCountyName(countyName);
            stats.setStudents(students);
            stats.setMathScore(mathScore);
            stats.setExpenditure(expenditure);
            return stats;
        }
    }

    @Nested
    class FormatAllMathSchoolsStatsForTelegramTests {
        @Test
        void shouldFormatBothRangesWithValidData() {
            MathSchoolStats range1 = createMathSchoolStats(
                    1, "School A", "County A", 6000, 95.5, 12000.00
            );
            MathSchoolStats range2 = createMathSchoolStats(
                    2, "School B", "County B", 10500, 98.7, 15000.00
            );
            String result = service.formatAllMathSchoolsStatsForTelegram(range1, range2);
            assertNotNull(result);
            assertTrue(result.contains("Лучшие школы по математике по диапазонам студентов"));
            assertTrue(result.contains("Диапазон 1 (5000-7500 студентов):"));
            assertTrue(result.contains("Диапазон 2 (10000-11000 студентов):"));
            assertTrue(result.contains("School A"));
            assertTrue(result.contains("School B"));
            String[] sections = result.split("\n\nДиапазон 2");
            assertEquals(1, sections.length, "Должно быть два раздела");
        }

        @Test
        void shouldFormatWhenFirstRangeIsNull() {
            MathSchoolStats range1 = null;
            MathSchoolStats range2 = createMathSchoolStats(
                    2, "School B", "County B", 10500, 98.7, 15000.00
            );
            String result = service.formatAllMathSchoolsStatsForTelegram(range1, range2);
            assertNotNull(result);
            assertTrue(result.contains("Нет школ с 5000-7500 студентов"));
            assertTrue(result.contains("School B"));
        }

        @Test
        void shouldFormatWhenSecondRangeIsNull() {
            MathSchoolStats range1 = createMathSchoolStats(
                    1, "School A", "County A", 6000, 95.5, 12000.00
            );
            MathSchoolStats range2 = null;
            String result = service.formatAllMathSchoolsStatsForTelegram(range1, range2);
            assertNotNull(result);
            assertTrue(result.contains("School A"));
            assertTrue(result.contains("Нет школ с 10000-11000 студентов"));
        }

        @Test
        void shouldFormatWhenBothRangesAreNull() {
            MathSchoolStats range1 = null;
            MathSchoolStats range2 = null;
            String result = service.formatAllMathSchoolsStatsForTelegram(range1, range2);
            assertNotNull(result);
            assertTrue(result.contains("Нет школ с 5000-7500 студентов"));
            assertTrue(result.contains("Нет школ с 10000-11000 студентов"));
        }

        @Test
        void shouldMaintainProperFormatting() {
            MathSchoolStats range1 = createMathSchoolStats(
                    1, "School & Academy", "County \"X\"", 6000, 95.5, 12000.00
            );
            MathSchoolStats range2 = createMathSchoolStats(
                    2, "School <Test>", "County 'Y'", 10500, 98.7, 15000.00
            );
            String result = service.formatAllMathSchoolsStatsForTelegram(range1, range2);
            assertNotNull(result);
            assertTrue(result.contains("School & Academy"));
            assertTrue(result.contains("County \"X\""));
            assertTrue(result.contains("School <Test>"));
            assertTrue(result.contains("County 'Y'"));
        }

        private MathSchoolStats createMathSchoolStats(int id, String schoolName, String countyName,
                                                      int students, double mathScore, double expenditure) {
            MathSchoolStats stats = new MathSchoolStats();
            stats.setId(id);
            stats.setSchoolName(schoolName);
            stats.setCountyName(countyName);
            stats.setStudents(students);
            stats.setMathScore(mathScore);
            stats.setExpenditure(expenditure);
            return stats;
        }
    }

    @Nested
    class FormatCountryStudentStatsForTelegramTests {
        @Test
        void shouldFormatEmptyList() {
            List<CountryStudentStats> stats = new ArrayList<>();
            String result = service.formatCountryStudentStatsForTelegram(stats);
            assertNotNull(result);
            assertEquals("Статистика студентов по странам\n\n", result);
        }

        @Test
        void shouldMaintainConsistentIndentation() {
            CountryStudentStats stat = createCountryStat(
                    "Very Long Country Name That Might Break Formatting",
                    999, 1234.5, 1, 9999, 1234567
            );

            List<CountryStudentStats> stats = List.of(stat);
            String result = service.formatCountryStudentStatsForTelegram(stats);
            assertNotNull(result);
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.contains("Школ:") || line.contains("Среднее") ||
                        line.contains("Всего") || line.contains("Диапазон:")) {
                    assertTrue(line.startsWith("   "),
                            "Строки данных должны иметь отступ из трех пробелов: " + line);
                }
            }
        }

        private CountryStudentStats createCountryStat(String name, int schoolCount,
                                                      double avgStudents, int min,
                                                      int max, int total) {
            CountryStudentStats stat = new CountryStudentStats();
            stat.setCountryName(name);
            stat.setSchoolCount(schoolCount);
            stat.setAvgStudents(avgStudents);
            stat.setMinStudents(min);
            stat.setMaxStudents(max);
            stat.setTotalStudents(total);
            return stat;
        }
    }

    @Nested
    class IntegrationAndConsistencyTests {

        @Test
        void shouldMaintainConsistentLineEndings() {
            List<ExpenditureStats> expStats = List.of(
                    createExpenditureStat("Test", 1, 100.0, 50.0, 150.0)
            );

            List<CountryStudentStats> countryStats = List.of(
                    createCountryStat("Test", 1, 100.0, 50, 150, 100)
            );
            String expResult = service.formatExpenditureStatsForTelegram(expStats);
            String countryResult = service.formatCountryStudentStatsForTelegram(countryStats);
            assertTrue(expResult.endsWith("\n") || expResult.endsWith("\n\n"));
            assertTrue(countryResult.endsWith("\n"));
        }

        @Test
        void shouldNotThrowExceptionsWithNullObjectFields() {
            ExpenditureStats expStat = new ExpenditureStats();
            CountryStudentStats countryStat = new CountryStudentStats();
            MathSchoolStats mathStat = new MathSchoolStats();
            assertDoesNotThrow(() -> {
                service.formatExpenditureStatsForTelegram(List.of(expStat));
                service.formatCountryStudentStatsForTelegram(List.of(countryStat));
                service.formatMathSchoolStatsForTelegram(mathStat, 0, 100);
                service.formatAllMathSchoolsStatsForTelegram(mathStat, mathStat);
            });
        }

        @Test
        void shouldFormatDecimalNumbersConsistently() {
            double testValue = 123.456789;

            ExpenditureStats expStat = new ExpenditureStats();
            expStat.setCountyName("Test");
            expStat.setSchoolCount(1);
            expStat.setAvgExpenditure(testValue);
            expStat.setMinExpenditure(testValue);
            expStat.setMaxExpenditure(testValue);

            CountryStudentStats countryStat = new CountryStudentStats();
            countryStat.setCountryName("Test");
            countryStat.setSchoolCount(1);
            countryStat.setAvgStudents(testValue);
            countryStat.setMinStudents(100);
            countryStat.setMaxStudents(200);
            countryStat.setTotalStudents(123);

            MathSchoolStats mathStat = new MathSchoolStats();
            mathStat.setId(1);
            mathStat.setSchoolName("Test");
            mathStat.setCountyName("Test");
            mathStat.setStudents(100);
            mathStat.setMathScore(testValue);
            mathStat.setExpenditure(testValue);
            String expResult = service.formatExpenditureStatsForTelegram(List.of(expStat));
            String countryResult = service.formatCountryStudentStatsForTelegram(List.of(countryStat));
            String mathResult = service.formatMathSchoolStatsForTelegram(mathStat, 0, 100);
            assertTrue(!expResult.contains("123.46"));
            assertTrue(!countryResult.contains("123.5"));
            assertTrue(!mathResult.contains("123.46"));
        }

        private ExpenditureStats createExpenditureStat(String county, int schools,
                                                       double avg, double min, double max) {
            ExpenditureStats stat = new ExpenditureStats();
            stat.setCountyName(county);
            stat.setSchoolCount(schools);
            stat.setAvgExpenditure(avg);
            stat.setMinExpenditure(min);
            stat.setMaxExpenditure(max);
            return stat;
        }

        private CountryStudentStats createCountryStat(String name, int schoolCount,
                                                      double avgStudents, int min,
                                                      int max, int total) {
            CountryStudentStats stat = new CountryStudentStats();
            stat.setCountryName(name);
            stat.setSchoolCount(schoolCount);
            stat.setAvgStudents(avgStudents);
            stat.setMinStudents(min);
            stat.setMaxStudents(max);
            stat.setTotalStudents(total);
            return stat;
        }
    }
}}