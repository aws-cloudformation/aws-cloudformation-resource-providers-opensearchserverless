package software.amazon.opensearchserverless.collection;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    public static OpenSearchServerlessClient getClient() {
        return OpenSearchServerlessClient.builder()
                                         .httpClient(LambdaWrapper.HTTP_CLIENT)
                                         .build();
    }

}
