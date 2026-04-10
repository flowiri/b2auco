package com.example.b2auco.burp;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.model.PreparedExport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MontoyaPreparedExportMapperTest {
    @Test
    void mapsHostPathAndRawRequestBytesIntoPreparedExport() {
        byte[] rawRequest = "GET /api/users HTTP/1.1\r\nHost: example.com\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        ExportTarget target = new ExportTarget(Path.of("build", "tmp", "mapper-tests"));
        MontoyaPreparedExportMapper mapper = new MontoyaPreparedExportMapper();

        PreparedExport preparedExport = mapper.toPreparedExport(httpRequestResponse(rawRequest), target);

        assertEquals(target, preparedExport.target());
        assertEquals("example.com-api-users.txt", preparedExport.fileName().finalFileName());
        assertArrayEquals(rawRequest, preparedExport.requestBytes());
    }

    private static HttpRequestResponse httpRequestResponse(byte[] rawRequest) {
        HttpService service = (HttpService) Proxy.newProxyInstance(
                HttpService.class.getClassLoader(),
                new Class<?>[]{HttpService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "host" -> "example.com";
                    case "port" -> 443;
                    case "secure" -> true;
                    case "toString" -> "https://example.com:443";
                    default -> defaultValue(method.getReturnType());
                }
        );

        ByteArray byteArray = (ByteArray) Proxy.newProxyInstance(
                ByteArray.class.getClassLoader(),
                new Class<?>[]{ByteArray.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBytes" -> Arrays.copyOf(rawRequest, rawRequest.length);
                    case "length" -> rawRequest.length;
                    case "getByte" -> rawRequest[(Integer) args[0]];
                    case "copy" -> Arrays.copyOfRange(rawRequest, (Integer) args[0], (Integer) args[1]);
                    case "toString" -> new String(rawRequest, StandardCharsets.UTF_8);
                    case "iterator" -> Arrays.stream(toBoxed(rawRequest)).iterator();
                    default -> defaultValue(method.getReturnType());
                }
        );

        HttpRequest request = (HttpRequest) Proxy.newProxyInstance(
                HttpRequest.class.getClassLoader(),
                new Class<?>[]{HttpRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "httpService" -> service;
                    case "pathWithoutQuery" -> "/api/users";
                    case "toByteArray" -> byteArray;
                    case "toString" -> new String(rawRequest, StandardCharsets.UTF_8);
                    default -> defaultValue(method.getReturnType());
                }
        );

        return (HttpRequestResponse) Proxy.newProxyInstance(
                HttpRequestResponse.class.getClassLoader(),
                new Class<?>[]{HttpRequestResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "request" -> request;
                    case "httpService" -> service;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Byte[] toBoxed(byte[] bytes) {
        Byte[] boxed = new Byte[bytes.length];
        for (int index = 0; index < bytes.length; index++) {
            boxed[index] = bytes[index];
        }
        return boxed;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
