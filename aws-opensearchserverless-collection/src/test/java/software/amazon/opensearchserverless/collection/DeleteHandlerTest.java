package software.amazon.opensearchserverless.collection;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.Collections;

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
public class DeleteHandlerTest extends AbstractTestBase {

    static final String COLLECTION_ID = "irbh23msi1";
    static final String COLLECTION_NAME = "collection_name";
    static final String COLLECTION_ARN = "arn:aws:aoss:us-east-1:123456789012:collection/irbh23msi1";
    static final String COLLECTION_DESCRIPTION = "Collection description";
    static final String COLLECTION_TYPE = "SEARCH";
    static final String COLLECTION_ENDPOINT = "irbh23msi1.us-east-1.aoss.amazonaws.com";
    static final String DASHBOARD_ENDPOINT = "irbh23msi1.us-east-1.aoss.amazonaws.com/_dashboards";
    static final long CREATED_DATE = 1234567;
    @Mock
    OpenSearchServerlessClient openSearchServerlessClient;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new DeleteHandler(openSearchServerlessClient);
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
        //Delete collection. After delete collection call, resource does not exist
        final ResourceModel model = ResourceModel.builder()
                .id(COLLECTION_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final BatchGetCollectionResponse batchGetCollectionResponse =
                BatchGetCollectionResponse.builder()
                        .collectionDetails(Collections.emptyList())
                        .build();
        when(proxyClient.client().batchGetCollection(any(BatchGetCollectionRequest.class)))
                .thenReturn(batchGetCollectionResponse);

        final DeleteCollectionResponse deleteCollectionResponse = DeleteCollectionResponse.builder().build();
        when(proxyClient.client().deleteCollection(any(DeleteCollectionRequest.class))).thenReturn(deleteCollectionResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteCollection(any(DeleteCollectionRequest.class));
    }

    @Test
    public void handleRequest_Stabilization_Success() {
        //Delete collection. After delete collection call, resource takes some time to be deleted.
        final ResourceModel model = ResourceModel.builder()
                .id(COLLECTION_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final BatchGetCollectionResponse batchGetCollectionResponseDeleting =
                BatchGetCollectionResponse.builder()
                        .collectionDetails(
                                CollectionDetail.builder()
                                        .id(COLLECTION_ID)
                                        .status(CollectionStatus.DELETING)
                                        .name(COLLECTION_NAME)
                                        .type(COLLECTION_TYPE)
                                        .description(COLLECTION_DESCRIPTION)
                                        .arn(COLLECTION_ARN)
                                        .collectionEndpoint(COLLECTION_ENDPOINT)
                                        .dashboardEndpoint(DASHBOARD_ENDPOINT)
                                        .createdDate(CREATED_DATE)
                                        .build())
                        .build();
        final BatchGetCollectionResponse batchGetCollectionResponseEmpty =
                BatchGetCollectionResponse.builder()
                        .collectionDetails(Collections.emptyList())
                        .build();
        when(proxyClient.client().batchGetCollection(any(BatchGetCollectionRequest.class)))
                .thenReturn(batchGetCollectionResponseDeleting, batchGetCollectionResponseEmpty);
        final DeleteCollectionResponse deleteCollectionResponse = DeleteCollectionResponse.builder().build();
        when(proxyClient.client().deleteCollection(any(DeleteCollectionRequest.class))).thenReturn(deleteCollectionResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
                .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteCollection(any(DeleteCollectionRequest.class));
    }

    @Test
    public void handleRequest_Failure_CollectionDeleteFailed() {
        //Delete collection. After delete collection call, delete collection workflow fails.
        final ResourceModel model = ResourceModel.builder()
                .id(COLLECTION_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final BatchGetCollectionResponse batchGetCollectionResponseDeleting =
                BatchGetCollectionResponse.builder()
                        .collectionDetails(
                                CollectionDetail.builder()
                                        .id(COLLECTION_ID)
                                        .status(CollectionStatus.DELETING)
                                        .name(COLLECTION_NAME)
                                        .type(COLLECTION_TYPE)
                                        .description(COLLECTION_DESCRIPTION)
                                        .arn(COLLECTION_ARN)
                                        .collectionEndpoint(COLLECTION_ENDPOINT)
                                        .dashboardEndpoint(DASHBOARD_ENDPOINT)
                                        .createdDate(CREATED_DATE)
                                        .build())
                        .build();
        final BatchGetCollectionResponse batchGetCollectionResponseFailed =
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
        when(proxyClient.client().batchGetCollection(any(BatchGetCollectionRequest.class)))
                .thenReturn(batchGetCollectionResponseDeleting, batchGetCollectionResponseFailed);
        final DeleteCollectionResponse deleteCollectionResponse = DeleteCollectionResponse.builder().build();
        when(proxyClient.client().deleteCollection(any(DeleteCollectionRequest.class))).thenReturn(deleteCollectionResponse);

        assertThrows(CfnNotStabilizedException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Fail_NonExistingCollection() {
        // If collection does not exist, delete handler returns Not Found.
        final ResourceModel model = ResourceModel.builder()
                .id(COLLECTION_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().deleteCollection(any(DeleteCollectionRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_Fail_InvalidRequest() {
        final ResourceModel model = ResourceModel.builder()
                .id(COLLECTION_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().deleteCollection(any(DeleteCollectionRequest.class)))
                .thenThrow(ValidationException.builder().build());

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_Fail_On_Conflict() {
        final ResourceModel model = ResourceModel.builder()
                .id(COLLECTION_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().deleteCollection(any(DeleteCollectionRequest.class)))
                .thenThrow(ConflictException.builder().build());

        assertThrows(CfnResourceConflictException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_Fail_ServerError() {
        final ResourceModel model = ResourceModel.builder()
                .id(COLLECTION_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().deleteCollection(any(DeleteCollectionRequest.class)))
                .thenThrow(InternalServerException.builder().build());

        assertThrows(CfnServiceInternalErrorException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_Fail_NoCollectionIdInput() {
        final ResourceModel requestModel = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
                .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
