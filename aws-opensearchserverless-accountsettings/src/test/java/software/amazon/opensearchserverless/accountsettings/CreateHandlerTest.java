package software.amazon.opensearchserverless.accountsettings;

import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private static final String MOCK_ACCOUNT_ID = "123456789012";
    public static final int MOCK_MAX_INDEXING_CAPACITY_IN_OCU = 5;
    public static final int MOCK_MAX_SEARCH_CAPACITY_IN_OCU = 6;

    private OpenSearchServerlessClient openSearchServerlessClient;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;

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
            .accountId(MOCK_ACCOUNT_ID)
            .capacityLimits(CapacityLimits.builder()
                .maxIndexingCapacityInOCU(MOCK_MAX_INDEXING_CAPACITY_IN_OCU)
                .maxSearchCapacityInOCU(MOCK_MAX_SEARCH_CAPACITY_IN_OCU)
                .build())
            .build();

        final ResourceModel model = ResourceModel.builder()
            .capacityLimits(CapacityLimits.builder()
                .maxIndexingCapacityInOCU(MOCK_MAX_INDEXING_CAPACITY_IN_OCU)
                .maxSearchCapacityInOCU(MOCK_MAX_SEARCH_CAPACITY_IN_OCU)
                .build())
            .build();

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(MOCK_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        final UpdateAccountSettingsResponse updateAccountSettingsResponse =
            UpdateAccountSettingsResponse.builder()
                .accountSettingsDetail(AccountSettingsDetail.builder()
                    .capacityLimits(software.amazon.awssdk.services.opensearchserverless.model.CapacityLimits.builder()
                        .maxIndexingCapacityInOCU(MOCK_MAX_INDEXING_CAPACITY_IN_OCU)
                        .maxSearchCapacityInOCU(MOCK_MAX_SEARCH_CAPACITY_IN_OCU)
                        .build())
                    .build())
                .build();

        when(openSearchServerlessClient.updateAccountSettings(any(UpdateAccountSettingsRequest.class)))
            .thenReturn(updateAccountSettingsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).updateAccountSettings(any(UpdateAccountSettingsRequest.class));
    }


    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WithoutCapacityLimits_Fail() {
        final ResourceModel model = ResourceModel.builder()
            .build();

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(MOCK_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_EmptyCapacityLimits_Fail() {
        final ResourceModel model = ResourceModel.builder()
            .capacityLimits(CapacityLimits.builder().build())
            .build();

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(MOCK_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WhenServerFailure_ThrowsException() {
        final ResourceModel model = ResourceModel.builder()
            .capacityLimits(CapacityLimits.builder()
                .maxIndexingCapacityInOCU(MOCK_MAX_INDEXING_CAPACITY_IN_OCU)
                .maxSearchCapacityInOCU(MOCK_MAX_SEARCH_CAPACITY_IN_OCU)
                .build())
            .build();

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(MOCK_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        when(openSearchServerlessClient.updateAccountSettings(any(UpdateAccountSettingsRequest.class)))
            .thenThrow(InternalServerException.builder().build());

        assertThrows(CfnInternalFailureException.class,
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).updateAccountSettings(any(UpdateAccountSettingsRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WhenAwsServiceException_ThrowsException() {
        final ResourceModel model = ResourceModel.builder()
            .capacityLimits(CapacityLimits.builder()
                .maxIndexingCapacityInOCU(MOCK_MAX_INDEXING_CAPACITY_IN_OCU)
                .maxSearchCapacityInOCU(MOCK_MAX_SEARCH_CAPACITY_IN_OCU)
                .build())
            .build();

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(MOCK_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        when(openSearchServerlessClient.updateAccountSettings(any(UpdateAccountSettingsRequest.class)))
            .thenThrow(AwsServiceException.builder().build());

        assertThrows(CfnGeneralServiceException.class,
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).updateAccountSettings(any(UpdateAccountSettingsRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WhenValidationException_ThrowsException() {
        final ResourceModel model = ResourceModel.builder()
            .capacityLimits(CapacityLimits.builder()
                .maxIndexingCapacityInOCU(MOCK_MAX_INDEXING_CAPACITY_IN_OCU)
                .maxSearchCapacityInOCU(MOCK_MAX_SEARCH_CAPACITY_IN_OCU)
                .build())
            .build();

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(MOCK_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        when(openSearchServerlessClient.updateAccountSettings(any(UpdateAccountSettingsRequest.class)))
            .thenThrow(ValidationException.builder().build());

        assertThrows(CfnInvalidRequestException.class,
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).updateAccountSettings(any(UpdateAccountSettingsRequest.class));
    }
}
