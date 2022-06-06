package software.amazon.opensearchserverless.collection;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final @NonNull AmazonWebServicesClientProxy proxy,
        final @NonNull ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
        final @NonNull Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        if(StringUtils.isNullOrEmpty(model.getId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Id cannot be empty");
        }

        return proxy.initiate("AWS-OpenSearchServerless-Collection::Read", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::getActiveCollection)
                .handleError((awsRequest, exception, client, model1, context) -> {
                    return handleGetActiveCollectionException(awsRequest, exception, client, model1, context);
                })
                .done(batchGetCollectionResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(batchGetCollectionResponse)));
    }
}
