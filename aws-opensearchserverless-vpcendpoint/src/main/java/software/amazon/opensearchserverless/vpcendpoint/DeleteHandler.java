package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.*;
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

    public static final int CLEANUP_WAIT_COUNT = 10;

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
                .makeServiceCall((deleteVpcEndpointRequest, proxyClient1) -> {
                    DeleteVpcEndpointResponse awsResponse =
                        deleteVpcEndpoint(deleteVpcEndpointRequest, proxyClient1, logger);
                    callbackContext.setCleanupWaitCount(CLEANUP_WAIT_COUNT);
                    return awsResponse;
                })
                .stabilize((awsRequest, awsResponse, proxyClient1, resourceModel, callbackContext1) ->
                    stabilizeVpcEndpointDelete(awsRequest, proxyClient1, callbackContext1, logger))
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
        final CallbackContext callbackContext,
        final Logger logger) {
        logger.log(String.format("Stabilize VpcEndpointDelete for resource %s", deleteVpcEndpointRequest));

        final BatchGetVpcEndpointRequest request = BatchGetVpcEndpointRequest.builder().ids(deleteVpcEndpointRequest.id()).build();
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::batchGetVpcEndpoint);
        if (batchGetVpcEndpointResponse.vpcEndpointDetails().isEmpty()) {
            int cleanupWaitCount = callbackContext.getCleanupWaitCount() - 1;
            if (cleanupWaitCount > 0 ) {
                callbackContext.setCleanupWaitCount(cleanupWaitCount);
                return false;
            } else {
                return true;
            }
        } else if (batchGetVpcEndpointResponse.vpcEndpointDetails().size() == 1) {
            final VpcEndpointDetail vpcEndpointDetail = batchGetVpcEndpointResponse.vpcEndpointDetails().get(0);
            if (vpcEndpointDetail.status().equals(VpcEndpointStatus.DELETING)) {
                return false;
            }
        }
        throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, deleteVpcEndpointRequest.id());
    }

    private DeleteVpcEndpointResponse deleteVpcEndpoint(
        final DeleteVpcEndpointRequest deleteVpcEndpointRequest,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {
        DeleteVpcEndpointResponse deleteVpcEndpointResponse;
        try {
            logger.log(String.format("Sending delete Vpc Endpoint request: %s",deleteVpcEndpointRequest));
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
        logger.log(String.format("%s successfully deleted. response: %s", ResourceModel.TYPE_NAME, deleteVpcEndpointResponse));
        return deleteVpcEndpointResponse;
    }
}
