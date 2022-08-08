package software.amazon.opensearchserverless.securitypolicy;

import java.time.Duration;

import org.mockito.Mockito;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
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
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    private static final String MOCK_POLICY_NAME = "policy-name";
    private static final String MOCK_POLICY_TYPE = "encryption";
    private static final String MOCK_POLICY_DESCRIPTION = "Policy description";
    private static final String MOCK_POLICY_DOCUMENT = "Policy Document";

    private AmazonWebServicesClientProxy proxy;

    private ProxyClient<OpenSearchServerlessClient> proxyClient;

    @Mock
    OpenSearchServerlessClient openSearchServerlessClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
    }

    @AfterEach
    public void tear_down() {
        Mockito.verify(openSearchServerlessClient, Mockito.atLeastOnce()).serviceName();
        Mockito.verifyNoMoreInteractions(openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final UpdateHandler handler = new UpdateHandler();

        final UpdateSecurityPolicyResponse updateSecurityPolicyResponse = UpdateSecurityPolicyResponse.builder()
                .securityPolicyDetail(
                        SecurityPolicyDetail.builder()
                                .policyName(MOCK_POLICY_NAME)
                                .policyType(MOCK_POLICY_TYPE)
                                .policyDescription(MOCK_POLICY_DESCRIPTION)
                                .policyDocument(MOCK_POLICY_DOCUMENT)
                                .build()
                ).build();
        Mockito.when(proxyClient.client().updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class))).thenReturn(updateSecurityPolicyResponse);

        final GetSecurityPolicyResponse getSecurityPolicyResponse = GetSecurityPolicyResponse.builder()
                .getSecurityPolicyDetail(
                        SecurityPolicyDetail.builder()
                                .policyName(MOCK_POLICY_NAME)
                                .policyType(MOCK_POLICY_TYPE)
                                .policyDescription(MOCK_POLICY_DESCRIPTION)
                                .policyDocument(MOCK_POLICY_DOCUMENT)
                                .build()
                ).build();
        Mockito.when(proxyClient.client().getSecurityPolicy(any(GetSecurityPolicyRequest.class)))
                .thenReturn(getSecurityPolicyResponse);

        final ResourceModel model = ResourceModel.builder()
                .policyName(MOCK_POLICY_NAME)
                .policyType(MOCK_POLICY_TYPE)
                .policyDescription(MOCK_POLICY_DESCRIPTION)
                .policyDocument(MOCK_POLICY_DOCUMENT)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        Mockito.verify(proxyClient.client()).updateSecurityPolicy(any(UpdateSecurityPolicyRequest.class));
    }
}
