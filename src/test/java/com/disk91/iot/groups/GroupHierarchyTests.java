package com.disk91.iot.groups;

import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.tools.GroupList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.PropertySource;
import org.junit.jupiter.api.Order;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@PropertySource(value = {"file:configuration/common-test.properties"}, ignoreResourceNotFound = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GroupHierarchyTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Test
    @Order(1)
    public void testSimpleHierarchy() {
        log.info("[groups][test] Running testSimpleHierarchy");

        // Create a simple hierarchy
        // Root
        //  |-- A
        //  |    |-- B
        //  |    |-- C
        //  |-- D
        //       |-- E

        Group Root = new Group();
        Root.init("Root", "Group Root", "Root", "en");
        Group A = new Group();
        A.init("A", "Group A", "A", "en");
        A.getReferringGroups().add(Root.getShortId());
        Group B = new Group();
        B.init("B", "Group B", "B", "en");
        B.getReferringGroups().add(Root.getShortId());
        B.getReferringGroups().add(A.getShortId());
        Group C = new Group();
        C.init("C", "Group C", "C", "en");
        C.getReferringGroups().add(Root.getShortId());
        C.getReferringGroups().add(A.getShortId());
        Group D = new Group();
        D.init("D", "Group D", "D", "en");
        D.getReferringGroups().add(Root.getShortId());
        Group E = new Group();
        E.init("E", "Group E", "E", "en");
        E.getReferringGroups().add(Root.getShortId());
        E.getReferringGroups().add(D.getShortId());

        GroupList gl = new GroupList(Root, 3);
        assertDoesNotThrow(() -> {
            gl.addElement(A);
            gl.addElement(B);
            gl.addElement(C);
            gl.addElement(D);
            gl.addElement(E);

            String json = gl.getHierarchy().toJson();
            log.info(json);

            assertEquals(5, gl.getUnder("Root").size());
            assertEquals(2, gl.getUnder("A").size());
            assertEquals(1, gl.getUnder("D").size());
            assertEquals(0, gl.getUnder("B").size());

            ArrayList<String> path = new ArrayList<>();
            path.add("Root");
            assertEquals(2, gl.getNextLevel(path).size());

            ArrayList<String> branch1 = new ArrayList<>(path);
            branch1.add("A");
            assertEquals(2, gl.getNextLevel(branch1).size());

            ArrayList<String> branch2 = new ArrayList<>(path);
            branch2.add("D");
            assertEquals(1, gl.getNextLevel(branch2).size());

            assertEquals(A,gl.getHierarchy().getChildren().getFirst().getGroup());
            assertEquals(B,gl.getHierarchy().getChildren().getFirst().getChildren().getFirst().getGroup());
            assertEquals(C,gl.getHierarchy().getChildren().getFirst().getChildren().get(1).getGroup());
            assertEquals(D,gl.getHierarchy().getChildren().get(1).getGroup());
            assertEquals(E,gl.getHierarchy().getChildren().get(1).getChildren().getFirst().getGroup());
        });

    }


    @Test
    @Order(1)
    public void testSimpleLoopHierarchy() {
        log.info("[groups][test] Running a loop in the hierarchy (A->C->A)");

        // Create a simple hierarchy
        // Root
        //  |-- A
        //  |    |-- B
        //  |    |-- C
        //  |         |-- A (loop)
        //  |-- D
        //       |-- E
        //
        // Real Result
        //  |-- D
        //       |-- E


        Group Root = new Group();
        Root.init("Root", "Group Root", "Root", "en");
        Group A = new Group();
        A.init("A", "Group A", "A", "en");
        A.getReferringGroups().add(Root.getShortId());
        Group B = new Group();
        B.init("B", "Group B", "B", "en");
        B.getReferringGroups().add(Root.getShortId());
        B.getReferringGroups().add(A.getShortId());
        Group C = new Group();
        C.init("C", "Group C", "C", "en");
        C.getReferringGroups().add(Root.getShortId());
        C.getReferringGroups().add(A.getShortId());
        A.getReferringGroups().add(C.getShortId());
        Group D = new Group();
        D.init("D", "Group D", "D", "en");
        D.getReferringGroups().add(Root.getShortId());
        Group E = new Group();
        E.init("E", "Group E", "E", "en");
        E.getReferringGroups().add(Root.getShortId());
        E.getReferringGroups().add(D.getShortId());

        GroupList gl = new GroupList(Root, 3);
        assertDoesNotThrow(() -> {
            gl.addElement(A);
            gl.addElement(B);
            gl.addElement(C);
            gl.addElement(D);
            gl.addElement(E);

            String json = gl.getHierarchy().toJson();
            log.info(json);


            assertEquals(5, gl.getUnder("Root").size());
            assertEquals(2, gl.getUnder("A").size());
            assertEquals(1, gl.getUnder("D").size());
            assertEquals(0, gl.getUnder("B").size());

            ArrayList<String> path = new ArrayList<>();
            path.add("Root");
            assertEquals(1, gl.getNextLevel(path).size());

            ArrayList<String> branch1 = new ArrayList<>(path);
            branch1.add("A");
            assertEquals(2, gl.getNextLevel(branch1).size());

            ArrayList<String> branch2 = new ArrayList<>(path);
            branch2.add("D");
            assertEquals(1, gl.getNextLevel(branch2).size());

            // In this case, the loop is removed from the hierarchy and B comes at the higher layer
            // ITs OK
            assertEquals(D,gl.getHierarchy().getChildren().getFirst().getGroup());
            assertEquals(E,gl.getHierarchy().getChildren().getFirst().getChildren().getFirst().getGroup());
            assertEquals(B,gl.getHierarchy().getChildren().get(1).getGroup());
        });

    }


    @Test
    @Order(1)
    public void testDifHierarchyHierarchy() {
        log.info("[groups][test] Running in different hierarchy");

        // Create a simple hierarchy
        // Root
        //  |-- A
        //  |    |-- B
        //  |    |-- C
        //  |-- D
        //       |-- E
        // Root2
        //  |-- B (dual)

        Group Root = new Group();
        Root.init("Root", "Group Root", "Root", "en");
        Group A = new Group();
        A.init("A", "Group A", "A", "en");
        A.getReferringGroups().add(Root.getShortId());
        Group B = new Group();
        B.init("B", "Group B", "B", "en");
        B.getReferringGroups().add(Root.getShortId());
        B.getReferringGroups().add(A.getShortId());
        Group C = new Group();
        C.init("C", "Group C", "C", "en");
        C.getReferringGroups().add(Root.getShortId());
        C.getReferringGroups().add(A.getShortId());
        Group D = new Group();
        D.init("D", "Group D", "D", "en");
        D.getReferringGroups().add(Root.getShortId());
        Group E = new Group();
        E.init("E", "Group E", "E", "en");
        E.getReferringGroups().add(Root.getShortId());
        E.getReferringGroups().add(D.getShortId());

        Group Root2 = new Group();
        Root2.init("Root2", "Group Root2", "Root2", "en");
        B.getReferringGroups().add(Root2.getShortId());


        GroupList gl = new GroupList(Root, 3);
        GroupList gl2 = new GroupList(Root2, 3);
        assertDoesNotThrow(() -> {
            gl.addElement(A);
            gl.addElement(B);
            gl.addElement(C);
            gl.addElement(D);
            gl.addElement(E);

            gl2.addElement(B);

            String json = gl.getHierarchy().toJson();
            log.info(json);

            assertEquals(5, gl.getUnder("Root").size());
            assertEquals(2, gl.getUnder("A").size());
            assertEquals(1, gl.getUnder("D").size());
            assertEquals(0, gl.getUnder("B").size());

            ArrayList<String> path = new ArrayList<>();
            path.add("Root");
            assertEquals(2, gl.getNextLevel(path).size());

            ArrayList<String> branch1 = new ArrayList<>(path);
            branch1.add("A");
            assertEquals(2, gl.getNextLevel(branch1).size());

            ArrayList<String> branch2 = new ArrayList<>(path);
            branch2.add("D");
            assertEquals(1, gl.getNextLevel(branch2).size());

            assertEquals(A,gl.getHierarchy().getChildren().getFirst().getGroup());
            assertEquals(C,gl.getHierarchy().getChildren().getFirst().getChildren().getFirst().getGroup());
            assertEquals(B,gl.getHierarchy().getChildren().getFirst().getChildren().get(1).getGroup());
            assertEquals(D,gl.getHierarchy().getChildren().get(1).getGroup());
            assertEquals(E,gl.getHierarchy().getChildren().get(1).getChildren().getFirst().getGroup());


        });

    }




    @Test
    @Order(1)
    public void testDualHierarchy() {
        log.info("[groups][test] Running testSimpleLoopHierarchy");

        // Create a simple hierarchy
        // Root
        //  |-- A
        //  |    |-- B
        //  |    |-- C
        //  |-- D
        //       |-- E
        //       |-- B (dual)

        Group Root = new Group();
        Root.init("Root", "Group Root", "Root", "en");
        Group A = new Group();
        A.init("A", "Group A", "A", "en");
        A.getReferringGroups().add(Root.getShortId());
        Group B = new Group();
        B.init("B", "Group B", "B", "en");
        B.getReferringGroups().add(Root.getShortId());
        B.getReferringGroups().add(A.getShortId());
        Group C = new Group();
        C.init("C", "Group C", "C", "en");
        C.getReferringGroups().add(Root.getShortId());
        C.getReferringGroups().add(A.getShortId());
        Group D = new Group();
        D.init("D", "Group D", "D", "en");
        D.getReferringGroups().add(Root.getShortId());
        Group E = new Group();
        E.init("E", "Group E", "E", "en");
        E.getReferringGroups().add(Root.getShortId());
        E.getReferringGroups().add(D.getShortId());
        B.getReferringGroups().add(D.getShortId());


        GroupList gl = new GroupList(Root, 3);
        assertDoesNotThrow(() -> {
            gl.addElement(A);
            gl.addElement(B);
            gl.addElement(C);
            gl.addElement(D);
            gl.addElement(E);

            String json = gl.getHierarchy().toJson();
            log.info(json);


            assertEquals(5, gl.getUnder("Root").size());
            assertEquals(2, gl.getUnder("A").size());
            assertEquals(2, gl.getUnder("D").size());
            assertEquals(0, gl.getUnder("B").size());

        });

    }



    @Test
    @Order(1)
    public void testComplexHierarchy() {
        log.info("[groups][test] Running test with complex hierarchy");

        LoggingSystem system = LoggingSystem.get(GroupList.class.getClassLoader());
        system.setLogLevel(GroupList.class.getName(), LogLevel.DEBUG);

        // Create a simple hierarchy
        // Root1
        //  |-- A
        //  |    |-- C
        //  |        |-- D
        //  |        |-- E
        //  |            |-- J
        //
        // Root2
        //  |-- R
        //       |-- K
        //           |-- E
        //               |-- J

        Group Root1 = new Group();
        Root1.init("Root1", "Group Root1", "Root1", "en");
        Group A = new Group();
        A.init("A", "Group A", "A", "en");
        A.getReferringGroups().add(Root1.getShortId());
        Group C = new Group();
        C.init("C", "Group C", "C", "en");
        C.getReferringGroups().add(Root1.getShortId());
        C.getReferringGroups().add(A.getShortId());
        Group D = new Group();
        D.init("D", "Group D", "D", "en");
        D.getReferringGroups().add(Root1.getShortId());
        D.getReferringGroups().add(A.getShortId());
        D.getReferringGroups().add(C.getShortId());

        Group Root2 = new Group();
        Root2.init("Root2", "Group Root2", "Root2", "en");
        Group R = new Group();
        R.init("R", "Group R", "R", "en");
        R.getReferringGroups().add(Root2.getShortId());
        Group K = new Group();
        K.init("K", "Group K", "K", "en");
        K.getReferringGroups().add(Root2.getShortId());
        K.getReferringGroups().add(R.getShortId());


        Group E = new Group();
        E.init("E", "Group E", "E", "en");
        E.getReferringGroups().add(Root1.getShortId());
        E.getReferringGroups().add(A.getShortId());
        E.getReferringGroups().add(C.getShortId());
        E.getReferringGroups().add(Root2.getShortId());
        E.getReferringGroups().add(R.getShortId());
        E.getReferringGroups().add(K.getShortId());

        Group J = new Group();
        J.init("J", "Group J", "J", "en");
        J.getReferringGroups().add(Root1.getShortId());
        J.getReferringGroups().add(A.getShortId());
        J.getReferringGroups().add(C.getShortId());
        J.getReferringGroups().add(E.getShortId());
        J.getReferringGroups().add(Root2.getShortId());
        J.getReferringGroups().add(R.getShortId());
        J.getReferringGroups().add(K.getShortId());


        GroupList gl1 = new GroupList(Root1, 5);
        GroupList gl2 = new GroupList(Root2, 5);
        assertDoesNotThrow(() -> {
            gl1.addElement(A);
            gl1.addElement(C);
            gl1.addElement(D);
            gl1.addElement(E);
            gl1.addElement(J);

            String json1 = gl1.getHierarchy().toJson();
            log.info(json1);

            gl2.addElement(R);
            gl2.addElement(K);
            gl2.addElement(E);
            gl2.addElement(J);

            String json2 = gl2.getHierarchy().toJson();
            log.info(json2);


            assertEquals(5, gl1.getUnder("Root1").size());
            assertEquals(4, gl1.getUnder("A").size());
            assertEquals(3, gl1.getUnder("C").size());
            assertEquals(1, gl1.getUnder("E").size());
            assertEquals(0, gl1.getUnder("J").size());
            assertEquals(4, gl2.getUnder("Root2").size());
            assertEquals(3, gl2.getUnder("R").size());
            assertEquals(2, gl2.getUnder("K").size());
            assertEquals(1, gl2.getUnder("E").size());
            assertEquals(0, gl2.getUnder("J").size());

            //             Root1                              A                        C                  E                       J
            assertEquals(E,gl1.getHierarchy().getChildren().getFirst().getChildren().get(0).getChildren().get(1).getGroup());
            assertEquals(J,gl1.getHierarchy().getChildren().getFirst().getChildren().get(0).getChildren().get(1).getChildren().get(0).getGroup());

            //             Root2                              R                        K                  E                       J
            assertEquals(E,gl2.getHierarchy().getChildren().getFirst().getChildren().get(0).getChildren().get(0).getGroup());
            assertEquals(J,gl2.getHierarchy().getChildren().getFirst().getChildren().get(0).getChildren().get(0).getChildren().get(0).getGroup());


        });

    }


    @Test
    @Order(1)
    public void testMutlipleHierarchy() {
        log.info("[groups][test] Running test with multiple hierarchy and addUnderGroup function");

        LoggingSystem system = LoggingSystem.get(GroupList.class.getClassLoader());
        system.setLogLevel(GroupList.class.getName(), LogLevel.DEBUG);

        // Create a mixed hierarchy
        // Root1
        //  |-- A
        //  |    |-- C
        //  |        |-- D
        //  |        |-- E
        //  |            |-- J
        //
        // Root2
        //  |-- R
        //       |-- K
        //           |-- E
        //               |-- J

        int maxDepth = 7;

        assertDoesNotThrow(() -> {
            Group Root1 = new Group();
            Root1.init("Root1", "Group Root1", "Root1", "en");
            Group A = new Group();
            A.init("A", "Group A", "A", "en");
            A.addUnderGroup(Root1, maxDepth);

            Group C = new Group();
            C.init("C", "Group C", "C", "en");
            C.addUnderGroup(A, maxDepth);

            Group D = new Group();
            D.init("D", "Group D", "D", "en");
            D.addUnderGroup(C, maxDepth);

            Group Root2 = new Group();
            Root2.init("Root2", "Group Root2", "Root2", "en");
            Group R = new Group();
            R.init("R", "Group R", "R", "en");
            R.addUnderGroup(Root2, maxDepth);

            Group K = new Group();
            K.init("K", "Group K", "K", "en");
            K.addUnderGroup(R, maxDepth);

            Group E = new Group();
            E.init("E", "Group E", "E", "en");
            E.addUnderGroup(K, maxDepth);
            E.addUnderGroup(C, maxDepth);

            Group J = new Group();
            J.init("J", "Group J", "J", "en");
            J.addUnderGroup(E, maxDepth);

            GroupList gl1 = new GroupList(Root1, 5);
            gl1.addElement(A);
            gl1.addElement(C);
            gl1.addElement(D);
            gl1.addElement(E);
            gl1.addElement(J);

            assertThrows(ITParseException.class, () -> {
                // out or hierarachy
                gl1.addElement(R);
                gl1.addElement(K);
            });

            // Root1
            //  |-- A
            //  |    |-- C
            //  |        |-- D
            //  |        |-- A (loop)
            //  |        |-- E
            //  |            |-- J
            assertThrows(ITParseException.class, () -> {
                A.addUnderGroup(C, maxDepth);
            });

            // Root1
            //  |-- A                                    D1
            //  |    |-- C                               D2
            //  |        |-- D                           D3
            //  |        |-- E                           D6 (due to the two attachments)
            //  |            |-- J                       D7
            //  |                |-- L (too deep)        D8 (too deep)

            assertThrows(ITTooManyException.class, () -> {
                Group L = new Group();
                L.init("L", "Group L", "L", "en");
                L.addUnderGroup(J, maxDepth);
            });


        });
    }


}
