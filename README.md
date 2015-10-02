# java_efactura_uy
Libreria de Comunicacion Java efactura Uruguay 


For it to work the following are required:<br>
 * 1. "security" folder located under this project's resources folder. The security folder contains the keystore and security properties (security.properties) file. There's an example of the security properties file under resources/example<br>
 * 3. Unlimited strength JCE policy installed (see http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)<br>


Notes:
 * 1. A hack to work around the issue on relative namespaces used in the service. This is achieved via a modified C14nHelper class in the
 * project's classpath (giving it higher precedence and being loaded instead of the default one). The only modification there is a hardcoded
 * short-circuit in namespaceIsAbsolute method.
