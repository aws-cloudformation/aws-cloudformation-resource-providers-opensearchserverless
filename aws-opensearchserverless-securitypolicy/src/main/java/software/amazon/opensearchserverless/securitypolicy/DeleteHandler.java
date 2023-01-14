package software.amazon.opensearchserverless.securitypolicy;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.amazonaws.util.StringUtils;

public class DeleteHandler extends BaseHandlerStd {

    public DeleteHandler() {
        super();
    }

    public DeleteHandler(OpenSearchServerlessClient openSearchServerlessClient) {
        super(openSearchServerlessClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Name cannot be empty");
        }
        if (StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Type cannot be empty");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-OpenSearchServerless-SecurityPolicy::Delete",
                        proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDeleteRequest)
                    .makeServiceCall((awsRequest, cbClient) ->
                        deleteSecurityPolicy(awsRequest, cbClient, logger))
                    .progress()
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private DeleteSecurityPolicyResponse deleteSecurityPolicy(
        final DeleteSecurityPolicyRequest deleteSecurityPolicyRequest,
        final ProxyClient<OpenSearchServerlessClient> proxyClient, Logger logger) {

        DeleteSecurityPolicyResponse deleteSecurityPolicyResponse;
        try {
            logger.log(String.format("Sending delete security policy request: %s",deleteSecurityPolicyRequest));
            deleteSecurityPolicyResponse = proxyClient.injectCredentialsAndInvokeV2(deleteSecurityPolicyRequest,
                proxyClient.client()::deleteSecurityPolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,deleteSecurityPolicyRequest.name(),e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(deleteSecurityPolicyRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully deleted. response: %s", ResourceModel.TYPE_NAME,
            deleteSecurityPolicyResponse));
        return deleteSecurityPolicyResponse;
    }
}
