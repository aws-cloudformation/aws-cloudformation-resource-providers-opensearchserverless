package software.amazon.opensearchserverless.collection;

import com.google.common.collect.ImmutableList;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionSummary;
import software.amazon.awssdk.services.opensearchserverless.model.ListCollectionsRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListCollectionsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<OpenSearchServerlessClient> proxyClient;

    @Mock
    OpenSearchServerlessClient openSearchServerlessClient;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new ListHandler(openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        Collection<CollectionSummary> collectionSummaries = ImmutableList.of(
                CollectionSummary.builder().id("123456").status(CollectionStatus.ACTIVE).build(),
                CollectionSummary.builder().id("123457").status(CollectionStatus.ACTIVE).build()
        );
        final ListCollectionsResponse listCollectionsResponse = ListCollectionsResponse.builder()
                .collectionSummaries(collectionSummaries)
                .build();
        when(proxyClient.client().listCollections(any(ListCollectionsRequest.class)))
                .thenReturn(listCollectionsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels().size()).isEqualTo(2);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).listCollections(any(ListCollectionsRequest.class));
    }

    @Test
    public void handleRequest_Success_ActiveState() {

        Collection<CollectionSummary> collectionSummaries = ImmutableList.of(
                CollectionSummary.builder().id("123456").status(CollectionStatus.ACTIVE).build(),
                CollectionSummary.builder().id("123457").status(CollectionStatus.DELETING).build()
        );
        final ListCollectionsResponse listCollectionsResponse = ListCollectionsResponse.builder()
                .collectionSummaries(collectionSummaries)
                .build();
        when(proxyClient.client().listCollections(any(ListCollectionsRequest.class)))
                .thenReturn(listCollectionsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels().size()).isEqualTo(1);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).listCollections(any(ListCollectionsRequest.class));
    }

    @Test
    public void handleRequest_HasNextToken() {
        Collection<CollectionSummary> collectionSummaries = ImmutableList.of(
                CollectionSummary.builder().id("123456").status(CollectionStatus.ACTIVE).build()
        );
        final ListCollectionsResponse listCollectionsResponse = ListCollectionsResponse.builder()
                .collectionSummaries(collectionSummaries)
                .nextToken("h-0d017be6744e85727")
                .build();
        when(proxyClient.client().listCollections(any(ListCollectionsRequest.class)))
                .thenReturn(listCollectionsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isNotNull();

        verify(proxyClient.client()).listCollections(any(ListCollectionsRequest.class));
    }
}
