package org.javashlook.util.comparatorfactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;


public class ComparatorFactoryTest extends TestCase {

    private static final TestBean[] TEST_BEANS = {
        new TestBean(2, "a", "1.1.2007."),
        new TestBean(2, "b", "1.2.2007."),
        new TestBean(1, "c", "3.2.2007.")
    };

    public void testFactory() {
        ComparatorFactory<TestBean> cf = ComparatorFactory.forClass(TestBean.class);
        // this could be programatically specified
        cf.addProperty("i", Integer.class, false);
        cf.addProperty("s", String.class, false);
        cf.addProperty("d", Date.class, false);
        
        Comparator<TestBean> c1 = cf.generate();

        assertNotNull(c1);
        assertEquals(0, c1.compare(TEST_BEANS[0], TEST_BEANS[0]));
        assertEquals(1, c1.compare(TEST_BEANS[0], TEST_BEANS[1]));
        assertEquals(-1, c1.compare(TEST_BEANS[1], TEST_BEANS[0]));

        List<TestBean> l = new ArrayList<TestBean>(Arrays.asList(TEST_BEANS));
        Collections.sort(l, c1);
        
        assertEquals(3, l.size());
        assertEquals("b", l.get(0).getS());
        assertEquals("a", l.get(1).getS());
        assertEquals("c", l.get(2).getS());
    }

}
