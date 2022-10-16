package software.amazon.opensearchserverless.collection;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.ConflictException;
import software.amazon.awssdk.services.opensearchserverless.model.CreateCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.InternalServerException;
import software.amazon.awssdk.services.opensearchserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;


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
        if (StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Name cannot be empty");
        }

        if (StringUtils.isNullOrEmpty(model.getType())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Type cannot be empty");
        }

        Map<String, String> allDesiredTags = Maps.newHashMap();
        allDesiredTags.putAll(Optional.ofNullable(request.getDesiredResourceTags()).orElse(Collections.emptyMap()));
        allDesiredTags.putAll(Optional.ofNullable(request.getSystemTags()).orElse(Collections.emptyMap()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-OpenSearchServerless-Collection::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(cbModel -> Translator.translateToCreateRequest(cbModel, allDesiredTags))
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
     *
     * @param createCollectionRequest  the aws service request to create collection resource
     * @param createCollectionResponse the aws service response to create collection resource
     * @param proxyClient              the aws service client to make the call
     * @param resourceModel            the resource model
     * @param callbackContext          the callback context for aws service request
     * @return true only if collection status is ACTIVE
     */
    protected boolean stabilizeCollectionCreate(
        final @NonNull CreateCollectionRequest createCollectionRequest,
        final @NonNull CreateCollectionResponse createCollectionResponse,
        final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient,
        final @NonNull ResourceModel resourceModel,
        final CallbackContext callbackContext) {
        String id = createCollectionResponse.createCollectionDetail().id();
        resourceModel.setId(id);
        logger.log(String.format("Stabilize CollectionCreate for resource %s", resourceModel));
        BatchGetCollectionRequest request = Translator.translateToReadRequest(resourceModel);
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
     *
     * @param createCollectionRequest the aws service request to create collection resource
     * @param proxyClient             the aws service client to make the call
     * @return the aws service response
     */
    private CreateCollectionResponse createCollection(
        final @NonNull CreateCollectionRequest createCollectionRequest,
        final @NonNull ProxyClient<OpenSearchServerlessClient> proxyClient) {
        final CreateCollectionResponse createCollectionResponse;
        try {
            logger.log(String.format("sending create collection request: %s", createCollectionRequest));
            createCollectionResponse =
                proxyClient.injectCredentialsAndInvokeV2(createCollectionRequest, proxyClient.client()::createCollection);
        } catch (ConflictException e) {
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME,createCollectionRequest.name(),e);
        } catch (ValidationException e) {
            throw new CfnInvalidRequestException(createCollectionRequest.toString(), e);
        } catch (InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s successfully created. response: %s", ResourceModel.TYPE_NAME, createCollectionResponse));
        return createCollectionResponse;
    }
}
