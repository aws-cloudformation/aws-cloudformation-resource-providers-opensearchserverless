package software.amazon.opensearchserverless.lifecyclepolicy;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetLifecyclePolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateLifecyclePolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
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

import static software.amazon.opensearchserverless.lifecyclepolicy.Translator.getResourceIdentifier;
import static software.amazon.opensearchserverless.lifecyclepolicy.Translator.getResourceIdentifierForUpdateLifecyclePolicyRequest;

public class UpdateHandler extends BaseHandlerStd {

    public UpdateHandler() {
        super();
    }

    public UpdateHandler(OpenSearchServerlessClient openSearchServerlessClient) {
        super(openSearchServerlessClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request,
                                                                          final CallbackContext callbackContext,
                                                                          final ProxyClient<OpenSearchServerlessClient> proxyClient,
                                                                          final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Type cannot be empty");
        }

        if (StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Name cannot be empty");
        }

        if (StringUtils.isNullOrEmpty(model.getDescription()) && StringUtils.isNullOrEmpty(model.getPolicy())) {
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
                proxy.initiate("AWS-OpenSearchServerless-LifecyclePolicy::Update::PreUpdateCheck", proxyClient,
                        progress.getResourceModel(), progress.getCallbackContext())

                    // STEP 1.1 [initialize a proxy context]
                    .translateToServiceRequest(Translator::translateToReadRequest)

                    // STEP 1.2 [make an api call]
                    .makeServiceCall((awsRequest, client) -> {
                        // add custom read resource logic
                        // If describe request does not return ResourceNotFoundException,
                        // you must throw ResourceNotFoundException based on
                        // awsResponse values
                        BatchGetLifecyclePolicyResponse awsResponse = batchGetLifecyclePolicy(awsRequest, client, logger);
                        callbackContext.setCurrentLifecyclePolicyDetail(awsResponse.lifecyclePolicyDetails().get(0));
                        return awsResponse;
                    })
                    .progress()
            )
            .then(progress ->
                // STEP 2.0 [initialize a proxy context]
                // Implement client invocation of the update request through the proxyClient,
                // which is already initialised with
                // caller credentials, correct region and retry settings
                proxy.initiate("AWS-OpenSearchServerless-LifecyclePolicy::Update",
                        proxyClient, request.getDesiredResourceState(), callbackContext)
                    .translateToServiceRequest(resourceModel -> Translator.translateToUpdateRequest(resourceModel,
                        callbackContext.getCurrentLifecyclePolicyDetail()))
                    .makeServiceCall((awsRequest, client) -> updateLifecyclePolicy(awsRequest, client, logger))
                    .done(awsResponse -> ProgressEvent.defaultSuccessHandler(
                        Translator.translateFromUpdateResponse(awsResponse))));
    }

    private UpdateLifecyclePolicyResponse updateLifecyclePolicy(final UpdateLifecyclePolicyRequest updateLifecyclePolicyRequest,
                                                                final ProxyClient<OpenSearchServerlessClient> proxyClient,
                                                                final Logger logger) {
        UpdateLifecyclePolicyResponse updateLifecyclePolicyResponse;
        try {
            updateLifecyclePolicyResponse = proxyClient.injectCredentialsAndInvokeV2(updateLifecyclePolicyRequest, proxyClient.client()::updateLifecyclePolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                getResourceIdentifierForUpdateLifecyclePolicyRequest(updateLifecyclePolicyRequest),
                e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(updateLifecyclePolicyRequest.toString() + ", " + e.getMessage(), e);
        } catch (ConflictException e) {
            throw new CfnResourceConflictException(ResourceModel.TYPE_NAME,
                getResourceIdentifierForUpdateLifecyclePolicyRequest(updateLifecyclePolicyRequest),
                e.getMessage(),
                e);
        } catch (ServiceQuotaExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully updated for %s", ResourceModel.TYPE_NAME, updateLifecyclePolicyRequest));
        return updateLifecyclePolicyResponse;
    }

    private BatchGetLifecyclePolicyResponse batchGetLifecyclePolicy(final BatchGetLifecyclePolicyRequest batchGetLifecyclePolicyRequest,
                                                                    final ProxyClient<OpenSearchServerlessClient> proxyClient,
                                                                    final Logger logger) {
        BatchGetLifecyclePolicyResponse batchGetLifecyclePolicyResponse;
        try {
            logger.log(String.format("Sending batchGet lifecycle policy request: %s", batchGetLifecyclePolicyRequest));
            batchGetLifecyclePolicyResponse = proxyClient.injectCredentialsAndInvokeV2(batchGetLifecyclePolicyRequest, proxyClient.client()::batchGetLifecyclePolicy);
            if (!batchGetLifecyclePolicyResponse.lifecyclePolicyDetails().isEmpty()) {
                logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                return batchGetLifecyclePolicyResponse;
            }
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                getResourceIdentifier(batchGetLifecyclePolicyRequest.identifiers().get(0)));
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(batchGetLifecyclePolicyRequest.toString() + ", " + e.getMessage(), e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("BatchGetLifecyclePolicy", e);
        }
    }
}
