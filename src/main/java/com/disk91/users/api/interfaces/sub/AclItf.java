/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.users.api.interfaces.sub;

import com.disk91.groups.tools.GroupsHierarchySimplified;
import com.disk91.users.mdb.entities.sub.UserAcl;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Acls information", description = "Acls structure for front-end usage")
public class AclItf {

    @Schema(
            description = "Master ACL the user has access to",
            example = "Acl structure",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected UserAcl acl;

    @Schema(
            description = "Sub-acls",
            example = "List ACLS herited from sub-groups",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<AclItf> subs;


    // ==========================
    // Constructors

    public static AclItf getAclItfFromUserAcl(UserAcl _a) {
        AclItf a = new AclItf();
        a.setAcl(_a.clone());
        a.setSubs(new ArrayList<>());
        return a;
    }

    public static AclItf getAclItfFromGroupsHierarchySimplified(UserAcl _a, GroupsHierarchySimplified _g) {
        AclItf g = new AclItf();
        UserAcl acl = new UserAcl();
        if ( _a.getGroup().compareTo(_g.getShortId()) == 0 ) {
            // head
            acl.setLocalName(_a.getLocalName());
        } else {
            acl.setLocalName(_g.getName());
        }
        acl.setGroup(_g.getShortId());
        acl.setRoles(_a.getRoles());
        g.setAcl(acl);
        g.setSubs(new ArrayList<>());
        for ( GroupsHierarchySimplified sg : _g.getChildren() ) {
            g.getSubs().add(AclItf.getAclItfFromGroupsHierarchySimplified(_a,sg));
        }
        return g;
    }


    // ==========================
    // Getters & Setters


    public UserAcl getAcl() {
        return acl;
    }

    public void setAcl(UserAcl acl) {
        this.acl = acl;
    }

    public List<AclItf> getSubs() {
        return subs;
    }

    public void setSubs(List<AclItf> subs) {
        this.subs = subs;
    }
}
