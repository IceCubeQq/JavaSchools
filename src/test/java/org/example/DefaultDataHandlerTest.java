package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.ports.input.LoadSchoolsUseCase;
import org.school.analysis.application.ports.output.DatabaseStatisticsPort;
import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;
import org.school.analysis.presentation.telegram.handlers.DefaultDataHandler;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultDataHandlerTest {

    @Mock
    private DatabaseStatisticsPort databaseStatisticsPort;

    @Mock
    private ExecutorService executorService;

    @Mock
    private LoadSchoolsUseCase loadSchoolsUseCase;

    @Mock
    private SchoolTelegramBot bot;

    @Mock
    private Logger logger;

    private DefaultDataHandler dataHandler;

    @BeforeEach
    void setUp() {
        dataHandler = new DefaultDataHandler(databaseStatisticsPort, executorService, loadSchoolsUseCase);
    }

    @Test
    void testConstructor() {
        DefaultDataHandler handler = new DefaultDataHandler(databaseStatisticsPort, executorService, loadSchoolsUseCase);
        assertNotNull(handler);
    }

    @Test
    void testHandleLoadData_Success(){
        Long chatId = 12345L;
        int loadedCount = 100;
        when(loadSchoolsUseCase.execute(any(FileInputStream.class))).thenReturn(loadedCount);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.handleLoadData(chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Начинаю загрузку данных"));
        verify(bot).sendText(eq(chatId), contains("Успешно загружено 100 школ"));
        verify(bot).sendText(eq(chatId), contains("Теперь вы можете выполнять запросы"));
    }

    @Test
    void testHandleLoadData_ZeroSchools() throws Exception {
        Long chatId = 12345L;

        when(loadSchoolsUseCase.execute(any(FileInputStream.class))).thenReturn(0);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.handleLoadData(chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Начинаю загрузку данных"));
        verify(bot).sendText(eq(chatId), contains("Загружено 0 школ"));
    }

    @Test
    void testHandleLoadData_FileNotFoundException() {
        Long chatId = 12345L;

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.handleLoadData(chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Начинаю загрузку данных"));
    }

    @Test
    void testHandleLoadData_LoadExceptionWithSpecificMessage() {
        Long chatId = 12345L;
        String errorMessage = "Не найдено школ в CSV";

        when(loadSchoolsUseCase.execute(any(FileInputStream.class)))
                .thenThrow(new RuntimeException(errorMessage));

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.handleLoadData(chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Начинаю загрузку данных"));
        verify(bot).sendText(eq(chatId), contains("Ошибка загрузки"));
        verify(bot).sendText(eq(chatId), contains("Проверьте формат CSV файла"));
    }

    @Test
    void testHandleLoadData_LoadExceptionWithGenericMessage(){
        Long chatId = 12345L;
        String errorMessage = "Database connection failed";

        when(loadSchoolsUseCase.execute(any(FileInputStream.class)))
                .thenThrow(new RuntimeException(errorMessage));

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.handleLoadData(chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Начинаю загрузку данных"));
        verify(bot).sendText(eq(chatId), contains("Ошибка загрузки"));
        verify(bot).sendText(eq(chatId), contains(errorMessage));
        verify(bot, never()).sendText(eq(chatId), contains("Проверьте формат CSV файла"));
    }

    @Test
    void testHandleLoadData_Timeout() {
        Long chatId = 12345L;
        doAnswer(invocation -> {
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.handleLoadData(chatId, bot);

        verify(bot).sendText(eq(chatId), contains("Начинаю загрузку данных"));
    }

    @Test
    void testShowStatistics_Success() {
        Long chatId = 12345L;
        String stats = "Статистика БД:\nШкол: 100\nСтудентов: 5000";

        when(databaseStatisticsPort.getDatabaseStatistics()).thenReturn(stats);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.showStatistics(chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Получаю статистику БД"));
        verify(bot).sendText(eq(chatId), eq(stats));
    }

    @Test
    void testShowStatistics_Exception() {
        Long chatId = 12345L;
        String errorMessage = "Database connection failed";
        when(databaseStatisticsPort.getDatabaseStatistics())
                .thenThrow(new RuntimeException(errorMessage));
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.showStatistics(chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Получаю статистику БД"));
        verify(bot).sendText(eq(chatId), contains("Ошибка получения статистики"));
        verify(bot).sendText(eq(chatId), contains(errorMessage));
    }

    @Test
    void testShowStatistics_Timeout() {
        Long chatId = 12345L;

        doAnswer(invocation -> {
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.showStatistics(chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Получаю статистику БД"));
    }

    @Test
    void testHandleCallback_DataReload() {
        Long chatId = 12345L;
        String callbackData = "data_reload";

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.handleCallback(chatId, callbackData, bot);
        verify(bot).sendText(eq(chatId), contains("Начинаю загрузку данных"));
    }

    @Test
    void testHandleCallback_DataStats() {
        Long chatId = 12345L;
        String callbackData = "data_stats";
        String stats = "Test statistics";

        when(databaseStatisticsPort.getDatabaseStatistics()).thenReturn(stats);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        dataHandler.handleCallback(chatId, callbackData, bot);
        verify(bot).sendText(eq(chatId), contains("Получаю статистику БД"));
        verify(bot).sendText(eq(chatId), eq(stats));
    }

    @Test
    void testHandleCallback_UnknownCallback() {
        Long chatId = 12345L;
        String callbackData = "unknown_data";
        dataHandler.handleCallback(chatId, callbackData, bot);
        verify(bot).sendText(eq(chatId), contains("Неизвестная команда данных"));
        verify(executorService, never()).execute(any(Runnable.class));
    }

    @Test
    void testHandleLoadData_NullBot() {
        Long chatId = 12345L;
        SchoolTelegramBot nullBot = null;
        assertThrows(NullPointerException.class,
                () -> dataHandler.handleLoadData(chatId, nullBot));
    }

    @Test
    void testShowStatistics_NullBot() {
        Long chatId = 12345L;
        SchoolTelegramBot nullBot = null;
        assertThrows(NullPointerException.class,
                () -> dataHandler.showStatistics(chatId, nullBot));
    }

    @Test
    void testHandleCallback_NullBot() {
        Long chatId = 12345L;
        String callbackData = "data_reload";
        SchoolTelegramBot nullBot = null;
        assertThrows(NullPointerException.class,
                () -> dataHandler.handleCallback(chatId, callbackData, nullBot));
    }

    @Test
    void testHandleLoadData_ExecutorServiceException() {
        Long chatId = 12345L;

        doThrow(new RuntimeException("Executor service error"))
                .when(executorService).execute(any(Runnable.class));
        assertThrows(RuntimeException.class,
                () -> dataHandler.handleLoadData(chatId, bot));

        verify(bot).sendText(eq(chatId), contains("Начинаю загрузку данных"));
    }

    @Test
    void testShowStatistics_ExecutorServiceException() {
        Long chatId = 12345L;

        doThrow(new RuntimeException("Executor service error"))
                .when(executorService).execute(any(Runnable.class));
        assertThrows(RuntimeException.class,
                () -> dataHandler.showStatistics(chatId, bot));

        verify(bot).sendText(eq(chatId), contains("Получаю статистику БД"));
    }

    @Test
    void testMultipleLoadDataCalls() {
        Long chatId = 12345L;
        int loadedCount = 100;

        when(loadSchoolsUseCase.execute(any(FileInputStream.class))).thenReturn(loadedCount);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        for (int i = 0; i < 3; i++) {
            dataHandler.handleLoadData(chatId, bot);
        }
        verify(bot, times(3)).sendText(eq(chatId), contains("Начинаю загрузку данных"));
        verify(bot, times(3)).sendText(eq(chatId), contains("Успешно загружено 100 школ"));
    }

    @Test
    void testMultipleStatisticsCalls() {
        Long chatId = 12345L;
        String stats = "Test statistics";

        when(databaseStatisticsPort.getDatabaseStatistics()).thenReturn(stats);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        for (int i = 0; i < 3; i++) {
            dataHandler.showStatistics(chatId, bot);
        }
        verify(bot, times(3)).sendText(eq(chatId), contains("Получаю статистику БД"));
        verify(bot, times(3)).sendText(eq(chatId), eq(stats));
    }

    @Test
    void testMixedCallbacks() {
        Long chatId = 12345L;
        String[] callbacks = {"data_reload", "data_stats", "data_reload", "unknown"};

        when(loadSchoolsUseCase.execute(any(FileInputStream.class))).thenReturn(100);
        when(databaseStatisticsPort.getDatabaseStatistics()).thenReturn("Stats");

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        for (String callback : callbacks) {
            try {
                dataHandler.handleCallback(chatId, callback, bot);
            } catch (Exception e) {
            }
        }
        verify(bot, times(2)).sendText(eq(chatId), contains("Начинаю загрузку данных"));
        verify(bot, times(1)).sendText(eq(chatId), contains("Получаю статистику БД"));
        verify(bot, times(1)).sendText(eq(chatId), contains("Неизвестная команда данных"));
    }

    @Test
    void testConcurrentOperations() throws InterruptedException {
        Long chatId = 12345L;

        when(loadSchoolsUseCase.execute(any(FileInputStream.class))).thenReturn(100);
        when(databaseStatisticsPort.getDatabaseStatistics()).thenReturn("Stats");
        ExecutorService realExecutor = java.util.concurrent.Executors.newFixedThreadPool(2);
        DefaultDataHandler concurrentHandler = new DefaultDataHandler(
                databaseStatisticsPort, realExecutor, loadSchoolsUseCase);

        SchoolTelegramBot mockBot = mock(SchoolTelegramBot.class);
        Thread loadThread = new Thread(() ->
                concurrentHandler.handleLoadData(chatId, mockBot));
        Thread statsThread = new Thread(() ->
                concurrentHandler.showStatistics(chatId, mockBot));

        loadThread.start();
        statsThread.start();

        loadThread.join();
        statsThread.join();

        realExecutor.shutdown();
        realExecutor.awaitTermination(5, TimeUnit.SECONDS);
        verify(mockBot, atLeast(1)).sendText(eq(chatId), contains("Начинаю загрузку данных"));
        verify(mockBot, atLeast(1)).sendText(eq(chatId), contains("Получаю статистику БД"));
    }

    @Test
    void testLoadDataWithDifferentChatIds() {
        Long[] chatIds = {11111L, 22222L, 33333L};
        int loadedCount = 50;

        when(loadSchoolsUseCase.execute(any(FileInputStream.class))).thenReturn(loadedCount);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        for (Long chatId : chatIds) {
            dataHandler.handleLoadData(chatId, bot);
        }
        for (Long chatId : chatIds) {
            verify(bot).sendText(eq(chatId), contains("Начинаю загрузку данных"));
            verify(bot).sendText(eq(chatId), contains("Успешно загружено 50 школ"));
        }
    }
}