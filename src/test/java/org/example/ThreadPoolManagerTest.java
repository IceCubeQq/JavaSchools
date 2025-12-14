package org.example;

import org.junit.jupiter.api.*;
import org.school.analysis.infrastructure.telegram.util.ThreadPoolManager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ThreadPoolManagerTest {

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = ThreadPoolManager.getExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void testGetExecutor_ShouldReturnNonNullInstance() {
        assertNotNull(executor, "Executor не должен быть null");
        assertInstanceOf(ThreadPoolExecutor.class, executor,
                "Должен возвращаться ThreadPoolExecutor");
    }

    @Test
    void testGetExecutor_ShouldReturnSingletonInstance() {
        ExecutorService executor1 = ThreadPoolManager.getExecutor();
        ExecutorService executor2 = ThreadPoolManager.getExecutor();

        assertSame(executor1, executor2,
                "Метод должен возвращать один и тот же экземпляр (singleton)");
    }

    @Test
    void testExecutorConfiguration() {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;

        assertEquals(5, threadPool.getCorePoolSize(),
                "Core pool size должен быть 5");
        assertEquals(20, threadPool.getMaximumPoolSize(),
                "Maximum pool size должен быть 20");
        assertEquals(60L, threadPool.getKeepAliveTime(TimeUnit.SECONDS),
                "Keep alive time должен быть 60 секунд");
        assertTrue(threadPool.allowsCoreThreadTimeOut(),
                "Core thread timeout должен быть включен");
        BlockingQueue<Runnable> queue = threadPool.getQueue();
        assertInstanceOf(LinkedBlockingQueue.class, queue,
                "Очередь должна быть LinkedBlockingQueue");
    }

    @Test
    @Timeout(5)
    void testExecutor_RejectionPolicy_IsCallerRuns() throws InterruptedException {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;
        threadPool.shutdownNow();
        threadPool = new ThreadPoolExecutor(
                2, 2, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        final int totalTasks = 10;
        final CountDownLatch latch = new CountDownLatch(totalTasks);
        final AtomicInteger tasksExecutedInMainThread = new AtomicInteger(0);
        final Thread mainThread = Thread.currentThread();

        for (int i = 0; i < totalTasks; i++) {
            threadPool.submit(() -> {
                if (Thread.currentThread() == mainThread) {
                    tasksExecutedInMainThread.incrementAndGet();
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean allCompleted = latch.await(3, TimeUnit.SECONDS);

        assertTrue(allCompleted, "Все задачи должны быть выполнены");
        assertTrue(tasksExecutedInMainThread.get() > 0,
                "Некоторые задачи должны быть выполнены в вызывающем потоке (CallerRunsPolicy)");

        threadPool.shutdownNow();
    }

    @Nested
    class ConcurrentExecutionTests {

    @Test
    @Timeout(3)
    void testTaskCancellation() {
        Future<?> future = executor.submit(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "interrupted";
            }
            return "completed";
        });
        boolean cancelled = future.cancel(true);

        assertTrue(cancelled, "Задача должна быть отменена");
        assertTrue(future.isCancelled(), "Задача должна быть помечена как отмененная");
        assertThrows(CancellationException.class, future::get,
                "При получении результата отмененной задачи должно быть CancellationException");
    }
}}