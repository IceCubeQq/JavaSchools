package org.school.analysis.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class School {
    private Integer id;
    private Integer districtId;
    private String name;
    private String country;
    private String grades;
    private Integer students;
    private Double teachers;
    private Double calworks;
    private Double lunch;
    private Integer computers;
    private Double expenditure;
    private Double income;
    private Double english;
    private Double readScore;
    private Double mathScore;

}
