package software.amazon.opensearchserverless.accesspolicy;

import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.AccessPolicyDetail;
import software.amazon.awssdk.services.opensearchserverless.model.AccessPolicyType;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.CreateAccessPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateAccessPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private static final String MOCK_ACCESS_POLICY_TYPE = AccessPolicyType.DATA.name();
    private static final String MOCK_ACCESS_POLICY_NAME = "access-policy-name";
    private static final String MOCK_ACCESS_POLICY_DESCRIPTION = "Access policy description";
    private static final Document MOCK_ACCESS_POLICY_DOCUMENT = Document.fromString("Access Policy Document");
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
                                                         .name(MOCK_ACCESS_POLICY_NAME)
                                                         .type(MOCK_ACCESS_POLICY_TYPE)
                                                         .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                                                         .policy(MOCK_ACCESS_POLICY_DOCUMENT.toString())
                                                         .build();

        final ResourceModel model = ResourceModel.builder()
                                                 .name(MOCK_ACCESS_POLICY_NAME)
                                                 .type(MOCK_ACCESS_POLICY_TYPE)
                                                 .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                                                 .policy(MOCK_ACCESS_POLICY_DOCUMENT.toString())
                                                 .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final CreateAccessPolicyResponse createAccessPolicyResponse =
                CreateAccessPolicyResponse.builder()
                                          .accessPolicyDetail(
                                                  AccessPolicyDetail.builder()
                                                                    .name(MOCK_ACCESS_POLICY_NAME)
                                                                    .type(MOCK_ACCESS_POLICY_TYPE)
                                                                    .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                                                                    .policy(MOCK_ACCESS_POLICY_DOCUMENT)
                                                                    .build()
                                                             )
                                          .build();
        when(openSearchServerlessClient.createAccessPolicy(any(CreateAccessPolicyRequest.class))).thenReturn(createAccessPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }


    @Test
    public void handleRequest_AlreadyExists_Fail() {
        when(openSearchServerlessClient.createAccessPolicy(any(CreateAccessPolicyRequest.class)))
                .thenThrow(ConflictException.builder()
                        .message(String.format("Policy with name %s and type %s already exists",
                                MOCK_ACCESS_POLICY_NAME, MOCK_ACCESS_POLICY_TYPE))
                        .build());

        final CreateHandler handler = new CreateHandler(openSearchServerlessClient);

        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_ACCESS_POLICY_NAME)
                .type(MOCK_ACCESS_POLICY_TYPE)
                .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                .policy(MOCK_ACCESS_POLICY_DOCUMENT.toString())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        assertThrows(CfnAlreadyExistsException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).createAccessPolicy(any(CreateAccessPolicyRequest.class));
    }

    @Test
    public void handleRequest_ConflictException_Fail() {
        when(openSearchServerlessClient.createAccessPolicy(any(CreateAccessPolicyRequest.class)))
                .thenThrow(ConflictException.builder().build());

        final CreateHandler handler = new CreateHandler(openSearchServerlessClient);

        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_ACCESS_POLICY_NAME)
                .type(MOCK_ACCESS_POLICY_TYPE)
                .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                .policy(MOCK_ACCESS_POLICY_DOCUMENT.toString())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).createAccessPolicy(any(CreateAccessPolicyRequest.class));
    }

    @Test
    public void handleRequest_ValidationException_Fail() {
        when(openSearchServerlessClient.createAccessPolicy(any(CreateAccessPolicyRequest.class)))
                .thenThrow(ValidationException.builder().build());

        final CreateHandler handler = new CreateHandler(openSearchServerlessClient);

        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_ACCESS_POLICY_NAME)
                .type(MOCK_ACCESS_POLICY_TYPE)
                .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                .policy(MOCK_ACCESS_POLICY_DOCUMENT.toString())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).createAccessPolicy(any(CreateAccessPolicyRequest.class));
    }

    @Test
    public void handleRequest_ServiceQuotaExceededException_Fail() {
        when(openSearchServerlessClient.createAccessPolicy(any(CreateAccessPolicyRequest.class)))
                .thenThrow(ServiceQuotaExceededException.builder().build());

        final CreateHandler handler = new CreateHandler(openSearchServerlessClient);

        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_ACCESS_POLICY_NAME)
                .type(MOCK_ACCESS_POLICY_TYPE)
                .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                .policy(MOCK_ACCESS_POLICY_DOCUMENT.toString())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        assertThrows(CfnServiceLimitExceededException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).createAccessPolicy(any(CreateAccessPolicyRequest.class));
    }

    @Test
    public void handleRequest_InternalServerException_Fail() {
        when(openSearchServerlessClient.createAccessPolicy(any(CreateAccessPolicyRequest.class)))
                .thenThrow(InternalServerException.builder().build());

        final CreateHandler handler = new CreateHandler(openSearchServerlessClient);

        final ResourceModel model = ResourceModel.builder()
                .name(MOCK_ACCESS_POLICY_NAME)
                .type(MOCK_ACCESS_POLICY_TYPE)
                .description(MOCK_ACCESS_POLICY_DESCRIPTION)
                .policy(MOCK_ACCESS_POLICY_DOCUMENT.toString())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        assertThrows(CfnServiceInternalErrorException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(openSearchServerlessClient).createAccessPolicy(any(CreateAccessPolicyRequest.class));
    }
}
