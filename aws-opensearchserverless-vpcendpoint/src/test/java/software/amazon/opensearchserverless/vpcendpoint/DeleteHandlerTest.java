package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointDetail;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointStatus;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import com.google.common.collect.ImmutableList;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    private static final String MOCK_VPC_ENDPOINT_ID = "id";
    private static final String MOCK_VPC_ENDPOINT_NAME = "vpcendpoint-name";
    private static final String MOCK_VPC_ENDPOINT_VPC_ID = "vpcid";
    private static final List<String> MOCK_VPC_ENDPOINT_SUBNET_IDS = ImmutableList.of("subnetid1", "subnetid2");
    private static final List<String> MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS = ImmutableList.of("securitygroupid", "securitygroupid");
    private static final long MOCK_CREATED_DATE = 1234567;

    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private OpenSearchServerlessClient openSearchServerlessClient;
    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = Mockito.mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new DeleteHandler();
    }

    @AfterEach
    public void tear_down(org.junit.jupiter.api.TestInfo testInfo) {
        if (!testInfo.getTags().contains("skipSdkInteraction")) {
            Mockito.verify(openSearchServerlessClient, Mockito.atLeastOnce()).serviceName();
            Mockito.verifyNoMoreInteractions(openSearchServerlessClient);
        }
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DeleteVpcEndpointResponse deleteVpcEndpointResponse = DeleteVpcEndpointResponse.builder().build();
        Mockito.when(openSearchServerlessClient.deleteVpcEndpoint(any(DeleteVpcEndpointRequest.class)))
               .thenReturn(deleteVpcEndpointResponse);

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
                BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(Collections.emptyList()).build();
        Mockito.when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
               .thenReturn(batchGetVpcEndpointResponse1, batchGetVpcEndpointResponse2);

        final ResourceModel requestModel = ResourceModel.builder().id(MOCK_VPC_ENDPOINT_ID).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                                    .desiredResourceState(requestModel)
                                                                                    .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        Mockito.verify(openSearchServerlessClient).deleteVpcEndpoint(any(DeleteVpcEndpointRequest.class));
    }

    @Test
    public void handleRequest_Stabilization_Success() {
        final DeleteVpcEndpointResponse deleteVpcEndpointResponse = DeleteVpcEndpointResponse.builder().build();
        Mockito.when(openSearchServerlessClient.deleteVpcEndpoint(any(DeleteVpcEndpointRequest.class)))
               .thenReturn(deleteVpcEndpointResponse);

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
                                                                    .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                                    .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                                    .createdDate(MOCK_CREATED_DATE)
                                                                    .status(VpcEndpointStatus.DELETING)
                                                                    .build())
                                           .build();
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse3 =
                BatchGetVpcEndpointResponse.builder().vpcEndpointDetails(Collections.emptyList()).build();
        Mockito.when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
               .thenReturn(batchGetVpcEndpointResponse1, batchGetVpcEndpointResponse2, batchGetVpcEndpointResponse3);

        final ResourceModel requestModel = ResourceModel.builder().id(MOCK_VPC_ENDPOINT_ID).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                                    .desiredResourceState(requestModel)
                                                                                    .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        Mockito.verify(openSearchServerlessClient).deleteVpcEndpoint(any(DeleteVpcEndpointRequest.class));
    }

    @Test
    public void handleRequest_NotStabilized_Fail() {
        final DeleteVpcEndpointResponse deleteVpcEndpointResponse = DeleteVpcEndpointResponse.builder().build();
        Mockito.when(openSearchServerlessClient.deleteVpcEndpoint(any(DeleteVpcEndpointRequest.class)))
               .thenReturn(deleteVpcEndpointResponse);

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
                                                                    .subnetIds(MOCK_VPC_ENDPOINT_SUBNET_IDS)
                                                                    .securityGroupIds(MOCK_VPC_ENDPOINT_SECURITY_GROUP_IDS)
                                                                    .createdDate(MOCK_CREATED_DATE)
                                                                    .status(VpcEndpointStatus.DELETING)
                                                                    .build())
                                           .build();
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse3 =
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
        Mockito.when(openSearchServerlessClient.batchGetVpcEndpoint(any(BatchGetVpcEndpointRequest.class)))
               .thenReturn(batchGetVpcEndpointResponse1, batchGetVpcEndpointResponse2, batchGetVpcEndpointResponse3);

        final ResourceModel requestModel = ResourceModel.builder().id(MOCK_VPC_ENDPOINT_ID).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                                    .desiredResourceState(requestModel)
                                                                                    .build();

        assertThrows(CfnNotStabilizedException.class,
                     () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        Mockito.verify(openSearchServerlessClient).deleteVpcEndpoint(any(DeleteVpcEndpointRequest.class));
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
