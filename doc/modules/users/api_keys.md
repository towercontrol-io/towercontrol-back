## Api key creation as a self-service - aka API tokens

A user can create API keys associated with their account. These keys are JWTs whose duration can be chosen, including 
for long periods. Each key must be revocable individually and has its own salt, but the revocation of standard JWTs 
through the logout process must not impact API keys. through the logout process must not impact API keys. 
A role API_KEY_SALT_XXX with XXXX being the SALT id allows for quickly finding it.

API keys will be able to inherit all the roles of the user who creates them, with an additional role which is API_KEY. 
However, they will not have the 1FA or 2FA roles but LOGIN_API, indicating that no login has occurred.

Groups and ACLs can be selected one by one to match the key's permissions as precisely as possible. 
It is also possible to transform a GROUP access into a more restrictive ACL access.

### API token rights & User Rights 

An API key cannot have more rights than the user who creates it, and over time, the user's rights may decrease
(removal of roles, leaving groups, etc.). In this case, the API keys must be deleted (modification will not be 
possible) to reflect the new reality of this user.  
Consequently, the structure of API tokens contains information allowing for quick searches, even if this 
information is not used for rights validation when using the token, which itself contains these rights.  
For this reason, when creating an API token, it is important to associate it with a minimalist level of
rights to avoid the deletion of tokens linked to too broad rights that may be removed from the user later.

### Groups vs ACLs

An API key has no groups, so its operation relies exclusively on ACLs. Roles associated with the API key are limited and 
should not pertain to access rights, only to non-assignable technical rights. However, there may be specific rights required 
for custom developments that can be added by configuration, and these rights may be necessary to access certain APIs.

Therefore, ACLs carry all access rights of the API key; they must be specified when creating the API key and are verified 
during key creation.