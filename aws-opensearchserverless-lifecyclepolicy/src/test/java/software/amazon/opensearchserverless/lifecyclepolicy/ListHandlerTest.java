package software.amazon.opensearchserverless.lifecyclepolicy;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.LifecyclePolicySummary;
import software.amazon.awssdk.services.opensearchserverless.model.LifecyclePolicyType;
import software.amazon.awssdk.services.opensearchserverless.model.ListLifecyclePoliciesRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListLifecyclePoliciesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private static final String MOCK_LIFECYCLE_POLICY_TYPE = LifecyclePolicyType.RETENTION.name();
    private static final String MOCK_LIFECYCLE_POLICY_NAME_1 = "policy-name-1";
    private static final String MOCK_LIFECYCLE_POLICY_NAME_2 = "policy-name-2";
    private OpenSearchServerlessClient openSearchServerlessClient;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler(openSearchServerlessClient);

        final Collection<LifecyclePolicySummary> lifecyclePolicySummaries = ImmutableList.of(
            LifecyclePolicySummary.builder().type(MOCK_LIFECYCLE_POLICY_TYPE).name(MOCK_LIFECYCLE_POLICY_NAME_1).build(),
            LifecyclePolicySummary.builder().type(MOCK_LIFECYCLE_POLICY_TYPE).name(MOCK_LIFECYCLE_POLICY_NAME_2).build()
        );
        final ListLifecyclePoliciesResponse listLifecyclePoliciesResponse = ListLifecyclePoliciesResponse.builder().lifecyclePolicySummaries(lifecyclePolicySummaries).build();
        when(openSearchServerlessClient.listLifecyclePolicies(any(ListLifecyclePoliciesRequest.class))).thenReturn(listLifecyclePoliciesResponse);

        final ResourceModel model = ResourceModel.builder().type(MOCK_LIFECYCLE_POLICY_TYPE).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).hasSize(lifecyclePolicySummaries.size());
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
