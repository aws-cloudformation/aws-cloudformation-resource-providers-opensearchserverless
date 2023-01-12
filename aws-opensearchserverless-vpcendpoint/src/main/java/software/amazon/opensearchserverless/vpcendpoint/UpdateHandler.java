package software.amazon.opensearchserverless.vpcendpoint;

import lombok.NonNull;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointDetail;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointStatus;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {

    private final ReadHandler readHandler;

    public UpdateHandler() {
        super();
        readHandler = new ReadHandler(getOpenSearchServerlessClient());
    }

    public UpdateHandler(OpenSearchServerlessClient openSearchServerlessClient) {
        super(openSearchServerlessClient);
        readHandler = new ReadHandler(getOpenSearchServerlessClient());
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        if (model.getId() == null) {
            throw new CfnInvalidRequestException("Id is required");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)

            // STEP 1 [check if resource already exists]
            // for more information ->
            // https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
            // if target API does not support 'ResourceNotFoundException' then following check is required
            .then(progress ->
                // STEP 1.0 [initialize a proxy context]
                // If your service API does not return ResourceNotFoundException
                // on update requests against some identifier (e.g; resource Name)
                // and instead returns a 200 even though a resource does not exist,
                // you must first check if the resource exists here
                // NOTE: If your service API throws 'ResourceNotFoundException'
                // for update requests this method is not necessary
                proxy.initiate("AWS-OpenSearchServerless-VpcEndpoint::Update::PreUpdateCheck", proxyClient,
                        progress.getResourceModel(), progress.getCallbackContext())

                    // STEP 1.1 [initialize a proxy context]
                    .translateToServiceRequest(Translator::translateToReadRequest)

                    // STEP 1.2 [make an api call]
                    .makeServiceCall((awsRequest, client) -> {
                        // add custom read resource logic
                        // If describe request does not return ResourceNotFoundException,
                        // you must throw ResourceNotFoundException based on
                        // awsResponse values
                        BatchGetVpcEndpointResponse awsResponse = getVpcEndpointForUpdate(awsRequest, client, logger);
                        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                        callbackContext.setCurrentVpcEndpointDetail(awsResponse.vpcEndpointDetails().get(0));
                        return awsResponse;
                    })
                    .progress()
            )
            // STEP 2 [first update/stabilize progress chain - required for resource update]
            .then(progress ->
                // STEP 2.0 [initialize a proxy context]
                // Implement client invocation of the update request through the proxyClient,
                // which is already initialised with
                // caller credentials, correct region and retry settings
                proxy.initiate("AWS-OpenSearchServerless-VpcEndpoint::Update::first", proxyClient,
                        progress.getResourceModel(), progress.getCallbackContext())

                    // STEP 2.1 [construct a body of a request]
                    .translateToServiceRequest(resourceModel -> Translator.translateToFirstUpdateRequest(resourceModel,
                        callbackContext.getCurrentVpcEndpointDetail()))

                    // STEP 2.2 [make an api call]
                    .makeServiceCall((awsRequest, client) -> updateVpcEndpoint(awsRequest, client, logger))

                    // STEP 2.3 [stabilize step is not necessarily required but typically involves
                    // describing the resource until it is in a certain status, though it can take many forms]
                    // stabilization step may or may not be needed after each API call
                    // for more information ->
                    // https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
                    .stabilize((awsRequest, awsResponse, client, cbModel, context) ->
                        stabilizeVpcEndpointUpdate(client, cbModel, logger))
                    .progress())
            // STEP 4 [describe call/chain to return the resource model]
            .then(progress -> readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Stabilization of VpcEndpoint for the Update operation.
     *
     * @param proxyClient               the aws service client to make the call
     * @param resourceModel             the resource model
     * @param logger                    the logger
     * @return true only if VPCEndpoint status is ACTIVE
     */
    private boolean stabilizeVpcEndpointUpdate(
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final ResourceModel resourceModel,
        final Logger logger) {
        logger.log(String.format("Stabilize VpcEndpointUpdate for resource %s", resourceModel));
        BatchGetVpcEndpointRequest batchGetVpcEndpointRequest = Translator.translateToReadRequest(resourceModel);
        BatchGetVpcEndpointResponse batchGetVpcEndpointResponse;
        try {
            logger.log(String.format("Sending batch get Vpc Endpoint request: %s",batchGetVpcEndpointRequest));
            batchGetVpcEndpointResponse = proxyClient.injectCredentialsAndInvokeV2(batchGetVpcEndpointRequest,
                proxyClient.client()::batchGetVpcEndpoint);
            if (batchGetVpcEndpointResponse.hasVpcEndpointDetails()
                && batchGetVpcEndpointResponse.vpcEndpointDetails().size() == 1) {
                VpcEndpointDetail vpcEndpointDetail = batchGetVpcEndpointResponse.vpcEndpointDetails().get(0);
                switch (vpcEndpointDetail.status()) {
                    case PENDING:
                        return false;
                    case ACTIVE:
                        return true;
                }
            }
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(batchGetVpcEndpointRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, resourceModel.getId());
    }

    protected BatchGetVpcEndpointResponse getVpcEndpointForUpdate(
        final @NonNull BatchGetVpcEndpointRequest batchGetVpcEndpointRequest,
        final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {
        final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse;
        try {
            logger.log(String.format("Sending batch get Vpc Endpoint request: %s",batchGetVpcEndpointRequest));
            batchGetVpcEndpointResponse = proxyClient.injectCredentialsAndInvokeV2(batchGetVpcEndpointRequest,
                proxyClient.client()::batchGetVpcEndpoint);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(batchGetVpcEndpointRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        if (batchGetVpcEndpointResponse.hasVpcEndpointDetails()
            && batchGetVpcEndpointResponse.vpcEndpointDetails().size() == 1) {
            final VpcEndpointDetail vpcEndpointDetail = batchGetVpcEndpointResponse.vpcEndpointDetails().get(0);
            if (VpcEndpointStatus.ACTIVE.equals(vpcEndpointDetail.status())) {
                return batchGetVpcEndpointResponse;
            } else {
                throw new CfnResourceConflictException(ResourceModel.TYPE_NAME,
                    batchGetVpcEndpointRequest.ids().get(0),"Resource is not in Active state" );
            }
        }
        throw new ResourceNotFoundException(ResourceModel.TYPE_NAME, batchGetVpcEndpointRequest.ids().get(0));
    }

    private UpdateVpcEndpointResponse updateVpcEndpoint(
        final UpdateVpcEndpointRequest updateVpcEndpointRequest,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {
        UpdateVpcEndpointResponse updateVpcEndpointResponse;
        try {
            logger.log(String.format("Sending update Vpc Endpoint request: %s",updateVpcEndpointRequest));
            updateVpcEndpointResponse = proxyClient.injectCredentialsAndInvokeV2(updateVpcEndpointRequest,
                proxyClient.client()::updateVpcEndpoint);
        } catch (ConflictException e) {
            throw new CfnResourceConflictException(ResourceModel.TYPE_NAME, updateVpcEndpointRequest.id(),
                e.getMessage(), e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(updateVpcEndpointRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully updated. response: %s", ResourceModel.TYPE_NAME,
            updateVpcEndpointResponse));
        return updateVpcEndpointResponse;
    }
}
