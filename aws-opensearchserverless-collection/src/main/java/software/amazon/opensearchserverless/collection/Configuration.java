package software.amazon.opensearchserverless.collection;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-opensearchserverless-collection.json");
    }

    /**
     * Providers should override this method if their resource has a 'Tags' property to define resource-level tags
     * @return Map containing tag keys and values.
     */
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        return Optional.ofNullable(resourceModel.getTags()).orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue, (value1, value2) -> value2));
    }
}
