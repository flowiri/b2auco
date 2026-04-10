package com.example.b2auco.workflow;

import com.example.b2auco.model.PreparedExport;

import java.io.IOException;
import java.nio.file.Path;

public interface PreparedExportPersister {
    Path write(PreparedExport preparedExport) throws IOException;
}
