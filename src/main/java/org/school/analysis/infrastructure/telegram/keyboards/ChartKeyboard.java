package org.school.analysis.infrastructure.telegram.keyboards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class ChartKeyboard {

    public static InlineKeyboardMarkup getChartMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton chartButton = new InlineKeyboardButton();
        chartButton.setText("Средние студенты по странам");
        chartButton.setCallbackData("chart_students");
        row1.add(chartButton);

        rows.add(row1);
        markup.setKeyboard(rows);
        return markup;
    }
}