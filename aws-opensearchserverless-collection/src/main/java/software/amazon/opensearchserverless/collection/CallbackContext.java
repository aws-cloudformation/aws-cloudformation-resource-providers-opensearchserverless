package software.amazon.opensearchserverless.collection;

import software.amazon.cloudformation.proxy.StdCallbackContext;

import lombok.Data;

@Data
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
}
