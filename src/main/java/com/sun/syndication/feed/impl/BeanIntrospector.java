/*
 * Copyright 2004 Sun Microsystems, Inc.
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
 *
 */
package com.sun.syndication.feed.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Obtains all property descriptors from a bean (interface or implementation).
 * <p>
 * The java.beans.Introspector does not process the interfaces hierarchy chain, this one does.
 * <p>
 * @author Alejandro Abdelnur
 *
 */
public class BeanIntrospector {

    private static final Map<Class<?>, PropertyDescriptor[]> _introspected = new HashMap<Class<?>, PropertyDescriptor[]>();

    public static synchronized PropertyDescriptor[] getPropertyDescriptors(Class<?> klass) throws IntrospectionException {
        PropertyDescriptor[] descriptors = _introspected.get(klass);
        if (descriptors==null) {
            descriptors = getPDs(klass);
            _introspected.put(klass,descriptors);
        }
        return descriptors;
    }

    private static PropertyDescriptor[] getPDs(Class<?> klass) throws IntrospectionException {
        Method[] methods = klass.getMethods();
        Map<String, PropertyDescriptor> getters = getPDs(methods,false);
        Map<String, PropertyDescriptor> setters = getPDs(methods,true);
        List<PropertyDescriptor> pds     = merge(getters,setters);
        PropertyDescriptor[] array = new PropertyDescriptor[pds.size()];
        pds.toArray(array);
        return array;
    }

    private static final String SETTER = "set";
    private static final String GETTER = "get";
    private static final String BOOLEAN_GETTER = "is";

    private static Map<String, PropertyDescriptor> getPDs(Method[] methods,boolean setters) throws IntrospectionException {
        Map<String, PropertyDescriptor> pds = new HashMap<String, PropertyDescriptor>();
        for (int i=0;i<methods.length;i++) {
            String pName = null;
            PropertyDescriptor pDescriptor = null;
            if ((methods[i].getModifiers()&Modifier.PUBLIC)!=0) {
                if (setters) {
                    if (methods[i].getName().startsWith(SETTER) &&
                        methods[i].getReturnType()==void.class && methods[i].getParameterTypes().length==1) {
                        pName = Introspector.decapitalize(methods[i].getName().substring(3));
                        pDescriptor = new PropertyDescriptor(pName,null,methods[i]);
                    }
                }
                else {
                    if (methods[i].getName().startsWith(GETTER) &&
                        methods[i].getReturnType()!=void.class && methods[i].getParameterTypes().length==0) {
                        pName = Introspector.decapitalize(methods[i].getName().substring(3));
                        pDescriptor = new PropertyDescriptor(pName,methods[i],null);
                    }
                    else
                    if (methods[i].getName().startsWith(BOOLEAN_GETTER) &&
                        methods[i].getReturnType()==boolean.class && methods[i].getParameterTypes().length==0) {
                        pName = Introspector.decapitalize(methods[i].getName().substring(2));
                        pDescriptor = new PropertyDescriptor(pName,methods[i],null);
                    }
                }
            }
            if (pName!=null) {
                pds.put(pName,pDescriptor);
            }
        }
        return pds;
    }

    private static List<PropertyDescriptor> merge(Map<String, PropertyDescriptor> getters,Map<String, PropertyDescriptor> setters) throws IntrospectionException {
        List<PropertyDescriptor> props = new ArrayList<PropertyDescriptor>();
        Set<String> processedProps = new HashSet<String>();
        Iterator<String> gs = getters.keySet().iterator();
        while (gs.hasNext()) {
            String name = gs.next();
            PropertyDescriptor getter = getters.get(name);
            PropertyDescriptor setter = setters.get(name);
            if (setter!=null) {
                processedProps.add(name);
                PropertyDescriptor prop = new PropertyDescriptor(name,getter.getReadMethod(),setter.getWriteMethod());
                props.add(prop);
            }
            else {
                props.add(getter);
            }
        }
        Set<String> writeOnlyProps = new HashSet<String>(setters.keySet());
        writeOnlyProps.removeAll(processedProps);
        Iterator<String> ss = writeOnlyProps.iterator();
        while (ss.hasNext()) {
            String name = ss.next();
            PropertyDescriptor setter = setters.get(name);
            props.add(setter);
        }
        return props;
    }

}
