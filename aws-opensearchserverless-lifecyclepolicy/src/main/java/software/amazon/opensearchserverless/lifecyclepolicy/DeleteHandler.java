package software.amazon.opensearchserverless.lifecyclepolicy;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteLifecyclePolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {

    public DeleteHandler() {
        super();
    }

    public DeleteHandler(OpenSearchServerlessClient openSearchServerlessClient) {
        super(openSearchServerlessClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
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

        return proxy.initiate("AWS-OpenSearchServerless-LifecyclePolicy::Delete", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToDeleteRequest)
            .makeServiceCall((awsRequest, client) -> deleteLifecyclePolicy(awsRequest, client, logger))
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(null));
    }

    private DeleteLifecyclePolicyResponse deleteLifecyclePolicy(final DeleteLifecyclePolicyRequest deleteLifecyclePolicyRequest,
                                                                final ProxyClient<OpenSearchServerlessClient> proxyClient,
                                                                final Logger logger) {
        DeleteLifecyclePolicyResponse deleteLifecyclePolicyResponse;
        try {
            logger.log(String.format("Sending delete lifecycle policy request: %s", deleteLifecyclePolicyRequest));
            deleteLifecyclePolicyResponse = proxyClient.injectCredentialsAndInvokeV2(deleteLifecyclePolicyRequest, proxyClient.client()::deleteLifecyclePolicy);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, String.format("Name:%s, Type:%s",
                deleteLifecyclePolicyRequest.name(), deleteLifecyclePolicyRequest.typeAsString()), e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(deleteLifecyclePolicyRequest.toString() + ", " + e.getMessage(), e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("DeleteLifecyclePolicy", e);
        }
        logger.log(String.format("%s successfully deleted. response: %s", ResourceModel.TYPE_NAME, deleteLifecyclePolicyResponse));
        return deleteLifecyclePolicyResponse;
    }
}
