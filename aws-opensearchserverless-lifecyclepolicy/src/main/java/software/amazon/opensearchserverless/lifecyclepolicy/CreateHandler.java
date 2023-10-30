package software.amazon.opensearchserverless.lifecyclepolicy;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.CreateLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateLifecyclePolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandlerStd {

    public CreateHandler() {
        super();
    }

    public CreateHandler(OpenSearchServerlessClient openSearchServerlessClient) {
        super(openSearchServerlessClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request,
                                                                          final CallbackContext callbackContext,
                                                                          final ProxyClient<OpenSearchServerlessClient> proxyClient,
                                                                          final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Name cannot be empty");
        }

        if (StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Type cannot be empty");
        }

        if (StringUtils.isNullOrEmpty(model.getPolicy())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Policy cannot be empty");
        }

        return proxy.initiate("AWS-OpenSearchServerless-LifecyclePolicy::Create", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToCreateRequest)
            .makeServiceCall((awsRequest, client) -> createLifecyclePolicy(awsRequest, client, logger))
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromCreateResponse(awsResponse)));
    }

    private CreateLifecyclePolicyResponse createLifecyclePolicy(final CreateLifecyclePolicyRequest createLifecyclePolicyRequest,
                                                                final ProxyClient<OpenSearchServerlessClient> proxyClient,
                                                                final Logger logger) {
        CreateLifecyclePolicyResponse createLifecyclePolicyResponse;
        try {
            logger.log(String.format("Sending create lifecycle policy request: %s", createLifecyclePolicyRequest));
            createLifecyclePolicyResponse = proxyClient.injectCredentialsAndInvokeV2(createLifecyclePolicyRequest, proxyClient.client()::createLifecyclePolicy);
        } catch (ConflictException e) {
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, String.format("Name:%s, Type:%s",
                createLifecyclePolicyRequest.name(), createLifecyclePolicyRequest.typeAsString()), e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(createLifecyclePolicyRequest.toString() + ", " + e.getMessage(), e);
        } catch (ServiceQuotaExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("CreateLifecyclePolicy", e);
        }
        logger.log(String.format("%s successfully created. response: %s", ResourceModel.TYPE_NAME, createLifecyclePolicyResponse));
        return createLifecyclePolicyResponse;
    }
}
