package com.example.b2auco.burp;

import burp.api.montoya.MontoyaApi;

import java.nio.file.Path;
import java.util.Optional;

public interface CurrentProjectIdentityProvider {
    Optional<Path> findCurrentProjectFilePath(MontoyaApi api);
}
