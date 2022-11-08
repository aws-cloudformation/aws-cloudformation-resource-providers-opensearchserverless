package software.amazon.opensearchserverless.accesspolicy;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.AccessPolicyDetail;
import software.amazon.awssdk.services.opensearchserverless.model.AccessPolicyType;
import software.amazon.awssdk.services.opensearchserverless.model.GetAccessPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetAccessPolicyResponse;
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
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    private static final String MOCK_ACCESS_POLICY_TYPE = AccessPolicyType.DATA.name();
    private static final String MOCK_ACCESS_POLICY_NAME = "access-policy-name";
    private static final String MOCK_ACCESS_POLICY_DESCRIPTION = "Access policy description";
    private static final String MOCK_ACCESS_POLICY_DOCUMENT = "Access Policy Document";
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
        final ReadHandler handler = new ReadHandler();

        final ResourceModel expectedModel = ResourceModel.builder()
                                                         .name(MOCK_ACCESS_POLICY_NAME)
                                                         .type(MOCK_ACCESS_POLICY_TYPE)
                                                         .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                                                         .policy(MOCK_ACCESS_POLICY_DOCUMENT)
                                                         .build();

        final ResourceModel model = ResourceModel.builder()
                                                 .name(MOCK_ACCESS_POLICY_NAME)
                                                 .type(MOCK_ACCESS_POLICY_TYPE)
                                                 .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final GetAccessPolicyResponse getAccessPolicyResponse =
                GetAccessPolicyResponse.builder()
                                       .accessPolicyDetail(
                                               AccessPolicyDetail.builder()
                                                                 .name(MOCK_ACCESS_POLICY_NAME)
                                                                 .type(MOCK_ACCESS_POLICY_TYPE)
                                                                 .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                                                                 .policy(MOCK_ACCESS_POLICY_DOCUMENT)
                                                                 .build()
                                                          ).build();
        when(openSearchServerlessClient.getAccessPolicy(any(GetAccessPolicyRequest.class))).thenReturn(getAccessPolicyResponse);

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
