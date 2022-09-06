package software.amazon.opensearchserverless.securitypolicy;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityPolicyResponse;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-OpenSearchServerless-SecurityPolicy::Update::first", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToFirstUpdateRequest)
                                .makeServiceCall(this::updateSecurityPolicy)
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateSecurityPolicyResponse updateSecurityPolicy(
            final UpdateSecurityPolicyRequest updateSecurityPolicyRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient) {
        UpdateSecurityPolicyResponse updateSecurityPolicyResponse;
        try {
            logger.log(String.format("Sending update security policy request: %s",updateSecurityPolicyRequest));
            updateSecurityPolicyResponse = proxyClient.injectCredentialsAndInvokeV2(updateSecurityPolicyRequest, proxyClient.client()::updateSecurityPolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(updateSecurityPolicyRequest.toString(), e);
        } catch (ConflictException e) {
            throw new CfnResourceConflictException(ResourceModel.TYPE_NAME, updateSecurityPolicyRequest.name(), e.getMessage(), e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully updated. response: %s", ResourceModel.TYPE_NAME, updateSecurityPolicyResponse));
        return updateSecurityPolicyResponse;
    }
}
