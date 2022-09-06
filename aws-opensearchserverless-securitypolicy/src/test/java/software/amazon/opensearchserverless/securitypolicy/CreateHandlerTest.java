package software.amazon.opensearchserverless.securitypolicy;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityPolicyDetail;
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
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private static final String MOCK_POLICY_NAME = "policy-name";
    private static final String MOCK_POLICY_TYPE = "encryption";
    private static final String MOCK_POLICY_DESCRIPTION = "Policy description";
    private static final String MOCK_POLICY_DOCUMENT = "Policy Document";
    private static final String MOCK_POLICY_VERSION = "policyversion";
    @Mock
    OpenSearchServerlessClient openSearchServerlessClient;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
    }

    @AfterEach
    public void tear_down() {
        verify(openSearchServerlessClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                                                 .name(MOCK_POLICY_NAME)
                                                 .type(MOCK_POLICY_TYPE)
                                                 .policyVersion(MOCK_POLICY_VERSION)
                                                 .description(MOCK_POLICY_DESCRIPTION)
                                                 .policy(MOCK_POLICY_DOCUMENT)
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

        when(proxyClient.client().createSecurityPolicy(any(CreateSecurityPolicyRequest.class))).thenReturn(createSecurityPolicyResponse);

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

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createSecurityPolicy(any(CreateSecurityPolicyRequest.class));
    }
}
