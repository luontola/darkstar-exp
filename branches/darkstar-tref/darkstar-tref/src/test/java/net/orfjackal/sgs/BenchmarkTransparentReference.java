/*
 * Copyright (c) 2008  Esko Luontola, www.orfjackal.net
 *
 * This program is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.orfjackal.sgs;

import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.ManagedReference;
import static net.orfjackal.sgs.BenchmarkTransparentReference.Strategy.CGLIB_PROXYING;
import static net.orfjackal.sgs.BenchmarkTransparentReference.Strategy.JDK_PROXYING;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Esko Luontola
 * @since 24.1.2008
 */
public class BenchmarkTransparentReference {

    enum Strategy {
        JDK_PROXYING, CGLIB_PROXYING
    }

    public static final int MILLIS_TO_NANOS = 1000 * 1000;

    public static final int REPEATS = 10;

    public static final int CREATE_ITERATIONS = 10 * 1000 * 1000;
    public static final int DESERIAL_ITERATIONS = 10 * 1000;
    public static final int CALL_ITERATIONS = 1000 * 1000 * 1000;

    //private static Strategy current = JDK_PROXYING;
    private static Strategy current = CGLIB_PROXYING;
    private static TransparentReferenceFactory factory;
    private static int junk = 0;

    public static void main(String[] args) throws Exception {
//        MockSGS.init();

        System.out.println("Current strategy: " + current + "\n");
        if (current == JDK_PROXYING) {
            factory = new TransparentReferenceJdkProxyFactory();
        } else if (current == CGLIB_PROXYING) {
            factory = new TransparentReferenceCglibProxyFactory();
        }

        for (int i = 0; i < REPEATS; i++) {
            long createRef = createReference(CREATE_ITERATIONS);
            long createProxy = createTransparentReference(CREATE_ITERATIONS);
            long deserialRef = deserializeReference(DESERIAL_ITERATIONS);
            long deserialProxy = deserializeTransparentReference(DESERIAL_ITERATIONS);
            long callDirect = callMethod(CALL_ITERATIONS);
            long callRef = callRefMethod(CALL_ITERATIONS);
            long callProxy = callProxyMethod(CALL_ITERATIONS / 10);

            System.out.println(result("create ref", createRef, CREATE_ITERATIONS));
            System.out.println(result("create proxy", createProxy, CREATE_ITERATIONS));
            System.out.println(result("deser ref", deserialRef, DESERIAL_ITERATIONS));
            System.out.println(result("deser proxy", deserialProxy, DESERIAL_ITERATIONS));
            System.out.println(result("call direct", callDirect, CALL_ITERATIONS));
            System.out.println(result("call ref", callRef, CALL_ITERATIONS));
            System.out.println(result("call proxy", callProxy, CALL_ITERATIONS / 10));
            System.out.println();
        }

        // prevent JIT compiler from optimizing away all benchmarks
        System.out.println("(Junk: " + junk + ")");
    }

    private static String result(String name, long totalMillis, int iterations) {
        double oneNanos = totalMillis * (((double) MILLIS_TO_NANOS) / iterations);
        return name + ": \t" + oneNanos + " ns  \t(total " + totalMillis + " ms)";
    }

    // create

    private static long createReference(int iterations) {
        DummyManagedObject managedObject = new DummyManagedObject();

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            Object reference = AppContext.getDataManager().createReference(managedObject);
            junk += (reference == null) ? 1 : 2;
        }
        long end = System.currentTimeMillis();
        return end - start;
    }

    private static long createTransparentReference(int iterations) {
        DummyManagedObject managedObject = new DummyManagedObject();

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            Object reference = factory.createTransparentReference(managedObject);
            junk += (reference == null) ? 1 : 2;
        }
        long end = System.currentTimeMillis();
        return end - start;
    }

    // deserialize

    private static long deserializeReference(int iterations) throws IOException, ClassNotFoundException {
        Object reference = AppContext.getDataManager().createReference(new DummyManagedObject());
        byte[] bytes = TestTransparentReference.serializeObject(reference);

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            Object o = TestTransparentReference.deserializeObject(bytes);
            junk += (o == null) ? 1 : 2;
        }
        long end = System.currentTimeMillis();
        return end - start;
    }

    private static long deserializeTransparentReference(int iterations) throws IOException, ClassNotFoundException {
        Object reference = factory.createTransparentReference(new DummyManagedObject());
        byte[] bytes = TestTransparentReference.serializeObject(reference);

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            Object o = TestTransparentReference.deserializeObject(bytes);
            junk += (o == null) ? 1 : 2;
        }
        long end = System.currentTimeMillis();
        return end - start;
    }

    // call

    private static long callMethod(int iterations) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        DummyInterface object = new DummyManagedObject();
//        Method method = DummyInterface.class.getMethod("dummyMethod");

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
//            dummy += (Integer) method.invoke(object);
            junk += object.dummyMethod();
        }
        long end = System.currentTimeMillis();
        return end - start;
    }

    private static long callRefMethod(int iterations) {
        ManagedReference reference = AppContext.getDataManager().createReference(new DummyManagedObject());

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            DummyInterface object = (DummyInterface) reference.get();
            junk += object.dummyMethod();
        }
        long end = System.currentTimeMillis();
        return end - start;
    }

    private static long callProxyMethod(int iterations) {
        DummyInterface reference = (DummyInterface) factory.createTransparentReference(new DummyManagedObject());

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            junk += reference.dummyMethod();
        }
        long end = System.currentTimeMillis();
        return end - start;
    }
}
