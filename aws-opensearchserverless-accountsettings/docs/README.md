# AWS::OpenSearchServerless::AccountSettings

Definition of AWS::OpenSearchServerless::AccountSettings Resource Type

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::OpenSearchServerless::AccountSettings",
    "Properties" : {
        "<a href="#capacitylimits" title="CapacityLimits">CapacityLimits</a>" : <i><a href="capacitylimits.md">CapacityLimits</a></i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::OpenSearchServerless::AccountSettings
Properties:
    <a href="#capacitylimits" title="CapacityLimits">CapacityLimits</a>: <i><a href="capacitylimits.md">CapacityLimits</a></i>
</pre>

## Properties

#### CapacityLimits

_Required_: No

_Type_: <a href="capacitylimits.md">CapacityLimits</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the AccountId.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### AccountId

The identifier of the account
