package org.school.analysis.infrastructure.csv;

import org.school.analysis.domain.model.School;
import org.school.analysis.parser.CsvParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CsvSchoolParser implements CsvParser {
    private static final Logger logger = LoggerFactory.getLogger(CsvSchoolParser.class);

    @Override
    public List<School> parseSchools(InputStream csvStream) throws IOException {
        List<School> schools = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvStream))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                School school = parseLine(line);
                if (school != null) {
                    schools.add(school);
                }
            }
        }
        return schools;
    }

    private School parseLine(String line) {
        try {
            String[] values = parseCsvLine(line);
            School school = new School();
            school.setId(parseInt(values[0]));
            school.setDistrictId(parseInt(values[1]));
            school.setName(values[2]);
            school.setCountry(values[3]);
            school.setGrades(values[4]);
            school.setStudents(parseInt(values[5]));
            school.setTeachers(parseDouble(values[6]));
            school.setCalworks(parseDouble(values[7]));
            school.setLunch(parseDouble(values[8]));
            school.setComputers(parseInt(values[9]));
            school.setExpenditure(parseDouble(values[10]));
            school.setIncome(parseDouble(values[11]));
            school.setEnglish(parseDouble(values[12]));
            school.setReadScore(parseDouble(values[13]));
            school.setMathScore(parseDouble(values[14]));
            return school;
        } catch (Exception e) {
            logger.error("Ошибка парсинга строки: {}", line, e);
            return null;
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Не удалось преобразовать в число {}", value);
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Не удалось преобразовать в дробное число {}", value);
            return null;
        }
    }
}