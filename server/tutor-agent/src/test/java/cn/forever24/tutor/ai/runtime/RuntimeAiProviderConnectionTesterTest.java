package cn.forever24.tutor.ai.runtime;

import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConnectionTestResult;
import cn.forever24.tutor.application.provider.AiProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RuntimeAiProviderConnectionTesterTest {

    @Test
    void mapsAProviderAuthenticationFailureWithoutLeakingTheResponseBody() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "{\"error\":\"credential rejected\"}".getBytes();
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            RuntimeAiProviderConnectionTester tester = new RuntimeAiProviderConnectionTester(new ObjectMapper());
            AiProviderConnectionTestResult result = tester.test(new ActiveAiProviderConfiguration(
                    "local",
                    AiProviderType.OPENAI_COMPATIBLE,
                    URI.create("http://localhost:" + server.getAddress().getPort()),
                    "test-key",
                    "test-model",
                    null,
                    null,
                    null,
                    Duration.ofSeconds(2)));

            assertFalse(result.success());
            assertEquals("INVALID_API_KEY", result.error());
        } finally {
            server.stop(0);
        }
    }
}
