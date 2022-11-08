# AWS::OpenSearchServerless::AccessPolicy

Amazon OpenSearchServerless access policy resource

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::OpenSearchServerless::AccessPolicy",
    "Properties" : {
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#type" title="Type">Type</a>" : <i>String</i>,
        "<a href="#description" title="Description">Description</a>" : <i>String</i>,
        "<a href="#policy" title="Policy">Policy</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::OpenSearchServerless::AccessPolicy
Properties:
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#type" title="Type">Type</a>: <i>String</i>
    <a href="#description" title="Description">Description</a>: <i>String</i>
    <a href="#policy" title="Policy">Policy</a>: <i>String</i>
</pre>

## Properties

#### Name

The name of the policy

_Required_: No

_Type_: String

_Minimum_: <code>3</code>

_Maximum_: <code>32</code>

_Pattern_: <code>^[a-z][a-z0-9-]{2,31}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Type

The possible types for the access policy

_Required_: No

_Type_: String

_Allowed Values_: <code>data</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Description

The description of the policy

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>1000</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Policy

The JSON policy document that is the content for the policy

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>20480</code>

_Pattern_: <code>[\u0009\u000A\u000D\u0020-\u007E\u00A1-\u00FF]+</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### PolicyVersion

Returns the <code>PolicyVersion</code> value.

