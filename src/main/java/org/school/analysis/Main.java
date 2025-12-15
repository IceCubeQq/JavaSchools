package org.school.analysis;

import org.school.analysis.di.DependencyContainer;
import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        System.out.println("Запуск телеграм бота");
        DependencyContainer diContainer = null;
        try {
            Properties config = loadConfig();
            String botToken = getBotToken(config);
            String botUsername = getBotUsername(config);
            validateConfiguration(botToken);
            diContainer = DependencyContainer.getInstance();
            diContainer.initializeApplication();

            SchoolTelegramBot bot = diContainer.createTelegramBot(botToken, botUsername);
            registerTelegramBot(bot);
            printStartupInfo(botUsername);
            setupShutdownHook(diContainer, bot);

        } catch (TelegramApiException e) {
            handleTelegramApiError(e);
            shutdownContainer(diContainer);
            System.exit(1);
        } catch (Exception e) {
            handleCriticalError(e);
            shutdownContainer(diContainer);
            System.exit(1);
        }
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        String configPath = "config.properties";

        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
        } catch (Exception e) {
            System.err.println("Не удалось загрузить config.properties");
        }
        return props;
    }

    private static String getBotToken(Properties config) {
        String token = config.getProperty("telegram.bot.token");
        if (token == null || token.isEmpty()) {
            token = System.getenv("TELEGRAM_BOT_TOKEN");
        }
        return token;
    }

    private static String getBotUsername(Properties config) {
        return config.getProperty("telegram.bot.username", "SchoolAnalysisBot");
    }

    private static void validateConfiguration(String botToken) {
        if (botToken == null || botToken.isEmpty()) {
            System.err.println("Токен бота не найден");
            System.exit(1);
        }
    }

    private static void registerTelegramBot(SchoolTelegramBot bot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
    }

    private static void printStartupInfo(String botUsername) {
        System.out.println("Telegram бот успешно запущен!");
    }

    private static void setupShutdownHook(DependencyContainer diContainer, SchoolTelegramBot bot) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.print("Завершение работы бота");
                if (bot != null) {
                    bot.shutdown();
                }
            } catch (Exception e) {
                System.err.println("Ошибка завершения бота " + e.getMessage());
            }

            try {
                if (diContainer != null) {
                    diContainer.shutdown();
                }
            } catch (Exception e) {
                System.err.println("Ошибка остановки DI контейнера: " + e.getMessage());
            }
        }));
    }

    private static void shutdownContainer(DependencyContainer diContainer) {
        if (diContainer != null) {
            try {
                diContainer.shutdown();
            } catch (Exception e) {
                System.err.println("Ошибка при завершении DI контейнера: " + e.getMessage());
            }
        }
    }

    private static void handleTelegramApiError(TelegramApiException e) {
        System.err.println("\nОшибка Telegram api");
        e.printStackTrace();
    }

    private static void handleCriticalError(Exception e) {
        System.err.println("\nОшибка:");
        e.printStackTrace();
    }
}