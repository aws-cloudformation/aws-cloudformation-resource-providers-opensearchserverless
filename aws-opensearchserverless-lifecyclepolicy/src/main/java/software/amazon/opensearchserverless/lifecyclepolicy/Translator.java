package software.amazon.opensearchserverless.lifecyclepolicy;

import software.amazon.awssdk.services.opensearchserverless.model.LifecyclePolicyDetail;
import software.amazon.awssdk.services.opensearchserverless.model.LifecyclePolicyIdentifier;
import software.amazon.awssdk.services.opensearchserverless.model.LifecyclePolicySummary;
import software.amazon.awssdk.services.opensearchserverless.model.CreateLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateLifecyclePolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetLifecyclePolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ListLifecyclePoliciesRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListLifecyclePoliciesResponse;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateLifecyclePolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateLifecyclePolicyResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

    /**
     * Request to create lifecycle policy
     *
     * @param model resource model
     * @return awsRequest the aws service request to create lifecycle policy
     */
    static CreateLifecyclePolicyRequest translateToCreateRequest(final ResourceModel model) {
        return CreateLifecyclePolicyRequest.builder()
            .name(model.getName())
            .type(model.getType())
            .description(model.getDescription())
            .policy(model.getPolicy())
            .build();
    }

    /**
     * Translates lifecycle policy object from sdk into a resource model
     *
     * @param createLifecyclePolicyResponse the aws service create lifecycle policy response
     * @return model resource model
     */
    static ResourceModel translateFromCreateResponse(final CreateLifecyclePolicyResponse createLifecyclePolicyResponse) {
        return translateLifecyclePolicyDetailFromSDK(createLifecyclePolicyResponse.lifecyclePolicyDetail());
    }

    /**
     * Request to read lifecycle policy
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe lifecycle policy
     */
    static BatchGetLifecyclePolicyRequest translateToReadRequest(final ResourceModel model) {
        return BatchGetLifecyclePolicyRequest.builder()
            .identifiers(LifecyclePolicyIdentifier.builder()
                .name(model.getName())
                .type(model.getType())
                .build())
            .build();
    }

    /**
     * Translates LifecyclePolicies policy object from sdk into a resource model
     *
     * @param getLifecyclePolicyResponse the aws service describe LifecyclePolicies policy response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final BatchGetLifecyclePolicyResponse getLifecyclePolicyResponse) {
        return translateLifecyclePolicyDetailFromSDK(getLifecyclePolicyResponse.lifecyclePolicyDetails());
    }

    /**
     * Request to delete LifecyclePolicies policy
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete LifecyclePolicies policy
     */
    static DeleteLifecyclePolicyRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteLifecyclePolicyRequest.builder()
            .type(model.getType())
            .name(model.getName())
            .build();
    }

    /**
     * Request to update properties of a previously created LifecyclePolicies policy
     *
     * @param model                        resource model
     * @param currentLifecyclePolicyDetail previously LifecyclePolicies policy
     * @return awsRequest the aws service request to modify LifecyclePolicies policy
     */
    public static UpdateLifecyclePolicyRequest translateToUpdateRequest(ResourceModel model,
                                                                        LifecyclePolicyDetail currentLifecyclePolicyDetail) {
        return UpdateLifecyclePolicyRequest.builder()
            .type(model.getType())
            .name(model.getName())
            .policyVersion(currentLifecyclePolicyDetail.policyVersion())
            .policy(model.getPolicy())
            .description(model.getDescription())
            .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param updateLifecyclePolicyResponse the aws service update LifecyclePolicies policy response
     * @return model resource model
     */
    static ResourceModel translateFromUpdateResponse(final UpdateLifecyclePolicyResponse updateLifecyclePolicyResponse) {
        return translateLifecyclePolicyDetailFromSDK(updateLifecyclePolicyResponse.lifecyclePolicyDetail());
    }

    /**
     * Request to list resources
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListLifecyclePoliciesRequest translateToListRequest(ResourceModel model, String nextToken) {
        return ListLifecyclePoliciesRequest.builder()
            .type(model.getType())
            .nextToken(nextToken)
            .build();

    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param listLifecyclePoliciesResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListRequest(final ListLifecyclePoliciesResponse listLifecyclePoliciesResponse) {
        return streamOfOrEmpty(listLifecyclePoliciesResponse.lifecyclePolicySummaries())
            .map(Translator::translateLifecyclePolicySummaryFromSDK)
            .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
    }

    private static ResourceModel translateLifecyclePolicySummaryFromSDK(LifecyclePolicySummary lifecyclePolicySummary) {
        return ResourceModel.builder()
            .type(lifecyclePolicySummary.typeAsString())
            .name(lifecyclePolicySummary.name())
            .description(lifecyclePolicySummary.description())
            .build();
    }

    private static ResourceModel translateLifecyclePolicyDetailFromSDK(List<LifecyclePolicyDetail> lifecyclePolicyDetails) {
        final LifecyclePolicyDetail lifecyclePolicyDetail = lifecyclePolicyDetails.get(0);
        return ResourceModel.builder()
            .type(lifecyclePolicyDetail.typeAsString())
            .name(lifecyclePolicyDetail.name())
            .description(lifecyclePolicyDetail.description())
            .policy(lifecyclePolicyDetail.policy().toString())
            .build();
    }

    private static ResourceModel translateLifecyclePolicyDetailFromSDK(LifecyclePolicyDetail lifecyclePolicyDetail) {
        return ResourceModel.builder()
            .type(lifecyclePolicyDetail.typeAsString())
            .name(lifecyclePolicyDetail.name())
            .description(lifecyclePolicyDetail.description())
            .policy(lifecyclePolicyDetail.policy().toString())
            .build();
    }
}
