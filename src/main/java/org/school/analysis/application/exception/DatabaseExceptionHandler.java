package org.school.analysis.application.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseExceptionHandler.class);

    public String handleExpenditureQueryError(Exception e) {
        logger.error("Ошибка при выполнении запроса расходов", e);
        if (e instanceof RepositoryException) {
            return "Ошибка базы данных при получении расходов: " + e.getMessage();
        }
        return "Ошибка при выполнении запроса расходов: " + (e != null ? e.getMessage() : "неизвестная ошибка");
    }

    public String handleMathSchoolsQueryError(Exception e, int minStudents, int maxStudents) {
        logger.error("Ошибка при выполнении запроса для диапазона {}-{}", minStudents, maxStudents, e);
        if (e instanceof RepositoryException) {
            return String.format("Ошибка базы данных для диапазона %d-%d: %s",
                    minStudents, maxStudents, e.getMessage());
        }
        return String.format("Ошибка для диапазона %d-%d: %s",
                minStudents, maxStudents, e != null ? e.getMessage() : "неизвестная ошибка");
    }
}