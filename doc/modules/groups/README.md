## Group Management module
This module is responsible for managing the ownership of devices to one or more groups and consequently managing common 
rights for multiple objects. The concept of a group is primarily administrative; it allows determining who has the 
right to access specific data. It also enables linking configurations common to multiple objects. These 
configurations are related to administrative management and primarily concern operations such as billing, 
integration keys with third-party platforms, alarming rules, etc.

More technical configurations directly associated with the object, such as data transformation, are managed differently 
through the concept of object type/profile.

The functions covered by this module are as follows:
[ ] Group management
[ ] Group creation
[ ] Group modification
[ ] Group membership management
[ ] Group rights management

A device, or any other component that can be attached to a group, may belong to multiple groups. In such cases, 
configurations are cumulative. For example, if different alerting rules are applied across multiple groups for the same 
element, each event can trigger alerts in each of the groups. In terms of access rights, write access to one group 
allows modification of the elements within it, even if access is restricted in other groups.

Users have rights, granted by [roles](../users/dynrole_structucre.md), over groups. For each user, a virtual group with their login name exists. 
In the absence of a group, it is possible to attach elements to the user in this way without additional complexity. 
A user's rights to a group are associated through the user's [ACLs](../users/user_structure.md) The roles assigned to the user apply only to 
the virtual group.

Groups are not hierarchical; there is no configuration inheritance between groups. However, it will be possible in the 
future to consider group templates to facilitate the creation of similar groups, particularly regarding configuration aspects. 

A group own a list of attributes that can be used by other modules. These attributes are not directly related to the 
group itself but are a list of additional configuration the other module can consume. As an example, a billing module 
need to manage a credit volume and a billing information. An attribute named `billing` will have multiple parameters: 
user login used as billing information, current credit balance, billing profile reference, etc.

A group, however, can have references to other groups for specific purposes. For example, for billing management, 
a billable group will reference a billing group, allowing costs to be shared between multiple groups and linking t
o the billing information carried by a user. A group have a list of referring groups, which are groups that are referenced 
by this group. The system will search in this list for the attributes a module need. The referring group is also a 
way to define a common default attribute set. The system does not cascade search in the referring group.

Groups are not deleted; they can be referenced in many places, and their deletion could lead to inconsistencies. They can 
be deactivated in this way, the rights associated with the groups are removed, but the references are retained. The 
simplest way to make a group "disappear" is to remove its references in the user profiles that you control, if applicable.

### Virtual owner's group

In many applications, it is not necessary to use a group entity because objects belong only to a user and are not shared. 
Instead of having database records per user, it is simpler to have a virtual group named after the user. This group has an 
ID `user_` followed by the user's login. It is dynamically initialized by the software as if it were a user group. 
The user's rights on this group are the same as the user's rights. No attributes can be attached to this type of group; 
it is also identifiable through the `virtual` field in the group structure. Its name and description are internalized and 
can be renamed in the front-end using `group-default-group` and `group-default-group-description`. A user can share 
their personal group with other users using ACLs.

### Group hierarchy
Groups are organized hierarchically, defined by a hierarchical key, but a group can belong to multiple
hierarchies. The principle is based on a unique ID and a dependency field. The dependency field will list

| Group | 1   | 2   | 3       | 4   | 5       |
|-------|-----|-----|---------|-----|---------|
| Key   | XYZ | ABC | 123     | ERT | TTT     |
| Dep   |     | XYZ | XYZ,ABC |     | XYZ,ABC |

Rights on ERT only allow access to group 4
Rights on ABC grant access to Groups 2 and 3
Rights on XYZ grant access to Groups 1, 2, and 3

Hierarchical view:
```
  (1) XYZ    (4) ERT
   |
  (2) ABC   
   +----------+    
   |          |  
  (3) 123   (5) TTT
```

### Root group
A virtual group named `root` exists to indicate you have a full admin/view... on all the groups. This is not a real group,
but a kind of shortcut used for super-admin users. super-admin is by default in that group and it can be affected to other
admin when a global delegation is required.

