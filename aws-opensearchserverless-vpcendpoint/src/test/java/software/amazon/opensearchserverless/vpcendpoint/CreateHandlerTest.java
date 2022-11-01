package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.CreateVpcEndpointDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CreateVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointDetail;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointStatus;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import com.google.common.collect.ImmutableList;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private static final String MOCK_VPC_ENDPOINT_ID = "id";
    private static final String MOCK_VPC_ENDPOINT_NAME = "vpcendpoint-name";
    private static final String MOCK_VPC_ENDPOINT_VPC_ID = "vpcid";
    private static final List<String> MOCK_VPC_ENDPOINT_SUBNET_IDS = ImmutableList.of("subnetid1", "subnetid2");
    private static final List<String> MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS = ImmutableList.of("securitygroupid", "securitygroupid");
    private static final long MOCK_CREATED_DATE = 1234567;

    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private OpenSearchServerlessClient openSearchServerlessClient;
    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new CreateHandler();
    }

    @AfterEach
    public void tear_down(org.junit.jupiter.api.TestInfo testInfo) {
        if (!testInfo.getTags().contains("skipSdkInteraction")) {
            verify(openSearchServerlessClient, atLeastOnce()).serviceName();
            verifyNoMoreInteractions(openSearchServerlessClient);
        }
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateVpcEndpointResponse createVpcEndpointResponse =
                CreateVpcEndpointResponse.builder().createVpcEndpointDetail(
                                                 CreateVpcEndpointDetail.builder().id(MOCK_VPC_ENDPOINT_ID).build())
                                         .build();
        when(openSearchServerlessClient.createVpcEndpoint(any(CreateVpcEndpointRequest.class)))
                .thenReturn(createVpcEndpointResponse);

        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse =
                BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(
                                                   VpcEndpointDetail.builder()
                                                                    .id(MOCK_VPC_ENDPOINT_ID)
                                                                    .name(MOCK_VPC_ENDPOINT_NAME)
                                                                    .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                                    .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                                    .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                                    .createdDate(MOCK_CREATED_DATE)
                                                                    .status(VpcEndpointStatus.ACTIVE)
                                                                    .build())
                                           .build();
        when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
                .thenReturn(batchGetVpcEndpointResponse);

        final ResourceModel requestModel = ResourceModel.builder()
                                                        .name(MOCK_VPC_ENDPOINT_NAME)
                                                        .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                        .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
                                                                                    desiredResourceState(requestModel)
                                                                                    .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        final ResourceModel expectedModel = ResourceModel.builder()
                                                         .id(MOCK_VPC_ENDPOINT_ID)
                                                         .name(MOCK_VPC_ENDPOINT_NAME)
                                                         .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                         .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                         .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                         .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(openSearchServerlessClient).createVpcEndpoint(any(CreateVpcEndpointRequest.class));
    }

    @Test
    public void handleRequest_Stabilization_Success() {
        final CreateVpcEndpointResponse createVpcEndpointResponse =
                CreateVpcEndpointResponse.builder().createVpcEndpointDetail(
                                                 CreateVpcEndpointDetail.builder()
                                                                        .id(MOCK_VPC_ENDPOINT_ID)
                                                                        .build())
                                         .build();
        when(openSearchServerlessClient.createVpcEndpoint(any(CreateVpcEndpointRequest.class)))
                .thenReturn(createVpcEndpointResponse);

        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse1 =
                BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(
                                                   VpcEndpointDetail.builder()
                                                                    .id(MOCK_VPC_ENDPOINT_ID)
                                                                    .name(MOCK_VPC_ENDPOINT_NAME)
                                                                    .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                                    .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                                    .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                                    .createdDate(MOCK_CREATED_DATE)
                                                                    .status(VpcEndpointStatus.PENDING)
                                                                    .build())
                                           .build();
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse2 =
                BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(
                                                   VpcEndpointDetail.builder()
                                                                    .id(MOCK_VPC_ENDPOINT_ID)
                                                                    .name(MOCK_VPC_ENDPOINT_NAME)
                                                                    .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                                    .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                                    .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                                    .createdDate(MOCK_CREATED_DATE)
                                                                    .status(VpcEndpointStatus.ACTIVE)
                                                                    .build())
                                           .build();
        when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
                .thenReturn(batchGetVpcEndpointResponse1, batchGetVpcEndpointResponse2, batchGetVpcEndpointResponse2);

        final ResourceModel requestModel = ResourceModel.builder()
                                                        .name(MOCK_VPC_ENDPOINT_NAME)
                                                        .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                        .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
                                                                                    desiredResourceState(requestModel)
                                                                                    .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        final ResourceModel expectedModel = ResourceModel.builder()
                                                         .id(MOCK_VPC_ENDPOINT_ID)
                                                         .name(MOCK_VPC_ENDPOINT_NAME)
                                                         .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                         .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                         .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                         .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(openSearchServerlessClient).createVpcEndpoint(any(CreateVpcEndpointRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WithId_Fail() {
        final ResourceModel desiredResourceModel = ResourceModel.builder().id(MOCK_VPC_ENDPOINT_ID).build();
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

    @Test
    public void handleRequest_ResourceAlreadyExists_Fail() {
        when(openSearchServerlessClient.createVpcEndpoint(any(CreateVpcEndpointRequest.class)))
                .thenThrow(ConflictException.builder().build());

        final ResourceModel requestModel = ResourceModel.builder()
                                                        .name(MOCK_VPC_ENDPOINT_NAME)
                                                        .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                        .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
                                                                                    desiredResourceState(requestModel)
                                                                                    .build();

        assertThrows(CfnAlreadyExistsException.class,
                     () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(openSearchServerlessClient).createVpcEndpoint(any(CreateVpcEndpointRequest.class));
    }

    @Test
    public void handleRequest_ValidationFailure_Fail() {
        when(openSearchServerlessClient.createVpcEndpoint(any(CreateVpcEndpointRequest.class)))
                .thenThrow(ValidationException.builder().build());

        final ResourceModel requestModel = ResourceModel.builder()
                                                        .name(MOCK_VPC_ENDPOINT_NAME)
                                                        .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                        .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
                                                                                    desiredResourceState(requestModel)
                                                                                    .build();

        assertThrows(CfnInvalidRequestException.class,
                     () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(openSearchServerlessClient).createVpcEndpoint(any(CreateVpcEndpointRequest.class));
    }

    @Test
    public void handleRequest_NotStabilized_Fail() {
        final CreateVpcEndpointResponse createVpcEndpointResponse =
                CreateVpcEndpointResponse.builder().createVpcEndpointDetail(
                                                 CreateVpcEndpointDetail.builder()
                                                                        .id(MOCK_VPC_ENDPOINT_ID)
                                                                        .build())
                                         .build();
        when(openSearchServerlessClient.createVpcEndpoint(any(CreateVpcEndpointRequest.class)))
                .thenReturn(createVpcEndpointResponse);

        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse =
                BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(
                                                   VpcEndpointDetail.builder()
                                                                    .id(MOCK_VPC_ENDPOINT_ID)
                                                                    .name(MOCK_VPC_ENDPOINT_NAME)
                                                                    .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                                    .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                                    .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                                    .createdDate(MOCK_CREATED_DATE)
                                                                    .status(VpcEndpointStatus.FAILED)
                                                                    .build())
                                           .build();
        when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
                .thenReturn(batchGetVpcEndpointResponse);

        final ResourceModel requestModel = ResourceModel.builder()
                                                        .name(MOCK_VPC_ENDPOINT_NAME)
                                                        .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                        .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
                                                                                    desiredResourceState(requestModel)
                                                                                    .build();

        assertThrows(CfnNotStabilizedException.class,
                     () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(openSearchServerlessClient).createVpcEndpoint(any(CreateVpcEndpointRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WithoutName_Fail() {
        final ResourceModel desiredResourceModel = ResourceModel.builder()
                                                                .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                                                                .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                                .build();
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

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WithoutVPCId_Fail() {
        final ResourceModel desiredResourceModel = ResourceModel.builder()
                                                                .name(MOCK_VPC_ENDPOINT_NAME)
                                                                .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                                .build();
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
