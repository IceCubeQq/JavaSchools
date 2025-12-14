package org.school.analysis.presentation.telegram.ports;

import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;

public interface CommandHandler {
    void sendWelcome(Long chatId, SchoolTelegramBot bot);
    void sendHelp(Long chatId, SchoolTelegramBot bot);
}
