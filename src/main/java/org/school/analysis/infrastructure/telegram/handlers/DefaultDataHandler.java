package org.school.analysis.infrastructure.telegram.handlers;

import org.school.analysis.application.ports.output.DatabaseStatisticsPort;
import org.school.analysis.application.ports.input.LoadSchoolsUseCase;
import org.school.analysis.infrastructure.telegram.bot.SchoolTelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultDataHandler implements DataHandler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultDataHandler.class);

    private final DatabaseStatisticsPort databaseStatisticsPort;
    private final ExecutorService executorService;
    private final LoadSchoolsUseCase loadSchoolsUseCase;

    public DefaultDataHandler(DatabaseStatisticsPort databaseStatisticsPort,
                              ExecutorService executorService, LoadSchoolsUseCase loadSchoolsUseCase) {
        this.databaseStatisticsPort = databaseStatisticsPort;
        this.executorService = executorService;
        this.loadSchoolsUseCase = loadSchoolsUseCase;
    }

    @Override
    public void handleLoadData(Long chatId, SchoolTelegramBot bot) {
        bot.sendText(chatId, "Начинаю загрузку данных из CSV");

        CompletableFuture.runAsync(() -> {
            try {
                String filePath = "data/schools.csv";
                FileInputStream fileInputStream = new FileInputStream(filePath);
                int loadedCount = loadSchoolsUseCase.execute(fileInputStream);
                fileInputStream.close();
                if (loadedCount > 0) {
                    bot.sendText(chatId, String.format("Успешно загружено %d школ", loadedCount));
                    bot.sendText(chatId, "Теперь вы можете выполнять запросы и создавать диаграммы!");
                } else {
                    bot.sendText(chatId, "Загружено 0 школ. Проверьте CSV файл");
                }

            } catch (java.io.FileNotFoundException e) {
                String errorMsg = "Файл data/schools.csv не найден";
                bot.sendText(chatId, errorMsg);
                logger.error("CSV файл не найден", e);

            } catch (Exception e) {
                String errorMsg = "Ошибка загрузки " + e.getMessage();
                if (e.getMessage().contains("Не найдено школ в CSV")) {
                    errorMsg += "\n\nПроверьте формат CSV файла";
                }
                bot.sendText(chatId, errorMsg);
                logger.error("Ошибка загрузки данных", e);
            }
        }, executorService).orTimeout(60, TimeUnit.SECONDS).exceptionally(throwable -> {
            bot.sendText(chatId, "Таймаут загрузки данных.");
            return null;
        });
    }

    @Override
    public void showStatistics(Long chatId, SchoolTelegramBot bot) {
        bot.sendText(chatId, "Получаю статистику БД");
        CompletableFuture.runAsync(() -> {
            try {
                String stats = databaseStatisticsPort.getDatabaseStatistics();
                bot.sendText(chatId, stats);
            } catch (Exception e) {
                bot.sendText(chatId, "Ошибка получения статистики: " + e.getMessage());
                logger.error("Ошибка получения статистики базы данных", e);
            }
        }, executorService).orTimeout(30, TimeUnit.SECONDS).exceptionally(throwable -> {
            bot.sendText(chatId, "Таймаут получения статистики");
            return null;
        });
    }

    @Override
    public void handleCallback(Long chatId, String callbackData, SchoolTelegramBot bot) {
        switch (callbackData) {
            case "data_reload":
                handleLoadData(chatId, bot);
                break;
            case "data_stats":
                showStatistics(chatId, bot);
                break;
            default:
                bot.sendText(chatId, "Неизвестная команда данных");
        }
    }
}