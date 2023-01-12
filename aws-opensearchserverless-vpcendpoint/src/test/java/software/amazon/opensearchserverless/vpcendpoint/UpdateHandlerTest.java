package software.amazon.opensearchserverless.vpcendpoint;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateVpcEndpointDetail;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointDetail;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointStatus;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    private static final String MOCK_VPC_ENDPOINT_ID = "id";
    private static final String MOCK_VPC_ENDPOINT_NAME = "vpcendpoint-name";
    private static final String MOCK_VPC_ENDPOINT_VPC_ID = "vpcid";
    private static final List<String> MOCK_VPC_ENDPOINT_SUBNET_IDS = ImmutableList.of("subnetid1", "subnetid2");
    private static final List<String> MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS = ImmutableList.of("securitygroupid1",
        "securitygroupid1");

    private static final List<String> MOCK_VPC_ENDPOINT_SUBNET_IDS_1 = ImmutableList.of("subnetid1", "subnetid3");

    private static final List<String> MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1 = ImmutableList.of("securitygroupid1",
        "securitygroupid3");
    private static final long MOCK_CREATED_DATE = 1234567;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private OpenSearchServerlessClient openSearchServerlessClient;
    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new UpdateHandler(openSearchServerlessClient);
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
        final UpdateVpcEndpointResponse updateVpcEndpointResponse =
                UpdateVpcEndpointResponse.builder().updateVpcEndpointDetail(
                    UpdateVpcEndpointDetail.builder()
                        .id(MOCK_VPC_ENDPOINT_ID)
                        .name(MOCK_VPC_ENDPOINT_NAME)
                        .status(VpcEndpointStatus.PENDING)
                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
                        .build())
                    .build();
        when(openSearchServerlessClient.updateVpcEndpoint(any(UpdateVpcEndpointRequest.class)))
                .thenReturn(updateVpcEndpointResponse);

        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse1 =
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
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse2 =
            BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(
                    VpcEndpointDetail.builder()
                        .id(MOCK_VPC_ENDPOINT_ID)
                        .name(MOCK_VPC_ENDPOINT_NAME)
                        .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
                        .createdDate(MOCK_CREATED_DATE)
                        .status(VpcEndpointStatus.ACTIVE)
                        .build())
                .build();
        when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
                .thenReturn(batchGetVpcEndpointResponse1, batchGetVpcEndpointResponse2, batchGetVpcEndpointResponse2);

        final ResourceModel requestModel = ResourceModel.builder()
            .id(MOCK_VPC_ENDPOINT_ID)
            .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
            .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
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
                                                         .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
                                                         .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
                                                         .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(openSearchServerlessClient).updateVpcEndpoint(any(UpdateVpcEndpointRequest.class));
    }

    @Test
    public void handleRequest_Stabilization_Success() {
        final UpdateVpcEndpointResponse updateVpcEndpointResponse =
            UpdateVpcEndpointResponse.builder().updateVpcEndpointDetail(
                    UpdateVpcEndpointDetail.builder()
                        .id(MOCK_VPC_ENDPOINT_ID)
                        .name(MOCK_VPC_ENDPOINT_NAME)
                        .status(VpcEndpointStatus.PENDING)
                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
                        .build())
                .build();
        when(openSearchServerlessClient.updateVpcEndpoint(any(UpdateVpcEndpointRequest.class)))
            .thenReturn(updateVpcEndpointResponse);

        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse1 =
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
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse2 =
            BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(
                    VpcEndpointDetail.builder()
                        .id(MOCK_VPC_ENDPOINT_ID)
                        .name(MOCK_VPC_ENDPOINT_NAME)
                        .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
                        .createdDate(MOCK_CREATED_DATE)
                        .status(VpcEndpointStatus.PENDING)
                        .build())
                .build();
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse3 =
            BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(
                    VpcEndpointDetail.builder()
                        .id(MOCK_VPC_ENDPOINT_ID)
                        .name(MOCK_VPC_ENDPOINT_NAME)
                        .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
                        .createdDate(MOCK_CREATED_DATE)
                        .status(VpcEndpointStatus.ACTIVE)
                        .build())
                .build();
        when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
            .thenReturn(batchGetVpcEndpointResponse1, batchGetVpcEndpointResponse2, batchGetVpcEndpointResponse3,
                batchGetVpcEndpointResponse3);

        final ResourceModel requestModel = ResourceModel.builder()
            .id(MOCK_VPC_ENDPOINT_ID)
            .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
            .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
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
            .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
            .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
            .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(openSearchServerlessClient).updateVpcEndpoint(any(UpdateVpcEndpointRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_WithId_Fail() {
        final ResourceModel desiredResourceModel = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(desiredResourceModel)
            .build();
        assertThrows(CfnInvalidRequestException.class,
            () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_ResourceNotFound_Fail() {
        when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
            .thenReturn(BatchGetVpcEndpointResponse.builder().build());

        final ResourceModel requestModel = ResourceModel.builder()
            .id(MOCK_VPC_ENDPOINT_ID)
            .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
            .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
            .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
            desiredResourceState(requestModel)
            .build();

        assertThrows(ResourceNotFoundException.class,
                     () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_ValidationFailure_Fail() {
        when(openSearchServerlessClient.updateVpcEndpoint(any(UpdateVpcEndpointRequest.class)))
            .thenThrow(ValidationException.builder().build());

        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse1 =
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
            .thenReturn(batchGetVpcEndpointResponse1);

        final ResourceModel requestModel = ResourceModel.builder()
            .id(MOCK_VPC_ENDPOINT_ID)
            .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
            .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
            .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
            desiredResourceState(requestModel)
            .build();
        assertThrows(CfnInvalidRequestException.class,
                     () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(openSearchServerlessClient).updateVpcEndpoint(any(UpdateVpcEndpointRequest.class));
    }

    @Test
    public void handleRequest_NotStabilized_Fail() {
        final UpdateVpcEndpointResponse updateVpcEndpointResponse =
            UpdateVpcEndpointResponse.builder().updateVpcEndpointDetail(
                    UpdateVpcEndpointDetail.builder()
                        .id(MOCK_VPC_ENDPOINT_ID)
                        .name(MOCK_VPC_ENDPOINT_NAME)
                        .status(VpcEndpointStatus.PENDING)
                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
                        .build())
                .build();
        when(openSearchServerlessClient.updateVpcEndpoint(any(UpdateVpcEndpointRequest.class)))
            .thenReturn(updateVpcEndpointResponse);

        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse1 =
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
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse2 =
            BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(
                    VpcEndpointDetail.builder()
                        .id(MOCK_VPC_ENDPOINT_ID)
                        .name(MOCK_VPC_ENDPOINT_NAME)
                        .vpcId(MOCK_VPC_ENDPOINT_VPC_ID)
                        .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
                        .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
                        .createdDate(MOCK_CREATED_DATE)
                        .status(VpcEndpointStatus.FAILED)
                        .build())
                .build();
        when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
            .thenReturn(batchGetVpcEndpointResponse1, batchGetVpcEndpointResponse2);

        final ResourceModel requestModel = ResourceModel.builder()
            .id(MOCK_VPC_ENDPOINT_ID)
            .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS_1)
            .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS_1)
            .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
            desiredResourceState(requestModel)
            .build();

        assertThrows(CfnNotStabilizedException.class,
                     () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(openSearchServerlessClient).updateVpcEndpoint(any(UpdateVpcEndpointRequest.class));
    }

}
