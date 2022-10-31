package software.amazon.opensearchserverless.accountsettings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    private static final String MOCK_ACCOUNT_ID = "123456789012";
    private static final String MOCK_ACCOUNT_ID_1 = "123456789013";

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
    public void tear_down(org.junit.jupiter.api.TestInfo testInfo) {
        if (!testInfo.getTags().contains("skipSdkInteraction")) {
            verify(openSearchServerlessClient, atLeastOnce()).serviceName();
            verifyNoMoreInteractions(openSearchServerlessClient);
        }
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_SimpleSuccess() {
        final DeleteHandler handler = new DeleteHandler();

        final ResourceModel model = ResourceModel.builder()
            .accountId(MOCK_ACCOUNT_ID)
            .build();

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(MOCK_ACCOUNT_ID)
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(null);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_IncorrectAccountId_NotFound() {
        final DeleteHandler handler = new DeleteHandler();

        final ResourceModel model = ResourceModel.builder()
            .accountId(MOCK_ACCOUNT_ID_1)
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
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
