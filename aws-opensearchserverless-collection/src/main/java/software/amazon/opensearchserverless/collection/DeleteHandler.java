package software.amazon.opensearchserverless.collection;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteCollectionResponse;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
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

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> proxy.initiate("AWS-OpenSearchServerless-Collection::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .makeServiceCall(this::getActiveCollection)
                        .handleError((awsRequest, exception, client, model1, context) -> {
                            return handleGetActiveCollectionException(awsRequest, exception, client, model1, context);
                        })
                        .progress())
                .then(progress -> proxy.initiate("AWS-OpenSearchServerless-Collection::Delete", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToDeleteRequest)
                        .makeServiceCall((deleteCollectionRequest, proxyClient1) -> proxyClient.injectCredentialsAndInvokeV2(deleteCollectionRequest, proxyClient1.client()::deleteCollection))
                        .stabilize(this::stabilizeCollectionDelete)
                        .done((deleteRequest) -> ProgressEvent.<ResourceModel, CallbackContext>builder().status(OperationStatus.SUCCESS).build()));


    }

    /**
     * Stabilization of Collection for the Delete operation.
     * Returns true only if collection is not found or status is FAILED
     * @param deleteCollectionRequest the aws service request to delete collection resource
     * @param deleteCollectionResponse the aws service response to delete collection resource
     * @param proxyClient the aws service client to make the call
     * @param model the resource model
     * @param callbackContext the callback context for aws service request
     * @return
     */
    private boolean stabilizeCollectionDelete(
            final @NonNull DeleteCollectionRequest deleteCollectionRequest,
            final @NonNull DeleteCollectionResponse deleteCollectionResponse,
            final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
            final @NonNull ResourceModel model,
            final CallbackContext callbackContext) {
        logger.log(String.format("Stabilize CollectionDelete for resource %s", deleteCollectionRequest));

        final BatchGetCollectionRequest request = BatchGetCollectionRequest.builder().ids(deleteCollectionRequest.id()).build();
        final BatchGetCollectionResponse batchGetCollectionResponse = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::batchGetCollection);
        //If the collection is not ACTIVE after it is Deleted, its a success scenario
        if (batchGetCollectionResponse.collectionDetails().isEmpty())
            return true;
        final CollectionDetail collectionDetail = batchGetCollectionResponse.collectionDetails().get(0); //The output will always have one collection with any status
        switch (collectionDetail.status()) {
            case FAILED:
                //This is a failure case
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, collectionDetail.id());
            case ACTIVE:
            case CREATING:
            case DELETING:
            default:
                //If the status is any of the above, the stabilization check can be retried
                return false;
        }
    }
}