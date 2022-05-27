package software.amazon.opensearchserverless.collection;

import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;
import software.amazon.awssdk.services.opensearchserverless.model.CreateCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListCollectionsResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return CreateCollectionRequest the aws service request to create a resource
   */
  static CreateCollectionRequest translateToCreateRequest(final ResourceModel model) {
    return CreateCollectionRequest.builder()
            .name(model.getName())
            .description(model.getDescription())
            .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return BatchGetCollectionRequest the aws service request to describe a resource
   */
  static BatchGetCollectionRequest translateToReadRequest(final ResourceModel model) {
    return BatchGetCollectionRequest.builder()
            .ids(model.getId())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param batchGetCollectionResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final BatchGetCollectionResponse batchGetCollectionResponse) {
    CollectionDetail collectionDetail = batchGetCollectionResponse.collectionDetails().get(0);
    return ResourceModel.builder()
            .id(collectionDetail.id())
            .name(collectionDetail.name())
            .description(collectionDetail.description())
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return DeleteCollectionRequest the aws service request to delete a resource
   */
  static DeleteCollectionRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteCollectionRequest.builder()
            .id(model.getId())
            .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param listCollectionsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListCollectionsResponse listCollectionsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
    return streamOfOrEmpty(listCollectionsResponse.collectionSummaries())
            .filter(collectionDetail -> collectionDetail.status().equals(CollectionStatus.ACTIVE))
            .map(collectionDetail -> ResourceModel.builder()
                    .id(collectionDetail.id())
                    .name(collectionDetail.name())
                .build())
            .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }

}
