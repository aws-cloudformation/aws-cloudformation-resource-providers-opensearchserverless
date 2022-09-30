package software.amazon.opensearchserverless.securityconfig;

import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityConfigRequest;
import software.amazon.awssdk.services.opensearchserverless.model.CreateSecurityConfigResponse;
import software.amazon.awssdk.services.opensearchserverless.model.DeleteSecurityConfigRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityConfigRequest;
import software.amazon.awssdk.services.opensearchserverless.model.GetSecurityConfigResponse;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityConfigsRequest;
import software.amazon.awssdk.services.opensearchserverless.model.ListSecurityConfigsResponse;
import software.amazon.awssdk.services.opensearchserverless.model.SecurityConfigDetail;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityConfigRequest;
import software.amazon.awssdk.services.opensearchserverless.model.UpdateSecurityConfigResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

    /**
     * Request to create security config
     *
     * @param model resource model
     * @return awsRequest the aws service request to create security config
     */
    static CreateSecurityConfigRequest translateToCreateRequest(final ResourceModel model) {
        return CreateSecurityConfigRequest.builder()
                                          .type(model.getType())
                                          .name(model.getName())
                                          .description(model.getDescription())
                                          .samlOptions(translateSamlConfigOptionsToSDK(model.getSamlOptions()))
                                          .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param createSecurityConfigResponse the aws service create resource response
     * @return model resource model
     */
    static ResourceModel translateFromCreateResponse(final CreateSecurityConfigResponse createSecurityConfigResponse) {
        return translateSecurityConfigDetailFromSDK(createSecurityConfigResponse.securityConfigDetail());
    }

    /**
     * Request to read security config
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe security config
     */
    static GetSecurityConfigRequest translateToReadRequest(final ResourceModel model) {
        return GetSecurityConfigRequest.builder().id(model.getId()).build();
    }

    /**
     * Translates security config object from sdk into a resource model
     *
     * @param getSecurityConfigResponse the aws service describe security config response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final GetSecurityConfigResponse getSecurityConfigResponse) {
        return translateSecurityConfigDetailFromSDK(getSecurityConfigResponse.securityConfigDetail());
    }

    /**
     * Request to delete security config
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete security config
     */
    static DeleteSecurityConfigRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteSecurityConfigRequest.builder().id(model.getId()).build();
    }

    /**
     * Request to update properties of a previously created security config
     *
     * @param model resource model
     * @return awsRequest the aws service request to modify security config
     */
    public static UpdateSecurityConfigRequest translateToUpdateRequest(ResourceModel model) {
        UpdateSecurityConfigRequest.Builder builder =
                UpdateSecurityConfigRequest.builder().id(model.getId()).configVersion(model.getConfigVersion());

        if (model.getDescription() != null) {
            builder.description(model.getDescription());
        }
        if (model.getSamlOptions() != null) {
            builder.samlOptions(translateSamlConfigOptionsToSDK(model.getSamlOptions()));
        }
        return builder.build();
    }

    /**
     * Translates security config object from sdk into a resource model
     *
     * @param updateSecurityConfigResponse the aws service update security config response
     * @return model resource model
     */
    static ResourceModel translateFromUpdateResponse(final UpdateSecurityConfigResponse updateSecurityConfigResponse) {
        return translateSecurityConfigDetailFromSDK(updateSecurityConfigResponse.securityConfigDetail());
    }

    /**
     * Request to list security configs
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list security configs within aws account
     */
    static ListSecurityConfigsRequest translateToListRequest(ResourceModel model, String nextToken) {
        return ListSecurityConfigsRequest.builder()
                                         .type(model.getType())
                                         .nextToken(nextToken)
                                         .build();
    }

    /**
     * Translates security config objects from sdk into a resource model (primary identifier only)
     *
     * @param listSecurityConfigsResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListRequest(final ListSecurityConfigsResponse listSecurityConfigsResponse) {
        return streamOfOrEmpty(listSecurityConfigsResponse.securityConfigDetails())
                .map(Translator::translateSecurityConfigDetailFromSDK)
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                       .map(Collection::stream)
                       .orElseGet(Stream::empty);
    }

    /**
     * Translate resource model SamlConfigOptions to SDK SamlConfigOptions
     *
     * @param samlConfigOptions resource model SamlConfigOptions
     * @return SDK SamlConfigOptions
     */
    public static software.amazon.awssdk.services.opensearchserverless.model.SamlConfigOptions translateSamlConfigOptionsToSDK(final SamlConfigOptions samlConfigOptions) {
        return samlConfigOptions == null ? null :
               software.amazon.awssdk.services.opensearchserverless.model.SamlConfigOptions.builder()
                                                                                           .metadata(samlConfigOptions.getMetadata())
                                                                                           .userAttribute(samlConfigOptions.getUserAttribute())
                                                                                           .groupAttribute(samlConfigOptions.getGroupAttribute())
                                                                                           .sessionTimeout(samlConfigOptions.getSessionTimeout())
                                                                                           .build();
    }

    /**
     * Translate resource model SamlConfigOptions from SDK SamlConfigOptions
     *
     * @param samlConfigOptions SDK SamlConfigOptions
     * @return resource model SamlConfigOptions
     */
    public static SamlConfigOptions translateSamlConfigOptionsFromSDK(final software.amazon.awssdk.services.opensearchserverless.model.SamlConfigOptions samlConfigOptions) {
        return samlConfigOptions == null ? null :
               SamlConfigOptions.builder()
                                .metadata(samlConfigOptions.metadata())
                                .userAttribute(samlConfigOptions.userAttribute())
                                .groupAttribute(samlConfigOptions.groupAttribute())
                                .sessionTimeout(samlConfigOptions.sessionTimeout())
                                .build();
    }

    private static ResourceModel translateSecurityConfigDetailFromSDK(SecurityConfigDetail securityConfigDetail) {
        return ResourceModel.builder()
                            .id(securityConfigDetail.id())
                            .configVersion(securityConfigDetail.configVersion())
                            .description(securityConfigDetail.description())
                            .samlOptions(translateSamlConfigOptionsFromSDK(securityConfigDetail.samlOptions()))
                            .build();
    }
}
