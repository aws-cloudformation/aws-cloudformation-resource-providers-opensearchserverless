package software.amazon.opensearchserverless.lifecyclepolicy;

import software.amazon.awssdk.services.opensearchserverless.model.LifecyclePolicyDetail;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private LifecyclePolicyDetail currentLifecyclePolicyDetail;
}
