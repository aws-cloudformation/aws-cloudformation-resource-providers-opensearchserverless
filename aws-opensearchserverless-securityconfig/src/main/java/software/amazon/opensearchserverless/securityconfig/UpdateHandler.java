package software.amazon.opensearchserverless.securityconfig;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.*;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.amazonaws.util.StringUtils;

public class UpdateHandler extends BaseHandlerStd {

    public UpdateHandler() {
        super();
    }

    public UpdateHandler(OpenSearchServerlessClient openSearchServerlessClient) {
        super(openSearchServerlessClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if(StringUtils.isNullOrEmpty(model.getId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Id cannot be empty");
        }

        if(StringUtils.isNullOrEmpty(model.getDescription()) && model.getSamlOptions() == null) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "One of description or saml-options is required");
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
                proxy.initiate("AWS-OpenSearchServerless-SecurityConfig::Update::PreUpdateCheck", proxyClient,
                        progress.getResourceModel(), progress.getCallbackContext())

                    // STEP 1.1 [initialize a proxy context]
                    .translateToServiceRequest(Translator::translateToReadRequest)

                    // STEP 1.2 [make an api call]
                    .makeServiceCall((awsRequest, client) -> {
                        // add custom read resource logic
                        // If describe request does not return ResourceNotFoundException,
                        // you must throw ResourceNotFoundException based on
                        // awsResponse values
                        GetSecurityConfigResponse awsResponse = getSecurityConfig(awsRequest, client, logger);
                        callbackContext.setCurrentSecurityConfigDetail(awsResponse.securityConfigDetail());
                        return awsResponse;
                    })
                    .progress()
            )
            .then(progress ->
                // STEP 2.0 [initialize a proxy context]
                // Implement client invocation of the update request through the proxyClient,
                // which is already initialised with
                // caller credentials, correct region and retry settings
                proxy.initiate("AWS-OpenSearchServerless-SecurityConfig::Update",
                        proxyClient, request.getDesiredResourceState(), callbackContext)
                    .translateToServiceRequest(resourceModel -> Translator.translateToUpdateRequest(resourceModel,
                        callbackContext.getCurrentSecurityConfigDetail()))
                    .makeServiceCall((awsRequest, client) -> updateSecurityConfig(awsRequest, client, logger))
                    .done(awsResponse -> ProgressEvent.defaultSuccessHandler(
                        Translator.translateFromUpdateResponse(awsResponse))));
    }

    private UpdateSecurityConfigResponse updateSecurityConfig(
            final UpdateSecurityConfigRequest updateSecurityConfigRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        UpdateSecurityConfigResponse updateSecurityConfigResponse;
        try {
            updateSecurityConfigResponse =
                    proxyClient.injectCredentialsAndInvokeV2(updateSecurityConfigRequest, proxyClient.client()::updateSecurityConfig);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, updateSecurityConfigRequest.id(), e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(updateSecurityConfigRequest.toString() + ", " + e.getMessage(), e);
        } catch (ConflictException e) {
            throw new CfnResourceConflictException(ResourceModel.TYPE_NAME, updateSecurityConfigRequest.id(),
                    e.getMessage(), e);
        } catch (ServiceQuotaExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("UpdateSecurityConfig", e);
        }
        logger.log(String.format("%s successfully updated for %s", ResourceModel.TYPE_NAME, updateSecurityConfigRequest));
        return updateSecurityConfigResponse;
    }

    private GetSecurityConfigResponse getSecurityConfig(
        final GetSecurityConfigRequest getSecurityConfigRequest,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        GetSecurityConfigResponse getSecurityConfigResponse;
        try {
            getSecurityConfigResponse = proxyClient.injectCredentialsAndInvokeV2(getSecurityConfigRequest, proxyClient.client()::getSecurityConfig);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, getSecurityConfigRequest.id(), e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(getSecurityConfigRequest.toString() + ", " + e.getMessage(), e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("GetSecurityConfig", e);
        }
        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return getSecurityConfigResponse;
    }
}
