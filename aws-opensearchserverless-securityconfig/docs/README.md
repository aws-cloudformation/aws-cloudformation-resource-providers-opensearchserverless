# AWS::OpenSearchServerless::SecurityConfig

Amazon OpenSearchServerless security config resource

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::OpenSearchServerless::SecurityConfig",
    "Properties" : {
        "<a href="#description" title="Description">Description</a>" : <i>String</i>,
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#samloptions" title="SamlOptions">SamlOptions</a>" : <i><a href="samlconfigoptions.md">SamlConfigOptions</a></i>,
        "<a href="#type" title="Type">Type</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::OpenSearchServerless::SecurityConfig
Properties:
    <a href="#description" title="Description">Description</a>: <i>String</i>
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#samloptions" title="SamlOptions">SamlOptions</a>: <i><a href="samlconfigoptions.md">SamlConfigOptions</a></i>
    <a href="#type" title="Type">Type</a>: <i>String</i>
</pre>

## Properties

#### Description

Security config description

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>1000</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Name

The friendly name of the security config

_Required_: Yes

_Type_: String

_Minimum_: <code>3</code>

_Maximum_: <code>32</code>

_Pattern_: <code>^[a-z][a-z0-9-]{2,31}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SamlOptions

Describes saml options in form of key value map

_Required_: No

_Type_: <a href="samlconfigoptions.md">SamlConfigOptions</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Type

Config type for security config

_Required_: Yes

_Type_: String

_Allowed Values_: <code>saml</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Id.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### ConfigVersion

The version of the policy

#### Id

The identifier of the security config

