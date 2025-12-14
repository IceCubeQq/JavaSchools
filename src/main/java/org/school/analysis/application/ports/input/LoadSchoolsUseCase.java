package org.school.analysis.application.ports.input;

import java.io.InputStream;

public interface LoadSchoolsUseCase {
    int execute(InputStream csvStream);
}
