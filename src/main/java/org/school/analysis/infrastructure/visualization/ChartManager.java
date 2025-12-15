package org.school.analysis.infrastructure.visualization;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;
import org.school.analysis.application.exception.RepositoryException;
import org.school.analysis.application.ports.output.SchoolRepository;
import org.school.analysis.domain.dto.CountryStudentStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ChartManager {
    private static final Logger logger = LoggerFactory.getLogger(ChartManager.class);
    private final SchoolRepository repository;

    public ChartManager(SchoolRepository repository) {
        this.repository = repository;
    }

    public byte[] createAverageStudentsByCountriesChart() {
        logger.info("Создаем диаграмму среднего количества студентов по странам");

        try {
            List<CountryStudentStats> stats = repository.findAverageStudentsByCountries(10);
            if (stats.isEmpty()) {
                throw new RepositoryException("Нет данных для создания диаграммы. Загрузите данные из CSV");
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            for (CountryStudentStats stat : stats) {
                dataset.addValue(stat.getAvgStudents(), "Среднее количество студентов", stat.getCountryName());
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    "Среднее количество студентов в 10 различных странах (округах)",
                    "Страна (округ)",
                    "Среднее количество студентов",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );

            customizeBarChart(chart, stats);
            return chartToBytes(chart, 1200, 800);

        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("Ошибка при создании диаграммы", e);
        }
    }

    public String getChartDescription() {
        try {
            List<CountryStudentStats> stats = repository.findAverageStudentsByCountries(10);

            if (stats.isEmpty()) {
                return "Нет данных для создания диаграммы";
            }

            StringBuilder description = new StringBuilder();
            description.append("Диаграмма: Среднее количество студентов по странам\n");
            description.append("Статистика по странам:\n");

            for (CountryStudentStats stat : stats) {
                description.append(String.format("• %s: %.1f студентов в среднем (%d школ)\n",
                        stat.getCountryName(), stat.getAvgStudents(), stat.getSchoolCount()
                ));
            }

            description.append("\nДиаграмма построена на основе данных из CSV файла");

            return description.toString();

        } catch (RepositoryException e) {
            return "Ошибка при получении данных для диаграммы " + e.getMessage();
        } catch (Exception e) {
            return "Ошибка при создании описания диаграммы " + e.getMessage();
        }
    }

    private void customizeBarChart(JFreeChart chart, List<CountryStudentStats> stats) {
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));
        domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(true);
        renderer.setMaximumBarWidth(0.1);

        Color[] colors = {new Color(65, 105, 225)};

        for (int i = 0; i < stats.size(); i++) {
            renderer.setSeriesPaint(i, colors[i % colors.length]);
            renderer.setSeriesOutlinePaint(i, Color.BLACK);
            renderer.setSeriesOutlineStroke(i, new BasicStroke(1.0f));
        }

        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultPositiveItemLabelPosition(
                new org.jfree.chart.labels.ItemLabelPosition(
                        org.jfree.chart.labels.ItemLabelAnchor.OUTSIDE12,
                        TextAnchor.BOTTOM_CENTER
                )
        );

        plot.setBackgroundPaint(new Color(240, 240, 240));
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);

        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
    }

    public byte[] chartToBytes(JFreeChart chart, int width, int height) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, width, height);
        return baos.toByteArray();
    }

    private void saveChartToFile(byte[] chartData, String filename) throws IOException {
        File outputFile = new File("charts/" + filename);
        outputFile.getParentFile().mkdirs();

        java.nio.file.Files.write(outputFile.toPath(), chartData);
        logger.info("Диаграмма сохранена {}", outputFile.getAbsolutePath());
    }
}