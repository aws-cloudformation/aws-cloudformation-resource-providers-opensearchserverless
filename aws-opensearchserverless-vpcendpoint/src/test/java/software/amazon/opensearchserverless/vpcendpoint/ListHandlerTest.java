package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ListVpcEndpointsRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListVpcEndpointsResponse;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointStatus;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointSummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    public static final String MOCK_NEXT_TOKEN = "a-bbvsdfff01011";
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<OpenSearchServerlessClient> proxyClient;
    private OpenSearchServerlessClient openSearchServerlessClient;
    private ListHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        openSearchServerlessClient = Mockito.mock(OpenSearchServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, openSearchServerlessClient);
        handler = new ListHandler(openSearchServerlessClient);
    }

    @AfterEach
    public void tear_down() {
        Mockito.verifyNoMoreInteractions(openSearchServerlessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final Collection<VpcEndpointSummary> vpcEndpointSummaries = ImmutableList.of(
                VpcEndpointSummary.builder().id("id1").status(VpcEndpointStatus.ACTIVE).build(),
                VpcEndpointSummary.builder().id("id2").status(VpcEndpointStatus.ACTIVE).build());
        final ListVpcEndpointsResponse listVpcEndpointsResponse =
                ListVpcEndpointsResponse.builder().vpcEndpointSummaries(vpcEndpointSummaries).build();
        Mockito.when(openSearchServerlessClient.listVpcEndpoints(any(ListVpcEndpointsRequest.class)))
               .thenReturn(listVpcEndpointsResponse);

        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
                                                                                    desiredResourceState(model)
                                                                                    .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).hasSize(2);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isNull();

        Mockito.verify(openSearchServerlessClient).listVpcEndpoints(any(ListVpcEndpointsRequest.class));
    }

    @Test
    public void handleRequest_Filters_ActiveState() {
        final Collection<VpcEndpointSummary> vpcEndpointSummaries = ImmutableList.of(
                VpcEndpointSummary.builder().id("id1").status(VpcEndpointStatus.ACTIVE).build(),
                VpcEndpointSummary.builder().id("id2").status(VpcEndpointStatus.PENDING).build());
        final ListVpcEndpointsResponse listVpcEndpointsResponse =
                ListVpcEndpointsResponse.builder().vpcEndpointSummaries(vpcEndpointSummaries).build();
        Mockito.when(openSearchServerlessClient.listVpcEndpoints(any(ListVpcEndpointsRequest.class)))
               .thenReturn(listVpcEndpointsResponse);

        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().
                                                                                    desiredResourceState(model)
                                                                                    .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).hasSize(1);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        Mockito.verify(openSearchServerlessClient).listVpcEndpoints(any(ListVpcEndpointsRequest.class));
    }

    @Test
    public void handleRequest_ReturnsNextToken() {
        final Collection<VpcEndpointSummary> vpcEndpointSummaries = ImmutableList.of(
                VpcEndpointSummary.builder().id("id1").status(VpcEndpointStatus.ACTIVE).build());
        final ListVpcEndpointsResponse listVpcEndpointsResponse =
                ListVpcEndpointsResponse.builder()
                                        .vpcEndpointSummaries(vpcEndpointSummaries)
                                        .nextToken(MOCK_NEXT_TOKEN)
                                        .build();
        Mockito.when(openSearchServerlessClient.listVpcEndpoints(any(ListVpcEndpointsRequest.class)))
               .thenReturn(listVpcEndpointsResponse);

        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).hasSize(1);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isEqualTo(MOCK_NEXT_TOKEN);

        Mockito.verify(openSearchServerlessClient).listVpcEndpoints(any(ListVpcEndpointsRequest.class));
    }
}
