package org.example;

import org.junit.jupiter.api.Test;
import org.school.analysis.infrastructure.telegram.keyboards.MainKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainKeyboardTest {

    @Test
    void getMainMenu_returnsCorrectMarkup() {
        ReplyKeyboardMarkup markup = MainKeyboard.getMainMenu();
        assertNotNull(markup);

        assertTrue(markup.getResizeKeyboard());
        assertFalse(markup.getOneTimeKeyboard());
        assertTrue(markup.getSelective());

        List<KeyboardRow> keyboard = markup.getKeyboard();
        assertEquals(3, keyboard.size());

        KeyboardRow row1 = keyboard.get(0);
        assertEquals(2, row1.size());
        assertEquals("Запросы", row1.get(0).getText());
        assertEquals("Диаграммы", row1.get(1).getText());

        KeyboardRow row2 = keyboard.get(1);
        assertEquals(2, row2.size());
        assertEquals("Загрузить данные", row2.get(0).getText());
        assertEquals("Статистика", row2.get(1).getText());

        KeyboardRow row3 = keyboard.get(2);
        assertEquals(2, row3.size());
        assertEquals("Помощь", row3.get(0).getText());
        assertEquals("Статус", row3.get(1).getText());
    }
}