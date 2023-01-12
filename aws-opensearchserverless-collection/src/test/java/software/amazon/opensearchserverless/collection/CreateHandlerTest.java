package software.amazon.opensearchserverless.collection;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;
import software.amazon.awssdk.services.opensearchserverless.model.CreateCollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CreateCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateCollectionResponse;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import com.google.common.collect.ImmutableList;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    static final String COLLECTION_ID = "irbh23msi1";
    static final String COLLECTION_NAME = "collection_name";
    static final String COLLECTION_ARN = "arn:aws:aoss:us-east-1:123456789012:collection/irbh23msi1";
    static final String COLLECTION_DESCRIPTION = "Collection description";
    static final String COLLECTION_TYPE = "SEARCH";
    static final String COLLECTION_ENDPOINT = "irbh23msi1.us-east-1.aoss.amazonaws.com";
    static final String DASHBOARD_ENDPOINT = "irbh23msi1.us-east-1.aoss.amazonaws.com/_dashboards";
    static final long CREATED_DATE = 1234567;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<OpenSearchServerlessClient> proxyClient;

    @Mock
    private OpenSearchServerlessClient openSearchServerlessClient;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new CreateHandler(openSearchServerlessClient);
    }

    @AfterEach
    public void tear_down(org.junit.jupiter.api.TestInfo testInfo) {
        if (!testInfo.getTags().contains("skipSdkInteraction")) {
            verify(openSearchServerlessClient, atLeastOnce()).serviceName();
            verifyNoMoreInteractions(openSearchServerlessClient);
        }
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel expectedModel = ResourceModel.builder()
                                                         .id(COLLECTION_ID)
                                                         .name(COLLECTION_NAME)
                                                         .type(COLLECTION_TYPE)
                                                         .description(COLLECTION_DESCRIPTION)
                                                         .arn(COLLECTION_ARN)
                                                         .collectionEndpoint(COLLECTION_ENDPOINT)
                                                         .dashboardEndpoint(DASHBOARD_ENDPOINT)
                                                         .build();

        final ResourceModel requestModel = ResourceModel.builder()
                                                        .name(COLLECTION_NAME)
                                                        .type(COLLECTION_TYPE)
                                                        .description(COLLECTION_DESCRIPTION)
                                                        .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                                    .desiredResourceState(requestModel)
                                                                                    .build();

        final CreateCollectionResponse createCollectionResponse =
                CreateCollectionResponse.builder()
                                        .createCollectionDetail(CreateCollectionDetail.builder().id(COLLECTION_ID).build())
                                        .build();
        when(proxyClient.client().createCollection(any(CreateCollectionRequest.class))).thenReturn(createCollectionResponse);

        final BatchGetCollectionResponse batchGetCollectionResponse =
                BatchGetCollectionResponse.builder()
                                          .collectionDetails(
                                                  CollectionDetail.builder()
                                                                  .id(COLLECTION_ID)
                                                                  .status(CollectionStatus.ACTIVE)
                                                                  .name(COLLECTION_NAME)
                                                                  .type(COLLECTION_TYPE)
                                                                  .description(COLLECTION_DESCRIPTION)
                                                                  .arn(COLLECTION_ARN)
                                                                  .collectionEndpoint(COLLECTION_ENDPOINT)
                                                                  .dashboardEndpoint(DASHBOARD_ENDPOINT)
                                                                  .createdDate(CREATED_DATE)
                                                                  .build()
                                                            ).build();
        when(proxyClient.client().batchGetCollection(any(BatchGetCollectionRequest.class)))
                .thenReturn(batchGetCollectionResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
                .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createCollection(any(CreateCollectionRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccessStabilization() {
        final ResourceModel requestModel = ResourceModel.builder()
                                                        .name(COLLECTION_NAME)
                                                        .type(COLLECTION_TYPE)
                                                        .description(COLLECTION_DESCRIPTION)
                                                        .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                                    .desiredResourceState(requestModel)
                                                                                    .build();

        final CreateCollectionResponse createCollectionResponse =
                CreateCollectionResponse.builder()
                                        .createCollectionDetail(
                                                CreateCollectionDetail.builder()
                                                                      .id(COLLECTION_ID)
                                                                      .name(COLLECTION_NAME)
                                                                      .type(COLLECTION_TYPE)
                                                                      .description(COLLECTION_DESCRIPTION)
                                                                      .build()
                                                               ).build();
        when(proxyClient.client().createCollection(any(CreateCollectionRequest.class))).thenReturn(createCollectionResponse);
        CollectionDetail collectionDetail1 = CollectionDetail.builder()
                                                             .id(COLLECTION_ID)
                                                             .status(CollectionStatus.CREATING)
                                                             .name(COLLECTION_NAME)
                                                             .type(COLLECTION_TYPE)
                                                             .description(COLLECTION_DESCRIPTION)
                                                             .build();
        CollectionDetail collectionDetail2 = CollectionDetail.builder()
                                                             .id(COLLECTION_ID)
                                                             .status(CollectionStatus.ACTIVE)
                                                             .name(COLLECTION_NAME)
                                                             .type(COLLECTION_TYPE)
                                                             .description(COLLECTION_DESCRIPTION)
                                                             .build();

        final BatchGetCollectionResponse batchGetCollectionResponse1 =
                BatchGetCollectionResponse.builder()
                                          .collectionDetails(ImmutableList.of(collectionDetail1)).
                                          build();
        final BatchGetCollectionResponse batchGetCollectionResponse2 =
                BatchGetCollectionResponse.builder()
                                          .collectionDetails(ImmutableList.of(collectionDetail2))
                                          .build();

        when(proxyClient.client().batchGetCollection(any(BatchGetCollectionRequest.class)))
                .thenReturn(batchGetCollectionResponse1)
                .thenReturn(batchGetCollectionResponse1)
                .thenReturn(batchGetCollectionResponse2);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
                .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(requestModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createCollection(any(CreateCollectionRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_CreateWithCollectionIdFail() {
        final ResourceModel desiredResourceModel = ResourceModel.builder().id(COLLECTION_ID).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                                    .desiredResourceState(desiredResourceModel)
                                                                                    .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler
                .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_CreateFail_Deleted() {
        final ResourceModel requestModel = ResourceModel.builder()
                                                        .name(COLLECTION_NAME)
                                                        .type(COLLECTION_TYPE)
                                                        .description(COLLECTION_DESCRIPTION)
                                                        .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                                    .desiredResourceState(requestModel)
                                                                                    .build();

        final CreateCollectionResponse createCollectionResponse =
                CreateCollectionResponse.builder()
                                        .createCollectionDetail(CreateCollectionDetail.builder().id(COLLECTION_ID).build())
                                        .build();
        when(proxyClient.client().createCollection(any(CreateCollectionRequest.class))).thenReturn(createCollectionResponse);

        final BatchGetCollectionResponse batchGetCollectionResponse =
                BatchGetCollectionResponse.builder()
                                          .collectionDetails(
                                                  CollectionDetail.builder()
                                                                  .id(COLLECTION_ID)
                                                                  .status(CollectionStatus.FAILED)
                                                                  .name(COLLECTION_NAME)
                                                                  .type(COLLECTION_TYPE)
                                                                  .description(COLLECTION_DESCRIPTION)
                                                                  .arn(COLLECTION_ARN)
                                                                  .collectionEndpoint(COLLECTION_ENDPOINT)
                                                                  .dashboardEndpoint(DASHBOARD_ENDPOINT)
                                                                  .createdDate(CREATED_DATE)
                                                                  .build())
                                          .build();
        when(proxyClient.client().batchGetCollection(any(BatchGetCollectionRequest.class))).thenReturn(batchGetCollectionResponse);
        assertThrows(CfnNotStabilizedException.class,
                     () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_NoNameFail() {
        final ResourceModel desiredResourceModel = ResourceModel.builder()
                                                                .type(COLLECTION_TYPE)
                                                                .description(COLLECTION_DESCRIPTION)
                                                                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                                    .desiredResourceState(desiredResourceModel)
                                                                                    .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler
                .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
