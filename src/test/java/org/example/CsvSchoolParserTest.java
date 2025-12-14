package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.school.analysis.domain.model.School;
import org.school.analysis.infrastructure.csv.CsvSchoolParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvSchoolParserTest {

    private CsvSchoolParser csvParser;
    private static final String VALID_CSV_LINE = "1,12345,\"Sunol Glen Unified\",Butte,\"KK-08\",195,10.5,0.51,2.04,67,6384.91,22.69,4.58,691.6,690";
    private static final String CSV_HEADER = "id,district,school,country,grades,students,teachers,calworks,lunch,computer,expenditure,income,english,read,math";

    @BeforeEach
    void setUp() {
        csvParser = new CsvSchoolParser();
    }

    @Test
    void testParseSchools_ValidCsv() throws IOException {
        String csvContent = CSV_HEADER + "\n" + VALID_CSV_LINE;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        List<School> schools = csvParser.parseSchools(inputStream);
        assertNotNull(schools);
        assertEquals(1, schools.size());

        School school = schools.get(0);
        assertNotNull(school);
        assertEquals(Integer.valueOf(1), school.getId());
        assertEquals(Integer.valueOf(12345), school.getDistrictId());
        assertEquals("Sunol Glen Unified", school.getName());
        assertEquals("Butte", school.getCountry());
        assertEquals("KK-08", school.getGrades());
        assertEquals(Integer.valueOf(195), school.getStudents());
        assertEquals(10.5, school.getTeachers(), 0.001);
        assertEquals(0.51, school.getCalworks(), 0.001);
        assertEquals(2.04, school.getLunch(), 0.001);
        assertEquals(Integer.valueOf(67), school.getComputers());
        assertEquals(6384.91, school.getExpenditure(), 0.001);
        assertEquals(22.69, school.getIncome(), 0.001);
        assertEquals(4.58, school.getEnglish(), 0.001);
        assertEquals(691.6, school.getReadScore(), 0.001);
        assertEquals(690.0, school.getMathScore(), 0.001);
    }

    @Test
    void testParseSchools_MultipleSchools() throws IOException {
        String csvContent = CSV_HEADER + "\n" +
                "1,12345,\"School 1\",County1,\"KK-08\",100,5.0,1.0,2.0,10,1000.0,10.0,1.0,100.0,100.0\n" +
                "2,23456,\"School 2\",County2,\"KK-06\",200,10.0,2.0,4.0,20,2000.0,20.0,2.0,200.0,200.0\n" +
                "3,34567,\"School 3\",County3,\"KK-12\",300,15.0,3.0,6.0,30,3000.0,30.0,3.0,300.0,300.0";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());


        List<School> schools = csvParser.parseSchools(inputStream);

        assertNotNull(schools);
        assertEquals(3, schools.size());

        assertEquals(Integer.valueOf(1), schools.get(0).getId());
        assertEquals("School 1", schools.get(0).getName());

        assertEquals(Integer.valueOf(2), schools.get(1).getId());
        assertEquals("School 2", schools.get(1).getName());

        assertEquals(Integer.valueOf(3), schools.get(2).getId());
        assertEquals("School 3", schools.get(2).getName());
    }

    @Test
    void testParseSchools_EmptyCsv() throws IOException {
        String csvContent = CSV_HEADER + "\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<School> schools = csvParser.parseSchools(inputStream);
        assertNotNull(schools);
        assertTrue(schools.isEmpty());
    }

    @Test
    void testParseSchools_OnlyHeader() throws IOException {
        String csvContent = CSV_HEADER;
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<School> schools = csvParser.parseSchools(inputStream);
        assertNotNull(schools);
        assertTrue(schools.isEmpty());
    }

    @Test
    void testParseSchools_InvalidLineIsSkipped() throws IOException {
        String csvContent = CSV_HEADER + "\n" +
                VALID_CSV_LINE + "\n" +
                "invalid,data,here\n" +
                "4,45678,\"Valid School\",County4,\"KK-06\",400,20.0,4.0,8.0,40,4000.0,40.0,4.0,400.0,400.0";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<School> schools = csvParser.parseSchools(inputStream);
        assertNotNull(schools);
        assertEquals(2, schools.size());
        assertEquals(Integer.valueOf(1), schools.get(0).getId());
        assertEquals(Integer.valueOf(4), schools.get(1).getId());
    }

    @Test
    void testParseSchools_WithQuotedFields() throws IOException {
        String csvContent = CSV_HEADER + "\n" +
                "5,55555,\"School, with comma\",\"County, with comma\",\"KK-08, special\",500,25.0,5.0,10.0,50,5000.0,50.0,5.0,500.0,500.0";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<School> schools = csvParser.parseSchools(inputStream);
        assertNotNull(schools);
        assertEquals(1, schools.size());

        School school = schools.get(0);
        assertEquals("School, with comma", school.getName());
        assertEquals("County, with comma", school.getCountry());
        assertEquals("KK-08, special", school.getGrades());
    }

    @Test
    void testParseLine_ValidLine() {
        School school = csvParser.parseLine(VALID_CSV_LINE);
        assertNotNull(school);
        assertEquals(Integer.valueOf(1), school.getId());
        assertEquals("Sunol Glen Unified", school.getName());
        assertEquals("Butte", school.getCountry());
    }

    @Test
    void testParseCsvLine_BasicParsing() {
        String line = "field1,field2,field3";
        String[] result = csvParser.parseCsvLine(line);

        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals("field1", result[0]);
        assertEquals("field2", result[1]);
        assertEquals("field3", result[2]);
    }

    @Test
    void testParseCsvLine_WithTrailingComma() {
        String line = "field1,field2,field3,";
        String[] result = csvParser.parseCsvLine(line);
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals("field1", result[0]);
        assertEquals("field2", result[1]);
        assertEquals("field3", result[2]);
        assertEquals("", result[3]);
    }

    @Test
    void testParseInt_ValidNumber() {
        assertEquals(Integer.valueOf(123), csvParser.parseInt("123"));
        assertEquals(Integer.valueOf(0), csvParser.parseInt("0"));
        assertEquals(Integer.valueOf(-456), csvParser.parseInt("-456"));
    }

    @Test
    void testParseInt_InvalidNumber() {
        assertNull(csvParser.parseInt("not a number"));
        assertNull(csvParser.parseInt("123.45"));
        assertNull(csvParser.parseInt(""));
        assertNull(csvParser.parseInt("   "));
        assertNull(csvParser.parseInt(null));
    }

    @Test
    void testParseInt_WithSpaces() {
        assertEquals(Integer.valueOf(123), csvParser.parseInt(" 123 "));
        assertEquals(Integer.valueOf(456), csvParser.parseInt("456   "));
        assertEquals(Integer.valueOf(789), csvParser.parseInt("   789"));
    }

    @Test
    void testParseDouble_ValidNumber() {
        assertEquals(123.45, csvParser.parseDouble("123.45"), 0.001);
        assertEquals(0.0, csvParser.parseDouble("0"), 0.001);
        assertEquals(-456.78, csvParser.parseDouble("-456.78"), 0.001);
        assertEquals(1.0E-5, csvParser.parseDouble("0.00001"), 0.000001);
    }

    @Test
    void testParseDouble_InvalidNumber() {
        assertNull(csvParser.parseDouble("not a number"));
        assertNull(csvParser.parseDouble(""));
        assertNull(csvParser.parseDouble("   "));
        assertNull(csvParser.parseDouble(null));
    }

    @Test
    void testParseDouble_WithSpaces() {
        assertEquals(123.45, csvParser.parseDouble(" 123.45 "), 0.001);
        assertEquals(456.78, csvParser.parseDouble("456.78   "), 0.001);
        assertEquals(789.01, csvParser.parseDouble("   789.01"), 0.001);
    }

    @Test
    void testParseSchools_NullInputStream() {
        assertThrows(NullPointerException.class, () -> {
            csvParser.parseSchools(null);
        });
    }

    @Test
    void testParseSchools_IOException() throws IOException {
        InputStream throwingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Test IO Exception");
            }
        };
        assertThrows(IOException.class, () -> {
            csvParser.parseSchools(throwingStream);
        });
    }

    @Test
    void testParseLine_SpecialCharacters() {
        String line = "9,99999,\"Schöol with Ümlaut\",\"Cöunty\",\"KK-12\",900,45.0,9.0,18.0,90,9000.0,90.0,9.0,900.0,900.0";
        School school = csvParser.parseLine(line);

        assertNotNull(school);
        assertEquals("Schöol with Ümlaut", school.getName());
        assertEquals("Cöunty", school.getCountry());
    }
}