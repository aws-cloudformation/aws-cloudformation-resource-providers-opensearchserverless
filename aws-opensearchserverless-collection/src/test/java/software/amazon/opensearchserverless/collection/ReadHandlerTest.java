package software.amazon.opensearchserverless.collection;

import java.time.Duration;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    static final String COLLECTION_ID = "irbh23msi1";
    static final String COLLECTION_NAME = "collection_name";
    static final String COLLECTION_ARN = "arn:aws:aoss:us-east-1:123456789012:collection/irbh23msi1";
    static final String COLLECTION_DESCRIPTION = "Collection description";
    static final String COLLECTION_ENDPOINT = "irbh23msi1.us-east-1.aoss.amazonaws.com";
    static final String DASHBOARD_ENDPOINT = "irbh23msi1.us-east-1.aoss.amazonaws.com/_dashboards";
    static final long CREATED_DATE = 1234567;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<OpenSearchServerlessClient> proxyClient;

    @Mock
    OpenSearchServerlessClient openSearchServerlessClient;

    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new ReadHandler(openSearchServerlessClient);
    }

    @AfterEach
    public void tear_down(org.junit.jupiter.api.TestInfo testInfo) {
        if(!testInfo.getTags().contains("skipSdkInteraction")) {
            verify(openSearchServerlessClient, atLeastOnce()).serviceName();
            verifyNoMoreInteractions(openSearchServerlessClient);
        }
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel expectedModel = ResourceModel.builder()
                .id(COLLECTION_ID)
                .name(COLLECTION_NAME)
                .description(COLLECTION_DESCRIPTION)
                .arn(COLLECTION_ARN)
                .collectionEndpoint(COLLECTION_ENDPOINT)
                .dashboardEndpoint(DASHBOARD_ENDPOINT)
                .build();

        final ResourceModel requestModel = ResourceModel.builder()
                .id(COLLECTION_ID)
                .name(COLLECTION_NAME)
                .description(COLLECTION_DESCRIPTION)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

        final BatchGetCollectionResponse batchGetCollectionResponse = BatchGetCollectionResponse.builder()
                .collectionDetails(
                        CollectionDetail.builder()
                                .id(COLLECTION_ID)
                                .status(CollectionStatus.ACTIVE)
                                .name(COLLECTION_NAME)
                                .description(COLLECTION_DESCRIPTION)
                                .arn(COLLECTION_ARN)
                                .collectionEndpoint(COLLECTION_ENDPOINT)
                                .dashboardEndpoint(DASHBOARD_ENDPOINT)
                                .createdDate(CREATED_DATE)
                                .build()
                ).build();
        when(proxyClient.client().batchGetCollection(any(BatchGetCollectionRequest.class)))
                .thenReturn(batchGetCollectionResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).batchGetCollection(any(BatchGetCollectionRequest.class));
    }

    @Test
    public void handleRequest_Failed_Release() {
        final ResourceModel requestModel = ResourceModel.builder()
                .id(COLLECTION_ID)
                .name(COLLECTION_NAME)
                .description(COLLECTION_DESCRIPTION)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

        final BatchGetCollectionResponse batchGetCollectionResponse = BatchGetCollectionResponse.builder()
                .collectionDetails(
                        CollectionDetail.builder()
                                .id(COLLECTION_ID)
                                .status(CollectionStatus.FAILED)
                                .name(COLLECTION_NAME)
                                .description(COLLECTION_DESCRIPTION)
                                .arn(COLLECTION_ARN)
                                .collectionEndpoint(COLLECTION_ENDPOINT)
                                .dashboardEndpoint(DASHBOARD_ENDPOINT)
                                .createdDate(CREATED_DATE)
                                .build()
                ).build();
        when(proxyClient.client().batchGetCollection(any(BatchGetCollectionRequest.class)))
                .thenReturn(batchGetCollectionResponse);

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_Failure_ResourceNotFound() {
        final ResourceModel requestModel = ResourceModel.builder()
                .id(COLLECTION_ID)
                .name(COLLECTION_NAME)
                .description(COLLECTION_DESCRIPTION)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

        final BatchGetCollectionResponse batchGetCollectionResponse = BatchGetCollectionResponse.builder()
                .collectionDetails(
                        CollectionDetail.builder()
                                .status(CollectionStatus.DELETING)
                                .build()
                ).build();
        when(proxyClient.client().batchGetCollection(any(BatchGetCollectionRequest.class)))
                .thenReturn(batchGetCollectionResponse);

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_NoInputFailure(){
        final ResourceModel requestModel = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(requestModel).build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
