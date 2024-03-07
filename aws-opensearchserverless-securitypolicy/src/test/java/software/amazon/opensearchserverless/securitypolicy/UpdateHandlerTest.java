package software.amazon.opensearchserverless.securitypolicy;

import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityPolicyDetail;
import software.amazon.awssdk.services.opensearchserverless.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    private static final String MOCK_POLICY_NAME = "policy-name";
    private static final String MOCK_POLICY_TYPE = "encryption";
    private static final String MOCK_POLICY_DESCRIPTION = "Policy description";
    private static final Document MOCK_POLICY_DOCUMENT = Document.fromString("Policy Document");
    private static final String MOCK_POLICY_VERSION = "policyversion";
    private static final String MOCK_POLICY_VERSION_UPDATED = "policyversion Updated";
    @Mock
    OpenSearchServerlessClient openSearchServerlessClient;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new UpdateHandler(openSearchServerlessClient);
    }

    @AfterEach
    public void tear_down() {
        verify(openSearchServerlessClient, Mockito.atLeastOnce()).serviceName();
        Mockito.verifyNoMoreInteractions(openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final UpdateSecurityPolicyResponse updateSecurityPolicyResponse = UpdateSecurityPolicyResponse.builder()
            .securityPolicyDetail(
                SecurityPolicyDetail.builder()
                    .name(MOCK_POLICY_NAME)
                    .type(MOCK_POLICY_TYPE)
                    .policyVersion(MOCK_POLICY_VERSION_UPDATED)
                    .description(MOCK_POLICY_DESCRIPTION)
                    .policy(MOCK_POLICY_DOCUMENT)
                    .build()
            ).build();

        when(openSearchServerlessClient.updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class)))
            .thenReturn(updateSecurityPolicyResponse);

        final GetSecurityPolicyResponse getSecurityPolicyResponse = GetSecurityPolicyResponse.builder()
            .securityPolicyDetail(
                SecurityPolicyDetail.builder()
                    .name(MOCK_POLICY_NAME)
                    .type(MOCK_POLICY_TYPE)
                    .policyVersion(MOCK_POLICY_VERSION)
                    .description(MOCK_POLICY_DESCRIPTION)
                    .policy(MOCK_POLICY_DOCUMENT)
                    .build()
            ).build();

        when(openSearchServerlessClient.getSecurityPolicy(any(GetSecurityPolicyRequest.class)))
            .thenReturn(getSecurityPolicyResponse);

        final ResourceModel model = ResourceModel.builder()
            .name(MOCK_POLICY_NAME)
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
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(openSearchServerlessClient).updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class));
    }

    @Test
    public void handleRequest_ResourceNotFoundException_Fail() {
        when(openSearchServerlessClient.updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        final GetSecurityPolicyResponse getSecurityPolicyResponse = GetSecurityPolicyResponse.builder()
                .securityPolicyDetail(
                        SecurityPolicyDetail.builder()
                                .name(MOCK_POLICY_NAME)
                                .type(MOCK_POLICY_TYPE)
                                .policyVersion(MOCK_POLICY_VERSION)
                                .description(MOCK_POLICY_DESCRIPTION)
                                .policy(MOCK_POLICY_DOCUMENT)
                                .build()
                ).build();

        when(openSearchServerlessClient.getSecurityPolicy(any(GetSecurityPolicyRequest.class)))
                .thenReturn(getSecurityPolicyResponse);

        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_POLICY_NAME)
                .type(MOCK_POLICY_TYPE)
                .description(MOCK_POLICY_DESCRIPTION)
                .policy(MOCK_POLICY_DOCUMENT.toString())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class));
    }

    @Test
    public void handleRequest_ValidationException_Fail() {
        when(openSearchServerlessClient.updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class)))
                .thenThrow(ValidationException.builder().build());

        final GetSecurityPolicyResponse getSecurityPolicyResponse = GetSecurityPolicyResponse.builder()
                .securityPolicyDetail(
                        SecurityPolicyDetail.builder()
                                .name(MOCK_POLICY_NAME)
                                .type(MOCK_POLICY_TYPE)
                                .policyVersion(MOCK_POLICY_VERSION)
                                .description(MOCK_POLICY_DESCRIPTION)
                                .policy(MOCK_POLICY_DOCUMENT)
                                .build()
                ).build();

        when(openSearchServerlessClient.getSecurityPolicy(any(GetSecurityPolicyRequest.class)))
                .thenReturn(getSecurityPolicyResponse);

        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_POLICY_NAME)
                .type(MOCK_POLICY_TYPE)
                .description(MOCK_POLICY_DESCRIPTION)
                .policy(MOCK_POLICY_DOCUMENT.toString())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class));
    }

    @Test
    public void handleRequest_ConflictException_Fail() {
        when(openSearchServerlessClient.updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class)))
                .thenThrow(ConflictException.builder().build());

        final GetSecurityPolicyResponse getSecurityPolicyResponse = GetSecurityPolicyResponse.builder()
                .securityPolicyDetail(
                        SecurityPolicyDetail.builder()
                                .name(MOCK_POLICY_NAME)
                                .type(MOCK_POLICY_TYPE)
                                .policyVersion(MOCK_POLICY_VERSION)
                                .description(MOCK_POLICY_DESCRIPTION)
                                .policy(MOCK_POLICY_DOCUMENT)
                                .build()
                ).build();

        when(openSearchServerlessClient.getSecurityPolicy(any(GetSecurityPolicyRequest.class)))
                .thenReturn(getSecurityPolicyResponse);

        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_POLICY_NAME)
                .type(MOCK_POLICY_TYPE)
                .description(MOCK_POLICY_DESCRIPTION)
                .policy(MOCK_POLICY_DOCUMENT.toString())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnResourceConflictException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class));
    }

    @Test
    public void handleRequest_ServiceQuotaExceededException_Fail() {
        when(openSearchServerlessClient.updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class)))
                .thenThrow(ServiceQuotaExceededException.builder().build());

        final GetSecurityPolicyResponse getSecurityPolicyResponse = GetSecurityPolicyResponse.builder()
                .securityPolicyDetail(
                        SecurityPolicyDetail.builder()
                                .name(MOCK_POLICY_NAME)
                                .type(MOCK_POLICY_TYPE)
                                .policyVersion(MOCK_POLICY_VERSION)
                                .description(MOCK_POLICY_DESCRIPTION)
                                .policy(MOCK_POLICY_DOCUMENT)
                                .build()
                ).build();

        when(openSearchServerlessClient.getSecurityPolicy(any(GetSecurityPolicyRequest.class)))
                .thenReturn(getSecurityPolicyResponse);

        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_POLICY_NAME)
                .type(MOCK_POLICY_TYPE)
                .description(MOCK_POLICY_DESCRIPTION)
                .policy(MOCK_POLICY_DOCUMENT.toString())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceLimitExceededException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class));
    }
}
