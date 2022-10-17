package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.time.Duration;

import static software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient.builder;
public class ClientBuilder {
  private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(55);
  public static OpenSearchServerlessClient getClient() {
    return builder()
        .httpClient(LambdaWrapper.HTTP_CLIENT)
        .overrideConfiguration(ClientOverrideConfiguration.builder()
            .apiCallAttemptTimeout(API_CALL_TIMEOUT)
            .build())
        .build();
  }
}
