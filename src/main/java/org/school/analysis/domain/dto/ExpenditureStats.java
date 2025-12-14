package org.school.analysis.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenditureStats {
    private String countyName;
    private int schoolCount;
    private double avgExpenditure;
    private double minExpenditure;
    private double maxExpenditure;
}
