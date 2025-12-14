package org.school.analysis.presentation.telegram.ports;

import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;

public interface QueryHandler {
    void showMenu(Long chatId, SchoolTelegramBot bot);
    void handleCallback(Long chatId, String callbackData, SchoolTelegramBot bot);
}