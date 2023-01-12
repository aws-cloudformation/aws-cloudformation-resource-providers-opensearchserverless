package software.amazon.opensearchserverless.securityconfig;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityConfigRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityConfigResponse;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityConfigDetail;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityConfigType;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    private static final String MOCK_SECURITY_CONFIG_ID = "1";
    private static final String MOCK_SECURITY_CONFIG_DESCRIPTION = "Security config description";
    private static final String MOCK_SECURITY_CONFIG_TYPE = SecurityConfigType.SAML.toString();
    private static final String MOCK_SECURITY_CONFIG_VERSION = "securityconfigversion";
    private static final String MOCK_METADATA = "metadata";
    private static final String MOCK_USER_ATTRIBUTE = "user-attribute";
    private static final String MOCK_GROUP_ATTRIBUTE = "group-attribute";
    private static final int MOCK_SESSION_TIMEOUT = 1;
    private static final SamlConfigOptions MOCK_SAML_OPTIONS = SamlConfigOptions.builder()
        .metadata(MOCK_METADATA)
        .userAttribute(MOCK_USER_ATTRIBUTE)
        .groupAttribute(MOCK_GROUP_ATTRIBUTE)
        .sessionTimeout(MOCK_SESSION_TIMEOUT)
        .build();
    private static final software.amazon.awssdk.services.opensearchserverless.model.SamlConfigOptions MOCK_SDK_SAML_OPTIONS =
        software.amazon.awssdk.services.opensearchserverless.model.SamlConfigOptions.builder()
            .metadata(MOCK_METADATA)
            .userAttribute(MOCK_USER_ATTRIBUTE)
            .groupAttribute(MOCK_GROUP_ATTRIBUTE)
            .sessionTimeout(MOCK_SESSION_TIMEOUT)
            .build();
    private OpenSearchServerlessClient openSearchServerlessClient;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new ReadHandler(openSearchServerlessClient);
    }

    @AfterEach
    public void tear_down(org.junit.jupiter.api.TestInfo testInfo) {
        if(!testInfo.getTags().contains("skipSdkInteraction")) {
            verify(openSearchServerlessClient, atLeastOnce()).serviceName();
            verifyNoMoreInteractions(openSearchServerlessClient);
        }
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel expectedModel = ResourceModel.builder()
            .id(MOCK_SECURITY_CONFIG_ID)
            .type(MOCK_SECURITY_CONFIG_TYPE)
            .description(MOCK_SECURITY_CONFIG_DESCRIPTION)
            .samlOptions(MOCK_SAML_OPTIONS)
            .build();

        final ResourceModel model = ResourceModel.builder().id(MOCK_SECURITY_CONFIG_ID).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();
        final GetSecurityConfigResponse getSecurityConfigResponse =
            GetSecurityConfigResponse.builder().securityConfigDetail(
                    SecurityConfigDetail.builder()
                        .id(MOCK_SECURITY_CONFIG_ID)
                        .configVersion(MOCK_SECURITY_CONFIG_VERSION)
                        .description(MOCK_SECURITY_CONFIG_DESCRIPTION)
                        .samlOptions(MOCK_SDK_SAML_OPTIONS)
                        .build())
                .build();
        when(openSearchServerlessClient.getSecurityConfig(any(GetSecurityConfigRequest.class)))
            .thenReturn(getSecurityConfigResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(openSearchServerlessClient).getSecurityConfig(any(GetSecurityConfigRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WithoutId_Fail() {
        final ResourceModel desiredResourceModel = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(desiredResourceModel)
            .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
