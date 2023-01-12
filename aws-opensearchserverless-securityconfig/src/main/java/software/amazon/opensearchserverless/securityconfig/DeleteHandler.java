package software.amazon.opensearchserverless.securityconfig;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteSecurityConfigRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteSecurityConfigResponse;
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

        if (StringUtils.isNullOrEmpty(model.getId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Id cannot be empty");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                            .then(progress ->
                                          proxy.initiate("AWS-OpenSearchServerless-SecurityConfig::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                               .translateToServiceRequest(Translator::translateToDeleteRequest)
                                               .makeServiceCall((awsRequest, client) -> deleteSecurityConfig(awsRequest, client, logger))
                                               .done(awsResponse -> ProgressEvent.defaultSuccessHandler(null)));
    }

    private DeleteSecurityConfigResponse deleteSecurityConfig(
            final DeleteSecurityConfigRequest deleteSecurityConfigRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        DeleteSecurityConfigResponse deleteSecurityConfigResponse;
        try {
            deleteSecurityConfigResponse =
                    proxyClient.injectCredentialsAndInvokeV2(deleteSecurityConfigRequest, proxyClient.client()::deleteSecurityConfig);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(deleteSecurityConfigRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully deleted for %s", ResourceModel.TYPE_NAME, deleteSecurityConfigRequest));
        return deleteSecurityConfigResponse;
    }

}
