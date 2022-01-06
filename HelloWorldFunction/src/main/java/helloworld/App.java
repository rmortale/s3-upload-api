package helloworld;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String BUCKET_NAME = System.getenv("BUCKET_NAME");
    private static final String URL_EXPIRATION_SECONDS = System.getenv("URL_EXPIRATION_SECONDS");
    private static final String REGION = System.getenv("A_REGION");

    private final S3Presigner presigner = S3Presigner.builder().region(Region.of(REGION)).build();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {

        String filename = UUID.randomUUID().toString();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(Map.of("Content-Type", "application/json"));
        try {
            final String url = buildSignedUrl(filename, Long.parseLong(URL_EXPIRATION_SECONDS));
            return response
                    .withStatusCode(200)
                    .withBody("{\"url\": \"" + url + "\", \"key\": \"" + filename + "\" }");
        } catch (IOException e) {
            return response
                    .withBody("{ \"error\": \"" + e.getMessage() + "\" }")
                    .withStatusCode(500);
        }
    }

    private String buildSignedUrl(String keyName, long durationSeconds) throws IOException {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(App.BUCKET_NAME)
                .key(keyName)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(durationSeconds))
                .putObjectRequest(objectRequest)
                .build();

        return presigner.presignPutObject(presignRequest).url().toString();
    }

}
