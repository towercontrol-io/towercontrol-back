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
package com.disk91.groups.tools;

import com.disk91.groups.mdb.entities.Group;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GroupsHierarchyNode {

    /**
     * This structure is used to generate a hierarchy from a flat group list
     * One nodes gives the links to the whole hierarchy from this node
     */

    protected ArrayList<String> path;
    protected Group group;
    protected LinkedList<GroupsHierarchyNode> children;

    public GroupsHierarchyNode(Group g, ArrayList<String> _path) {
        this.group = g;
        this.path = new ArrayList<>(_path);
        this.children = new LinkedList<>();
    }

    public GroupsHierarchyNode addChild(Group child)  {
        ArrayList<String> newPath = new ArrayList<>(this.path);
        newPath.add(this.group.getShortId());
        GroupsHierarchyNode childNode = new GroupsHierarchyNode(child, newPath);
        this.children.add(childNode);
        return childNode;
    }

    public void addChild(GroupsHierarchyNode child) {
        this.children.add(child);
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    /**
     * The children path includes the current group shortId
     * @return
     */
    public ArrayList<String> getChildrenPath() {
        ArrayList<String> newPath = new ArrayList<>(this.path);
        newPath.add(this.group.getShortId());
        return newPath;
    }

    // ==== Getters

    public List<GroupsHierarchyNode> getChildren() {
        return this.children;
    }

    public Group getGroup() {
        return group;
    }

    public ArrayList<String> getPath() {
        return path;
    }
}
