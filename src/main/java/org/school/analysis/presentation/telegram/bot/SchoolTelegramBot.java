package org.school.analysis.presentation.telegram.bot;

import org.school.analysis.presentation.telegram.ports.ChartHandler;
import org.school.analysis.presentation.telegram.ports.CommandHandler;
import org.school.analysis.presentation.telegram.ports.DataHandler;
import org.school.analysis.presentation.telegram.ports.QueryHandler;
import org.school.analysis.presentation.telegram.util.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class SchoolTelegramBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(SchoolTelegramBot.class);
    private final String botUsername;
    private final String botToken;
    private final ExecutorService executorService;
    private final MessageSender messageSender;
    private final CommandHandler commandHandler;
    private final QueryHandler queryHandler;
    private final ChartHandler chartHandler;
    private final DataHandler dataHandler;

    public SchoolTelegramBot(String botToken, String botUsername, CommandHandler commandHandler, QueryHandler queryHandler,
                             ChartHandler chartHandler, DataHandler dataHandler) {
        super(botToken);
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.executorService = createExecutorService();
        this.messageSender = new MessageSender(this);
        this.commandHandler = commandHandler;
        this.queryHandler = queryHandler;
        this.chartHandler = chartHandler;
        this.dataHandler = dataHandler;
        logger.info("SchoolTelegramBot инициализирован: @{}", botUsername);
    }

    private ExecutorService createExecutorService() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        return Executors.newFixedThreadPool(corePoolSize, r -> {
            Thread thread = new Thread(r);
            thread.setName("telegram-bot-worker-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> {
            try {
                processUpdate(update);
            } catch (Exception e) {
                logger.error("Ошибка обработки обновления", e);
            }
        });
    }

    private void processUpdate(Update update) {
        Long chatId = getChatId(update);
        if (chatId == null) {
            return;
        }
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                processMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                processCallbackQuery(update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasDocument()) {
                processDocument(update.getMessage());
            }
        } catch (Exception e) {
            sendText(chatId, "Произошла ошибка при обработке запроса");
        }
    }

    private void processMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        if (text.startsWith("/")) {
            handleCommand(chatId, text);
        } else {
            handleTextMessage(chatId, text);
        }
    }

    private void processCallbackQuery(org.telegram.telegrambots.meta.api.objects.CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String callbackData = callbackQuery.getData();
        try {
            answerCallbackQuery(callbackQuery.getId());
            if (callbackData.startsWith("query_")) {
                queryHandler.handleCallback(chatId, callbackData, this);
            } else if (callbackData.startsWith("chart_")) {
                chartHandler.handleCallback(chatId, callbackData, this);
            } else if (callbackData.startsWith("data_")) {
                dataHandler.handleCallback(chatId, callbackData, this);
            }

        } catch (Exception e) {
            sendText(chatId, "Ошибка обработки запроса");
        }
    }

    private void processDocument(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long chatId = message.getChatId();
        sendText(chatId, "Загрузка файлов через Telegram недоступна");
    }

    private void handleCommand(Long chatId, String command) {
        String normalizedCommand = command.split("@")[0].toLowerCase();
        switch (normalizedCommand) {
            case "/start":
                commandHandler.sendWelcome(chatId, this);
                break;
            case "/help":
                commandHandler.sendHelp(chatId, this);
                break;
            case "/load":
                dataHandler.handleLoadData(chatId, this);
                break;
            case "/queries":
                queryHandler.showMenu(chatId, this);
                break;
            case "/charts":
                chartHandler.showMenu(chatId, this);
                break;
            case "/stats":
                dataHandler.showStatistics(chatId, this);
                break;
            case "/status":
                sendBotStatus(chatId);
                break;
            default:
                sendText(chatId, "Неизвестная команда. Используйте /help для списка команд");
        }
    }

    private void handleTextMessage(Long chatId, String text) {
        switch (text) {
            case "Запросы":
                queryHandler.showMenu(chatId, this);
                break;
            case "Диаграммы":
                chartHandler.showMenu(chatId, this);
                break;
            case "Загрузить данные":
                dataHandler.handleLoadData(chatId, this);
                break;
            case "Статистика":
                dataHandler.showStatistics(chatId, this);
                break;
            case "ℹПомощь":
                commandHandler.sendHelp(chatId, this);
                break;
            case "Статус":
                sendBotStatus(chatId);
                break;
            default:
                sendText(chatId, "Используйте кнопки меню или команды из /help");
        }
    }

    private void sendBotStatus(Long chatId) {
        String status = String.format("""
            Статус бота
            
            Потоков в пуле: %d/%d
            Завершено задач: %d
            """,
                ((java.util.concurrent.ThreadPoolExecutor) executorService).getActiveCount(),
                ((java.util.concurrent.ThreadPoolExecutor) executorService).getPoolSize(),
                ((java.util.concurrent.ThreadPoolExecutor) executorService).getCompletedTaskCount()
        );
        sendText(chatId, status);
    }

    private Long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }

    private void answerCallbackQuery(String callbackQueryId) {
        try {
            execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId).build());
        } catch (TelegramApiException e) {
            logger.warn("Не удалось отправить подтверждение callback: {}", e.getMessage());
        }
    }

    public void sendText(Long chatId, String text) {
        messageSender.sendText(chatId, text);
    }

    public void sendMenu(Long chatId, String text,
                         org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard) {
        messageSender.sendMenu(chatId, text, keyboard);
    }

    public void sendInlineMenu(Long chatId, String text,
                               org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        messageSender.sendInlineMenu(chatId, text, keyboard);
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    public String getBotToken() {
        return botToken;
    }

    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            logger.info("Бот завершил работу");
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}