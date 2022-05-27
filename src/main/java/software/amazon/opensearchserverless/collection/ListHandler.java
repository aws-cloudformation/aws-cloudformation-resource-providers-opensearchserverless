package software.amazon.opensearchserverless.collection;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ListCollectionsRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListCollectionsResponse;
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

        final ListCollectionsRequest listCollectionsRequest = ListCollectionsRequest.builder().nextToken(request.getNextToken()).build();
        final ListCollectionsResponse listCollectionsResponse = proxy.injectCredentialsAndInvokeV2(listCollectionsRequest, proxyClient.client()::listCollections);
        final List<ResourceModel> models = Translator.translateFromListRequest(listCollectionsResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .nextToken(listCollectionsResponse.nextToken())
            .status(OperationStatus.SUCCESS)
            .build();
    }

}
