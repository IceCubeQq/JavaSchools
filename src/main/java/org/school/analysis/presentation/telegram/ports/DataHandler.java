package org.school.analysis.presentation.telegram.ports;

import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;

public interface DataHandler {
    void handleLoadData(Long chatId, SchoolTelegramBot bot);
    void showStatistics(Long chatId, SchoolTelegramBot bot);
    void handleCallback(Long chatId, String callbackData, SchoolTelegramBot bot);
}