package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.infrastructure.telegram.bot.SchoolTelegramBot;
import org.school.analysis.infrastructure.telegram.handlers.*;
import org.school.analysis.infrastructure.telegram.util.MessageSender;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolTelegramBotTest {

    private SchoolTelegramBot bot;

    @Mock
    private CommandHandler commandHandler;

    @Mock
    private QueryHandler queryHandler;

    @Mock
    private ChartHandler chartHandler;

    @Mock
    private DataHandler dataHandler;

    @Mock
    private MessageSender messageSender;

    @Mock
    private ExecutorService executorService;

    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    private final String TEST_BOT_TOKEN = "test-bot-token";
    private final String TEST_BOT_USERNAME = "test_bot";

    @BeforeEach
    void setUp() throws Exception {
        bot = new SchoolTelegramBot(TEST_BOT_TOKEN, TEST_BOT_USERNAME,
                commandHandler, queryHandler, chartHandler, dataHandler) {
        };

        var messageSenderField = SchoolTelegramBot.class.getDeclaredField("messageSender");
        messageSenderField.setAccessible(true);
        messageSenderField.set(bot, messageSender);
    }

    @AfterEach
    void tearDown() {
        if (bot != null) {
            bot.shutdown();
        }
    }

    @Test
    void testConstructor() {
        SchoolTelegramBot newBot = new SchoolTelegramBot(TEST_BOT_TOKEN, TEST_BOT_USERNAME,
                commandHandler, queryHandler, chartHandler, dataHandler);
        assertNotNull(newBot);
        assertEquals(TEST_BOT_USERNAME, newBot.getBotUsername());
        assertEquals(TEST_BOT_TOKEN, newBot.getBotToken());
    }

    @Test
    void testProcessUpdate_TextMessage() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getText()).thenReturn("/start");
        invokePrivateMethod("processUpdate", update);
        verify(commandHandler).sendWelcome(eq(12345L), eq(bot));
    }

    @Test
    void testProcessUpdate_CallbackQuery() {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message callbackMessage = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.getMessage()).thenReturn(callbackMessage);
        when(callbackMessage.getChatId()).thenReturn(12345L);
        when(callbackQuery.getData()).thenReturn("query_test");
        when(callbackQuery.getId()).thenReturn("callback123");
        invokePrivateMethod("processUpdate", update);
        verify(queryHandler).handleCallback(eq(12345L), eq("query_test"), eq(bot));
    }

    @Test
    void testProcessUpdate_DocumentMessage() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasDocument()).thenReturn(true);
        when(message.getChatId()).thenReturn(12345L);
        invokePrivateMethod("processUpdate", update);
        verify(messageSender).sendText(eq(12345L), contains("Загрузка файлов"));
    }

    @Test
    void testProcessUpdate_ExceptionHandling() {
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        Message message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getText()).thenReturn("/start");
        doThrow(new RuntimeException("Handler error")).when(commandHandler).sendWelcome(anyLong(), any());
        invokePrivateMethod("processUpdate", update);
        verify(messageSender).sendText(eq(12345L), contains("Произошла ошибка"));
    }

    @Test
    void testProcessMessage_Command() {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getText()).thenReturn("/start");
        invokePrivateMethod("processMessage", message);
        verify(commandHandler).sendWelcome(eq(12345L), eq(bot));
    }

    @Test
    void testProcessMessage_TextMessage() {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getText()).thenReturn("Запросы");
        invokePrivateMethod("processMessage", message);
        verify(queryHandler).showMenu(eq(12345L), eq(bot));
    }

    @Test
    void testProcessMessage_UnknownText() {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getText()).thenReturn("Неизвестный текст");
        invokePrivateMethod("processMessage", message);
        verify(messageSender).sendText(eq(12345L), contains("Используйте кнопки меню"));
    }

    @Test
    void testProcessMessage_CommandWithBotUsername() {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getText()).thenReturn("/start@" + TEST_BOT_USERNAME);
        invokePrivateMethod("processMessage", message);
        verify(commandHandler).sendWelcome(eq(12345L), eq(bot));
    }

    @Test
    void testProcessCallbackQuery_ChartCallback() {
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(12345L);
        when(callbackQuery.getData()).thenReturn("chart_students");
        when(callbackQuery.getId()).thenReturn("callback123");
        invokePrivateMethod("processCallbackQuery", callbackQuery);
        verify(chartHandler).handleCallback(eq(12345L), eq("chart_students"), eq(bot));
    }

    @Test
    void testProcessCallbackQuery_DataCallback() {
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(12345L);
        when(callbackQuery.getData()).thenReturn("data_stats");
        when(callbackQuery.getId()).thenReturn("callback123");
        invokePrivateMethod("processCallbackQuery", callbackQuery);
        verify(dataHandler).handleCallback(eq(12345L), eq("data_stats"), eq(bot));
    }

    @Test
    void testProcessCallbackQuery_Exception(){
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(12345L);
        when(callbackQuery.getData()).thenReturn("query_test");
        when(callbackQuery.getId()).thenReturn("callback123");

        doThrow(new RuntimeException("Handler error")).when(queryHandler)
                .handleCallback(anyLong(), anyString(), any());
        invokePrivateMethod("processCallbackQuery", callbackQuery);
        verify(messageSender).sendText(eq(12345L), contains("Ошибка обработки запроса"));
    }

    @Test
    void testProcessDocument() {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(12345L);
        invokePrivateMethod("processDocument", message);
        verify(messageSender).sendText(eq(12345L), contains("Загрузка файлов"));
    }

    @Test
    void testHandleCommand_Start() {
        Long chatId = 12345L;
        String command = "/start";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(commandHandler).sendWelcome(eq(chatId), eq(bot));
    }

    @Test
    void testHandleCommand_Help() {
        Long chatId = 12345L;
        String command = "/help";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(commandHandler).sendHelp(eq(chatId), eq(bot));
    }

    @Test
    void testHandleCommand_Load() {
        Long chatId = 12345L;
        String command = "/load";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(dataHandler).handleLoadData(eq(chatId), eq(bot));
    }

    @Test
    void testHandleCommand_Queries() {
        Long chatId = 12345L;
        String command = "/queries";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(queryHandler).showMenu(eq(chatId), eq(bot));
    }

    @Test
    void testHandleCommand_Charts() {
        Long chatId = 12345L;
        String command = "/charts";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(chartHandler).showMenu(eq(chatId), eq(bot));
    }

    @Test
    void testHandleCommand_Stats() {
        Long chatId = 12345L;
        String command = "/stats";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(dataHandler).showStatistics(eq(chatId), eq(bot));
    }

    @Test
    void testHandleCommand_Status() {
        Long chatId = 12345L;
        String command = "/status";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(messageSender).sendText(eq(chatId), contains("Статус бота"));
    }

    @Test
    void testHandleCommand_UnknownCommand() {
        Long chatId = 12345L;
        String command = "/unknown";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(messageSender).sendText(eq(chatId), contains("Неизвестная команда"));
    }

    @Test
    void testHandleCommand_CaseInsensitive() {
        Long chatId = 12345L;
        String command = "/START";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(commandHandler).sendWelcome(eq(chatId), eq(bot));
    }

    @Test
    void testHandleTextMessage_Queries() {
        Long chatId = 12345L;
        String text = "Запросы";
        invokePrivateMethod("handleTextMessage", chatId, text);
        verify(queryHandler).showMenu(eq(chatId), eq(bot));
    }

    @Test
    void testHandleTextMessage_Charts() {
        Long chatId = 12345L;
        String text = "Диаграммы";
        invokePrivateMethod("handleTextMessage", chatId, text);
        verify(chartHandler).showMenu(eq(chatId), eq(bot));
    }

    @Test
    void testHandleTextMessage_LoadData() {
        Long chatId = 12345L;
        String text = "Загрузить данные";
        invokePrivateMethod("handleTextMessage", chatId, text);
        verify(dataHandler).handleLoadData(eq(chatId), eq(bot));
    }

    @Test
    void testHandleTextMessage_Statistics() {
        Long chatId = 12345L;
        String text = "Статистика";
        invokePrivateMethod("handleTextMessage", chatId, text);
        verify(dataHandler).showStatistics(eq(chatId), eq(bot));
    }

    @Test
    void testHandleTextMessage_Help() {
        Long chatId = 12345L;
        String text = "ℹПомощь";
        invokePrivateMethod("handleTextMessage", chatId, text);
        verify(commandHandler).sendHelp(eq(chatId), eq(bot));
    }

    @Test
    void testHandleTextMessage_Status() {
        Long chatId = 12345L;
        String text = "Статус";
        invokePrivateMethod("handleTextMessage", chatId, text);
        verify(messageSender).sendText(eq(chatId), contains("Статус бота"));
    }

    @Test
    void testHandleTextMessage_UnknownText() {
        Long chatId = 12345L;
        String text = "Неизвестный текст";
        invokePrivateMethod("handleTextMessage", chatId, text);
        verify(messageSender).sendText(eq(chatId), contains("Используйте кнопки меню"));
    }

    @Test
    void testGetChatId_FromMessage() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(12345L);
        Long chatId = invokePrivateMethod("getChatId", update);

        assertEquals(12345L, chatId);
    }

    @Test
    void testGetChatId_FromCallbackQuery() {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(67890L);
        Long chatId = invokePrivateMethod("getChatId", update);
        assertEquals(67890L, chatId);
    }

    @Test
    void testGetChatId_NoChatId() {
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(false);
        when(update.hasCallbackQuery()).thenReturn(false);

        Long chatId = invokePrivateMethod("getChatId", update);
        assertNull(chatId);
    }

    @Test
    void testSendText() {
        Long chatId = 12345L;
        String text = "Test message";
        bot.sendText(chatId, text);
        verify(messageSender).sendText(eq(chatId), eq(text));
    }

    @Test
    void testSendMenu() {
        Long chatId = 12345L;
        String text = "Menu text";
        org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard =
                mock(org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard.class);
        bot.sendMenu(chatId, text, keyboard);
        verify(messageSender).sendMenu(eq(chatId), eq(text), eq(keyboard));
    }

    @Test
    void testSendInlineMenu() {
        Long chatId = 12345L;
        String text = "Inline menu text";
        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard =
                mock(org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.class);
        bot.sendInlineMenu(chatId, text, keyboard);
        verify(messageSender).sendInlineMenu(eq(chatId), eq(text), eq(keyboard));
    }

    @Test
    void testGetMessageSender() {
        MessageSender result = bot.getMessageSender();
        assertEquals(messageSender, result);
    }

    @Test
    void testGetBotUsername() {
        assertEquals(TEST_BOT_USERNAME, bot.getBotUsername());
    }

    @Test
    void testGetBotToken() {
        assertEquals(TEST_BOT_TOKEN, bot.getBotToken());
    }

    @Test
    void testCreateExecutorService() throws Exception {
        SchoolTelegramBot testBot = new SchoolTelegramBot(TEST_BOT_TOKEN, TEST_BOT_USERNAME,
                commandHandler, queryHandler, chartHandler, dataHandler);
        var method = SchoolTelegramBot.class.getDeclaredMethod("createExecutorService");
        method.setAccessible(true);
        ExecutorService executor = (ExecutorService) method.invoke(testBot);
        assertNotNull(executor);
        assertFalse(executor.isShutdown());
        executor.shutdown();
    }

    @Test
    void testProcessUpdate_NoMessageText() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(false);
        when(message.hasDocument()).thenReturn(false);
        when(message.getChatId()).thenReturn(12345L);
        invokePrivateMethod("processUpdate", update);
        verifyNoInteractions(commandHandler, queryHandler, chartHandler, dataHandler);
    }

    @Test
    void testProcessMessage_EmptyText() {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getText()).thenReturn("   ");
        invokePrivateMethod("processMessage", message);
        verify(messageSender).sendText(eq(12345L), contains("Используйте кнопки меню"));
    }

    @Test
    void testHandleCommand_EmptyCommand() {
        Long chatId = 12345L;
        String command = "/";
        invokePrivateMethod("handleCommand", chatId, command);
        verify(messageSender).sendText(eq(chatId), contains("Неизвестная команда"));
    }

    @Test
    void testProcessCallbackQuery_NullCallbackData() {
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(12345L);
        when(callbackQuery.getData()).thenReturn(null);
        when(callbackQuery.getId()).thenReturn("callback123");
        invokePrivateMethod("processCallbackQuery", callbackQuery);
        verify(messageSender).sendText(eq(12345L), contains("Ошибка обработки запроса"));
    }

    private <T> T invokePrivateMethod(String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }

            var method = SchoolTelegramBot.class.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return (T) method.invoke(bot, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }
}