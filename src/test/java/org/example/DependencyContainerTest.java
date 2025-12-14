package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.school.analysis.application.ports.input.LoadSchoolsUseCase;
import org.school.analysis.application.ports.output.ChartGenerator;
import org.school.analysis.application.ports.output.DatabaseStatisticsPort;
import org.school.analysis.application.ports.output.SchoolRepository;
import org.school.analysis.application.services.SchoolStatisticsService;
import org.school.analysis.di.DependencyContainer;
import org.school.analysis.application.ports.output.DatabaseManager;
import org.school.analysis.infrastructure.database.DatabaseManagerImpl;
import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;
import org.school.analysis.presentation.telegram.util.ThreadPoolManager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DependencyContainerTest {

    private DependencyContainer container;
    private MockedStatic<ThreadPoolManager> threadPoolManagerMock;

    @BeforeEach
    void setUp() {
        resetContainerSingleton();
        container = DependencyContainer.getInstance();
        threadPoolManagerMock = Mockito.mockStatic(ThreadPoolManager.class);
    }

    @AfterEach
    void tearDown() {
        if (threadPoolManagerMock != null) {
            threadPoolManagerMock.close();
        }
        resetContainerSingleton();
    }

    private void resetContainerSingleton() {
        try {
            Field field = DependencyContainer.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
        }
    }
    private boolean getPrivateBooleanField(String fieldName) throws Exception {
        Field field = DependencyContainer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (boolean) field.get(container);
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = DependencyContainer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(container, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateObjectField(String fieldName) throws Exception {
        Field field = DependencyContainer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(container);
    }

    @Test
    void getInstance_ShouldReturnSameInstance_WhenCalledMultipleTimes() {
        DependencyContainer instance1 = DependencyContainer.getInstance();
        DependencyContainer instance2 = DependencyContainer.getInstance();
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2, "Должен возвращаться один и тот же экземпляр синглтона");
    }

    @Test
    void initializeDi_ShouldSetDiInitializedToTrue_WhenSuccessful() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));
        container.initializeDi();
        assertTrue(getPrivateBooleanField("diInitialized"), "Флаг diInitialized должен быть true");
        assertNotNull(getPrivateObjectField("telegramOutputService"), "TelegramOutputService должен быть создан");
        assertNotNull(getPrivateObjectField("exceptionHandler"), "DatabaseExceptionHandler должен быть создан");
        assertNotNull(getPrivateObjectField("csvSchoolParser"), "CsvSchoolParser должен быть создан");
    }

    @Test
    void initializeDi_ShouldDoNothing_WhenAlreadyInitialized() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));
        container.initializeDi();
        Object firstOutputService = getPrivateObjectField("telegramOutputService");
        container.initializeDi();
        Object secondOutputService = getPrivateObjectField("telegramOutputService");
        assertSame(firstOutputService, secondOutputService,
                "Повторный вызов не должен пересоздавать объекты");
    }

    @Test
    void initializeDi_ShouldThrowRuntimeException_WhenThreadPoolManagerFails() {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenThrow(new RuntimeException("Thread pool error"));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> container.initializeDi());

        assertTrue(exception.getMessage().contains("Не удалось инициализировать DI контейнер"));
        try {
            assertFalse(getPrivateBooleanField("diInitialized"), "Флаг diInitialized должен остаться false");
        } catch (Exception e) {
            fail("Не удалось получить доступ к приватному полю: " + e.getMessage());
        }
    }

    @Test
    void connectToDatabase_ShouldThrowIllegalStateException_WhenDiNotInitialized() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> container.connectToDatabase());

        assertEquals("DI контейнер должен быть создан перед подключением к БД", exception.getMessage());
    }

    @Test
    void connectToDatabase_ShouldInitializeDatabase_WhenSuccessful() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        DatabaseManager mockDbManager = mock(DatabaseManager.class);
        Connection mockConnection = mock(Connection.class);

        container.initializeDi();
        setPrivateField("databaseManager", mockDbManager);

        when(mockDbManager.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isClosed()).thenReturn(false);
        container.connectToDatabase();
        verify(mockDbManager).connect();
        verify(mockDbManager).getConnection();
        assertTrue(getPrivateBooleanField("databaseInitialized"), "Флаг databaseInitialized должен быть true");
    }

    @Test
    void connectToDatabase_ShouldThrowRuntimeException_WhenConnectionIsClosed() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        DatabaseManager mockDbManager = mock(DatabaseManager.class);
        Connection mockConnection = mock(Connection.class);

        container.initializeDi();
        setPrivateField("databaseManager", mockDbManager);

        when(mockDbManager.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isClosed()).thenReturn(true);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> container.connectToDatabase());

        assertTrue(exception.getMessage().contains("Не удалось подключиться к базе данных"));
        assertFalse(getPrivateBooleanField("databaseInitialized"), "Флаг databaseInitialized должен остаться false");
    }

    @Test
    void connectToDatabase_ShouldThrowRuntimeException_WhenConnectionIsNull() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        DatabaseManager mockDbManager = mock(DatabaseManager.class);

        container.initializeDi();
        setPrivateField("databaseManager", mockDbManager);

        when(mockDbManager.getConnection()).thenReturn(null);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> container.connectToDatabase());

        assertTrue(exception.getMessage().contains("Не удалось подключиться к базе данных"));
        assertFalse(getPrivateBooleanField("databaseInitialized"), "Флаг databaseInitialized должен остаться false");
    }

    @Test
    void createDatabaseTables_ShouldThrowIllegalStateException_WhenDatabaseNotInitialized() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        container.initializeDi();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> container.createDatabaseTables());

        assertEquals("Необходимо подключиться к БД перед созданием таблиц", exception.getMessage());
    }


    @Test
    void initializeRepositories_ShouldThrowIllegalStateException_WhenTablesNotCreated() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        DatabaseManager mockDbManager = mock(DatabaseManager.class);

        container.initializeDi();
        setPrivateField("databaseManager", mockDbManager);
        setPrivateField("databaseInitialized", true);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> container.initializeRepositories());

        assertEquals("Таблицы БД должны быть созданы перед созданием репозиториев", exception.getMessage());
    }

    @Test
    void initializeRepositories_ShouldCreateRepositories_WhenSuccessful() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        DatabaseManager mockDbManager = mock(DatabaseManager.class);
        Connection mockConnection = mock(Connection.class);

        container.initializeDi();
        setPrivateField("databaseManager", mockDbManager);
        setPrivateField("databaseInitialized", true);
        setPrivateField("tablesCreated", true);

        when(mockDbManager.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isClosed()).thenReturn(false);

        container.initializeRepositories();

        SchoolRepository repository = getPrivateObjectField("repository");
        DatabaseStatisticsPort databaseStatisticsPort = getPrivateObjectField("databaseStatisticsPort");

        assertNotNull(repository, "Repository должен быть создан");
        assertNotNull(databaseStatisticsPort, "DatabaseStatisticsPort должен быть создан");
        assertSame(repository, databaseStatisticsPort,
                "Repository и DatabaseStatisticsPort должны быть одним объектом");
    }


    @Test
    void initializeRepositories_ShouldThrowRuntimeException_WhenConnectionIsNull() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        DatabaseManager mockDbManager = mock(DatabaseManager.class);

        container.initializeDi();
        setPrivateField("databaseManager", mockDbManager);
        setPrivateField("databaseInitialized", true);
        setPrivateField("tablesCreated", true);

        when(mockDbManager.getConnection()).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> container.initializeRepositories());

        assertTrue(exception.getMessage().contains("Не удалось создать репозитории"));
        assertNull(getPrivateObjectField("repository"), "Repository не должен быть создан");
    }

    @Test
    void initializeServices_ShouldThrowIllegalStateException_WhenRepositoryNotInitialized() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        container.initializeDi();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> container.initializeServices());

        assertEquals("Репозитории должны быть созданы перед сервисами", exception.getMessage());
    }

    @Test
    void initializeServices_ShouldCreateServices_WhenSuccessful() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        container.initializeDi();
        SchoolRepository mockRepository = mock(SchoolRepository.class);
        setPrivateField("repository", mockRepository);

        container.initializeServices();

        assertNotNull(getPrivateObjectField("statisticsService"), "StatisticsService должен быть создан");
        assertNotNull(getPrivateObjectField("chartGenerator"), "ChartGenerator должен быть создан");
        assertNotNull(getPrivateObjectField("loadSchoolsUseCase"), "LoadSchoolsUseCase должен быть создан");
        assertTrue(getPrivateBooleanField("servicesInitialized"), "Флаг servicesInitialized должен быть true");
    }

    @Test
    void isApplicationInitialized_ShouldReturnFalse_WhenNotAllComponentsInitialized() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        container.initializeDi();

        assertFalse(container.isApplicationInitialized(),
                "Должно быть false, пока не все компоненты инициализированы");
    }

    @Test
    void createTelegramBot_ShouldThrowIllegalStateException_WhenApplicationNotInitialized() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> container.createTelegramBot("token", "username"));

        assertEquals("Приложение должно быть полностью инициализировано перед созданием бота",
                exception.getMessage());
    }

    @Test
    void createTelegramBot_ShouldCreateBot_WhenApplicationInitialized() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        container.initializeDi();
        setPrivateField("databaseInitialized", true);
        setPrivateField("tablesCreated", true);
        setPrivateField("servicesInitialized", true);
        setPrivateField("repository", mock(SchoolRepository.class));
        setPrivateField("databaseStatisticsPort", mock(DatabaseStatisticsPort.class));
        setPrivateField("statisticsService", mock(SchoolStatisticsService.class));
        setPrivateField("chartGenerator", mock(ChartGenerator.class));
        setPrivateField("loadSchoolsUseCase", mock(LoadSchoolsUseCase.class));

        SchoolTelegramBot bot = container.createTelegramBot("test-token", "test-bot");

        assertNotNull(bot, "Бот должен быть создан");
    }

    @Test
    void shutdown_ShouldResetAllFlagsAndClearReferences() throws Exception {
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));

        container.initializeDi();
        setPrivateField("databaseInitialized", true);
        setPrivateField("tablesCreated", true);
        setPrivateField("servicesInitialized", true);

        DatabaseManagerImpl mockDbManager = mock(DatabaseManagerImpl.class);
        setPrivateField("databaseManager", mockDbManager);

        container.shutdown();

        assertFalse(getPrivateBooleanField("diInitialized"), "diInitialized должен быть сброшен");
        assertFalse(getPrivateBooleanField("databaseInitialized"), "databaseInitialized должен быть сброшен");
        assertFalse(getPrivateBooleanField("tablesCreated"), "tablesCreated должен быть сброшен");
        assertFalse(getPrivateBooleanField("servicesInitialized"), "servicesInitialized должен быть сброшен");

        assertNull(getPrivateObjectField("repository"), "repository должен быть null");
        assertNull(getPrivateObjectField("statisticsService"), "statisticsService должен быть null");
        assertNull(getPrivateObjectField("chartGenerator"), "chartGenerator должен быть null");
        assertNull(getPrivateObjectField("loadSchoolsUseCase"), "loadSchoolsUseCase должен быть null");

        verify(mockDbManager).close();
        threadPoolManagerMock.verify(ThreadPoolManager::shutdown, times(1));
    }

    @Test
    void threadSafetyTest_InitializeDi_ShouldBeThreadSafe() throws Exception {
        final int threadCount = 10;
        final AtomicBoolean failed = new AtomicBoolean(false);
        threadPoolManagerMock.when(ThreadPoolManager::getExecutor)
                .thenReturn(mock(ExecutorService.class));
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    DependencyContainer localContainer = DependencyContainer.getInstance();
                    localContainer.initializeDi();
                } catch (Exception e) {
                    failed.set(true);
                }
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertFalse(failed.get(), "Не должно быть ошибок в многопоточном доступе");
        assertTrue(getPrivateBooleanField("diInitialized"), "DI должен быть инициализирован");
    }
}