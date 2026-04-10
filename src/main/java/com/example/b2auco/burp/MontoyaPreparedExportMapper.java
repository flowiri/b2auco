package com.example.b2auco.burp;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.model.PreparedExport;
import com.example.b2auco.naming.FilenamePolicy;

import java.util.Objects;

public final class MontoyaPreparedExportMapper {
    private final FilenamePolicy filenamePolicy;

    public MontoyaPreparedExportMapper() {
        this(new FilenamePolicy());
    }

    public MontoyaPreparedExportMapper(FilenamePolicy filenamePolicy) {
        this.filenamePolicy = Objects.requireNonNull(filenamePolicy, "filenamePolicy");
    }

    public PreparedExport toPreparedExport(HttpRequestResponse requestResponse, ExportTarget target) {
        Objects.requireNonNull(requestResponse, "requestResponse");
        ExportTarget exportTarget = Objects.requireNonNull(target, "target");

        HttpRequest request = requestResponse.request();
        String host = request.httpService().host();
        String pathWithoutQuery = request.pathWithoutQuery();
        byte[] requestBytes = request.toByteArray().getBytes();

        return new PreparedExport(
                exportTarget,
                filenamePolicy.deriveFileName(host, pathWithoutQuery),
                requestBytes
        );
    }
}
