package software.amazon.opensearchserverless.securityconfig;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityConfigRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityConfigResponse;
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

import com.amazonaws.util.StringUtils;

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

        final ResourceModel model = request.getDesiredResourceState();

        if (StringUtils.isNullOrEmpty(model.getId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Id cannot be empty");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                            .then(progress ->
                                          proxy.initiate("AWS-OpenSearchServerless-SecurityConfig::Read", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                               .translateToServiceRequest(Translator::translateToReadRequest)
                                               .makeServiceCall((awsRequest, client) -> getSecurityConfig(awsRequest, client, logger))
                                               .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse))));
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
