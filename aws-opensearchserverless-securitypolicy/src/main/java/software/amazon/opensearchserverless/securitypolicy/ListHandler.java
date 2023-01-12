package software.amazon.opensearchserverless.securitypolicy;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityPoliciesRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityPoliciesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ListHandler extends BaseHandlerStd {

    public ListHandler() {
        super();
    }

    public ListHandler(OpenSearchServerlessClient openSearchServerlessClient) {
        super(openSearchServerlessClient);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();
        final ListSecurityPoliciesRequest listSecurityPoliciesRequest =
            Translator.translateToListRequest(model, request.getNextToken());
        final ListSecurityPoliciesResponse listSecurityPoliciesResponse = proxy.injectCredentialsAndInvokeV2(
            listSecurityPoliciesRequest, proxyClient.client()::listSecurityPolicies);
        String nextToken = listSecurityPoliciesResponse.nextToken();
        final List<ResourceModel> models = Translator.translateFromListRequest(listSecurityPoliciesResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .nextToken(nextToken)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
