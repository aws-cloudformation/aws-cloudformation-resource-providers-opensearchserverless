package software.amazon.opensearchserverless.securityconfig;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityConfigRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityConfigResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.amazonaws.util.StringUtils;


public class CreateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();
        if (callbackContext == null && model.getId() != null) {
            throw new CfnInvalidRequestException("Id should not be set");
        }
        if (StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Name cannot be empty");
        }
        if (StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Type cannot be empty");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                            .then(progress ->
                                          proxy.initiate("AWS-OpenSearchServerless-SecurityConfig::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                               .translateToServiceRequest(Translator::translateToCreateRequest)
                                               .makeServiceCall((awsRequest, client) -> createSecurityConfig(awsRequest, client, logger))
                                               .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromCreateResponse(awsResponse))));
    }

    private CreateSecurityConfigResponse createSecurityConfig(
            final CreateSecurityConfigRequest createSecurityConfigRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        CreateSecurityConfigResponse createSecurityConfigResponse;
        try {
            createSecurityConfigResponse =
                    proxyClient.injectCredentialsAndInvokeV2(createSecurityConfigRequest, proxyClient.client()::createSecurityConfig);
        } catch (ConflictException e) {
            throw new CfnAlreadyExistsException(e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(createSecurityConfigRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully created for %s", ResourceModel.TYPE_NAME, createSecurityConfigRequest));
        return createSecurityConfigResponse;
    }
}
