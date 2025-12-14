package org.school.analysis.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CountryStudentStats {
    private String countryName;
    private int schoolCount;
    private double avgStudents;
    private int minStudents;
    private int maxStudents;
    private int totalStudents;
}