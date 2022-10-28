package software.amazon.opensearchserverless.accountsettings;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.cloudformation.proxy.*;

public class DeleteHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final @NonNull AmazonWebServicesClientProxy proxy,
        final @NonNull ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
        final @NonNull Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if (!StringUtils.isNullOrEmpty(model.getAccountId())
            && !model.getAccountId().equals(request.getAwsAccountId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "Resource not found");
        }

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
