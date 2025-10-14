## Role details

This page provides more information about the available roles and the authorizations they allow.


### `ROLE_GOD_ADMIN`

This role is the super admin role, it does not give specific permissions but influence the other role to give full access 
on not owned artifacts.

### `ROLE_USER_ADMIN`

This role is the user admin role, it has the right to manage users. 
#### Perimeter: 
- The `USER_ADMIN` can manage all the users sharing a Group (not ACLs) in common.
- Only the Roles the `USER_ADMIN` owns 
#### Actions allowed:
- Create users
- Deactivate users
- Modify User Roles
- Update user profile (tbd)
- Assign / unassign groups & ACLs within the `USER_ADMIN` groups & ACLs

### `ROLE_GROUP_ADMIN`

This role manage the group creation, configuration.
#### Perimeter:
- The `GROUP_ADMIN` can manage all the groups he owns and their sub-groups
- The `GROUP_ADMIN` can create groups on the Root (no parent) hierarchy level

### `ROLE_GROUP_LADMIN`:

This role allows a user to create and configure an owned group and self assign to it.
#### Perimeter:
- The `GROUP_LADMIN` can manage all the groups he owns and their sub-groups
- The `GROUP_LADMIN` can create groups only under groups he owns


### `ROLE_DEVICE_ADMIN`
this role allows to register devices

### `ROLE_DEVICE_READ`
this role gives access to owned device data.

### `ROLE_DEVICE_WRITE`
this role gives access to device description metadata modifications.

### `ROLE_DEVICE_CONFIG`
this role allows to modify the device technical configuration

### `ROLE_DEVICE_ALERTING`
this role allows to receive device alerts

### `ROLE_BACKEND_CAPTURE`
this role is dedicated to technical account allowed to report device data to the platform
