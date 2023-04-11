package software.amazon.opensearchserverless.securitypolicy;

import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityPolicyDetail;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private static final String MOCK_POLICY_NAME = "policy-name";
    private static final String MOCK_POLICY_TYPE = "encryption";
    private static final String MOCK_POLICY_DESCRIPTION = "Policy description";
    private static final Document MOCK_POLICY_DOCUMENT = Document.fromString("Policy Document");
    private static final String MOCK_POLICY_VERSION = "policyversion";
    @Mock
    OpenSearchServerlessClient openSearchServerlessClient;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
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
        final ResourceModel model = ResourceModel.builder()
            .name(MOCK_POLICY_NAME)
            .type(MOCK_POLICY_TYPE)
            .description(MOCK_POLICY_DESCRIPTION)
            .policy(MOCK_POLICY_DOCUMENT.toString())
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final CreateSecurityPolicyResponse createSecurityPolicyResponse =
            CreateSecurityPolicyResponse.builder()
                .securityPolicyDetail(
                    SecurityPolicyDetail.builder()
                        .name(MOCK_POLICY_NAME)
                        .type(MOCK_POLICY_TYPE)
                        .policyVersion(MOCK_POLICY_VERSION)
                        .description(MOCK_POLICY_DESCRIPTION)
                        .policy(MOCK_POLICY_DOCUMENT)
                        .build()
                ).build();

        when(proxyClient.client().createSecurityPolicy(any(CreateSecurityPolicyRequest.class)))
            .thenReturn(createSecurityPolicyResponse);

        final GetSecurityPolicyResponse getSecurityPolicyResponse =
            GetSecurityPolicyResponse.builder()
                .securityPolicyDetail(
                    SecurityPolicyDetail.builder()
                        .name(MOCK_POLICY_NAME)
                        .type(MOCK_POLICY_TYPE)
                        .policyVersion(MOCK_POLICY_VERSION)
                        .description(MOCK_POLICY_DESCRIPTION)
                        .policy(MOCK_POLICY_DOCUMENT)
                        .build()
                ).build();

        when(proxyClient.client().getSecurityPolicy(any(GetSecurityPolicyRequest.class)))
            .thenReturn(getSecurityPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createSecurityPolicy(any(CreateSecurityPolicyRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_CreateWithoutNameFail() {
        final ResourceModel model = ResourceModel.builder()
                .type(MOCK_POLICY_TYPE)
                .description(MOCK_POLICY_DESCRIPTION)
                .policy(MOCK_POLICY_DOCUMENT.toString())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_CreateWithoutTypeFail() {
        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_POLICY_NAME)
                .description(MOCK_POLICY_DESCRIPTION)
                .policy(MOCK_POLICY_DOCUMENT.toString())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_CreateWithoutPolicyFail() {
        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_POLICY_NAME)
                .type(MOCK_POLICY_TYPE)
                .description(MOCK_POLICY_DESCRIPTION)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_CreateWhenAlreadyExistsFail() {
        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_POLICY_NAME)
                .type(MOCK_POLICY_TYPE)
                .description(MOCK_POLICY_DESCRIPTION)
                .policy(MOCK_POLICY_DOCUMENT.toString())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().createSecurityPolicy(any(CreateSecurityPolicyRequest.class)))
                .thenThrow(ConflictException.class);

        Throwable throwable = catchThrowable(() ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        assertThat(throwable).isInstanceOf(CfnAlreadyExistsException.class);
    }

    @Test
    public void handleRequest_CreateWhenInvalidRequestFail() {
        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_POLICY_NAME)
                .type(MOCK_POLICY_TYPE)
                .description(MOCK_POLICY_DESCRIPTION)
                .policy(MOCK_POLICY_DOCUMENT.toString())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().createSecurityPolicy(any(CreateSecurityPolicyRequest.class)))
                .thenThrow(ValidationException.class);

        Throwable throwable = catchThrowable(() ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        assertThat(throwable).isInstanceOf(CfnInvalidRequestException.class);
    }

    @Test
    public void handleRequest_CreateWhenInternalServerExceptionFail() {
        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_POLICY_NAME)
                .type(MOCK_POLICY_TYPE)
                .description(MOCK_POLICY_DESCRIPTION)
                .policy(MOCK_POLICY_DOCUMENT.toString())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().createSecurityPolicy(any(CreateSecurityPolicyRequest.class)))
                .thenThrow(InternalServerException.class);

        Throwable throwable = catchThrowable(() ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        assertThat(throwable).isInstanceOf(CfnServiceInternalErrorException.class);
    }
}
