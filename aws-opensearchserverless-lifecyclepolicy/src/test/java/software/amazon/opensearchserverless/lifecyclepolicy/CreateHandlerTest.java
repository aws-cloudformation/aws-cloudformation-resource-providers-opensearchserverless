package software.amazon.opensearchserverless.lifecyclepolicy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.LifecyclePolicyDetail;
import software.amazon.awssdk.services.opensearchserverless.model.LifecyclePolicyType;
import software.amazon.awssdk.services.opensearchserverless.model.CreateLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateLifecyclePolicyResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private static final String MOCK_LIFECYCLE_POLICY_TYPE = LifecyclePolicyType.RETENTION.name();
    private static final String MOCK_LIFECYCLE_POLICY_NAME = "lifecycle-policy-name";
    private static final String MOCK_LIFECYCLE_POLICY_DESCRIPTION = "Lifecycle policy description";
    private static final Document MOCK_LIFECYCLE_POLICY_DOCUMENT = Document.fromString("Lifecycle Policy Document");
    private OpenSearchServerlessClient openSearchServerlessClient;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
    }

    @AfterEach
    public void tear_down() {
        verify(openSearchServerlessClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler(openSearchServerlessClient);

        final ResourceModel expectedModel = ResourceModel.builder()
            .name(MOCK_LIFECYCLE_POLICY_NAME)
            .type(MOCK_LIFECYCLE_POLICY_TYPE)
            .description(MOCK_LIFECYCLE_POLICY_DESCRIPTION)
            .policy(MOCK_LIFECYCLE_POLICY_DOCUMENT.toString())
            .build();

        final ResourceModel model = ResourceModel.builder()
            .name(MOCK_LIFECYCLE_POLICY_NAME)
            .type(MOCK_LIFECYCLE_POLICY_TYPE)
            .description(MOCK_LIFECYCLE_POLICY_DESCRIPTION)
            .policy(MOCK_LIFECYCLE_POLICY_DOCUMENT.toString())
            .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final CreateLifecyclePolicyResponse createLifecyclePolicyResponse = CreateLifecyclePolicyResponse.builder()
            .lifecyclePolicyDetail(LifecyclePolicyDetail.builder()
                .name(MOCK_LIFECYCLE_POLICY_NAME)
                .type(MOCK_LIFECYCLE_POLICY_TYPE)
                .description(MOCK_LIFECYCLE_POLICY_DESCRIPTION)
                .policy(MOCK_LIFECYCLE_POLICY_DOCUMENT)
                .build()
            )
            .build();
        when(openSearchServerlessClient.createLifecyclePolicy(any(CreateLifecyclePolicyRequest.class))).thenReturn(createLifecyclePolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
