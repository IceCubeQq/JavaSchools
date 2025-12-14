package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.exception.DatabaseExceptionHandler;
import org.school.analysis.application.exception.RepositoryException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DatabaseExceptionHandlerTest {

    private final DatabaseExceptionHandler exceptionHandler = new DatabaseExceptionHandler();

    @Test
    void testHandleExpenditureQueryError_WithRepositoryException() {
        String errorMessage = "Connection failed";
        RepositoryException repositoryException = new RepositoryException(errorMessage);
        String result = exceptionHandler.handleExpenditureQueryError(repositoryException);
        assertNotNull(result);
        assertTrue(result.contains("Ошибка базы данных при получении расходов"));
        assertTrue(result.contains(errorMessage));
    }

    @Test
    void testHandleExpenditureQueryError_WithGenericException() {
        String errorMessage = "Generic error occurred";
        Exception genericException = new RuntimeException(errorMessage);
        String result = exceptionHandler.handleExpenditureQueryError(genericException);
        assertNotNull(result);
        assertTrue(result.contains("Ошибка при выполнении запроса расходов"));
        assertTrue(result.contains(errorMessage));
    }

    @Test
    void testHandleExpenditureQueryError_WithNullException() {
        String result = exceptionHandler.handleExpenditureQueryError(null);
        assertNotNull(result);
        assertTrue(result.contains("Ошибка при выполнении запроса расходов"));
        assertTrue(result.contains("неизвестная ошибка"));
    }

    @Test
    void testHandleExpenditureQueryError_WithEmptyMessageException() {
        Exception emptyException = new RuntimeException();
        String result = exceptionHandler.handleExpenditureQueryError(emptyException);
        assertNotNull(result);
        assertTrue(result.contains("Ошибка при выполнении запроса расходов"));
    }

    @Test
    void testHandleMathSchoolsQueryError_WithRepositoryException() {
        int minStudents = 5000;
        int maxStudents = 7500;
        String errorMessage = "Database timeout";
        RepositoryException repositoryException = new RepositoryException(errorMessage);
        String result = exceptionHandler.handleMathSchoolsQueryError(
                repositoryException, minStudents, maxStudents
        );
        assertNotNull(result);
        assertTrue(result.contains("Ошибка базы данных для диапазона"));
        assertTrue(result.contains("5000-7500"));
        assertTrue(result.contains(errorMessage));
    }

    @Test
    void testHandleMathSchoolsQueryError_WithGenericException() {
        int minStudents = 10000;
        int maxStudents = 11000;
        String errorMessage = "Network error";
        Exception genericException = new RuntimeException(errorMessage);
        String result = exceptionHandler.handleMathSchoolsQueryError(
                genericException, minStudents, maxStudents
        );
        assertNotNull(result);
        assertTrue(result.contains("Ошибка для диапазона"));
        assertTrue(result.contains("10000-11000"));
        assertTrue(result.contains(errorMessage));
    }

    @Test
    void testHandleMathSchoolsQueryError_WithNestedException() {
        int minStudents = 1;
        int maxStudents = 100;
        String rootCauseMessage = "Root cause";
        Exception rootCause = new RuntimeException(rootCauseMessage);
        RepositoryException repositoryException = new RepositoryException("Wrapper", rootCause);
        String result = exceptionHandler.handleMathSchoolsQueryError(
                repositoryException, minStudents, maxStudents
        );
        assertNotNull(result);
        assertTrue(result.contains("Ошибка базы данных для диапазона"));
        assertTrue(result.contains("1-100"));
        assertTrue(result.contains("Wrapper"));
    }

    @Test
    void testHandleMathSchoolsQueryError_WithSameMinMaxValues() {
        int minStudents = 5000;
        int maxStudents = 5000;
        Exception exception = new RuntimeException("Test error");
        String result = exceptionHandler.handleMathSchoolsQueryError(
                exception, minStudents, maxStudents
        );
        assertNotNull(result);
        assertTrue(result.contains("5000-5000"));
        assertTrue(result.contains("Test error"));
    }

    @Test
    void testHandleMathSchoolsQueryError_WithNegativeValues() {
        int minStudents = -100;
        int maxStudents = -50;
        Exception exception = new RuntimeException("Negative range");
        String result = exceptionHandler.handleMathSchoolsQueryError(
                exception, minStudents, maxStudents
        );
        assertNotNull(result);
        assertTrue(result.contains("-100--50"));
        assertTrue(result.contains("Negative range"));
    }

    @Test
    void testHandleMathSchoolsQueryError_WithNullException() {
        String result = exceptionHandler.handleMathSchoolsQueryError(null, 0, 100);
        assertNotNull(result);
        assertTrue(result.contains("Ошибка для диапазона"));
        assertTrue(result.contains("0-100"));
        assertTrue(result.contains("неизвестная ошибка"));
    }

    @Test
    void testHandleMathSchoolsQueryError_Formatting() {
        int minStudents = 1234;
        int maxStudents = 5678;
        String errorMessage = "Custom error with special chars: áéíóú";
        Exception exception = new RuntimeException(errorMessage);
        String result = exceptionHandler.handleMathSchoolsQueryError(
                exception, minStudents, maxStudents
        );
        assertNotNull(result);
        assertEquals(
                String.format("Ошибка для диапазона %d-%d: %s", minStudents, maxStudents, errorMessage),
                result
        );
    }

    @Test
    void testBothMethodsReturnNonNull() {
        Exception testException = new RuntimeException("Test");
        String result1 = exceptionHandler.handleExpenditureQueryError(testException);
        String result2 = exceptionHandler.handleMathSchoolsQueryError(testException, 0, 100);
        assertNotNull(result1);
        assertNotNull(result2);
        assertFalse(result1.isEmpty());
        assertFalse(result2.isEmpty());
    }

    @Test
    void testExceptionMessageInclusion() {
        String detailedErrorMessage = "Failed to execute query: SELECT * FROM schools WHERE id = 123; "
                + "Connection was closed unexpectedly. "
                + "Retry count: 3";
        Exception detailedException = new RuntimeException(detailedErrorMessage);
        String result = exceptionHandler.handleExpenditureQueryError(detailedException);
        assertTrue(result.contains(detailedErrorMessage));
    }
}