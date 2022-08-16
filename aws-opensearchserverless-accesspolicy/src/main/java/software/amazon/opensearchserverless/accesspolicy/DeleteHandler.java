package software.amazon.opensearchserverless.accesspolicy;


import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteAccessPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteAccessPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {

        return proxy.initiate("AWS-OpenSearchServerless-AccessPolicy::Delete", proxyClient, request.getDesiredResourceState(), callbackContext)
                    .translateToServiceRequest(Translator::translateToDeleteRequest)
                    .makeServiceCall((awsRequest, client) -> deleteAccessPolicy(awsRequest, client, logger))
                    .done(awsResponse -> ProgressEvent.defaultSuccessHandler(null));
    }

    private DeleteAccessPolicyResponse deleteAccessPolicy(
            final DeleteAccessPolicyRequest deleteAccessPolicyRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        DeleteAccessPolicyResponse deleteAccessPolicyResponse;
        try {
            deleteAccessPolicyResponse = proxyClient.injectCredentialsAndInvokeV2(deleteAccessPolicyRequest, proxyClient.client()::deleteAccessPolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(deleteAccessPolicyRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully deleted for %s", ResourceModel.TYPE_NAME, 31));
        return deleteAccessPolicyResponse;
    }
}
