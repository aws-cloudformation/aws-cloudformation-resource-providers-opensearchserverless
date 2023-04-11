package software.amazon.opensearchserverless.securitypolicy;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyResponse;
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

public class ReadHandler extends BaseHandlerStd {

    public ReadHandler() {
    }
    public ReadHandler(OpenSearchServerlessClient openSearchServerlessClient) {
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

        return proxy.initiate("AWS-OpenSearchServerless-SecurityPolicy::Read", proxyClient,
                request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((awsRequest, cbClient) -> getSecurityPolicy(awsRequest, cbClient, logger))
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(
                Translator.translateFromReadResponse(awsResponse)));
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
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, String.format("Name:%s, Type:%s",
                    getSecurityPolicyRequest.name(), getSecurityPolicyRequest.typeAsString()), e);
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
