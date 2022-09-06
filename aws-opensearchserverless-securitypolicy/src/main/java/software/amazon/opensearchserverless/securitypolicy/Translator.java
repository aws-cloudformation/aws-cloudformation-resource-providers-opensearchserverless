package software.amazon.opensearchserverless.securitypolicy;

import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityPolicyResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityPoliciesRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityPoliciesResponse;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityPolicyDetail;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityPolicyRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

    /**
     * Request to create a security policy
     *
     * @param model resource model
     * @return CreateSecurityPolicyRequest the aws service request to create a security policy
     */
    static CreateSecurityPolicyRequest translateToCreateRequest(final ResourceModel model) {
        return CreateSecurityPolicyRequest.builder()
                                          .name(model.getName())
                                          .type(model.getType())
                                          .description(model.getDescription())
                                          .policy(model.getPolicy())
                                          .build();
    }

    /**
     * Request to read a security policy
     *
     * @param model resource model
     * @return GetSecurityPolicyRequest the aws service request to describe a security policy
     */
    static GetSecurityPolicyRequest translateToReadRequest(final ResourceModel model) {
        return GetSecurityPolicyRequest.builder()
                                       .name(model.getName())
                                       .type(model.getType())
                                       .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param getSecurityPolicyResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final GetSecurityPolicyResponse getSecurityPolicyResponse) {
        SecurityPolicyDetail securityPolicyDetail = getSecurityPolicyResponse.securityPolicyDetail();
        return ResourceModel.builder()
                            .name(securityPolicyDetail.name())
                            .type(securityPolicyDetail.typeAsString())
                            .policyVersion(securityPolicyDetail.policyVersion())
                            .description(securityPolicyDetail.description())
                            .policy(securityPolicyDetail.policy())
                            .build();
    }

    /**
     * Request to delete a security policy
     *
     * @param model resource model
     * @return DeleteSecurityPolicyRequest the aws service request to delete a resource
     */
    static DeleteSecurityPolicyRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteSecurityPolicyRequest.builder()
                                          .name(model.getName())
                                          .type(model.getType())
                                          .build();
    }

    /**
     * Request to update properties of a previously created security policy
     *
     * @param model resource model
     * @return UpdateSecurityPolicyRequest the aws service request to modify a security policy
     */
    static UpdateSecurityPolicyRequest translateToFirstUpdateRequest(final ResourceModel model) {
        return UpdateSecurityPolicyRequest.builder()
                                          .name(model.getName())
                                          .type(model.getType())
                                          .policyVersion(model.getPolicyVersion())
                                          .description(model.getDescription())
                                          .policy(model.getPolicy())
                                          .build();
    }

    /**
     * Request to list security policies
     *
     * @param model resource model
     * @param nextToken token passed to the aws service list resources request
     * @return ListSecurityPoliciesRequest the aws service request to list resources within aws
     * account
     */
    static ListSecurityPoliciesRequest translateToListRequest(ResourceModel model, final String nextToken) {
        return ListSecurityPoliciesRequest.builder()
                                          .type(model.getType())
                                          .nextToken(nextToken)
                                          .build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary
     * identifier only)
     *
     * @param listSecurityPoliciesResponse the aws service describe security policy response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListRequest(final ListSecurityPoliciesResponse listSecurityPoliciesResponse) {
        return streamOfOrEmpty(listSecurityPoliciesResponse.securityPolicyDetails())
                .map(resource -> ResourceModel.builder()
                                              .name(resource.name())
                                              .type(resource.typeAsString())
                                              .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                       .map(Collection::stream)
                       .orElseGet(Stream::empty);
    }

}
