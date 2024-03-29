package software.amazon.opensearchserverless.vpcendpoint;

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
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.amazonaws.util.CollectionUtils;
import com.amazonaws.util.StringUtils;

public class CreateHandler extends BaseHandlerStd {

    private final ReadHandler readHandler;

    public CreateHandler() {
        super();
        readHandler = new ReadHandler(getOpenSearchServerlessClient());
    }

    public CreateHandler(OpenSearchServerlessClient openSearchServerlessClient) {
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
                    .stabilize((awsRequest, awsResponse, client, cbModel, context) -> stabilizeVpcEndpointCreate(awsResponse, client, cbModel, logger))
                    .done((createVpcEndpointRequest, createVpcEndpointResponse, client, resourceModel, callbackContext1) -> ProgressEvent.progress(resourceModel, callbackContext1))
            )
            .then(progress -> readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Stabilization of VpcEndpoint for the Create operation.
     *
     * @param createVpcEndpointResponse the aws service response to create VpcEndpoint resource
     * @param proxyClient               the aws service client to make the call
     * @param resourceModel             the resource model
     * @return true only if VpcEndpoint status is ACTIVE
     */
    private boolean stabilizeVpcEndpointCreate(
        final CreateVpcEndpointResponse createVpcEndpointResponse,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final ResourceModel resourceModel,
        final Logger logger) {
        String id = createVpcEndpointResponse.createVpcEndpointDetail().id();
        resourceModel.setId(id);
        logger.log(String.format("Stabilize VpcEndpointCreate for resource %s", resourceModel));

        BatchGetVpcEndpointRequest request = Translator.translateToReadRequest(resourceModel);
        BatchGetVpcEndpointResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::batchGetVpcEndpoint);

        if (response.hasVpcEndpointDetails() && response.vpcEndpointDetails().size() == 1) {
            VpcEndpointDetail vpcEndpointDetail = response.vpcEndpointDetails().get(0);
            switch (vpcEndpointDetail.status()) {
                case PENDING:
                    return false;
                case ACTIVE:
                    return true;
            }
        }
        throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, id);
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
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, createVpcEndpointRequest.name(), e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(createVpcEndpointRequest.toString() + ", " + e.getMessage(), e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("CreateVpcEndpoint", e);
        }
        logger.log(String.format("%s successfully created. response: %s", ResourceModel.TYPE_NAME, createVpcEndpointResponse));
        return createVpcEndpointResponse;
    }
}
