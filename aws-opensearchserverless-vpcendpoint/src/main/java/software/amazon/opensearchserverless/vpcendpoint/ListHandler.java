package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ListVpcEndpointsRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListVpcEndpointsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {

        final ListVpcEndpointsRequest listVpcEndpointsRequest = ListVpcEndpointsRequest.builder().nextToken(request.getNextToken()).build();
        final ListVpcEndpointsResponse listVpcEndpointsResponse = proxy.injectCredentialsAndInvokeV2(listVpcEndpointsRequest, proxyClient.client()::listVpcEndpoints);
        final List<ResourceModel> models = Translator.translateFromListResponse(listVpcEndpointsResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModels(models)
                            .nextToken(listVpcEndpointsResponse.nextToken())
                            .status(OperationStatus.SUCCESS)
                            .build();
    }
}
