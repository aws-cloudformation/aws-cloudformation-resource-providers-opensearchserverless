package software.amazon.opensearchserverless.lifecyclepolicy;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetLifecyclePolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
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

        return proxy.initiate("AWS-OpenSearchServerless-LifecyclePolicy::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((awsRequest, client) -> batchGetLifecyclePolicy(awsRequest, client, logger))
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }

    private BatchGetLifecyclePolicyResponse batchGetLifecyclePolicy(final BatchGetLifecyclePolicyRequest batchGetLifecyclePolicyRequest,
                                                                    final ProxyClient<OpenSearchServerlessClient> proxyClient,
                                                                    final Logger logger) {
        BatchGetLifecyclePolicyResponse batchGetLifecyclePolicyResponse;
        try {
            logger.log(String.format("Sending batchGet lifecycle policy request: %s", batchGetLifecyclePolicyRequest));
            batchGetLifecyclePolicyResponse = proxyClient.injectCredentialsAndInvokeV2(batchGetLifecyclePolicyRequest, proxyClient.client()::batchGetLifecyclePolicy);
            if (!batchGetLifecyclePolicyResponse.lifecyclePolicyDetails().isEmpty()) {
                logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                return batchGetLifecyclePolicyResponse;
            }
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, String.format("Name:%s, Type:%s",
                batchGetLifecyclePolicyRequest.identifiers().get(0).name(), batchGetLifecyclePolicyRequest.identifiers().get(0).typeAsString()));
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(batchGetLifecyclePolicyRequest.toString() + ", " + e.getMessage(), e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("BatchGetLifecyclePolicy", e);
        }
    }
}
