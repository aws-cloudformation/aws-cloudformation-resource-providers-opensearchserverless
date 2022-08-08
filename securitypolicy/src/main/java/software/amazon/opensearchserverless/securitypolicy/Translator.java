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
            .policyName(model.getPolicyName())
            .policyType(model.getPolicyType())
            .policyDescription(model.getPolicyDescription())
            .policyDocument(model.getPolicyDocument())
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
            .policyName(model.getPolicyName())
            .policyType(model.getPolicyType())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   *
   * @param getSecurityPolicyResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetSecurityPolicyResponse getSecurityPolicyResponse) {
    SecurityPolicyDetail securityPolicyDetail = getSecurityPolicyResponse.getSecurityPolicyDetail();
    return ResourceModel.builder()
            .policyName(securityPolicyDetail.policyName())
            .policyType(securityPolicyDetail.policyTypeAsString())
            .policyDescription(securityPolicyDetail.policyDescription())
            .policyDocument(securityPolicyDetail.policyDocument())
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
            .policyName(model.getPolicyName())
            .policyType(model.getPolicyType())
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
            .policyName(model.getPolicyName())
            .policyType(model.getPolicyType())
            .policyDescription(model.getPolicyDescription())
            .policyDocument(model.getPolicyDocument())
            .build();
  }

  /**
   * Request to list security policies
   *
   * @param model
   * @param nextToken token passed to the aws service list resources request
   * @return ListSecurityPoliciesRequest the aws service request to list resources within aws
   *         account
   */
  static ListSecurityPoliciesRequest translateToListRequest(ResourceModel model, final String nextToken) {
    return ListSecurityPoliciesRequest.builder()
            .policyType(model.getPolicyType())
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
    return streamOfOrEmpty(listSecurityPoliciesResponse.securityPolicySummaries())
            .map(resource -> ResourceModel.builder()
                    .policyName(resource.policyName())
                    .policyType(resource.policyTypeAsString())
                    .build())
            .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }

}
