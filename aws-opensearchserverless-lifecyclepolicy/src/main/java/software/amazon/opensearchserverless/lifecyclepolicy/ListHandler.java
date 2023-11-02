package software.amazon.opensearchserverless.lifecyclepolicy;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ListLifecyclePoliciesRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListLifecyclePoliciesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
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
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request,
                                                                       final CallbackContext callbackContext,
                                                                       final ProxyClient<OpenSearchServerlessClient> proxyClient,
                                                                       final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if (StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Type cannot be empty");
        }

        final ListLifecyclePoliciesRequest listLifecyclePoliciesRequest = Translator.translateToListRequest(model, request.getNextToken());
        final ListLifecyclePoliciesResponse listLifecyclePoliciesResponse = proxy.injectCredentialsAndInvokeV2(listLifecyclePoliciesRequest, proxyClient.client()::listLifecyclePolicies);
        final String nextToken = listLifecyclePoliciesResponse.nextToken();
        final List<ResourceModel> models = Translator.translateFromListRequest(listLifecyclePoliciesResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .nextToken(nextToken)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
