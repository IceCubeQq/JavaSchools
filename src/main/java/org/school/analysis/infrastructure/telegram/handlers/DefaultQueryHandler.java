package org.school.analysis.infrastructure.telegram.handlers;

import org.school.analysis.application.services.SchoolStatisticsService;
import org.school.analysis.domain.dto.CountryStudentStats;
import org.school.analysis.infrastructure.telegram.bot.SchoolTelegramBot;
import org.school.analysis.infrastructure.telegram.keyboards.QueryKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultQueryHandler implements  QueryHandler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultQueryHandler.class);

    private final SchoolStatisticsService statisticsService; // Используем сервис напрямую
    private final ExecutorService executorService;
    public DefaultQueryHandler(SchoolStatisticsService statisticsService, ExecutorService executorService) {
        this.statisticsService = statisticsService;
        this.executorService = executorService;
    }

    public void showMenu(Long chatId, SchoolTelegramBot bot) {
        String menu = """
            SQL Запросы к БД
            
            Выберите запрос для выполнения:
            
            1. Средние расходы
            • Округа: Fresno, Contra Costa, El Dorado, Glenn
            • Только расходы > 10
            
            2. Лучшие школы по математике
            • Диапазон 1: 5000-7500 студентов
            • Диапазон 2: 10000-11000 студентов
            
            3. Статистика студентов по странам
            • Среднее количество студентов
            • Топ стран по количеству школ
            
            4. Все запросы сразу
            • Параллельное выполнение
            
            Нажмите на кнопку ниже для выбора запроса
            """;

        bot.sendInlineMenu(chatId, menu, QueryKeyboard.getQueryMenu());
    }

    public void handleCallback(Long chatId, String callbackData, SchoolTelegramBot bot) {
        switch (callbackData) {
            case "query_expenditure":
                executeExpenditureQueryAsync(chatId, bot);
                break;
            case "query_math_schools":
                executeMathSchoolsQueryAsync(chatId, bot);
                break;
            case "query_student_stats":
                executeStudentStatsQueryAsync(chatId, bot);
                break;
            case "query_all":
                executeAllQueriesAsync(chatId, bot);
                break;
            default:
                bot.sendText(chatId, "Неизвестный запрос");
        }
    }

    public void executeExpenditureQueryAsync(Long chatId, SchoolTelegramBot bot) {
        bot.sendText(chatId, "Запрос 1: Средние расходы\n\nВыполняю запрос. Это может занять несколько секунд");

        CompletableFuture.runAsync(() -> {
            try {
                String result = statisticsService.getExpenditureReportForTelegram();
                bot.sendText(chatId, result);

            } catch (Exception e) {
                String errorMessage = String.format("""
                    Ошибка выполнения запроса
                    
                    %s
                    
                    Попробуйте загрузить данные командой /load
                    """, e.getMessage());
                bot.sendText(chatId, errorMessage);
            }
        }, executorService).orTimeout(30, TimeUnit.SECONDS).exceptionally(throwable -> {
            bot.sendText(chatId, "Таймаут запроса\n\nЗапрос выполняется слишком долго");
            return null;
        });
    }

    public void executeMathSchoolsQueryAsync(Long chatId, SchoolTelegramBot bot) {
        bot.sendText(chatId, "Запрос 2: Лучшие школы по математике\n\nВыполняю запрос");

        CompletableFuture.runAsync(() -> {
            try {
                String result = statisticsService.getMathSchoolsReportForTelegram();
                bot.sendText(chatId, result);

            } catch (Exception e) {
                String errorMessage = String.format("""
                    Ошибка выполнения запроса
                    
                    %s
                    
                    Проверьте, есть ли школы с таким количеством студентов в базе
                    """, e.getMessage());
                bot.sendText(chatId, errorMessage);
            }
        }, executorService).orTimeout(30, TimeUnit.SECONDS).exceptionally(throwable -> {
            bot.sendText(chatId, "Таймаут запроса\n\nПопробуйте позже");
            return null;
        });
    }

    public void executeStudentStatsQueryAsync(Long chatId, SchoolTelegramBot bot) {
        bot.sendText(chatId, "Запрос 3: Статистика студентов по странам\n\nПолучаю данные");

        CompletableFuture.runAsync(() -> {
            try {
                List<CountryStudentStats> stats = statisticsService.getStudentStatistics(10);
                if (stats == null || stats.isEmpty()) {
                    bot.sendText(chatId, "Нет данных о студентах\n\nБаза данных пуста. Загрузите данные командой /load");
                    return;
                }

                StringBuilder result = new StringBuilder();
                result.append("Статистика студентов по странам (топ-10)\n\n");

                for (int i = 0; i < stats.size(); i++) {
                    CountryStudentStats stat = stats.get(i);
                    result.append(String.format("%d. %s\n", i + 1, stat.getCountryName()));
                    result.append(String.format("   Школ: %d\n", stat.getSchoolCount()));
                    result.append(String.format("   Среднее студентов: <b>%.1f</b>\n", stat.getAvgStudents()));
                    result.append(String.format("   Всего студентов: %d\n", stat.getTotalStudents()));
                    result.append(String.format("   Диапазон: %d - %d студентов\n\n",
                            stat.getMinStudents(), stat.getMaxStudents()));
                }

                result.append("Данные основаны на загруженных школах из CSV файла<");
                bot.sendText(chatId, result.toString());

            } catch (Exception e) {
                String errorMessage = String.format("""
                    Ошибка получения статистики
                    
                    %s
                    
                    Попробуйте загрузить данные командой /load
                    """, e.getMessage());
                bot.sendText(chatId, errorMessage);
            }
        }, executorService).orTimeout(30, TimeUnit.SECONDS).exceptionally(throwable -> {
            bot.sendText(chatId, "Таймаут запроса.\n\nПопробуйте позже");
            return null;
        });
    }

    public void executeAllQueriesAsync(Long chatId, SchoolTelegramBot bot) {
        bot.sendText(chatId, "Все запросы\n\nЗапускаю параллельное выполнение всех запросов");

        CompletableFuture<Void> expenditureQuery = CompletableFuture.runAsync(() -> {
            try {
                bot.sendText(chatId, "Запрос 1: Средние расходы\n" +
                        statisticsService.getExpenditureReportForTelegram());
            } catch (Exception e) {
                bot.sendText(chatId, "Ошибка в запросе 1: " + e.getMessage());
            }
        }, executorService);

        CompletableFuture<Void> mathSchoolsQuery = CompletableFuture.runAsync(() -> {
            try {
                bot.sendText(chatId, "Запрос 2: Лучшие школы по математике\n" +
                        statisticsService.getMathSchoolsReportForTelegram());
            } catch (Exception e) {
                bot.sendText(chatId, "Ошибка в запросе 2: " + e.getMessage());
            }
        }, executorService);

        CompletableFuture<Void> studentStatsQuery = CompletableFuture.runAsync(() -> {
            try {
                List<CountryStudentStats> stats = statisticsService.getStudentStatistics(5);
                if (stats != null && !stats.isEmpty()) {
                    StringBuilder result = new StringBuilder();
                    result.append("Запрос 3: Статистика студентов (топ-5)\n\n");

                    for (int i = 0; i < Math.min(3, stats.size()); i++) {
                        CountryStudentStats stat = stats.get(i);
                        result.append(String.format("%s: %.1f студентов в среднем\n",
                                stat.getCountryName(), stat.getAvgStudents()));
                    }

                    if (stats.size() > 3) {
                        result.append(String.format("\n.. и еще %d стран", stats.size() - 3));
                    }
                    bot.sendText(chatId, result.toString());
                }
            } catch (Exception e) {
                bot.sendText(chatId, "Ошибка в запросе 3: " + e.getMessage());
            }
        }, executorService);

        CompletableFuture<Void> allQueries = CompletableFuture.allOf(
                expenditureQuery, mathSchoolsQuery, studentStatsQuery
        );

        allQueries.thenRun(() -> {
            bot.sendText(chatId, "Все запросы выполнены!\n\n" +
                    "Для детального просмотра выберите отдельный запрос из меню");
        }).exceptionally(throwable -> {
            bot.sendText(chatId, "Некоторые запросы завершились с ошибкой\n\n" +
                    "Попробуйте выполнить запросы по отдельности.");
            return null;
        });
    }
}