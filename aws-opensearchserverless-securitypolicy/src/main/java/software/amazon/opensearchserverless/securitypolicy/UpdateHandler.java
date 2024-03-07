package software.amazon.opensearchserverless.securitypolicy;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.opensearchserverless.securitypolicy.Translator.getResourceIdentifierForGetSecurityPolicyRequest;
import static software.amazon.opensearchserverless.securitypolicy.Translator.getResourceIdentifierForUpdateSecurityPolicyRequest;

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

        ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Name cannot be empty");
        }
        if (StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Type cannot be empty");
        }
        if(StringUtils.isNullOrEmpty(model.getDescription()) && model.getPolicy() == null) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "One of description or policy is required");
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
                proxy.initiate("AWS-OpenSearchServerless-SecurityPolicy::Update::PreUpdateCheck", proxyClient,
                        progress.getResourceModel(), progress.getCallbackContext())

                    // STEP 1.1 [initialize a proxy context]
                    .translateToServiceRequest(Translator::translateToReadRequest)

                    // STEP 1.2 [make an api call]
                    .makeServiceCall((awsRequest, client) -> {
                        // add custom read resource logic
                        // If describe request does not return ResourceNotFoundException,
                        // you must throw ResourceNotFoundException based on
                        // awsResponse values
                        GetSecurityPolicyResponse awsResponse = getSecurityPolicy(awsRequest, client, logger);
                        callbackContext.setCurrentSecurityPolicyDetail(awsResponse.securityPolicyDetail());
                        return awsResponse;
                    })
                    .progress()
            )
            .then(progress ->
                // STEP 2.0 [initialize a proxy context]
                // Implement client invocation of the update request through the proxyClient,
                // which is already initialised with
                // caller credentials, correct region and retry settings
                proxy.initiate("AWS-OpenSearchServerless-SecurityPolicy::Update",
                        proxyClient, request.getDesiredResourceState(), callbackContext)
                    .translateToServiceRequest(resourceModel -> Translator.translateToUpdateRequest(resourceModel,
                        callbackContext.getCurrentSecurityPolicyDetail()))
                    .makeServiceCall((awsRequest, client) -> updateSecurityPolicy(awsRequest, client, logger))
                    .done(awsResponse -> ProgressEvent.defaultSuccessHandler(
                        Translator.translateFromUpdateResponse(awsResponse))));
    }

    private UpdateSecurityPolicyResponse updateSecurityPolicy(
        final UpdateSecurityPolicyRequest updateSecurityPolicyRequest,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        UpdateSecurityPolicyResponse updateSecurityPolicyResponse;
        try {
            logger.log(String.format("Sending update security policy request: %s",updateSecurityPolicyRequest));
            updateSecurityPolicyResponse = proxyClient.injectCredentialsAndInvokeV2(updateSecurityPolicyRequest,
                proxyClient.client()::updateSecurityPolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                getResourceIdentifierForUpdateSecurityPolicyRequest(updateSecurityPolicyRequest),
                e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(updateSecurityPolicyRequest.toString() + ", " + e.getMessage(), e);
        } catch (ConflictException e) {
            throw new CfnResourceConflictException(ResourceModel.TYPE_NAME,
                getResourceIdentifierForUpdateSecurityPolicyRequest(updateSecurityPolicyRequest),
                e.getMessage(),
                e);
        } catch (ServiceQuotaExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("UpdateSecurityPolicy", e);
        }
        logger.log(String.format("%s successfully updated. response: %s", ResourceModel.TYPE_NAME,
            updateSecurityPolicyResponse));
        return updateSecurityPolicyResponse;
    }

    private GetSecurityPolicyResponse getSecurityPolicy(
        final GetSecurityPolicyRequest getSecurityPolicyRequest,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        GetSecurityPolicyResponse getSecurityPolicyResponse;
        try {
            logger.log(String.format("Sending get security policy request: %s",getSecurityPolicyRequest));
            getSecurityPolicyResponse = proxyClient.injectCredentialsAndInvokeV2(getSecurityPolicyRequest,
                proxyClient.client()::getSecurityPolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                getResourceIdentifierForGetSecurityPolicyRequest(getSecurityPolicyRequest),
                e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(getSecurityPolicyRequest.toString() + ", " + e.getMessage(), e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("GetSecurityPolicy", e);
        }
        logger.log(String.format("%s successfully read. response: %s", ResourceModel.TYPE_NAME,
            getSecurityPolicyResponse));
        return getSecurityPolicyResponse;
    }
}
