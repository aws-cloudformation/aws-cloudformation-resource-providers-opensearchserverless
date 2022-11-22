package software.amazon.opensearchserverless.securityconfig;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityConfigsRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityConfigsResponse;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityConfigSummary;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityConfigType;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import com.google.common.collect.ImmutableList;

import java.time.Duration;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private static final String MOCK_SECURITY_CONFIG_ID_1 = "1";
    private static final String MOCK_SECURITY_CONFIG_ID_2 = "2";
    private static final String MOCK_SECURITY_CONFIG_DESCRIPTION_1 = "Security config description 1";
    private static final String MOCK_SECURITY_CONFIG_DESCRIPTION_2 = "Security config description 2";
    private static final String MOCK_SECURITY_CONFIG_TYPE = SecurityConfigType.SAML.name();
    public static final String MOCK_NEXT_TOKEN = "mock_next_token";

    private OpenSearchServerlessClient openSearchServerlessClient;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private ListHandler handler;


    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new ListHandler();
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final Collection<SecurityConfigSummary> securityConfigSummaries = ImmutableList.of(
            SecurityConfigSummary.builder().id(MOCK_SECURITY_CONFIG_ID_1)
                .description(MOCK_SECURITY_CONFIG_DESCRIPTION_1)
                .build(),
            SecurityConfigSummary.builder().id(MOCK_SECURITY_CONFIG_ID_2)
                .description(MOCK_SECURITY_CONFIG_DESCRIPTION_2)
                .build()
        );
        final ListSecurityConfigsResponse listSecurityConfigsResponse =
            ListSecurityConfigsResponse.builder().securityConfigSummaries(securityConfigSummaries).build();
        when(openSearchServerlessClient.listSecurityConfigs(any(ListSecurityConfigsRequest.class))).thenReturn(listSecurityConfigsResponse);

        final ResourceModel model = ResourceModel.builder().type(MOCK_SECURITY_CONFIG_TYPE).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).hasSize(2);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(openSearchServerlessClient).listSecurityConfigs(any(ListSecurityConfigsRequest.class));
    }

    @Test
    public void handleRequest_RetrunsNextToken() {
        final Collection<SecurityConfigSummary> securityConfigSummaries = ImmutableList.of(
            SecurityConfigSummary.builder().id(MOCK_SECURITY_CONFIG_ID_1)
                .description(MOCK_SECURITY_CONFIG_DESCRIPTION_1)
                .build(),
            SecurityConfigSummary.builder().id(MOCK_SECURITY_CONFIG_ID_2)
                .description(MOCK_SECURITY_CONFIG_DESCRIPTION_2)
                .build()
        );
        final ListSecurityConfigsResponse listSecurityConfigsResponse =
            ListSecurityConfigsResponse.builder().securityConfigSummaries(securityConfigSummaries).nextToken(MOCK_NEXT_TOKEN).build();
        when(openSearchServerlessClient.listSecurityConfigs(any(ListSecurityConfigsRequest.class))).thenReturn(listSecurityConfigsResponse);

        final ResourceModel model = ResourceModel.builder().type(MOCK_SECURITY_CONFIG_TYPE).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).hasSize(2);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isEqualTo(MOCK_NEXT_TOKEN);

        verify(openSearchServerlessClient).listSecurityConfigs(any(ListSecurityConfigsRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WithoutType_Fail() {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
