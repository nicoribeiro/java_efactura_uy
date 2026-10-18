/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.c14n.helper;

import org.w3c.dom.Attr;

/*
 * Copia de la clase de xmlsec (santuario) con el mismo paquete y nombre, para la consulta de RUT a DGI
 * (WS_PersonaGetActEmpresarial, ver ConsultaRutServiceImpl).
 *
 * Ese servicio usa el namespace "DGI_Modernizacion_Consolidado", que no es una URI: para xmlsec es un namespace relativo
 * y al canonicalizar el mensaje para firmarlo (WS-Security) tira un error. El unico cambio respecto de la original es
 * namespaceIsAbsolute, que lo acepta como absoluto.
 *
 * Reemplaza a la de la libreria porque el jar de la aplicacion va primero en el classpath (bin/java_efactura_uy del
 * dist de Play): no hace falta un javaagent. Si cambia el orden del classpath o se actualiza xmlsec y la clase cambia,
 * hay que revisar que esta copia siga teniendo los mismos metodos publicos que la original. Copiada de xmlsec 2.3.4
 * (la que trae CXF 3.5.11).
 */

/**
 * Temporary swapped static functions from the normalizer Section
 *
 */
public final class C14nHelper {

    /**
     * Constructor C14nHelper
     */
    private C14nHelper() {
        // don't allow instantiation
    }

    /**
     * Method namespaceIsRelative
     *
     * @param namespace
     * @return true if the given namespace is relative.
     */
    public static boolean namespaceIsRelative(Attr namespace) {
        return !namespaceIsAbsolute(namespace);
    }

    /**
     * Method namespaceIsRelative
     *
     * @param namespaceValue
     * @return true if the given namespace is relative.
     */
    public static boolean namespaceIsRelative(String namespaceValue) {
        return !namespaceIsAbsolute(namespaceValue);
    }

    /**
     * Method namespaceIsAbsolute
     *
     * @param namespace
     * @return true if the given namespace is absolute.
     */
    public static boolean namespaceIsAbsolute(Attr namespace) {
        return namespaceIsAbsolute(namespace.getValue());
    }

    /**
     * Method namespaceIsAbsolute
     *
     * @param namespaceValue
     * @return true if the given namespace is absolute.
     */
    public static boolean namespaceIsAbsolute(String namespaceValue) {
        // assume empty namespaces are absolute
        if (namespaceValue.length() == 0) {
            return true;
        }

        // Unico cambio respecto de xmlsec: el namespace del servicio de consulta de RUT de DGI no es una URI
        if ("DGI_Modernizacion_Consolidado".equals(namespaceValue)) {
            return true;
        }
        return namespaceValue.indexOf(':') > 0;
    }

}
