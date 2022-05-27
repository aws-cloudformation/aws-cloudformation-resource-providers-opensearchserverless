package software.amazon.opensearchserverless.collection;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListCollectionsRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListCollectionsResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  static final String INVALID_COLLECTION_ID_NOT_FOUND = "InvalidCollectionID.NotFound";

  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final Logger logger) {
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
   * batchGetCollection returns objects with status as CollectionStatus.ACTIVE only
   * @param batchGetCollectionRequest
   * @param proxyClient
   * @return
   */
  protected BatchGetCollectionResponse batchGetCollection(
          final BatchGetCollectionRequest batchGetCollectionRequest,
          final ProxyClient<OpenSearchServerlessClient> proxyClient) {
    BatchGetCollectionResponse response = proxyClient.injectCredentialsAndInvokeV2(batchGetCollectionRequest, proxyClient.client()::batchGetCollection);
    if (!response.collectionDetails().isEmpty() && response.collectionDetails().get(0).status().equals(CollectionStatus.ACTIVE)){
      return response;
    }
    throw new CfnNotFoundException(ResourceModel.TYPE_NAME, batchGetCollectionRequest.ids().get(0));

  }

  /**
   * listCollections returns objects
   * @param listCollectionsRequest
   * @param proxyClient
   * @return
   */
  protected ListCollectionsResponse listCollections(
          final ListCollectionsRequest listCollectionsRequest,
          final ProxyClient<OpenSearchServerlessClient> proxyClient) {
    ListCollectionsResponse response = proxyClient.injectCredentialsAndInvokeV2(listCollectionsRequest, proxyClient.client()::listCollections);
    return response;

  }

  /**
   * Get failed status when the collection is not found with given Id, throws exception otherwise.
   * @param awsRequest
   * @param exception
   * @param client
   * @param model
   * @param context
   * @return
   * @throws Exception
   */
  protected ProgressEvent<ResourceModel, CallbackContext> handleBatchGetCollectionException
  (AwsRequest awsRequest, Exception exception, ProxyClient<OpenSearchServerlessClient> client, ResourceModel model, CallbackContext context)
          throws Exception {
    if((exception instanceof AwsServiceException && ((AwsServiceException) exception).awsErrorDetails().errorCode().equals(INVALID_COLLECTION_ID_NOT_FOUND))
            || exception instanceof CfnNotFoundException){
      return ProgressEvent.failed(model, context, HandlerErrorCode.NotFound, exception.getMessage());
    }
    throw exception;
  }
}
