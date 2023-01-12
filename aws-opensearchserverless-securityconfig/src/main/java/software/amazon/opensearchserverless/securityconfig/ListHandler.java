package software.amazon.opensearchserverless.securityconfig;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityConfigsRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityConfigsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import com.amazonaws.util.StringUtils;

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

        final ResourceModel model = request.getDesiredResourceState();

        if(StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Type cannot be empty");
        }

        final ListSecurityConfigsRequest listSecurityConfigsRequest = Translator.translateToListRequest(model, request.getNextToken());
        final ListSecurityConfigsResponse listSecurityConfigsResponse = proxy.injectCredentialsAndInvokeV2(listSecurityConfigsRequest, proxyClient.client()::listSecurityConfigs);
        String nextToken = listSecurityConfigsResponse.nextToken();
        final List<ResourceModel> models = Translator.translateFromListRequest(listSecurityConfigsResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModels(models)
                            .nextToken(nextToken)
                            .status(OperationStatus.SUCCESS)
                            .build();
    }
}
