package org.school.analysis.presentation.telegram.handlers;

import org.school.analysis.presentation.telegram.ports.CommandHandler;
import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;
import org.school.analysis.presentation.telegram.keyboards.MainKeyboard;

public class DefaultCommandHandler implements CommandHandler {

    @Override
    public void sendWelcome(Long chatId, SchoolTelegramBot bot) {
        String welcome = """
            Анализ данных школ - JavaSchoolRTFBot

            Я помогу вам анализировать данные школ из CSV файла.

            Основные функции:
            Запросы к БД - SQL запросы по заданию
            Диаграммы - Визуализация данных
            Загрузка данных - Импорт из CSV в БД
            Статистика - Общая статистика БД

            Для начала работы:
            1. Загрузите данные командой "Загрузить данные"
            2. Выполните SQL запросы из задания
            3. Создайте диаграмму

            Используйте кнопки ниже или команды:
            /load - Загрузить данные
            /queries - SQL запросы
            /charts - Диаграммы
            /stats - Статистика
            /help - Справка
            """;

        bot.sendMenu(chatId, welcome, MainKeyboard.getMainMenu());
    }

    @Override
    public void sendHelp(Long chatId, SchoolTelegramBot bot) {
        String help = """
            Справка по командам:

            Основные команды:
            /start - Главное меню
            /help - Эта справка
            /load - Загрузить данные из CSV
            /queries - SQL запросы задания
            /charts - Создать диаграмму
            /stats - Статистика БД
            /status - Статус бота

            Текстовые команды (кнопки):
            Запросы - SQL запросы
            Диаграммы - Графики
            Загрузить данные - Импорт CSV
            Статистика - Статистика БД
            Помощь - Эта справка
            Статус - Статус бота

            SQL запросы задания:
            1. Средние расходы в Fresno, Contra Costa, El Dorado, Glenn
            2. Школы с лучшей математикой по диапазонам студентов

            Диаграммы:
            • Среднее количество студентов по 10 странам
            """;

        bot.sendText(chatId, help);
    }
}
