package software.amazon.opensearchserverless.securitypolicy;

import com.google.common.collect.ImmutableList;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.*;
import software.amazon.cloudformation.proxy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private static final String MOCK_POLICY_NAME_1 = "policy-name-1";
    private static final String MOCK_POLICY_TYPE_1 = "encryption";
    private static final String MOCK_POLICY_NAME_2 = "policy-name-2";
    private static final String MOCK_POLICY_TYPE_2 = "network";

    private AmazonWebServicesClientProxy proxy;

    private ProxyClient<OpenSearchServerlessClient> proxyClient;

    @Mock
    OpenSearchServerlessClient openSearchServerlessClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();

        Collection<SecurityPolicySummary> securityPolicySummaries = ImmutableList.of(
                SecurityPolicySummary.builder().policyName(MOCK_POLICY_NAME_1).policyType(MOCK_POLICY_TYPE_1).build(),
                SecurityPolicySummary.builder().policyName(MOCK_POLICY_NAME_2).policyType(MOCK_POLICY_TYPE_1).build()
        );
        final ListSecurityPoliciesResponse listSecurityPoliciesResponse = ListSecurityPoliciesResponse.builder()
                .securityPolicySummaries(securityPolicySummaries)
                .build();
        when(proxyClient.client().listSecurityPolicies(any(ListSecurityPoliciesRequest.class)))
                .thenReturn(listSecurityPoliciesResponse);

        final ResourceModel model = ResourceModel.builder()
                .policyType(MOCK_POLICY_TYPE_1)
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
