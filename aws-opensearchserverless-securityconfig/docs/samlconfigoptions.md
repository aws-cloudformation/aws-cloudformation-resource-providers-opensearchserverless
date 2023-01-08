# AWS::OpenSearchServerless::SecurityConfig SamlConfigOptions

Describes saml options in form of key value map

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#metadata" title="Metadata">Metadata</a>" : <i>String</i>,
    "<a href="#userattribute" title="UserAttribute">UserAttribute</a>" : <i>String</i>,
    "<a href="#groupattribute" title="GroupAttribute">GroupAttribute</a>" : <i>String</i>,
    "<a href="#sessiontimeout" title="SessionTimeout">SessionTimeout</a>" : <i>Integer</i>
}
</pre>

### YAML

<pre>
<a href="#metadata" title="Metadata">Metadata</a>: <i>String</i>
<a href="#userattribute" title="UserAttribute">UserAttribute</a>: <i>String</i>
<a href="#groupattribute" title="GroupAttribute">GroupAttribute</a>: <i>String</i>
<a href="#sessiontimeout" title="SessionTimeout">SessionTimeout</a>: <i>Integer</i>
</pre>

## Properties

#### Metadata

The XML saml provider metadata document that you want to use

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>20480</code>

_Pattern_: <code>[\u0009\u000A\u000D\u0020-\u007E\u00A1-\u00FF]+</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### UserAttribute

Custom attribute for this saml integration

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>2048</code>

_Pattern_: <code>[\w+=,.@-]+</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### GroupAttribute

Group attribute for this saml integration

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>2048</code>

_Pattern_: <code>[\w+=,.@-]+</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SessionTimeout

Defines the session timeout in minutes

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

