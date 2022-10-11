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
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.amazonaws.util.StringUtils;

public class DeleteHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if (StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Name cannot be empty");
        }

        if (StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Type cannot be empty");
        }

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
            logger.log(String.format("Sending delete access policy request: %s", deleteAccessPolicyRequest));
            deleteAccessPolicyResponse = proxyClient.injectCredentialsAndInvokeV2(deleteAccessPolicyRequest, proxyClient.client()::deleteAccessPolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,String.format("Name:%s, Type:%s", deleteAccessPolicyRequest.name(), deleteAccessPolicyRequest.typeAsString()),e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(deleteAccessPolicyRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully deleted. response: %s", ResourceModel.TYPE_NAME, deleteAccessPolicyResponse));
        return deleteAccessPolicyResponse;
    }
}
