package software.amazon.opensearchserverless.accesspolicy;

import software.amazon.awssdk.services.opensearchserverless.model.AccessPolicyDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CreateAccessPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateAccessPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteAccessPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetAccessPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetAccessPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ListAccessPoliciesRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListAccessPoliciesResponse;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateAccessPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateAccessPolicyResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

    /**
     * Request to create access policy
     *
     * @param model resource model
     * @return awsRequest the aws service request to create access policy
     */
    static CreateAccessPolicyRequest translateToCreateRequest(final ResourceModel model) {
        return CreateAccessPolicyRequest.builder()
                                        .name(model.getName())
                                        .type(model.getType())
                                        .description(model.getDescription())
                                        .policy(model.getPolicy())
                                        .build();
    }

    /**
     * Translates access policy object from sdk into a resource model
     *
     * @param createAccessPolicyResponse the aws service create access policy response
     * @return model resource model
     */
    static ResourceModel translateFromCreateResponse(final CreateAccessPolicyResponse createAccessPolicyResponse) {
        return translateAccessPolicyDetailFromSDK(createAccessPolicyResponse.accessPolicyDetail());
    }

    /**
     * Request to read access policy
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe access policy
     */
    static GetAccessPolicyRequest translateToReadRequest(final ResourceModel model) {
        return GetAccessPolicyRequest.builder()
                                     .name(model.getName())
                                     .type(model.getType())
                                     .build();
    }

    /**
     * Translates access policy object from sdk into a resource model
     *
     * @param getAccessPolicyResponse the aws service describe access policy response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final GetAccessPolicyResponse getAccessPolicyResponse) {
        return translateAccessPolicyDetailFromSDK(getAccessPolicyResponse.accessPolicyDetail());
    }

    /**
     * Request to delete access policy
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete access policy
     */
    static DeleteAccessPolicyRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteAccessPolicyRequest.builder()
                                        .type(model.getType())
                                        .name(model.getName())
                                        .build();
    }

    /**
     * Request to update properties of a previously created access policy
     *
     * @param model resource model
     * @return awsRequest the aws service request to modify access policy
     */
    public static UpdateAccessPolicyRequest translateToUpdateRequest(ResourceModel model) {
        UpdateAccessPolicyRequest.Builder builder = UpdateAccessPolicyRequest.builder()
                                                                             .type(model.getType())
                                                                             .name(model.getName())
                                                                             .policyVersion(model.getPolicyVersion());
        if (model.getDescription() != null) {
            builder.description(model.getDescription());
        }
        if (model.getPolicy() != null) {
            builder.policy(model.getPolicy());
        }
        return builder.build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param updateAccessPolicyResponse the aws service update access policy response
     * @return model resource model
     */
    static ResourceModel translateFromUpdateResponse(final UpdateAccessPolicyResponse updateAccessPolicyResponse) {
        return translateAccessPolicyDetailFromSDK(updateAccessPolicyResponse.accessPolicyDetail());
    }

    /**
     * Request to list resources
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListAccessPoliciesRequest translateToListRequest(ResourceModel model, String nextToken) {
        return ListAccessPoliciesRequest.builder()
                                        .type(model.getType())
                                        .nextToken(nextToken)
                                        .build();

    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param listAccessPoliciesResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListRequest(final ListAccessPoliciesResponse listAccessPoliciesResponse) {
        return streamOfOrEmpty(listAccessPoliciesResponse.accessPolicyDetails())
                .map(Translator::translateAccessPolicyDetailFromSDK)
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                       .map(Collection::stream)
                       .orElseGet(Stream::empty);
    }

    private static ResourceModel translateAccessPolicyDetailFromSDK(AccessPolicyDetail accessPolicyDetail) {
        return ResourceModel.builder()
                .type(accessPolicyDetail.typeAsString())
                .name(accessPolicyDetail.name())
                .policyVersion(accessPolicyDetail.policyVersion())
                .description(accessPolicyDetail.description())
                .policy(accessPolicyDetail.policy())
                .build();
    }
}
