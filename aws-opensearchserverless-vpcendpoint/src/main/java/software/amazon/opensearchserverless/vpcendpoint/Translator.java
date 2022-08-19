package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetVpcEndpointResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CreateVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteVpcEndpointRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListVpcEndpointsResponse;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointDetail;
import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

  /**
   * Request to create a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateVpcEndpointRequest translateToCreateRequest(final ResourceModel model) {
    CreateVpcEndpointRequest.Builder createVpcEndpointRequestBuilder =
            CreateVpcEndpointRequest.builder().name(model.getName()).vpcId(model.getVpcId()).subnetIds(model.getSubnetIds());
    if (model.getSecurityGroupIds() != null) {
      createVpcEndpointRequestBuilder.securityGroupIds(model.getSecurityGroupIds());
    }
    return createVpcEndpointRequestBuilder.build();
  }

  /**
   * Request to read a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static BatchGetVpcEndpointRequest translateToReadRequest(final ResourceModel model) {
    return BatchGetVpcEndpointRequest.builder().ids(model.getId()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   *
   * @param batchGetVpcEndpointResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final BatchGetVpcEndpointResponse batchGetVpcEndpointResponse) {
    VpcEndpointDetail vpcEndpointDetail = batchGetVpcEndpointResponse.vpcEndpointDetails().get(0);
    return ResourceModel.builder()
                        .id(vpcEndpointDetail.id())
                        .name(vpcEndpointDetail.name())
                        .vpcId(vpcEndpointDetail.vpcId())
                        .subnetIds(vpcEndpointDetail.subnetIds())
                        .securityGroupIds(vpcEndpointDetail.securityGroupIds())
                        .build();
  }

  /**
   * Request to delete a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteVpcEndpointRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteVpcEndpointRequest.builder().id(model.getId()).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   *
   * @param listVpcEndpointsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListResponse(final ListVpcEndpointsResponse listVpcEndpointsResponse) {
    return streamOfOrEmpty(listVpcEndpointsResponse.vpcEndpointSummaries())
            .filter(vpcEndpointSummary -> vpcEndpointSummary.status().equals(VpcEndpointStatus.ACTIVE))
            .map(vpcEndpointSummary -> ResourceModel.builder().id(vpcEndpointSummary.id()).build())
            .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
                   .map(Collection::stream)
                   .orElseGet(Stream::empty);
  }
}
