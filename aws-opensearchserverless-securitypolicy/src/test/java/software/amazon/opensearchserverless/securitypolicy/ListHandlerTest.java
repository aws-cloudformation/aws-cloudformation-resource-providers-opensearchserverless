package software.amazon.opensearchserverless.securitypolicy;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityPoliciesRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityPoliciesResponse;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityPolicySummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import com.google.common.collect.ImmutableList;

import java.time.Duration;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private static final String MOCK_POLICY_NAME_1 = "policy-name-1";
    private static final String MOCK_POLICY_TYPE_1 = "encryption";
    private static final String MOCK_POLICY_NAME_2 = "policy-name-2";
    @Mock
    OpenSearchServerlessClient openSearchServerlessClient;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private ListHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new ListHandler(openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        Collection<SecurityPolicySummary> securityPolicySummaries = ImmutableList.of(
            SecurityPolicySummary.builder().name(MOCK_POLICY_NAME_1).type(MOCK_POLICY_TYPE_1).build(),
            SecurityPolicySummary.builder().name(MOCK_POLICY_NAME_2).type(MOCK_POLICY_TYPE_1).build());
        final ListSecurityPoliciesResponse listSecurityPoliciesResponse =
            ListSecurityPoliciesResponse.builder()
                .securityPolicySummaries(securityPolicySummaries)
                .build();
        when(proxyClient.client().listSecurityPolicies(any(ListSecurityPoliciesRequest.class)))
            .thenReturn(listSecurityPoliciesResponse);

        final ResourceModel model = ResourceModel.builder()
            .type(MOCK_POLICY_TYPE_1)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

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

        verify(proxyClient.client()).listSecurityPolicies(any(ListSecurityPoliciesRequest.class));
    }
}
