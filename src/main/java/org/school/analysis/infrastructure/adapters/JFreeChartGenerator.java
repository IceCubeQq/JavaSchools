package org.school.analysis.infrastructure.adapters;

import org.school.analysis.application.exception.RepositoryException;
import org.school.analysis.application.ports.output.ChartGenerator;
import org.school.analysis.infrastructure.visualization.ChartManager;

public class JFreeChartGenerator implements ChartGenerator {
    private final ChartManager chartService;

    public JFreeChartGenerator(ChartManager chartManager) {
        this.chartService = chartManager;
    }

    @Override
    public byte[] createAverageStudentsChart() {
        try {
            return chartService.createAverageStudentsByCountriesChart();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать диаграмму", e);
        }
    }

    @Override
    public String getChartDescription() {
        try {
            return chartService.getChartDescription();
        } catch (RepositoryException e) {
            return "Ошибка получения данных для диаграммы" + e.getMessage();
        } catch (Exception e) {
            return "Ошибка создания диаграммы";
        }
    }
}