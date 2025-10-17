/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2025.
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
package com.disk91.groups.tools;

import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.groups.mdb.entities.Group;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Group Hierarchy", description = "Describes one group hierarchy with childrens")
public class GroupsHierarchySimplified {

    @Schema(
            description = "short ID of the group",
            example = "HgbE5ErU",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String shortId;


    @Schema(
            description = "name of the group",
            example = "My devices",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String name;

    @Schema(
            description = "description of the group",
            example = "my favorite devices",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String description;


    @Schema(
            description = "children of that group",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<GroupsHierarchySimplified> children;

    @Schema(
            description = "Special rights associated to that group for the user, only applicable to ACLs",
            example = "['ROLE_GROUP_LADMIN', 'ROLE_DEVICE_READ']",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<String> roles;

    // ============================================================================
    // Builder
    // ============================================================================

    /**
     * Recursively build the hierarchy response from a hierarchy node
     * Filter the data to only keep the active groups.
     * @param ghn - Group Hierarchy Node
     * @return the group hierarchy response
     * @throws ITRightException
     */
    protected static GroupsHierarchySimplified buildFromHierarchyNode(GroupsHierarchyNode ghn) throws ITRightException {
        Group g = ghn.getGroup();
        if ( ! g.isActive() ) throw new ITRightException("groups-hierarchy-inactive-group");
        GroupsHierarchySimplified resp = new GroupsHierarchySimplified();
        resp.shortId = g.getShortId();
        resp.name = g.getName();
        resp.description = g.getDescription();
        resp.children = new ArrayList<>();
        resp.roles = new ArrayList<>();
        if (!ghn.getChildren().isEmpty()) {
            for ( GroupsHierarchyNode child : ghn.getChildren() ) {
                try {
                    GroupsHierarchySimplified childResp = buildFromHierarchyNode(child);
                    resp.children.add(childResp);
                } catch (ITRightException x) {} // do nothing, normal
            }
        }
        return resp;
    }

    /**
     * Build a group hierarchy response from a group list, build the hierarchy and clean the data to keep
     * what is needed for the front-end
     * @param glist
     * @return
     * @throws ITNotFoundException
     */
    public static GroupsHierarchySimplified getGroupsHierarchyResponseFromGroupList(GroupsList glist) throws ITNotFoundException {
        try {
            GroupsHierarchyNode ghn = glist.getHierarchy();
            return buildFromHierarchyNode(ghn);
        } catch (ITRightException | ITTooManyException x) {
            // return empty
            throw new ITNotFoundException(x.getMessage());
        }
    }

    // ============================================================================
    // Getters & Setters
    // ============================================================================

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<GroupsHierarchySimplified> getChildren() {
        return children;
    }

    public void setChildren(List<GroupsHierarchySimplified> children) {
        this.children = children;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
