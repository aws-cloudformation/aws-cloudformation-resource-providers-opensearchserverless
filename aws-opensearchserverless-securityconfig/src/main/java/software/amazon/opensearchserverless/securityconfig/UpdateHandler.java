package software.amazon.opensearchserverless.securityconfig;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityConfigRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityConfigResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
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

        if(StringUtils.isNullOrEmpty(model.getId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Id cannot be empty");
        }

        if(StringUtils.isNullOrEmpty(model.getConfigVersion())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "ConfigVersion cannot be empty");
        }

        if(StringUtils.isNullOrEmpty(model.getDescription()) && model.getSamlOptions() == null) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "One of description or saml-options is required");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> proxy.initiate("AWS-OpenSearchServerless-SecurityConfig::Update", proxyClient, request.getDesiredResourceState(), callbackContext)
                    .translateToServiceRequest(Translator::translateToUpdateRequest)
                    .makeServiceCall((awsRequest, client) -> updateSecurityConfig(awsRequest, client, logger))
                    .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromUpdateResponse(awsResponse))));
    }

    private UpdateSecurityConfigResponse updateSecurityConfig(
            final UpdateSecurityConfigRequest updateSecurityConfigRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        UpdateSecurityConfigResponse updateSecurityConfigResponse;
        try {
            updateSecurityConfigResponse =
                    proxyClient.injectCredentialsAndInvokeV2(updateSecurityConfigRequest, proxyClient.client()::updateSecurityConfig);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(updateSecurityConfigRequest.toString(), e);
        } catch (ConflictException e) {
            throw new CfnResourceConflictException(ResourceModel.TYPE_NAME, updateSecurityConfigRequest.id(),e.getMessage(),e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully updated for %s", ResourceModel.TYPE_NAME, updateSecurityConfigRequest));
        return updateSecurityConfigResponse;
    }
}
