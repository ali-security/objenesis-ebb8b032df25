/**
 * Copyright 2006-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.objenesis.instantiator.sun;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Before;
import org.junit.Test;
import org.objenesis.EmptyClass;
import org.objenesis.ObjenesisException;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.PlatformDescription;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * @author Henri Tremblay
 */
public class MagicInstantiatorTest {

   @Before
   public void before() {
      // I know it works on Hotspot and OpenJDK. Before JDK 9. Not sure on others
      assumeTrue((PlatformDescription.isThisJVM(PlatformDescription.HOTSPOT) || PlatformDescription.isThisJVM(PlatformDescription.OPENJDK))
         && !PlatformDescription.SPECIFICATION_VERSION.equals("9")
      );
   }

   @Test
   public void testNewInstance() throws Exception {
      ObjectInstantiator<EmptyClass> o1 = new MagicInstantiator<EmptyClass>(EmptyClass.class);
      assertEquals(EmptyClass.class, o1.newInstance().getClass());

      ObjectInstantiator<EmptyClass> o2 = new MagicInstantiator<EmptyClass>(EmptyClass.class);
      assertEquals(EmptyClass.class, o2.newInstance().getClass());
   }

   @Test
   public void testInternalInstantiator() {
      ObjectInstantiator<EmptyClass> o1 = new MagicInstantiator<EmptyClass>(EmptyClass.class).getInstantiator();
      assertEquals(EmptyClass.class, o1.newInstance().getClass());
   }

   @Test
   public void testSameSimpleNameGetsDistinctInstantiators() {
      ObjectInstantiator<OuterA.Same> a = new MagicInstantiator<OuterA.Same>(OuterA.Same.class).getInstantiator();
      ObjectInstantiator<OuterB.Same> b = new MagicInstantiator<OuterB.Same>(OuterB.Same.class).getInstantiator();

      // Both classes are named Same but each one must get its own instantiator
      assertNotSame(a.getClass(), b.getClass());
      assertEquals(OuterA.Same.class, a.newInstance().getClass());
      assertEquals(OuterB.Same.class, b.newInstance().getClass());

      // The generated instantiator is still reused for the same class
      ObjectInstantiator<OuterA.Same> otherA = new MagicInstantiator<OuterA.Same>(OuterA.Same.class).getInstantiator();
      assertSame(a.getClass(), otherA.getClass());
      assertEquals(OuterA.Same.class, otherA.newInstance().getClass());
   }

   @Test
   public void testSameNameFromAnotherClassLoaderIsRejected() throws Exception {
      URL[] urls = new URL[] {new File("target/test-classes").toURI().toURL()};
      URLClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader().getParent());
      Class<?> other = Class.forName(OuterA.Same.class.getName(), false, loader);
      assertNotSame(OuterA.Same.class, other); // sanity check: same name but loaded by another class loader

      try {
         newMagicInstantiator(other);
         fail("Should not create an instantiator that would instantiate " + OuterA.Same.class + " instead");
      } catch (ObjenesisException e) {
         // expected: the generated class can only resolve OuterA.Same from the class loader of MagicInstantiator
      }
   }

   private static <T> MagicInstantiator<T> newMagicInstantiator(Class<T> type) {
      return new MagicInstantiator<T>(type);
   }

   static class OuterA {
      static class Same {
      }
   }

   static class OuterB {
      static class Same {
      }
   }
}
