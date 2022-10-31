package software.amazon.opensearchserverless.accountsettings;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.*;

public class ReadHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final @NonNull AmazonWebServicesClientProxy proxy,
        final @NonNull ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
        final @NonNull Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if (!StringUtils.isNullOrEmpty(model.getAccountId())
            && !model.getAccountId().equals(request.getAwsAccountId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "Resource not found");
        }

        return proxy.initiate("AWS-OpenSearchServerless-AccountSettings::Read",
                proxyClient, model, callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((awsRequest, client) -> getAccountSettings(awsRequest, client, logger))
            .done(response -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(
                response, request.getAwsAccountId())));
    }

    private GetAccountSettingsResponse getAccountSettings(
        final GetAccountSettingsRequest getAccountSettingsRequest,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        GetAccountSettingsResponse getAccountSettingsResponse;
        try {
            logger.log(String.format("Sending get account settings request: %s", getAccountSettingsRequest));
            getAccountSettingsResponse = proxyClient.injectCredentialsAndInvokeV2(getAccountSettingsRequest,
                proxyClient.client()::getAccountSettings);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(getAccountSettingsRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return getAccountSettingsResponse;
    }
}
