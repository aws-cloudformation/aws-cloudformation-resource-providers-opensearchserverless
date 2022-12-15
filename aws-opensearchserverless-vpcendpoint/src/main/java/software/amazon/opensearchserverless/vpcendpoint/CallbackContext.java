package software.amazon.opensearchserverless.vpcendpoint;

import software.amazon.awssdk.services.opensearchserverless.model.VpcEndpointDetail;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private VpcEndpointDetail currentVpcEndpointDetail;
    private int cleanupWaitCount;
}
