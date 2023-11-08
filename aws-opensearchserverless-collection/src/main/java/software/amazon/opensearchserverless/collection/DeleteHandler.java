package software.amazon.opensearchserverless.collection;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    public DeleteHandler() {
        super();
    }

    public DeleteHandler(OpenSearchServerlessClient openSearchServerlessClient) {
        super(openSearchServerlessClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final @NonNull AmazonWebServicesClientProxy proxy,
            final @NonNull ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
            final @NonNull Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        if (StringUtils.isNullOrEmpty(model.getId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Id cannot be empty");
        }

        return proxy.initiate("AWS-OpenSearchServerless-Collection::Delete", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> deleteCollection(awsRequest, client, logger))
                .stabilize(this::stabilizeCollectionDelete)
                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(null));
    }

    private DeleteCollectionResponse deleteCollection(
            final DeleteCollectionRequest deleteCollectionRequest,
            final ProxyClient<OpenSearchServerlessClient> proxyClient,
            final Logger logger) {
        DeleteCollectionResponse deleteCollectionResponse;
        try {
            logger.log(String.format("Sending DeleteCollectionRequest: %s", deleteCollectionRequest));
            deleteCollectionResponse = proxyClient.injectCredentialsAndInvokeV2(deleteCollectionRequest, proxyClient.client()::deleteCollection);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, deleteCollectionRequest.id(), e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(deleteCollectionRequest.toString() + ", " + e.getMessage(), e);
        } catch (ConflictException e) {
            throw new CfnResourceConflictException(ResourceModel.TYPE_NAME, deleteCollectionRequest.id(), e.getMessage(), e);
        } catch (InternalServerException e) {
            throw new CfnServiceInternalErrorException("DeleteCollection", e);
        }
        logger.log(String.format("%s DeleteCollection successfully initiated. response: %s",
            ResourceModel.TYPE_NAME, deleteCollectionResponse));
        return deleteCollectionResponse;
    }

    /**
     * Stabilization of Collection for the Delete operation.
     *
     * @param deleteCollectionRequest  the aws service request to delete collection resource
     * @param deleteCollectionResponse the aws service response to delete collection resource
     * @param proxyClient              the aws service client to make the call
     * @param model                    the resource model
     * @param callbackContext          the callback context for aws service request
     * @return Returns true only if collection is not found.
     */
    private boolean stabilizeCollectionDelete(
            final @NonNull DeleteCollectionRequest deleteCollectionRequest,
            final @NonNull DeleteCollectionResponse deleteCollectionResponse,
            final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
            final @NonNull ResourceModel model,
            final CallbackContext callbackContext) {
        logger.log(String.format("Stabilize CollectionDelete for resource %s", deleteCollectionRequest));

        final BatchGetCollectionRequest request = BatchGetCollectionRequest.builder().ids(deleteCollectionRequest.id()).build();
        final BatchGetCollectionResponse batchGetCollectionResponse = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::batchGetCollection);

        if (batchGetCollectionResponse.collectionDetails().isEmpty()) {
            return true;
        } else if (batchGetCollectionResponse.collectionDetails().size() == 1) {
            final CollectionDetail collectionDetail = batchGetCollectionResponse.collectionDetails().get(0);
            if (collectionDetail.status().equals(CollectionStatus.DELETING)) {
                return false;
            }
        }
        throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, deleteCollectionRequest.id());
    }
}
