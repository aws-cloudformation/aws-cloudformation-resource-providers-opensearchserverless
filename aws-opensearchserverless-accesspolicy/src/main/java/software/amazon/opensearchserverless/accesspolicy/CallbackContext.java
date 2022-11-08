package software.amazon.opensearchserverless.accesspolicy;

import software.amazon.awssdk.services.opensearchserverless.model.AccessPolicyDetail;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private AccessPolicyDetail currentAccessPolicyDetail;
}
