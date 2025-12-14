package org.example;

import org.junit.jupiter.api.Test;
import org.school.analysis.infrastructure.telegram.keyboards.ChartKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChartKeyboardTest {

    @Test
    void testGetChartMenu_ReturnsNonNullMarkup() {
        InlineKeyboardMarkup markup = ChartKeyboard.getChartMenu();
        assertNotNull(markup, "Метод должен возвращать не-null объект InlineKeyboardMarkup");
    }

    @Test
    void testGetChartMenu_HasKeyboard() {
        InlineKeyboardMarkup markup = ChartKeyboard.getChartMenu();
        assertNotNull(markup.getKeyboard(), "Markup должен иметь keyboard");
        assertFalse(markup.getKeyboard().isEmpty(), "Keyboard не должен быть пустым");
    }

    @Test
    void testGetChartMenu_HasOneRow() {
        InlineKeyboardMarkup markup = ChartKeyboard.getChartMenu();
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        assertEquals(1, keyboard.size(), "Должна быть ровно одна строка кнопок");
    }

    @Test
    void testGetChartMenu_RowHasOneButton() {
        InlineKeyboardMarkup markup = ChartKeyboard.getChartMenu();
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        List<InlineKeyboardButton> firstRow = keyboard.get(0);
        assertEquals(1, firstRow.size(), "Первая строка должна содержать ровно одну кнопку");
    }

    @Test
    void testGetChartMenu_ButtonHasCorrectText() {
        InlineKeyboardMarkup markup = ChartKeyboard.getChartMenu();
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        InlineKeyboardButton button = keyboard.get(0).get(0);
        assertEquals("Средние студенты по странам", button.getText(),
                "Текст кнопки должен соответствовать ожидаемому");
    }

    @Test
    void testGetChartMenu_ButtonHasCorrectCallbackData() {
        InlineKeyboardMarkup markup = ChartKeyboard.getChartMenu();
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        InlineKeyboardButton button = keyboard.get(0).get(0);
        assertEquals("chart_students", button.getCallbackData(),
                "Callback data кнопки должна соответствовать ожидаемой");
    }

    @Test
    void testGetChartMenu_ButtonIsNotNull() {
        InlineKeyboardMarkup markup = ChartKeyboard.getChartMenu();
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        InlineKeyboardButton button = keyboard.get(0).get(0);
        assertNotNull(button, "Кнопка не должна быть null");
        assertNotNull(button.getText(), "Текст кнопки не должен быть null");
        assertNotNull(button.getCallbackData(), "Callback data кнопки не должна быть null");
    }

    @Test
    void testGetChartMenu_MultipleCallsReturnNewInstances() {
        InlineKeyboardMarkup markup1 = ChartKeyboard.getChartMenu();
        InlineKeyboardMarkup markup2 = ChartKeyboard.getChartMenu();
        assertNotSame(markup1, markup2, "Каждый вызов должен возвращать новый экземпляр");
    }

    @Test
    void testGetChartMenu_EmptyKeyboardIfNoButtonsAdded() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        assertTrue(markup.getKeyboard() == null || markup.getKeyboard().isEmpty());
    }
}