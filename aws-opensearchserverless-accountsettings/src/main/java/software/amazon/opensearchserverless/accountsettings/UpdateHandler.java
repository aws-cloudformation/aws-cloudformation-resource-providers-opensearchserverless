package software.amazon.opensearchserverless.accountsettings;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateAccountSettingsRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateAccountSettingsResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.*;

public class UpdateHandler extends BaseHandlerStd {

    public UpdateHandler() {
        super();
    }

    public UpdateHandler(OpenSearchServerlessClient openSearchServerlessClient) {
        super(openSearchServerlessClient);
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if (!StringUtils.isNullOrEmpty(model.getAccountId())
            && !model.getAccountId().equals(request.getAwsAccountId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound,
                "Resource not found");
        }

        if (model.getCapacityLimits() == null) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Provide capacity limits.");
        }
        CapacityLimits capacityLimits = model.getCapacityLimits();
        if (capacityLimits.getMaxIndexingCapacityInOCU() == null
            && capacityLimits.getMaxSearchCapacityInOCU() == null) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                "Provide  at least one capacity limit.");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> proxy.initiate("AWS-OpenSearchServerless-AccountSettings::Update", proxyClient,
                    progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateRequest)
                .makeServiceCall((awsRequest, client) -> updateAccountSettings(awsRequest, client, logger))
                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromUpdateResponse(
                    awsResponse, request.getAwsAccountId()))));
    }

    private UpdateAccountSettingsResponse updateAccountSettings(
        final UpdateAccountSettingsRequest updateAccountSettingsRequest,
        final ProxyClient<OpenSearchServerlessClient> proxyClient,
        final Logger logger) {
        UpdateAccountSettingsResponse updateAccountSettingsResponse;
        try {
            logger.log(String.format("Sending update account settings request: %s", updateAccountSettingsRequest));
            updateAccountSettingsResponse = proxyClient.injectCredentialsAndInvokeV2(updateAccountSettingsRequest,
                proxyClient.client()::updateAccountSettings);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(updateAccountSettingsRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully updated. response: %s", ResourceModel.TYPE_NAME,
            updateAccountSettingsResponse));
        return updateAccountSettingsResponse;
    }
}
