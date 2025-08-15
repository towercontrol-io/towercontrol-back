## Api key creation as a self-service - @TODO

A user can create API keys associated with their account. These keys are JWTs whose duration can be chosen, including 
for long periods. Each key must be revocable individually and has its own salt, but the revocation of standard JWTs 
through the logout process must not impact API keys. through the logout process must not impact API keys. 
A role API_KEY_SALT_XXX with XXXX being the SALT id allows for quickly finding it.

API keys will be able to inherit all the roles of the user who creates them, with an additional role which is API_KEY. 
However, they will not have the 1FA or 2FA roles, indicating that no login has occurred.

Groups and ACLs can be selected one by one to match the key's permissions as precisely as possible. 
It is also possible to transform a GROUP access into a more restrictive ACL access.

