package org.school.analysis.infrastructure.telegram.keyboards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class QueryKeyboard {

    public static InlineKeyboardMarkup getQueryMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton query1 = new InlineKeyboardButton();
        query1.setText("Средние расходы");
        query1.setCallbackData("query_expenditure");
        row1.add(query1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton query2 = new InlineKeyboardButton();
        query2.setText("Лучшие школы по математике");
        query2.setCallbackData("query_math_schools");
        row2.add(query2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton query3 = new InlineKeyboardButton();
        query3.setText("Статистика студентов");
        query3.setCallbackData("query_student_stats");
        row3.add(query3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton query4 = new InlineKeyboardButton();
        query4.setText("Все запросы");
        query4.setCallbackData("query_all");
        row4.add(query4);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        markup.setKeyboard(rows);
        return markup;
    }
}