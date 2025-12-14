package org.school.analysis.infrastructure.telegram.handlers;

import org.school.analysis.infrastructure.telegram.bot.SchoolTelegramBot;

public interface DataHandler {
    void handleLoadData(Long chatId, SchoolTelegramBot bot);
    void showStatistics(Long chatId, SchoolTelegramBot bot);
    void handleCallback(Long chatId, String callbackData, SchoolTelegramBot bot);
}