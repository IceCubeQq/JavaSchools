package org.school.analysis.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MathSchoolStats {
    private int id;
    private String schoolName;
    private String countyName;
    private int students;
    private double mathScore;
    private double expenditure;
}
