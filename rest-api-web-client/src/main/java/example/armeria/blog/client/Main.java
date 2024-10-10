package example.armeria.blog.client;

import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.WebClientBuilder;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.newrelic.api.agent.Trace;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.armeria.v1_3.ArmeriaTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final OpenTelemetry openTelemetry = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
    private static final AtomicInteger currentBlogPostId = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        WebClient httpWebClient = newClient("http://127.0.0.1:8080");
        WebClient httpsWebClient = newClient("https://127.0.0.1:8443");

        while (true) {
            makeRequestsToServer(httpWebClient);
            makeRequestsToServer(httpsWebClient);

            Thread.sleep(1000);
        }
    }

    public static WebClient newClient(String uri) {
        // Disable the verification of server's TLS certificate chain for testing.
        ClientFactory factory = ClientFactory.builder().tlsNoVerify().build();

        // Add OTel ArmeriaTelemetry decorator to WebClientBuilder
        WebClientBuilder wcb = WebClient.builder(uri).decorator(ArmeriaTelemetry.builder(openTelemetry).build().newClientDecorator());

        logger.info("HTTP WebClient has been created for {}.", uri);

        return wcb.factory(factory).build();
    }

    public static void makeRequestsToServer(WebClient client) {
        createBlogPost(client);
        getBlogPosts(client);
        updateBlogPost(client);
        getBlogPost(client);
        deleteBlogPost(client);
    }

    // curl -XPOST -H 'content-type: application/json; charset=utf-8' 'http://127.0.0.1:8080/blogs' -d '{"title":"My first blog", "content":"Hello Armeria!"}'
    @Trace(dispatcher = true, metricName = "createBlogPost")
    public static void createBlogPost(WebClient client) {
        RequestHeaders postJson = RequestHeaders.of(HttpMethod.POST, "/blogs", HttpHeaderNames.CONTENT_TYPE, "application/json");

        AggregatedHttpResponse response = client.execute(postJson, "{\"title\":\"My first blog\", \"content\":\"Hello Armeria!\"}").aggregate().join();

        String content = response.content(Charset.defaultCharset());
        try {
            JSONObject jsonContent = new JSONObject(content);
            Integer id = (Integer) jsonContent.get("id");
            currentBlogPostId.set(id);
        } catch (JSONException err) {
            logger.error("Error parsing blog ID: {}", err.toString());
        }

        logResponse(response, "createBlogPost");
    }

    // curl -XDELETE -H 'content-type: application/json; charset=utf-8' 'http://127.0.0.1:8080/blogs/0' -d '{}'
    @Trace(dispatcher = true, metricName = "deleteBlogPost")
    public static void deleteBlogPost(WebClient client) {
        RequestHeaders deleteJson = RequestHeaders.of(HttpMethod.DELETE, "/blogs/" + currentBlogPostId.get(), HttpHeaderNames.CONTENT_TYPE, "application/json");

        AggregatedHttpResponse response = client.execute(deleteJson).aggregate().join();

        logResponse(response, "deleteBlogPost");
    }

    // curl -XGET -H 'content-type: application/json; charset=utf-8' 'http://127.0.0.1:8080/blogs/0'
    @Trace(dispatcher = true, metricName = "getBlogPost")
    public static void getBlogPost(WebClient client) {
        RequestHeaders getJson = RequestHeaders.of(HttpMethod.GET, "/blogs/" + currentBlogPostId.get(), HttpHeaderNames.CONTENT_TYPE, "application/json");

        AggregatedHttpResponse response = client.execute(getJson).aggregate().join();

        logResponse(response, "getBlogPost");
    }

    // curl -XGET -H 'content-type: application/json; charset=utf-8' 'http://127.0.0.1:8080/blogs'
    @Trace(dispatcher = true, metricName = "getBlogPosts")
    public static void getBlogPosts(WebClient client) {
        RequestHeaders getJson = RequestHeaders.of(HttpMethod.GET, "/blogs", HttpHeaderNames.CONTENT_TYPE, "application/json");

        AggregatedHttpResponse response = client.execute(getJson).aggregate().join();

        logResponse(response, "getBlogPosts");
    }

    // curl -XPUT -H 'content-type: application/json; charset=utf-8' 'http://127.0.0.1:8080/blogs/0' -d '{}'
    @Trace(dispatcher = true, metricName = "updateBlogPost")
    public static void updateBlogPost(WebClient client) {
        RequestHeaders putJson = RequestHeaders.of(HttpMethod.PUT, "/blogs/" + currentBlogPostId.get(), HttpHeaderNames.CONTENT_TYPE, "application/json");

        AggregatedHttpResponse response = client.execute(putJson, "{\"title\":\"My first blog UPDATED\", \"content\":\"Hello Armeria!\"}").aggregate().join();

        logResponse(response, "updateBlogPost");
    }

    public static void logResponse(AggregatedHttpResponse response, String method) {
        logger.info("====================================");
        logger.info("WebClient {}", method);
        logger.info("Response headers: {}", response.headers());
        logger.info("Response content: {}", response.content(Charset.defaultCharset()));
        logger.info("Response status: {}", response.status());
        logger.info("====================================");
    }
}
