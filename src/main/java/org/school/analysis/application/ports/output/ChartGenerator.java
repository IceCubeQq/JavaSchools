package org.school.analysis.application.ports.output;

public interface ChartGenerator {
    byte[] createAverageStudentsChart();
    String getChartDescription();
}