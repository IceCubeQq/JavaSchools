package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.ports.output.ChartGenerator;
import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;
import org.school.analysis.presentation.telegram.handlers.DefaultChartHandler;
import org.school.analysis.presentation.telegram.util.MessageSender;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultChartHandlerTest {

    @Mock
    private ChartGenerator chartGenerator;

    @Mock
    private ExecutorService executorService;

    @Mock
    private SchoolTelegramBot bot;

    @Mock
    private MessageSender messageSender;

    private DefaultChartHandler chartHandler;

    @BeforeEach
    void setUp() {
        chartHandler = new DefaultChartHandler(chartGenerator, executorService);
    }

    @Test
    void testConstructor() {
        DefaultChartHandler handler = new DefaultChartHandler(chartGenerator, executorService);
        assertNotNull(handler);
    }

    @Test
    void testShowMenu() {
        Long chatId = 12345L;
        chartHandler.showMenu(chatId, bot);
        verify(bot).sendInlineMenu(eq(chatId), contains("Диаграммы"), any());
    }

    @Test
    void testHandleCallback_UnknownCallback() {
        Long chatId = 12345L;
        String callbackData = "unknown_chart";
        chartHandler.handleCallback(chatId, callbackData, bot);
        verify(bot, never()).sendText(anyLong(), anyString());
    }

    @Test
    void testCreateStudentsChartAsync_Success() {
        Long chatId = 12345L;
        byte[] chartBytes = new byte[]{1, 2, 3, 4, 5};
        String description = "Test chart description";

        when(chartGenerator.createAverageStudentsChart()).thenReturn(chartBytes);
        when(chartGenerator.getChartDescription()).thenReturn(description);
        when(bot.getMessageSender()).thenReturn(messageSender);
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("createStudentsChartAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Создаю диаграмму"));
        verify(messageSender).sendPhoto(eq(chatId), eq(chartBytes), eq(description));
        verify(bot).sendText(eq(chatId), contains("Диаграмма успешно создана"));
    }

    @Test
    void testCreateStudentsChartAsync_EmptyChartBytes() {
        Long chatId = 12345L;
        byte[] emptyBytes = new byte[0];
        String description = "Empty chart";

        when(chartGenerator.createAverageStudentsChart()).thenReturn(emptyBytes);
        when(chartGenerator.getChartDescription()).thenReturn(description);
        when(bot.getMessageSender()).thenReturn(messageSender);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("createStudentsChartAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Создаю диаграмму"));
        verify(messageSender).sendPhoto(eq(chatId), eq(emptyBytes), eq(description));
        verify(bot).sendText(eq(chatId), contains("Диаграмма успешно создана"));
    }

    @Test
    void testCreateStudentsChartAsync_NullDescription() {
        Long chatId = 12345L;
        byte[] chartBytes = new byte[]{1, 2, 3};

        when(chartGenerator.createAverageStudentsChart()).thenReturn(chartBytes);
        when(chartGenerator.getChartDescription()).thenReturn(null);
        when(bot.getMessageSender()).thenReturn(messageSender);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("createStudentsChartAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Создаю диаграмму"));
        verify(messageSender).sendPhoto(eq(chatId), eq(chartBytes), eq(null));
        verify(bot).sendText(eq(chatId), contains("Диаграмма успешно создана"));
    }

    @Test
    void testCreateStudentsChartAsync_EmptyDescription() {
        Long chatId = 12345L;
        byte[] chartBytes = new byte[]{1, 2, 3};
        String emptyDescription = "";

        when(chartGenerator.createAverageStudentsChart()).thenReturn(chartBytes);
        when(chartGenerator.getChartDescription()).thenReturn(emptyDescription);
        when(bot.getMessageSender()).thenReturn(messageSender);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("createStudentsChartAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Создаю диаграмму"));
        verify(messageSender).sendPhoto(eq(chatId), eq(chartBytes), eq(emptyDescription));
        verify(bot).sendText(eq(chatId), contains("Диаграмма успешно создана"));
    }

    @Test
    void testShowMenu_WithKeyboard() {
        Long chatId = 12345L;
        chartHandler.showMenu(chatId, bot);
        verify(bot).sendInlineMenu(eq(chatId), anyString(), notNull());
    }

    @Test
    void testShowMenu_ForDifferentChatIds() {
        Long[] chatIds = {12345L, 67890L, 11111L};
        for (Long chatId : chatIds) {
            chartHandler.showMenu(chatId, bot);
        }
        verify(bot, times(chatIds.length)).sendInlineMenu(anyLong(), anyString(), any());
    }

    @Test
    void testHandleCallback_MultipleStudentsChartRequests() {
        Long chatId = 12345L;
        String callbackData = "chart_students";

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        for (int i = 0; i < 3; i++) {
            chartHandler.handleCallback(chatId, callbackData, bot);
        }
        verify(bot, times(3)).sendText(eq(chatId), contains("Создаю диаграмму"));
    }

    @Test
    void testCreateStudentsChartAsync_WithPhotoSendingException() {
        Long chatId = 12345L;
        byte[] chartBytes = new byte[]{1, 2, 3};
        String description = "Test chart";

        when(chartGenerator.createAverageStudentsChart()).thenReturn(chartBytes);
        when(chartGenerator.getChartDescription()).thenReturn(description);
        when(bot.getMessageSender()).thenReturn(messageSender);

        doThrow(new RuntimeException("Photo sending failed")).when(messageSender)
                .sendPhoto(anyLong(), any(), anyString());

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        invokePrivateMethod("createStudentsChartAsync", chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Создаю диаграмму"));
        verify(messageSender).sendPhoto(eq(chatId), eq(chartBytes), eq(description));
        verify(bot).sendText(eq(chatId), contains("Ошибка создания диаграммы"));
    }

    @Test
    void testShowMenu_NullBot() {
        Long chatId = 12345L;
        SchoolTelegramBot nullBot = null;
        assertThrows(NullPointerException.class,
                () -> chartHandler.showMenu(chatId, nullBot));
    }

    @Test
    void testHandleCallback_NullBot() {
        Long chatId = 12345L;
        String callbackData = "chart_students";
        SchoolTelegramBot nullBot = null;
        assertThrows(NullPointerException.class,
                () -> chartHandler.handleCallback(chatId, callbackData, nullBot));
    }

    @Test
    void testConcurrentChartCreation() throws InterruptedException {
        Long chatId = 12345L;
        byte[] chartBytes = new byte[]{1, 2, 3};
        String description = "Test chart";

        when(chartGenerator.createAverageStudentsChart()).thenReturn(chartBytes);
        when(chartGenerator.getChartDescription()).thenReturn(description);
        ExecutorService realExecutor = java.util.concurrent.Executors.newFixedThreadPool(2);
        DefaultChartHandler concurrentHandler = new DefaultChartHandler(chartGenerator, realExecutor);

        SchoolTelegramBot mockBot = mock(SchoolTelegramBot.class);
        MessageSender mockMessageSender = mock(MessageSender.class);
        when(mockBot.getMessageSender()).thenReturn(mockMessageSender);
        Thread thread1 = new Thread(() -> {
            try {
                invokePrivateMethod(concurrentHandler, "createStudentsChartAsync", chatId, mockBot);
            } catch (Exception e) {
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                invokePrivateMethod(concurrentHandler, "createStudentsChartAsync", chatId, mockBot);
            } catch (Exception e) {
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        realExecutor.shutdown();
        realExecutor.awaitTermination(5, TimeUnit.SECONDS);
        verify(mockBot, atLeast(2)).sendText(eq(chatId), contains("Создаю диаграмму"));
        verify(mockMessageSender, atLeast(1)).sendPhoto(eq(chatId), eq(chartBytes), eq(description));
    }

    private void invokePrivateMethod(String methodName, Object... args) {
        try {
            var method = DefaultChartHandler.class.getDeclaredMethod(methodName,
                    Long.class, SchoolTelegramBot.class);
            method.setAccessible(true);
            method.invoke(chartHandler, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }

    private void invokePrivateMethod(DefaultChartHandler handler, String methodName, Object... args) {
        try {
            var method = DefaultChartHandler.class.getDeclaredMethod(methodName,
                    Long.class, SchoolTelegramBot.class);
            method.setAccessible(true);
            method.invoke(handler, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }
}