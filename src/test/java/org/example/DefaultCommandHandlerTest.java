package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;
import org.school.analysis.presentation.telegram.handlers.DefaultCommandHandler;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultCommandHandlerTest {

    @Mock
    private SchoolTelegramBot bot;

    private DefaultCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new DefaultCommandHandler();
    }

    @Test
    void testConstructor() {
        DefaultCommandHandler handler = new DefaultCommandHandler();
        assertNotNull(handler);
    }

    @Test
    void testSendWelcome() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId), contains("Анализ данных школ"), any());
        verify(bot).sendMenu(eq(chatId), contains("JavaSchoolRTFBot"), any());
    }

    @Test
    void testSendWelcome_ContainsKeyElements() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId),
                argThat(message ->
                        message.contains("Основные функции:") &&
                                message.contains("Запросы к БД") &&
                                message.contains("Диаграммы") &&
                                message.contains("Загрузка данных") &&
                                message.contains("Статистика") &&
                                message.contains("/load") &&
                                message.contains("/queries") &&
                                message.contains("/charts") &&
                                message.contains("/stats") &&
                                message.contains("/help")
                ),
                any()
        );
    }

    @Test
    void testSendWelcome_WithKeyboard() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId), anyString(), notNull());
    }

    @Test
    void testSendWelcome_MultipleCalls() {
        Long[] chatIds = {12345L, 67890L, 11111L};
        for (Long chatId : chatIds) {
            commandHandler.sendWelcome(chatId, bot);
        }
        verify(bot, times(chatIds.length)).sendMenu(anyLong(), anyString(), any());
        for (Long chatId : chatIds) {
            verify(bot).sendMenu(eq(chatId), anyString(), any());
        }
    }

    @Test
    void testSendWelcome_SpecificChatId() {
        Long chatId = 99999L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(99999L), anyString(), any());
        verify(bot, never()).sendMenu(eq(12345L), anyString(), any());
    }

    @Test
    void testSendHelp() {
        Long chatId = 12345L;
        commandHandler.sendHelp(chatId, bot);
        verify(bot).sendText(eq(chatId), contains("Справка по командам"));
        verify(bot).sendText(eq(chatId), contains("Основные команды:"));
    }

    @Test
    void testSendHelp_ContainsAllCommands() {
        Long chatId = 12345L;
        commandHandler.sendHelp(chatId, bot);
        verify(bot).sendText(eq(chatId),
                argThat(message ->
                        message.contains("/start") &&
                                message.contains("/help") &&
                                message.contains("/load") &&
                                message.contains("/queries") &&
                                message.contains("/charts") &&
                                message.contains("/stats") &&
                                message.contains("/status") &&
                                message.contains("Запросы") &&
                                message.contains("Диаграммы") &&
                                message.contains("Загрузить данные") &&
                                message.contains("Статистика") &&
                                message.contains("Помощь") &&
                                message.contains("Статус") &&
                                message.contains("Средние расходы") &&
                                message.contains("Школы с лучшей математикой") &&
                                message.contains("Среднее количество студентов")
                )
        );
    }

    @Test
    void testSendHelp_MultipleCalls() {
        Long[] chatIds = {12345L, 67890L, 11111L};
        for (Long chatId : chatIds) {
            commandHandler.sendHelp(chatId, bot);
        }
        verify(bot, times(chatIds.length)).sendText(anyLong(), anyString());
        for (Long chatId : chatIds) {
            verify(bot).sendText(eq(chatId), anyString());
        }
    }

    @Test
    void testSendHelp_DifferentChatIds() {
        Long chatId1 = 11111L;
        Long chatId2 = 22222L;
        commandHandler.sendHelp(chatId1, bot);
        commandHandler.sendHelp(chatId2, bot);
        verify(bot).sendText(eq(11111L), anyString());
        verify(bot).sendText(eq(22222L), anyString());
        verify(bot, times(2)).sendText(anyLong(), anyString());
    }

    @Test
    void testSendWelcomeAndSendHelp_Combined() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        commandHandler.sendHelp(chatId, bot);
        verify(bot).sendMenu(eq(chatId), anyString(), any());
        verify(bot).sendText(eq(chatId), anyString());
        verify(bot, times(1)).sendMenu(anyLong(), anyString(), any());
        verify(bot, times(1)).sendText(anyLong(), anyString());
    }

    @Test
    void testSendWelcome_NullBot() {
        Long chatId = 12345L;
        SchoolTelegramBot nullBot = null;
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> commandHandler.sendWelcome(chatId, nullBot));
    }

    @Test
    void testSendHelp_NullBot() {
        Long chatId = 12345L;
        SchoolTelegramBot nullBot = null;
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> commandHandler.sendHelp(chatId, nullBot));
    }

    @Test
    void testSendWelcome_MessageLength() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId),
                argThat(message -> message.length() > 100),
                any()
        );
    }

    @Test
    void testSendHelp_MessageLength() {
        Long chatId = 12345L;
        commandHandler.sendHelp(chatId, bot);
        verify(bot).sendText(eq(chatId),
                argThat(message -> message.length() > 100)
        );
    }

    @Test
    void testSendWelcome_KeyboardType() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId), anyString(),
                argThat(keyboard -> keyboard != null)
        );
    }

    @Test
    void testSendWelcome_WithMainKeyboard() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId), anyString(), any());
    }

    @Test
    void testSendHelp_NoKeyboard() {
        Long chatId = 12345L;
        commandHandler.sendHelp(chatId, bot);
        verify(bot).sendText(eq(chatId), anyString());
        verify(bot, never()).sendMenu(anyLong(), anyString(), any());
        verify(bot, never()).sendInlineMenu(anyLong(), anyString(), any());
    }

    @Test
    void testSendWelcome_SpecialCharacters() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId),
                argThat(message ->
                        !message.contains("null") &&
                                !message.isEmpty()
                ),
                any()
        );
    }

    @Test
    void testSendHelp_SpecialCharacters() {
        Long chatId = 12345L;
        commandHandler.sendHelp(chatId, bot);
        verify(bot).sendText(eq(chatId),
                argThat(message ->
                        !message.contains("null") &&
                                !message.isEmpty()
                )
        );
    }

    @Test
    void testSendWelcome_InstructionSteps() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId),
                argThat(message ->
                        message.contains("1. Загрузите данные") &&
                                message.contains("2. Выполните SQL запросы") &&
                                message.contains("3. Создайте диаграмму")
                ),
                any()
        );
    }

    @Test
    void testSendHelp_ContainsSQLQueriesDescription() {
        Long chatId = 12345L;
        commandHandler.sendHelp(chatId, bot);
        verify(bot).sendText(eq(chatId),
                argThat(message ->
                        message.contains("SQL запросы задания:") &&
                                message.contains("1. Средние расходы") &&
                                message.contains("2. Школы с лучшей математикой")
                )
        );
    }

    @Test
    void testSendHelp_ContainsChartsDescription() {
        Long chatId = 12345L;
        commandHandler.sendHelp(chatId, bot);
        verify(bot).sendText(eq(chatId),
                argThat(message ->
                        message.contains("Диаграммы:") &&
                                message.contains("• Среднее количество студентов")
                )
        );
    }

    @Test
    void testSendWelcome_ContainsBotName() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId),
                argThat(message -> message.contains("JavaSchoolRTFBot")),
                any()
        );
    }

    @Test
    void testSendWelcome_TextCommandsMentioned() {
        Long chatId = 12345L;
        commandHandler.sendWelcome(chatId, bot);
        verify(bot).sendMenu(eq(chatId),
                argThat(message ->
                        message.contains("Используйте кнопки ниже") ||
                                message.contains("Запросы") ||
                                message.contains("Диаграммы") ||
                                message.contains("Загрузить данные") ||
                                message.contains("Статистика")
                ),
                any()
        );
    }
}