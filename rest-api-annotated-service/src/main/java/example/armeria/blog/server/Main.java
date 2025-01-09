package example.armeria.blog.server;

import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.docs.DocService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.instrumentation.armeria.v1_3.ArmeriaTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final OpenTelemetry openTelemetry = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();

    public static void main(String[] args) throws Exception {
        final Server server = newServer(8080, 8443);

        server.closeOnJvmShutdown();

        server.start().join();

        logger.info("Server has been started");
        logger.info("Serving DocService at http://127.0.0.1:{}/docs", server.activeLocalPort(SessionProtocol.HTTP));
        logger.info("Serving DocService at http://127.0.0.1:{}/docs", server.activeLocalPort(SessionProtocol.HTTPS));

        GlobalOpenTelemetry.get().getMeterProvider().get("armeria-server").histogramBuilder("armeria.server.started.metric").build();
    }

    /**
     * Returns a new {@link Server} instance which serves the blog service.
     *
     * @param httpPort the HTTP port that the server is to be bound to
     * @param httpsPort the HTTPS port that the server is to be bound to
     */
    static Server newServer(int httpPort, int httpsPort) {
        // Add OTel ArmeriaTelemetry decorator to ServerBuilder
        final ServerBuilder sb = Server.builder().decorator(ArmeriaTelemetry.builder(openTelemetry)
                .build()
                .newServiceDecorator());

        final DocService docService = DocService.builder()
                .exampleRequests(BlogService.class,
                        "createBlogPost",
                        "{\"title\":\"My first blog\", \"content\":\"Hello Armeria!\"}")
                .build();

        LongCounter longCounter = GlobalOpenTelemetry.get().getMeterProvider().get("armeria-server").counterBuilder("armeria.new.server.counter").build();
        longCounter.add(1, Attributes.of(AttributeKey.stringKey("foo"), "bar"));

        DoubleHistogram doubleHistogram = GlobalOpenTelemetry.get().getMeterProvider().get("armeria-server").histogramBuilder("armeria.new.server.histogram").build();
        doubleHistogram.record(1, Attributes.of(AttributeKey.stringKey("foo"), "bar"));

        return sb.http(httpPort)
                .https(httpsPort)
                .tlsSelfSigned() // so we can serve HTTPS locally
                .annotatedService(new BlogService())
                .serviceUnder("/docs", docService)
                .build();
    }
}
