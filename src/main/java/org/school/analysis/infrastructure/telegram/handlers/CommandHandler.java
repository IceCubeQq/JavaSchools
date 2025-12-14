package org.school.analysis.infrastructure.telegram.handlers;

import org.school.analysis.infrastructure.telegram.bot.SchoolTelegramBot;

public interface CommandHandler {
    void sendWelcome(Long chatId, SchoolTelegramBot bot);
    void sendHelp(Long chatId, SchoolTelegramBot bot);
}
