package software.amazon.opensearchserverless.accountsettings;

import software.amazon.awssdk.services.opensearchserverless.model.*;

public class Translator {

    /**
     * Request to update account settings
     *
     * @param model resource model
     * @return awsRequest the aws service request to create account settings
     */
    static UpdateAccountSettingsRequest translateToUpdateRequest(final ResourceModel model) {
        CapacityLimits capcityLimits = model.getCapacityLimits();
        return UpdateAccountSettingsRequest.builder()
            .capacityLimits(software.amazon.awssdk.services.opensearchserverless.model.CapacityLimits.builder()
                .maxIndexingCapacityInOCU(capcityLimits.getMaxIndexingCapacityInOCU())
                .maxSearchCapacityInOCU(capcityLimits.getMaxSearchCapacityInOCU())
                .build())
            .build();
    }


    /**
     * Request to read account settings
     *
     * @param ignoredModel resource model
     * @return awsRequest the aws service request to describe account settings
     */
    public static GetAccountSettingsRequest translateToReadRequest(final ResourceModel ignoredModel) {
        return GetAccountSettingsRequest.builder()
            .build();
    }

    /**
     * Translates account settings response from sdk into a resource model
     *
     * @param getAccountSettingsResponse the aws service describe account settings response
     * @param accountId the aws account Id
     * @return model resource model
     */
    public static ResourceModel translateFromReadResponse(final GetAccountSettingsResponse getAccountSettingsResponse,
                                                          String accountId) {
        software.amazon.awssdk.services.opensearchserverless.model.CapacityLimits capacityLimits =
            getAccountSettingsResponse.accountSettingsDetail().capacityLimits();

        return ResourceModel.builder()
            .accountId(accountId)
            .capacityLimits(CapacityLimits.builder()
                .maxIndexingCapacityInOCU(capacityLimits.maxIndexingCapacityInOCU())
                .maxSearchCapacityInOCU(capacityLimits.maxSearchCapacityInOCU())
                .build())
            .build();
    }

    /**
     * Translates account settings response from sdk into a resource model
     *
     * @param updateAccountSettingsResponse the aws service update  account settings response
     * @param accountId the aws account Id
     * @return model resource model
     */
    public static ResourceModel translateFromUpdateResponse(UpdateAccountSettingsResponse updateAccountSettingsResponse,
                                                            String accountId) {
        software.amazon.awssdk.services.opensearchserverless.model.CapacityLimits capacityLimits =
            updateAccountSettingsResponse.accountSettingsDetail().capacityLimits();

        return ResourceModel.builder()
            .accountId(accountId)
            .capacityLimits(CapacityLimits.builder()
                .maxIndexingCapacityInOCU(capacityLimits.maxIndexingCapacityInOCU())
                .maxSearchCapacityInOCU(capacityLimits.maxSearchCapacityInOCU())
                .build())
            .build();
    }
}
