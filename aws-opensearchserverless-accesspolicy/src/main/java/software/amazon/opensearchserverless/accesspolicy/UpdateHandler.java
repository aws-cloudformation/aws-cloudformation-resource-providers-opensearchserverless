package software.amazon.opensearchserverless.accesspolicy;


import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.GetAccessPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetAccessPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateAccessPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateAccessPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.amazonaws.util.StringUtils;

public class UpdateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        if(StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Type cannot be empty");
        }

        if(StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Name cannot be empty");
        }

        if(StringUtils.isNullOrEmpty(model.getDescription()) && StringUtils.isNullOrEmpty(model.getPolicy())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "One of Description or Policy is required");
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
                proxy.initiate("AWS-OpenSearchServerless-AccessPolicy::Update::PreUpdateCheck", proxyClient,
                        progress.getResourceModel(), progress.getCallbackContext())

                    // STEP 1.1 [initialize a proxy context]
                    .translateToServiceRequest(Translator::translateToReadRequest)

                    // STEP 1.2 [make an api call]
                    .makeServiceCall((awsRequest, client) -> {
                        // add custom read resource logic
                        // If describe request does not return ResourceNotFoundException,
                        // you must throw ResourceNotFoundException based on
                        // awsResponse values
                        GetAccessPolicyResponse awsResponse = getAccessPolicy(awsRequest, client, logger);
                        callbackContext.setCurrentAccessPolicyDetail(awsResponse.accessPolicyDetail());
                        return awsResponse;
                    })
                    .progress()
            )
            .then(progress ->
                // STEP 2.0 [initialize a proxy context]
                // Implement client invocation of the update request through the proxyClient,
                // which is already initialised with
                // caller credentials, correct region and retry settings
                proxy.initiate("AWS-OpenSearchServerless-AccessPolicy::Update",
                        proxyClient, request.getDesiredResourceState(), callbackContext)
                    .translateToServiceRequest(resourceModel -> Translator.translateToUpdateRequest(resourceModel,
                        callbackContext.getCurrentAccessPolicyDetail()))
                    .makeServiceCall((awsRequest, client) -> updateAccessPolicy(awsRequest, client, logger))
                    .done(awsResponse -> ProgressEvent.defaultSuccessHandler(
                        Translator.translateFromUpdateResponse(awsResponse))));
    }

    private UpdateAccessPolicyResponse updateAccessPolicy(
            final UpdateAccessPolicyRequest updateAccessPolicyRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        UpdateAccessPolicyResponse updateAccessPolicyResponse;
        try {
            updateAccessPolicyResponse = proxyClient.injectCredentialsAndInvokeV2(updateAccessPolicyRequest,
                proxyClient.client()::updateAccessPolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(updateAccessPolicyRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully updated for %s", ResourceModel.TYPE_NAME, updateAccessPolicyRequest));
        return updateAccessPolicyResponse;
    }

    private GetAccessPolicyResponse getAccessPolicy(
        final GetAccessPolicyRequest getAccessPolicyRequest,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        GetAccessPolicyResponse getAccessPolicyResponse;
        try {
            logger.log(String.format("Sending get access policy request: %s", getAccessPolicyRequest));
            getAccessPolicyResponse = proxyClient.injectCredentialsAndInvokeV2(getAccessPolicyRequest,
                proxyClient.client()::getAccessPolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,String.format("Name:%s, Type:%s",
                getAccessPolicyRequest.name(), getAccessPolicyRequest.typeAsString()),e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(getAccessPolicyRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return getAccessPolicyResponse;
    }
}
