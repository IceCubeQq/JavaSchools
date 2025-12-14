package org.school.analysis.application.services;

import org.school.analysis.application.ports.input.LoadSchoolsUseCase;
import org.school.analysis.domain.model.School;
import org.school.analysis.infrastructure.csv.CsvSchoolParser;
import org.school.analysis.application.ports.output.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

public class LoadSchoolsService implements LoadSchoolsUseCase {
    private static final Logger logger = LoggerFactory.getLogger(LoadSchoolsService.class);

    private final CsvSchoolParser csvParser;
    private final DatabaseManager databaseManager;

    public LoadSchoolsService(CsvSchoolParser csvParser, DatabaseManager databaseManager) {
        this.csvParser = csvParser;
        this.databaseManager = databaseManager;
    }

    @Override
    public int execute(InputStream csvStream) {
        try {
            logger.info("Начинаю загрузку школ из CSV");
            List<School> schools = csvParser.parseSchools(csvStream);

            validateSchools(schools);
            try {
                databaseManager.getConnection();
                logger.info("База данных подключена, начинаю сохранение");
            } catch (IllegalStateException e) {
                logger.error("База данных не подключена");
                throw new RuntimeException("База данных не подключена. Подключитесь к БД", e);
            }

            databaseManager.insertSchools(schools);
            return schools.size();

        } catch (Exception e) {
            logger.error("Ошибка загрузки школ", e);
            throw new RuntimeException("Ошибка загрузки школ из CSV", e);
        }
    }

    private void validateSchools(List<School> schools) {
        if (schools == null || schools.isEmpty()) {
            throw new IllegalArgumentException("В CSV нет школ или файл пуст");
        }
        int validSchools = 0;
        for (School school : schools) {
            if (school.getId() != null && school.getName() != null && !school.getName().isEmpty()) {
                validSchools++;
            }
        }

        if (validSchools == 0) {
            throw new IllegalArgumentException("Нет школ в CSV файле");
        }
    }
}