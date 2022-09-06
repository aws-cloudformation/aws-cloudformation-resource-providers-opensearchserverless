package software.amazon.opensearchserverless.collection;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import lombok.NonNull;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    static final String INVALID_COLLECTION_ID_NOT_FOUND = "InvalidCollectionID.NotFound";

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final @NonNull AmazonWebServicesClientProxy proxy,
            final @NonNull ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final @NonNull Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
                            );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger);

    /**
     * getActiveCollection returns object with status as CollectionStatus.ACTIVE only
     *
     * @param batchGetCollectionRequest the aws service request to describe a resource
     * @param proxyClient               the aws service client to make the call
     * @return aws service response
     */
    protected BatchGetCollectionResponse getActiveCollection(
            final @NonNull BatchGetCollectionRequest batchGetCollectionRequest,
            final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient) {
        final BatchGetCollectionResponse response = proxyClient.injectCredentialsAndInvokeV2(batchGetCollectionRequest, proxyClient.client()::batchGetCollection);
        if (!response.collectionDetails().isEmpty() && response.collectionDetails().get(0).status().equals(CollectionStatus.ACTIVE)) {
            return response;
        }
        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, batchGetCollectionRequest.ids().get(0));

    }

    /**
     * Get failed status when the collection is not found with given Id, throws exception otherwise.
     *
     * @param awsRequest the aws service request
     * @param exception  the exception object upon the request
     * @param client     the aws service client to make the call
     * @param model      the resource model
     * @param context    the context for aws service request
     * @return  ProgressEvent
     * @throws Exception while invoking aws service
     */
    protected ProgressEvent<ResourceModel, CallbackContext> handleGetActiveCollectionException(
            final @NonNull AwsRequest awsRequest,
            final @NonNull Exception exception,
            final @NonNull ProxyClient<OpenSearchServerlessClient> client,
            final @NonNull ResourceModel model,
            final CallbackContext context) throws Exception {
        if ((exception instanceof AwsServiceException && ((AwsServiceException) exception).awsErrorDetails().errorCode().equals(INVALID_COLLECTION_ID_NOT_FOUND))
                    || exception instanceof CfnNotFoundException) {
            return ProgressEvent.failed(model, context, HandlerErrorCode.NotFound, exception.getMessage());
        }
        throw exception;
    }
}
