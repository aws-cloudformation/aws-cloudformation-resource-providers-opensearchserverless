package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointDetail;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.amazonaws.util.StringUtils;

public class DeleteHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(model.getId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Id cannot be empty");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                            .then(progress -> proxy.initiate("AWS-OpenSearchServerless-VpcEndpoint::Delete::PreDeletionCheck",
                                                             proxyClient,
                                                             progress.getResourceModel(),
                                                             progress.getCallbackContext())
                                                   .translateToServiceRequest(Translator::translateToReadRequest)
                                                   .makeServiceCall(this::getActiveVpcEndpoint)
                                                   .handleError(this::handleGetActiveVpcEndpointException)
                                                   .progress())
                            .then(progress -> proxy.initiate("AWS-OpenSearchServerless-VpcEndpoint::Delete",
                                                             proxyClient,
                                                             progress.getResourceModel(),
                                                             progress.getCallbackContext())
                                                   .translateToServiceRequest(Translator::translateToDeleteRequest)
                                                   .makeServiceCall((deleteVpcEndpointRequest, proxyClient1) ->
                                                                            deleteVpcEndpoint(deleteVpcEndpointRequest, proxyClient1, logger))
                                                   .stabilize((awsRequest, awsResponse, proxyClient1, resourceModel, callbackContext1) ->
                                                                      stabilizeVpcEndpointDelete(awsRequest, proxyClient1, logger))
                                                   .done((deleteRequest) -> ProgressEvent.<ResourceModel, CallbackContext>builder().status(OperationStatus.SUCCESS).build()));
    }

    /**
     * Stabilization of VpcEndpoint for the Delete operation.
     *
     * @param deleteVpcEndpointRequest the aws service request to delete VpcEndpoint resource
     * @param proxyClient              the aws service client to make the call
     *                                 Returns true only if VpcEndpoint is not found or status is FAILED
     */
    private boolean stabilizeVpcEndpointDelete(
            final DeleteVpcEndpointRequest deleteVpcEndpointRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        logger.log(String.format("Stabilize VpcEndpointDelete for resource %s", deleteVpcEndpointRequest));

        final BatchGetVpcEndpointRequest request = BatchGetVpcEndpointRequest.builder().ids(deleteVpcEndpointRequest.id()).build();
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::batchGetVpcEndpoint);
        if (batchGetVpcEndpointResponse.vpcEndpointDetails().isEmpty())
            return true;
        final VpcEndpointDetail vpcEndpointDetail = batchGetVpcEndpointResponse.vpcEndpointDetails().get(0); //The output will always have one vpcEndpointDetail with any status
        switch (vpcEndpointDetail.status()) {
            case FAILED:
                //This is a failure case
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, vpcEndpointDetail.id());
            case ACTIVE:
            case CREATING:
            case DELETING:
            default:
                //If the status is any of the above, the stabilization check can be retried
                return false;
        }
    }

    private DeleteVpcEndpointResponse deleteVpcEndpoint(
            final DeleteVpcEndpointRequest deleteVpcEndpointRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        DeleteVpcEndpointResponse deleteVpcEndpointResponse;
        try {
            deleteVpcEndpointResponse = proxyClient.injectCredentialsAndInvokeV2(deleteVpcEndpointRequest, proxyClient.client()::deleteVpcEndpoint);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(deleteVpcEndpointRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully deleted for %s", ResourceModel.TYPE_NAME, 31));
        return deleteVpcEndpointResponse;
    }
}
