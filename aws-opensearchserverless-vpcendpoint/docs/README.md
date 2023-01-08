# AWS::OpenSearchServerless::VpcEndpoint

Amazon OpenSearchServerless vpc endpoint resource

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::OpenSearchServerless::VpcEndpoint",
    "Properties" : {
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#subnetids" title="SubnetIds">SubnetIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#vpcid" title="VpcId">VpcId</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::OpenSearchServerless::VpcEndpoint
Properties:
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>: <i>
      - String</i>
    <a href="#subnetids" title="SubnetIds">SubnetIds</a>: <i>
      - String</i>
    <a href="#vpcid" title="VpcId">VpcId</a>: <i>String</i>
</pre>

## Properties

#### Name

The name of the VPC Endpoint

_Required_: Yes

_Type_: String

_Minimum Length_: <code>3</code>

_Maximum Length_: <code>32</code>

_Pattern_: <code>^[a-z][a-z0-9-]{2,31}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SecurityGroupIds

The ID of one or more security groups to associate with the endpoint network interface

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SubnetIds

The ID of one or more subnets in which to create an endpoint network interface

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VpcId

The ID of the VPC in which the endpoint will be used.

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>255</code>

_Pattern_: <code>^vpc-[0-9a-z]*$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Id.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Id

The identifier of the VPC Endpoint

