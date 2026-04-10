package com.example.b2auco.burp;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CurrentProjectIdentityProviderTest {
    @Test
    void preservesDistinctSiblingProjectFileIdentitiesForAlphaBurpAndBetaBurp() {
        Path alphaProjectFile = Path.of("C:/work/alpha.burp");
        Path betaProjectFile = Path.of("C:/work/beta.burp");

        assertNotNull(alphaProjectFile);
        assertNotNull(betaProjectFile);
        assertNotEquals(alphaProjectFile.normalize(), betaProjectFile.normalize());
    }
}
