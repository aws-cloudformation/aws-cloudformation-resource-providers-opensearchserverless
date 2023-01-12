package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointStatus;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import lombok.NonNull;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
  static final String INVALID_VpcEndpoint_ID_NOT_FOUND = "InvalidVpcEndpointID.NotFound";
  private final OpenSearchServerlessClient openSearchServerlessClient;

  protected BaseHandlerStd() {
    this(ClientBuilder.getClient());
  }

  protected BaseHandlerStd(OpenSearchServerlessClient openSearchServerlessClient ) {
    this.openSearchServerlessClient = openSearchServerlessClient;
  }

  protected OpenSearchServerlessClient getOpenSearchServerlessClient() {
    return openSearchServerlessClient;
  }

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
            proxy.newProxy(this::getOpenSearchServerlessClient),
            logger);
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
          final AmazonWebServicesClientProxy proxy,
          final ResourceHandlerRequest<ResourceModel> request,
          final CallbackContext callbackContext,
          final ProxyClient<OpenSearchServerlessClient> proxyClient,
          final Logger logger);

  /**
   * getActiveVpcEndpoint returns object with status as VpcEndpointStatus.ACTIVE only
   *
   * @param batchGetVpcEndpointRequest the aws service request to describe a resource
   * @param proxyClient                the aws service client to make the call
   * @return batchGetVpcEndpoint response
   */
  protected BatchGetVpcEndpointResponse getActiveVpcEndpoint(
          final @NonNull BatchGetVpcEndpointRequest batchGetVpcEndpointRequest,
          final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient) {
    final BatchGetVpcEndpointResponse response = proxyClient.injectCredentialsAndInvokeV2(batchGetVpcEndpointRequest, proxyClient.client()::batchGetVpcEndpoint);
    if (!response.vpcEndpointDetails().isEmpty() && response.vpcEndpointDetails().get(0).status().equals(VpcEndpointStatus.ACTIVE)) {
      return response;
    }
    throw new CfnNotFoundException(ResourceModel.TYPE_NAME, batchGetVpcEndpointRequest.ids().get(0));
  }

  /**
   * Get failed status when the VpcEndpoint is not found with given Id, throws exception otherwise.
   *
   * @param awsRequest the aws service request
   * @param exception  the exception object upon the request
   * @param client     the aws service client to make the call
   * @param model      the resource model
   * @param context    the context for aws service request
   * @return ProgressEvent
   * @throws Exception exception
   */
  protected ProgressEvent<ResourceModel, CallbackContext> handleGetActiveVpcEndpointException(
          final @NonNull AwsRequest awsRequest,
          final @NonNull Exception exception,
          final @NonNull ProxyClient<OpenSearchServerlessClient> client,
          final @NonNull ResourceModel model,
          final CallbackContext context) throws Exception {
    if ((exception instanceof AwsServiceException &&
                 ((AwsServiceException) exception).awsErrorDetails().errorCode().equals(INVALID_VpcEndpoint_ID_NOT_FOUND))
                || exception instanceof CfnNotFoundException) {
      return ProgressEvent.failed(model, context, HandlerErrorCode.NotFound, exception.getMessage());
    }
    throw exception;
  }
}
