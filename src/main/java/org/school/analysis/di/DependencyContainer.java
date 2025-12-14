package org.school.analysis.di;

import org.school.analysis.application.exception.DatabaseExceptionHandler;
import org.school.analysis.application.ports.input.LoadSchoolsUseCase;
import org.school.analysis.application.ports.output.ChartGenerator;
import org.school.analysis.application.ports.output.DatabaseStatisticsPort;
import org.school.analysis.application.ports.output.SchoolRepository;
import org.school.analysis.application.services.LoadSchoolsService;
import org.school.analysis.application.services.SchoolStatisticsService;
import org.school.analysis.presentation.telegram.ports.ChartHandler;
import org.school.analysis.presentation.telegram.ports.CommandHandler;
import org.school.analysis.presentation.telegram.ports.DataHandler;
import org.school.analysis.presentation.telegram.ports.QueryHandler;
import org.school.analysis.infrastructure.adapters.JFreeChartGenerator;
import org.school.analysis.infrastructure.csv.CsvSchoolParser;
import org.school.analysis.application.ports.output.DatabaseManager;
import org.school.analysis.infrastructure.database.DatabaseManagerImpl;
import org.school.analysis.infrastructure.database.SchoolStatisticsRepository;
import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;
import org.school.analysis.presentation.telegram.handlers.DefaultChartHandler;
import org.school.analysis.presentation.telegram.handlers.DefaultCommandHandler;
import org.school.analysis.presentation.telegram.handlers.DefaultDataHandler;
import org.school.analysis.presentation.telegram.handlers.DefaultQueryHandler;
import org.school.analysis.presentation.telegram.util.ThreadPoolManager;
import org.school.analysis.infrastructure.visualization.ChartService;
import org.school.analysis.presentation.TelegramOutputService;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;


public class DependencyContainer {
    private static DependencyContainer instance;
    private DatabaseManager databaseManager;
    private SchoolRepository repository;
    private DatabaseStatisticsPort databaseStatisticsPort;
    private TelegramOutputService telegramOutputService;
    private DatabaseExceptionHandler exceptionHandler;
    private SchoolStatisticsService statisticsService;
    private ChartGenerator chartGenerator;
    private LoadSchoolsUseCase loadSchoolsUseCase;
    private ExecutorService executorService;
    private CsvSchoolParser csvSchoolParser;
    private boolean diInitialized = false;
    private boolean databaseInitialized = false;
    private boolean tablesCreated = false;
    private boolean servicesInitialized = false;

    private DependencyContainer() {}

    public static synchronized DependencyContainer getInstance() {
        if (instance == null) {
            instance = new DependencyContainer();
        }
        return instance;
    }

    public synchronized void initializeDi() {
        if (diInitialized) {
            return;
        }
        System.out.println("Создание DI контейнера");

        try {
            this.databaseManager = createDatabaseManager();
            this.telegramOutputService = createTelegramOutputService();
            this.exceptionHandler = createDatabaseExceptionHandler();
            this.executorService = createExecutorService();
            this.csvSchoolParser = createCsvSchoolParser(); // Создаем парсер
            this.diInitialized = true;

            System.out.println("DI создан");

        } catch (Exception e) {
            throw new RuntimeException("Не удалось инициализировать DI контейнер", e);
        }
    }

    public synchronized void connectToDatabase() {
        if (!diInitialized) {
            throw new IllegalStateException("DI контейнер должен быть создан перед подключением к БД");
        }
        if (databaseInitialized) {
            return;
        }
        try {
            databaseManager.connect();
            Connection connection = databaseManager.getConnection();
            if (connection == null || connection.isClosed()) {
                throw new IllegalStateException("Не удалось подключиться с БД");
            }
            databaseInitialized = true;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось подключиться к базе данных", e);
        }
    }

    public synchronized void createDatabaseTables() {
        if (!databaseInitialized) {
            throw new IllegalStateException("Необходимо подключиться к БД перед созданием таблиц");
        }
        if (tablesCreated) {
            return;
        }
        try {
            databaseManager.createTables();
            tablesCreated = true;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать таблицы базы данных", e);
        }
    }

    public synchronized void initializeRepositories() {
        if (!tablesCreated) {
            throw new IllegalStateException("Таблицы БД должны быть созданы перед созданием репозиториев");
        }
        try {
            Connection connection = databaseManager.getConnection();
            if (connection == null || connection.isClosed()) {
                throw new IllegalStateException("Соединение с БД не активно");
            }
            SchoolStatisticsRepository repoImpl = createSchoolRepository(connection);
            this.repository = repoImpl;
            this.databaseStatisticsPort = repoImpl;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать репозитории", e);
        }
    }

    public synchronized void initializeServices() {
        if (repository == null) {
            throw new IllegalStateException("Репозитории должны быть созданы перед сервисами");
        }
        if (servicesInitialized) {
            return;
        }
        try {
            this.statisticsService = createSchoolStatisticsService();
            this.chartGenerator = createChartGenerator();
            this.loadSchoolsUseCase = createLoadSchoolsUseCase();
            servicesInitialized = true;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать сервисы", e);
        }
    }

    public synchronized void initializeApplication() {
        try {
            initializeDi();
            connectToDatabase();
            createDatabaseTables();
            initializeRepositories();
            initializeServices();
        } catch (Exception e) {
            shutdown();
            throw e;
        }
    }

    public boolean isApplicationInitialized() {
        return diInitialized && databaseInitialized && tablesCreated && servicesInitialized;
    }

    private DatabaseManager createDatabaseManager() {
        return new DatabaseManagerImpl();
    }

    private CsvSchoolParser createCsvSchoolParser() {
        return new CsvSchoolParser();
    }

    private SchoolStatisticsRepository createSchoolRepository(Connection connection) {
        return new SchoolStatisticsRepository(connection);
    }

    private TelegramOutputService createTelegramOutputService() {
        return new TelegramOutputService();
    }

    private DatabaseExceptionHandler createDatabaseExceptionHandler() {
        return new DatabaseExceptionHandler();
    }

    private SchoolStatisticsService createSchoolStatisticsService() {
        if (repository == null) {
            throw new IllegalStateException("Репозиторий должен быть создан перед созданием SchoolStatisticsService");
        }
        return new SchoolStatisticsService(repository, telegramOutputService, exceptionHandler);
    }

    private ChartGenerator createChartGenerator() {
        if (repository == null) {
            throw new IllegalStateException("Репозиторий должен быть создан перед созданием ChartGenerator");
        }
        ChartService chartService = new ChartService(repository);
        return new JFreeChartGenerator(chartService);
    }

    private LoadSchoolsUseCase createLoadSchoolsUseCase() {
        if (databaseManager == null) {
            throw new IllegalStateException("DatabaseManager должен быть создан перед созданием LoadSchoolsUseCase");
        }
        return new LoadSchoolsService(csvSchoolParser, databaseManager);
    }

    private ExecutorService createExecutorService() {
        return ThreadPoolManager.getExecutor();
    }


    public SchoolTelegramBot createTelegramBot(String botToken, String botUsername) {
        if (!isApplicationInitialized()) {
            throw new IllegalStateException("Приложение должно быть полностью инициализировано перед созданием бота");
        }
        CommandHandler commandHandler = new DefaultCommandHandler();
        QueryHandler queryHandler = new DefaultQueryHandler(statisticsService, executorService);
        ChartHandler chartHandler = new DefaultChartHandler(chartGenerator, executorService);
        DataHandler dataHandler = new DefaultDataHandler(databaseStatisticsPort, executorService,
                loadSchoolsUseCase
        );

        return new SchoolTelegramBot(botToken, botUsername, commandHandler, queryHandler, chartHandler, dataHandler
        );
    }

    public synchronized void shutdown() {
        System.out.println("Завершение работы DI контейнера");

        try {
            ThreadPoolManager.shutdown();
            if (databaseManager != null && databaseManager instanceof DatabaseManagerImpl) {
                ((DatabaseManagerImpl) databaseManager).close();
            }
            diInitialized = false;
            databaseInitialized = false;
            tablesCreated = false;
            servicesInitialized = false;
            repository = null;
            databaseStatisticsPort = null;
            statisticsService = null;
            chartGenerator = null;
            loadSchoolsUseCase = null;
            csvSchoolParser = null;
        } catch (Exception e) {
            System.err.println("Ошибка при завершении DI контейнера" + e.getMessage());
        }
    }
}