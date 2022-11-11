package software.amazon.opensearchserverless.collection;


import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateCollectionResponse;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final @NonNull AmazonWebServicesClientProxy proxy,
        final @NonNull ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
        final @NonNull Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        if (StringUtils.isNullOrEmpty(model.getId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Id cannot be empty");
        }

        if (StringUtils.isNullOrEmpty(model.getDescription())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Description cannot be empty");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> proxy.initiate("AWS-OpenSearchServerless-Collection::Update::PreUpdateCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::getActiveCollection)
                .handleError(this::handleGetActiveCollectionException)
                .progress())
            .then(progress -> proxy.initiate("AWS-OpenSearchServerless-Collection::Update", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToUpdateRequest)
                .makeServiceCall((updateCollectionRequest, proxyClient1) -> proxyClient.injectCredentialsAndInvokeV2(updateCollectionRequest, proxyClient1.client()::updateCollection))
                .stabilize(this::stabilizeCollectionUpdate)
                .progress())
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Stabilization of Collection for the Update operation.
     *
     * @param updateCollectionRequest  the aws service request to update collection resource
     * @param updateCollectionResponse the aws service response to update collection resource
     * @param proxyClient              the aws service client to make the call
     * @param resourceModel                    the resource model
     * @return Returns true only if collection status is ACTIVE.
     */
    private boolean stabilizeCollectionUpdate(
        final @NonNull UpdateCollectionRequest updateCollectionRequest,
        final @NonNull UpdateCollectionResponse updateCollectionResponse,
        final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
        final @NonNull ResourceModel resourceModel,
        final CallbackContext callbackContext) {
        logger.log(String.format("Stabilize CollectionUpdate for resource %s", resourceModel));
        BatchGetCollectionRequest request = Translator.translateToReadRequest(resourceModel);
        BatchGetCollectionResponse response = proxyClient.injectCredentialsAndInvokeV2(request,
            proxyClient.client()::batchGetCollection);

        if (response.hasCollectionDetails() && response.collectionDetails().size() == 1) {
            CollectionDetail collectionDetail = response.collectionDetails().get(0);
            switch (collectionDetail.status()) {
                case ACTIVE:
                    return true;
                default:
                    return false;
            }
        }
        throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, resourceModel.getId());
    }
}
