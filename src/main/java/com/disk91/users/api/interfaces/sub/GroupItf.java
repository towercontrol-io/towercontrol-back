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

import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.mdb.entities.sub.GroupAttribute;
import com.disk91.groups.tools.GroupsHierarchySimplified;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Group information", description = "Group structure for front-end usage")
public class GroupItf {

    @Schema(
            description = "Group shortId, required for modifications",
            example = "XyJl1djk",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String shortId;

    @Schema(
            description = "Group name@",
            example = "My Favorite Group",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String name;

    @Schema(
            description = "Group description",
            example = "User administrator",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String description;

    @Schema(
            description = "Group attributes",
            example = "User administrator",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<GroupAttribute> attributes;

    @Schema(
            description = "Sub-groups",
            example = "Lis of SubGroups",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<GroupItf> subs;


    // ==========================
    // Constructors

    public static GroupItf getGroupItfFromGroup(Group _g) {
        GroupItf g = new GroupItf();
        g.setShortId(_g.getShortId());
        g.setName(_g.getName());
        g.setDescription(_g.getDescription());
        g.setAttributes(_g.getAttributes());
        g.setSubs(new ArrayList<>());
        return g;
    }

    public static GroupItf getGroupItfFromGroupsHierarchySimplified(GroupsHierarchySimplified _g) {
        GroupItf g = new GroupItf();
        g.setShortId(_g.getShortId());
        g.setName(_g.getName());
        g.setDescription(_g.getDescription());
        g.setSubs(new ArrayList<>());
        for ( GroupsHierarchySimplified sg : _g.getChildren() ) {
            g.getSubs().add( getGroupItfFromGroupsHierarchySimplified(sg) );
        }
        return g;
    }


    // ==========================
    // Getters & Setters

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

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public List<GroupAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<GroupAttribute> attributes) {
        this.attributes = attributes;
    }

    public List<GroupItf> getSubs() {
        return subs;
    }

    public void setSubs(List<GroupItf> subs) {
        this.subs = subs;
    }
}
