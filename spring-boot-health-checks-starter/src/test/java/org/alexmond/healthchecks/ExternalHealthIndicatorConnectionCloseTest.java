package org.alexmond.healthchecks;

import org.alexmond.healthchecks.actuator.ActuatorSite;
import org.alexmond.healthchecks.actuator.ExternalActuatorHealthIndicator;
import org.alexmond.healthchecks.actuator.HealthActuatorProperties;
import org.alexmond.healthchecks.http.ExternalHttpHealthIndicator;
import org.alexmond.healthchecks.http.HealthHttpProperties;
import org.alexmond.healthchecks.http.HttpSite;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against the HTTP client leak: each check builds its own client, so the client
 * (and its pooled keep-alive connection) must be closed before the check returns. The
 * test server keeps the connection open after answering and reports whether the client
 * hung up.
 */
class ExternalHealthIndicatorConnectionCloseTest {

    private static final int CHECKS = 3;

    @Test
    void httpIndicatorClosesItsConnectionAfterEveryCheck() throws Exception {
        try (KeepAliveServer server = new KeepAliveServer("")) {
            HttpSite site = new HttpSite();
            site.setUrl(server.url());
            site.setInterval(Duration.ZERO);
            site.setTimeout(Duration.ofSeconds(5));
            HealthHttpProperties properties = new HealthHttpProperties();
            properties.getSites().put("local", site);

            assertConnectionClosedAfterEveryCheck(new ExternalHttpHealthIndicator(properties), server);
        }
    }

    @Test
    void actuatorIndicatorClosesItsConnectionAfterEveryCheck() throws Exception {
        try (KeepAliveServer server = new KeepAliveServer("{\"status\":\"UP\"}")) {
            ActuatorSite site = new ActuatorSite();
            site.setUrl(server.url());
            site.setInterval(Duration.ZERO);
            site.setTimeout(Duration.ofSeconds(5));
            HealthActuatorProperties properties = new HealthActuatorProperties();
            properties.getSites().put("local", site);

            assertConnectionClosedAfterEveryCheck(new ExternalActuatorHealthIndicator(properties), server);
        }
    }

    private static void assertConnectionClosedAfterEveryCheck(HealthIndicator indicator, KeepAliveServer server)
            throws InterruptedException {
        for (int check = 1; check <= CHECKS; check++) {
            assertThat(indicator.health().getStatus()).as("status of check %d", check).isEqualTo(Status.UP);
            assertThat(server.clientClosedConnection())
                    .as("check %d left its keep-alive connection open: the HTTP client was not closed", check)
                    .isTrue();
        }
    }

    /**
     * Minimal HTTP/1.1 server that answers each request with a keep-alive response and
     * then waits for the client to close the connection.
     */
    private static final class KeepAliveServer implements Closeable {

        private static final int WAIT_FOR_CLOSE_MILLIS = 2000;

        private final ServerSocket serverSocket = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());

        private final BlockingQueue<Boolean> closedByClient = new LinkedBlockingQueue<>();

        private final String body;

        KeepAliveServer(String body) throws IOException {
            this.body = body;
            Thread thread = new Thread(this::serve, "keep-alive-test-server");
            thread.setDaemon(true);
            thread.start();
        }

        String url() {
            return "http://127.0.0.1:" + serverSocket.getLocalPort() + "/";
        }

        /**
         * Reports whether the client closed the connection of the next handled request.
         *
         * @return {@code true} if the client closed it, {@code false} if it stayed open
         * @throws InterruptedException if interrupted while waiting
         */
        boolean clientClosedConnection() throws InterruptedException {
            Boolean closed = closedByClient.poll(10, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(closed);
        }

        private void serve() {
            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    closedByClient.add(answerAndWaitForClose(socket));
                } catch (IOException ex) {
                    // server socket closed, or a broken connection: stop or take the next
                }
            }
        }

        private boolean answerAndWaitForClose(Socket socket) throws IOException {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            String line = reader.readLine();
            while (line != null && !line.isEmpty()) {
                line = reader.readLine();
            }
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            String head = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nConnection: keep-alive\r\n"
                    + "Content-Length: " + payload.length + "\r\n\r\n";
            socket.getOutputStream().write(head.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().write(payload);
            socket.getOutputStream().flush();
            socket.setSoTimeout(WAIT_FOR_CLOSE_MILLIS);
            try {
                return reader.read() == -1;
            } catch (SocketTimeoutException ex) {
                return false;
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }

    }

}
