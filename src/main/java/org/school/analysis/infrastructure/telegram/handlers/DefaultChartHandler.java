package org.school.analysis.infrastructure.telegram.handlers;

import org.school.analysis.application.ports.output.ChartGenerator;
import org.school.analysis.infrastructure.telegram.bot.SchoolTelegramBot;
import org.school.analysis.infrastructure.telegram.keyboards.ChartKeyboard;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultChartHandler implements ChartHandler {
    private final ChartGenerator chartGenerator;
    private final ExecutorService executorService;

    public DefaultChartHandler(ChartGenerator chartGenerator, ExecutorService executorService) {
        this.chartGenerator = chartGenerator;
        this.executorService = executorService;
    }

    @Override
    public void showMenu(Long chatId, SchoolTelegramBot bot) {
        String menu = """
            Диаграммы
            
            Доступные диаграммы:
            • Среднее количество студентов по странам
            
            Нажмите на кнопку ниже для создания диаграммы
            """;

        bot.sendInlineMenu(chatId, menu, ChartKeyboard.getChartMenu());
    }

    @Override
    public void handleCallback(Long chatId, String callbackData, SchoolTelegramBot bot) {
        switch (callbackData) {
            case "chart_students":
                createStudentsChartAsync(chatId, bot);
                break;
        }
    }

    private void createStudentsChartAsync(Long chatId, SchoolTelegramBot bot) {
        bot.sendText(chatId, "Создаю диаграмму студентов по странам");

        CompletableFuture.runAsync(() -> {
            try {
                byte[] chartBytes = chartGenerator.createAverageStudentsChart();
                String description = chartGenerator.getChartDescription();

                bot.getMessageSender().sendPhoto(chatId, chartBytes, description);
                bot.sendText(chatId, "Диаграмма успешно создана и отправлена");

            } catch (Exception e) {
                bot.sendText(chatId, "Ошибка создания диаграммы" + e.getMessage());
            }
        }, executorService).orTimeout(60, TimeUnit.SECONDS).exceptionally(throwable -> {
            bot.sendText(chatId, "Таймаут создания диаграммы");
            return null;
        });
    }
}