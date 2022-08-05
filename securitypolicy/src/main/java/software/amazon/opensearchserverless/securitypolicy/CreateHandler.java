package software.amazon.opensearchserverless.securitypolicy;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityPolicyResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-OpenSearchServerless-SecurityPolicy::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall((awsRequest, cbClient) -> createSecurityPolicy(awsRequest, cbClient, logger))
                                .progress()
                )
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private CreateSecurityPolicyResponse createSecurityPolicy(
            final CreateSecurityPolicyRequest createSecurityPolicyRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        final CreateSecurityPolicyResponse createSecurityPolicyResponse =
                proxyClient.injectCredentialsAndInvokeV2(createSecurityPolicyRequest, proxyClient.client()::createSecurityPolicy);
        logger.log(String.format("%s successfully created for %s", ResourceModel.TYPE_NAME, createSecurityPolicyRequest));
        return createSecurityPolicyResponse;
    }
}
