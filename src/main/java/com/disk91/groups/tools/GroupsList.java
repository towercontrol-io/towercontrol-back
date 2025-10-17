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

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.groups.mdb.entities.Group;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GroupsList implements CloneableObject<GroupsList> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    /**
     * This is a technical class to manage a list of groups, this is used by the group hierarchy
     * to manage a hierarchical group in cache.
     * Its allows in a group to find a head and subgroup hierarchy.
     */

    protected ArrayList<Group> list;
    protected String head;
    protected Group headElement;   // accelerator
    protected ArrayList<String> hierarchy;
    protected int maxDepth;


    public GroupsList(Group _headElement, int _maxDepth) {
        this.list = new ArrayList<>();
        this.list.add(_headElement);
        this.head = _headElement.getShortId();
        this.headElement = _headElement;
        this.hierarchy = new ArrayList<>();
        this.hierarchy.add(this.head);
        this.maxDepth = _maxDepth;
    }

    /**
     * Add an element in this, this element must refer the head element
     * @param g
     */
    public void addElement(Group g) throws ITParseException {
        for ( String _g : g.getReferringGroups() ) {
            if ( _g.compareTo(this.head) == 0 ) {
                this.list.add(g);
                this.hierarchy.add(g.getShortId());
                return;
            }
        }
        throw new ITParseException("group-not-in-hierarchy");
    }


    /**
     * Get the head of the group
     * @return
     */
    public Group getHead() {
        return this.headElement;
    }

    /**
     * Return the list of group under a given node
     * @param node - the node we want as a common parent, does not return the node itseft
     * @return
     * @throws ITParseException
     */
    public List<Group> getUnder(String node) throws ITNotFoundException {
        ArrayList<Group> res = new ArrayList<>();
        for ( Group g : this.list ) {
            for ( String ref : g.getReferringGroups() ) {
                if ( ref.compareTo(node) == 0 ) {
                    res.add(g);
                    break;
                }
            }
        }
        return res;
    }


    /**
     * Generate a hierarchy iteratively from a given path
     * This takes into configuration the alternative path
     * @param path shortId path to the current level
     * @return Lis of group at the next level
     */
    public List<Group> getNextLevel(ArrayList<String> path) {
        ArrayList<Group> ret = new ArrayList<>();
        ArrayList<Group> secondTestPhase = new ArrayList<>();
        log.debug("Path size is {}", path.size());
        for ( Group g : this.list ) {
            int found= 0 ;
            // Search if the group is lower in the hierarchy
            for ( String r : g.getReferringGroups() ) {
                if( path.contains(r) ) found++;
            }
            if ( found == path.size() ) {
                // this group is under in the hierarchy but the level is not known
                //log.info("Refering size is {} for group {}", g.getReferringGroups().size(), g.getShortId());
                if ( g.getReferringGroups().size() == path.size() ) {
                    // easy case, there is no external reference
                    ret.add(g);
                    log.debug("step 1 - add group {} as direct child", g.getShortId());
                } else {
                    // otherwise a second test will be required to determined in in a next layer
                    secondTestPhase.add(g);
                }
            }
        }
        ArrayList<Group> candidates = new ArrayList<>();
        for ( Group g : secondTestPhase ) {
            boolean isSub=false;
            // Search if the group is lower in the hierarchy ; means it have on of the found group in references
            for ( Group _r : ret ) {
                if( g.getReferringGroups().contains(_r.getShortId()) ) {
                    isSub=true;
                    break;
                }
            }
            // when the reference is not found, it means the other references are out of the hierarchy
            // so we can add the group at this level. But in regard to the processing order, a sub
            // element can be considered as a candidate
            if ( ! isSub ) {
                log.debug("step 2 - condidate group {} as child", g.getShortId());
                candidates.add(g);
            }
        }
        // Now we need to keep only the groups that do not refer a group in the candidate list
        ArrayList<Group> cleanCandidates = new ArrayList<>();
        for ( Group g : candidates ) {
            boolean found=false;
            for ( Group _g : candidates ) {
                log.debug("Step3 - process {} with {} - {}", g.getShortId(),_g.getShortId(),_g.getReferringGroups());
                if ( g.getReferringGroups().contains(_g.getShortId()) ) {
                    // g is a parent of _g so we do not keep it
                    log.debug("... found");
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                log.debug("step 3 - retain group {} as child", g.getShortId());
                cleanCandidates.add(g);
            }
        }
        ret.addAll(cleanCandidates);
        return ret;
    }

    /**
     * Build the hierarchy recursively
     */
    protected void buildHierarchy(GroupsHierarchyNode node, int depth, int maxDepth) throws ITTooManyException {
        if ( depth >= maxDepth ) throw new ITTooManyException("group-hierarchy-too-deep-possible-loop");
        List<Group> under = this.getNextLevel(node.getChildrenPath());
        for ( Group g : under ) {
            GroupsHierarchyNode child = node.addChild(g,null);
            this.buildHierarchy(child, depth+1, maxDepth);
        }
    }

    /**
     * Get the hierarchy of this group list
     * @return
     */
    public GroupsHierarchyNode getHierarchy() throws ITTooManyException {
        GroupsHierarchyNode root = new GroupsHierarchyNode(this.headElement,new ArrayList<>(),null);
        this.buildHierarchy(root,1,10);
        return root;
    }

    public String toJson() throws ITTooManyException, ITParseException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this.getHierarchy());
        } catch (JsonProcessingException e) {
            log.error("[group] something went wrong exporting hierarchy: {}", e.getMessage());
            throw new ITParseException("group-hierarchy-export-error");
        }
    }

    // ===========================================================================

    public GroupsList clone() {
        GroupsList c = new GroupsList(this.headElement,this.maxDepth);
        for ( Group g : this.list ) {
            c.list.add(g.clone());
        }
        return c;
    }

    // ===========================================================================


    public ArrayList<Group> getList() {
        return list;
    }
}