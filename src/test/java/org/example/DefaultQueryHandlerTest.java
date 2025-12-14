package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.services.SchoolStatisticsService;
import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.infrastructure.telegram.bot.SchoolTelegramBot;
import org.school.analysis.infrastructure.telegram.handlers.DefaultQueryHandler;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultQueryHandlerTest {

    @Mock
    private SchoolStatisticsService statisticsService;

    @Mock
    private ExecutorService executorService;

    @Mock
    private SchoolTelegramBot bot;

    private DefaultQueryHandler queryHandler;

    @BeforeEach
    void setUp() {
        queryHandler = new DefaultQueryHandler(statisticsService, executorService);
    }

    @Test
    void testConstructor() {
        DefaultQueryHandler handler = new DefaultQueryHandler(statisticsService, executorService);
        assertNotNull(handler);
    }

    @Test
    void testShowMenu() {
        Long chatId = 12345L;
        queryHandler.showMenu(chatId, bot);
        verify(bot).sendInlineMenu(eq(chatId), contains("SQL Запросы к БД"), any());
        verify(bot).sendInlineMenu(eq(chatId), contains("Средние расходы"), any());
        verify(bot).sendInlineMenu(eq(chatId), contains("Лучшие школы по математике"), any());
    }

    @Test
    void testShowMenu_WithKeyboard() {
        Long chatId = 12345L;
        queryHandler.showMenu(chatId, bot);
        verify(bot).sendInlineMenu(eq(chatId), anyString(), notNull());
    }

    @Test
    void testHandleCallback_ExpenditureQuery() {
        Long chatId = 12345L;
        String callbackData = "query_expenditure";

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        queryHandler.handleCallback(chatId, callbackData, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 1: Средние расходы"));
    }

    @Test
    void testHandleCallback_MathSchoolsQuery() {
        Long chatId = 12345L;
        String callbackData = "query_math_schools";

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        queryHandler.handleCallback(chatId, callbackData, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 2: Лучшие школы по математике"));
    }

    @Test
    void testHandleCallback_StudentStatsQuery() {
        Long chatId = 12345L;
        String callbackData = "query_student_stats";

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        queryHandler.handleCallback(chatId, callbackData, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 3: Статистика студентов по странам"));
    }

    @Test
    void testHandleCallback_UnknownCallback() {
        Long chatId = 12345L;
        String callbackData = "unknown_query";
        queryHandler.handleCallback(chatId, callbackData, bot);
        verify(bot).sendText(eq(chatId), contains("Неизвестный запрос"));
    }

    @Test
    void testHandleCallback_NullCallback() {
        Long chatId = 12345L;
        String callbackData = null;
        assertThrows(NullPointerException.class,
                () -> queryHandler.handleCallback(chatId, callbackData, bot));
    }

    @Test
    void testExecuteExpenditureQueryAsync_Success() {
        Long chatId = 12345L;
        String result = "Средние расходы:\nFresno: $5000\nContra Costa: $4500";

        when(statisticsService.getExpenditureReportForTelegram()).thenReturn(result);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("executeExpenditureQueryAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 1: Средние расходы"));
        verify(bot).sendText(eq(chatId), eq(result));
    }

    @Test
    void testExecuteExpenditureQueryAsync_Exception() {
        Long chatId = 12345L;
        String errorMessage = "Database connection failed";

        when(statisticsService.getExpenditureReportForTelegram())
                .thenThrow(new RuntimeException(errorMessage));

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("executeExpenditureQueryAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 1: Средние расходы"));
        verify(bot).sendText(eq(chatId), contains("Ошибка выполнения запроса"));
        verify(bot).sendText(eq(chatId), contains("Попробуйте загрузить данные командой /load"));
    }

    @Test
    void testExecuteExpenditureQueryAsync_Timeout() {
        Long chatId = 12345L;

        doAnswer(invocation -> {
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("executeExpenditureQueryAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 1: Средние расходы"));
    }

    @Test
    void testExecuteMathSchoolsQueryAsync_Success() {
        Long chatId = 12345L;
        String result = "Лучшие школы по математике:\nШкола А: 95.5 баллов";

        when(statisticsService.getMathSchoolsReportForTelegram()).thenReturn(result);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("executeMathSchoolsQueryAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 2: Лучшие школы по математике"));
        verify(bot).sendText(eq(chatId), eq(result));
    }

    @Test
    void testExecuteMathSchoolsQueryAsync_Exception() {
        Long chatId = 12345L;
        String errorMessage = "No schools found";

        when(statisticsService.getMathSchoolsReportForTelegram())
                .thenThrow(new RuntimeException(errorMessage));

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("executeMathSchoolsQueryAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 2: Лучшие школы по математике"));
        verify(bot).sendText(eq(chatId), contains("Ошибка выполнения запроса"));
        verify(bot).sendText(eq(chatId), contains("Проверьте, есть ли школы с таким количеством студентов в базе"));
    }

    @Test
    void testExecuteStudentStatsQueryAsync_EmptyResult() {
        Long chatId = 12345L;
        List<CountryStudentStats> emptyList = Arrays.asList();

        when(statisticsService.getStudentStatistics(10)).thenReturn(emptyList);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("executeStudentStatsQueryAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 3: Статистика студентов по странам"));
        verify(bot).sendText(eq(chatId), contains("Нет данных о студентах"));
        verify(bot).sendText(eq(chatId), contains("База данных пуста"));
    }

    @Test
    void testExecuteStudentStatsQueryAsync_NullResult() {
        Long chatId = 12345L;

        when(statisticsService.getStudentStatistics(10)).thenReturn(null);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("executeStudentStatsQueryAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 3: Статистика студентов по странам"));
        verify(bot).sendText(eq(chatId), contains("Нет данных о студентах"));
    }

    @Test
    void testExecuteStudentStatsQueryAsync_Exception() {
        Long chatId = 12345L;
        String errorMessage = "Database error";

        when(statisticsService.getStudentStatistics(10))
                .thenThrow(new RuntimeException(errorMessage));

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("executeStudentStatsQueryAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Запрос 3: Статистика студентов по странам"));
        verify(bot).sendText(eq(chatId), contains("Ошибка получения статистики"));
        verify(bot).sendText(eq(chatId), contains("Попробуйте загрузить данные командой /load"));
    }

    @Test
    void testShowMenu_NullBot() {
        Long chatId = 12345L;
        SchoolTelegramBot nullBot = null;
        assertThrows(NullPointerException.class,
                () -> queryHandler.showMenu(chatId, nullBot));
    }

    @Test
    void testHandleCallback_NullBot() {
        Long chatId = 12345L;
        String callbackData = "query_expenditure";
        SchoolTelegramBot nullBot = null;
        assertThrows(NullPointerException.class,
                () -> queryHandler.handleCallback(chatId, callbackData, nullBot));
    }

    private void invokePrivateMethod(String methodName, Object... args) {
        try {
            var method = DefaultQueryHandler.class.getDeclaredMethod(methodName,
                    Long.class, SchoolTelegramBot.class);
            method.setAccessible(true);
            method.invoke(queryHandler, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }

    private CountryStudentStats createCountryStats(String name, int schoolCount,
                                                   double avgStudents, int minStudents,
                                                   int maxStudents, int totalStudents) {
        CountryStudentStats stats = new CountryStudentStats();
        stats.setCountryName(name);
        stats.setSchoolCount(schoolCount);
        stats.setAvgStudents(avgStudents);
        stats.setMinStudents(minStudents);
        stats.setMaxStudents(maxStudents);
        stats.setTotalStudents(totalStudents);
        return stats;
    }
}