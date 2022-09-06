package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.CreateVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointDetail;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.amazonaws.util.CollectionUtils;
import com.amazonaws.util.StringUtils;

public class CreateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        if (callbackContext == null && model.getId() != null) {
            throw new CfnInvalidRequestException("Id should not be set");
        }
        if (StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Name cannot be empty");
        }
        if (StringUtils.isNullOrEmpty(model.getVpcId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "VpcId cannot be empty");
        }
        if (CollectionUtils.isNullOrEmpty(model.getSubnetIds())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "SubnetIds cannot be empty");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                            .then(progress ->
                                          proxy.initiate("AWS-OpenSearchServerless-VpcEndpoint::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                               .translateToServiceRequest(Translator::translateToCreateRequest)
                                               .makeServiceCall((awsRequest, client) -> createVpcEndpoint(awsRequest, client, logger))
                                               .stabilize((awsRequest, awsResponse, client, cbMmodel, context) -> stabilizeVpcEndpointCreate(awsRequest, awsResponse, client, logger))
                                               .done((createVpcEndpointRequest, createVpcEndpointResponse, client, resourceModel, callbackContext1) -> {
                                                   resourceModel.setId(createVpcEndpointResponse.createVpcEndpointDetail().id());
                                                   return ProgressEvent.progress(resourceModel, callbackContext1);
                                               })
                                 )
                            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Stabilization of VpcEndpoint for the Create operation.
     *
     * @param createVpcEndpointRequest  the aws service request to create VpcEndpoint resource
     * @param createVpcEndpointResponse the aws service response to create VpcEndpoint resource
     * @param proxyClient               the aws service client to make the call
     * @return true only if collection status is ACTIVE
     */
    private boolean stabilizeVpcEndpointCreate(
            final CreateVpcEndpointRequest createVpcEndpointRequest,
            final CreateVpcEndpointResponse createVpcEndpointResponse,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        logger.log(String.format("Stabilize VpcEndpointCreate for resource %s", createVpcEndpointRequest));

        BatchGetVpcEndpointRequest request = BatchGetVpcEndpointRequest.builder().ids(createVpcEndpointResponse.createVpcEndpointDetail().id()).build();
        BatchGetVpcEndpointResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::batchGetVpcEndpoint);

        if (!response.hasVpcEndpointDetails() || response.vpcEndpointDetails().size() != 1) {
            return false; //This will retry the stabilization as the vpcEndpoint does not exist yet
        }
        VpcEndpointDetail vpcEndpointDetail = response.vpcEndpointDetails().get(0);
        switch (vpcEndpointDetail.status()) {
            case ACTIVE:
                return true;
            case FAILED:
            case DELETING:
                //This is a failure case
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, vpcEndpointDetail.id());
            case CREATING:
            default:
                //The vpcEndpoint is not in ACTIVE state and stabilization can be retried.
                return false;
        }
    }

    private CreateVpcEndpointResponse createVpcEndpoint(
            final CreateVpcEndpointRequest createVpcEndpointRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        CreateVpcEndpointResponse createVpcEndpointResponse;
        try {
            logger.log(String.format("Sending create Vpc Endpoint request: %s",createVpcEndpointRequest));
            createVpcEndpointResponse = proxyClient.injectCredentialsAndInvokeV2(createVpcEndpointRequest, proxyClient.client()::createVpcEndpoint);
        } catch (ConflictException e) {
            throw new CfnAlreadyExistsException(e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(createVpcEndpointRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully created. response: %s", ResourceModel.TYPE_NAME, createVpcEndpointResponse));
        return createVpcEndpointResponse;
    }
}
