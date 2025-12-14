package org.school.analysis.infrastructure.telegram.handlers;

import org.school.analysis.infrastructure.telegram.bot.SchoolTelegramBot;

public interface QueryHandler {
    void showMenu(Long chatId, SchoolTelegramBot bot);
    void handleCallback(Long chatId, String callbackData, SchoolTelegramBot bot);
}