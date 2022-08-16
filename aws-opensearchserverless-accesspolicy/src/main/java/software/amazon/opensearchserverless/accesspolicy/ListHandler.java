package software.amazon.opensearchserverless.accesspolicy;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ListAccessPoliciesRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListAccessPoliciesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
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

        ResourceModel model = request.getDesiredResourceState();
        final ListAccessPoliciesRequest listAccessPoliciesRequest = Translator.translateToListRequest(model, request.getNextToken());
        final ListAccessPoliciesResponse listAccessPoliciesResponse = proxy.injectCredentialsAndInvokeV2(listAccessPoliciesRequest, proxyClient.client()::listAccessPolicies);
        String nextToken = listAccessPoliciesResponse.nextToken();
        final List<ResourceModel> models = Translator.translateFromListRequest(listAccessPoliciesResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModels(models)
                            .nextToken(nextToken)
                            .status(OperationStatus.SUCCESS)
                            .build();
    }
}
