package software.amazon.opensearchserverless.collection;

import lombok.NonNull;

import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;
import software.amazon.awssdk.services.opensearchserverless.model.CreateCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListCollectionsResponse;
import software.amazon.awssdk.services.opensearchserverless.model.Tag;
import software.amazon.awssdk.utils.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
  static CreateCollectionRequest translateToCreateRequest(final @NonNull ResourceModel model, final Map<String, String> desiredTags) {

    CreateCollectionRequest.Builder createCollectionRequestBuilder = CreateCollectionRequest.builder()
        .name(model.getName())
        .description(model.getDescription());

    if (!CollectionUtils.isNullOrEmpty(desiredTags)) {
      createCollectionRequestBuilder = createCollectionRequestBuilder.tags(translateModelTagsToSDK(desiredTags));
    }

    return createCollectionRequestBuilder.build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return BatchGetCollectionRequest the aws service request to describe a resource
   */
  static BatchGetCollectionRequest translateToReadRequest(final @NonNull ResourceModel model) {
    return BatchGetCollectionRequest.builder()
            .ids(model.getId())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param batchGetCollectionResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final @NonNull BatchGetCollectionResponse batchGetCollectionResponse) {
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
  static DeleteCollectionRequest translateToDeleteRequest(final @NonNull ResourceModel model) {
    return DeleteCollectionRequest.builder()
            .id(model.getId())
            .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param listCollectionsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final @NonNull ListCollectionsResponse listCollectionsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
    return streamOfOrEmpty(listCollectionsResponse.collectionSummaries())
            .filter(collectionDetail -> collectionDetail.status().equals(CollectionStatus.ACTIVE))
            .map(collectionDetail -> ResourceModel.builder()
                    .id(collectionDetail.id())
                    .name(collectionDetail.name())
                .build())
            .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final @NonNull Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }

  /**
   * Translate Map of Tags to SDK Tags
   * @param tags
   * @return
   */
  public static List<Tag> translateModelTagsToSDK(final Map<String, String> tags) {
    return tags.entrySet().stream()
            .map(e -> Tag.builder()
                    .key(e.getKey())
                    .value(e.getValue())
                    .build())
            .collect(Collectors.toList());
  }

}
