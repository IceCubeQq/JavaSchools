package org.school.analysis.parser;

import org.school.analysis.domain.model.School;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface CsvParser {
    List<School> parseSchools(InputStream inputStream) throws IOException;
}
