package software.amazon.opensearchserverless.collection;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CreateCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateCollectionResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final @NonNull AmazonWebServicesClientProxy proxy,
            final @NonNull ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
            final @NonNull Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        if (callbackContext == null && model.getId() != null) {
            throw new CfnInvalidRequestException("Id should not be set");
        }
        if(StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Name cannot be empty");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-OpenSearchServerless-Collection::Create", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToCreateRequest)
                        .makeServiceCall(this::createCollection)
                        .stabilize(this::stabilizeCollectionCreate)
                        .done((createCollectionRequest, createCollectionResponse, client, resourceModel, callbackContext1) -> {
                            resourceModel.setId(createCollectionResponse.createCollectionDetail().id());
                            return ProgressEvent.progress(resourceModel, callbackContext1);
                        })

            )
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Stabilization of Collection for the Create operation.
     * Returns true only if collection status is ACTIVE
     * @param createCollectionRequest the aws service request to create collection resource
     * @param createCollectionResponse the aws service response to create collection resource
     * @param proxyClient the aws service client to make the call
     * @param model the resource model
     * @param callbackContext the callback context for aws service request
     * @return
     */
    protected boolean stabilizeCollectionCreate(
            final @NonNull CreateCollectionRequest createCollectionRequest,
            final @NonNull CreateCollectionResponse createCollectionResponse,
            final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
            final @NonNull ResourceModel model,
            final CallbackContext callbackContext) {
        logger.log(String.format("Stabilize CollectionCreate for resource %s", createCollectionRequest));

        BatchGetCollectionRequest request = BatchGetCollectionRequest.builder().ids(createCollectionResponse.createCollectionDetail().id()).build();
        BatchGetCollectionResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::batchGetCollection);

        if (!response.hasCollectionDetails() || response.collectionDetails().size() != 1) {
            return false; //This actually retry the stabilization as the collection is not exist yet
        }
        CollectionDetail collectionDetail = response.collectionDetails().get(0);
        switch (collectionDetail.status()) {
            case ACTIVE:
                return true;
            case FAILED:
            case DELETING:
                //This is a failure case
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, collectionDetail.id());
            case CREATING:
            default:
                //The collection is not in ACTIVE state and stabilization can be retried.
                return false;
        }
    }

    /**
     * Create operation will be called.
     * @param createCollectionRequest the aws service request to create collection resource
     * @param proxyClient the aws service client to make the call
     * @return
     */
    private CreateCollectionResponse createCollection(
            final @NonNull CreateCollectionRequest createCollectionRequest,
            final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient) {
        final CreateCollectionResponse createCollectionResponse =
                proxyClient.injectCredentialsAndInvokeV2(createCollectionRequest, proxyClient.client()::createCollection);

        logger.log(String.format("%s successfully created for %s", ResourceModel.TYPE_NAME, createCollectionRequest));
        return createCollectionResponse;
    }
}
