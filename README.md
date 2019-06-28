# Biblioteca de Comunicación efactura Uruguay \| Electronic Invoice Uruguay

This project aims to wrapper the Uruguayan's efactura protocol and communication with DGI \(Direccion General Impositiva\) and provide a simple REST API with json.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

* Signing Certificate
* Computing resource \(VM, server, etc\). We would be using a VM with Ubuntu 18.04 for purpose of this howto.
* Java 8
* sbt
* play4jpa
* Unlimited strength JCE policy installed
* Database, Postgres or Mysql

#### Signing Certificate

First of all the system would need a certificate to sign the documents. At this moment there are 2 entities who can create a valid certificate: Abitab or Correo Uruguayo. The creation of the PKCS10 works for both but then I'll be choosing Abitab as a Certificate Authority.

1 - Create an rsa key and the PKCS10 request, I'll would be using this info for the example:

* {fantasyName} : MARVELOUS COMPANY
* {CompanyName} : ABC S.A.
* {RUC} : 217875770014
* {City} : Montevideo

Replace whats inside {} with your actual company information:

```
openssl req  -new -newkey rsa:2048 -nodes -keyout mykey.pem -out myreq.pem -subj '/CN={FantasyName}/O={CompanyName},SERIALNUMBER=RUC{RUC}/ST={City}/C=UY'
```

Save `mykey.pem` securely, you wold need it down the road. Certificate would be obsolete without this key.

2 - Verify the signature:

```
openssl req -in myreq.pem -noout -verify -key mykey.pem
```

Output:

```
verify OK
```

3 - Check information

```
openssl req -in myreq.pem -noout -text
```

Output \(check just the header\):

```
Certificate Request:
    Data:
        Version: 0 (0x0)
        Subject: CN=MARVELOUS COMPANY, O=ABC S.A.,SERIALNUMBER=RUC217875770014, ST=Montevideo, C=UY
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                Public-Key: (2048 bit)
                Modulus:
                    00:c2:11:23:3c:32:4d:ea:e4:1f:ad:e9:dc:35:14:
                    4d:cb:c8:32:f1:a0:43:4b:47:78:d3:4f:22:01:03:
                    9d:e6:d8:82:f4:b8:75:b8:d3:af:b6:4f:5d:ab:d5:
                    10:f9:93:56:68:72:51:19:af:9a:cb:2e:d6:00:be:
                    31:45:7a:67:4c:a3:1d:5c:4d:79:39:a4:e7:38:35:
                    57:eb:ce:28:d0:60:23:20:4a:a3:03:6f:c8:5f:45:
                    82:0c:9f:1d:5e:92:03:1c:63:c1:63:1f:29:8b:29:
                    33:9b:63:1b:d6:67:f3:38:39:d1:a0:97:d1:47:bc:
                    73:11:f2:44:9b:67:ef:45:77:ad:c8:6f:5e:a3:b2:
                    13:f7:c4:76:a5:d6:31:e4:15:5a:6a:bf:a7:da:a2:
                    39:54:46:0a:44:0f:7e:22:95:c5:b2:ea:8b:fe:00:
                    0a:81:54:8b:82:bd:0f:da:b0:b0:ae:31:5e:16:35:
                    07:ff:50:5b:9d:42:1d:be:b5:8c:a0:e0:bb:e6:b4:
                    f8:68:69:c3:f8:1b:9d:5c:58:b7:30:fd:5b:54:4e:
                    da:90:2f:79:db:3f:5a:9a:f7:7b:97:33:93:18:52:
                    2d:d9:34:fd:0d:f1:81:33:01:b5:a4:d7:36:9c:57:
                    ee:c4:39:5c:fe:bb:ba:af:9e:a3:72:76:8a:a3:10:
                    cc:6b
                Exponent: 65537 (0x10001)
        Attributes:
            a0:00
    Signature Algorithm: sha256WithRSAEncryption
         4b:94:2e:b7:cf:6c:5f:b7:de:64:6e:50:09:57:ad:e3:6a:36:
         21:1c:56:90:ac:b9:70:89:c8:88:98:c1:94:b1:dc:50:33:7d:
         bf:7d:eb:dc:87:68:c5:59:8d:45:a6:d7:2c:82:a0:5d:f4:ff:
         25:f8:89:3d:9e:8f:2b:f8:e7:5f:53:7a:f8:0a:03:4a:1d:ee:
         a7:d9:31:ae:68:b4:f4:23:59:77:8c:10:55:4b:55:0f:22:47:
         26:ee:1e:4e:b6:83:db:9d:9d:ff:3c:6c:30:14:29:ab:61:86:
         41:c6:f3:14:95:12:89:22:90:d7:8f:19:9f:99:dd:67:42:15:
         5e:9d:cd:9b:ca:4b:79:c9:e1:43:06:41:eb:4b:f0:af:f6:56:
         5a:66:3c:ee:95:ea:ad:cf:c0:99:1c:44:be:10:d4:5a:04:84:
         48:a2:b8:11:a2:97:43:4d:53:dc:5c:ed:7b:6c:fa:df:65:cb:
         c0:a1:ff:f8:e4:46:61:26:72:d4:03:f7:eb:6d:81:f1:ea:85:
         16:32:1b:a7:d5:e9:19:09:3c:d1:8c:c8:0a:34:88:20:75:5a:
         a5:83:0a:f5:6a:a2:3a:bb:89:6d:5a:3d:2e:2c:18:a7:f1:40:
         9a:75:eb:51:98:63:e5:42:fc:6a:6e:88:fb:31:49:44:5f:d1:
         9f:28:46:6d
```

4 - Go to [Abitab](https://www.iddigital.com.uy/es/solicitud-de-certificado/empresa/facturacion_electronica/) webpage, select Request PKCS10 and fill the forms. When the page ask you for your PKCS10 request do:

```
cat myreq.pem
```

copy and paste the result, should look like this:

```
-----BEGIN CERTIFICATE REQUEST-----
MIICszCCAZsCAQAwbjEaMBgGA1UEAwwRTUFSVkVMT1VTIENPTVBBTlkxLjAsBgNV
BAoMJUFCQyBTLkEuLFNFUklBTE5VTUJFUj1SVUMyMTc4NzU3NzAwMTQxEzARBgNV
BAgMCk1vbnRldmlkZW8xCzAJBgNVBAYTAlVZMIIBIjANBgkqhkiG9w0BAQEFAAOC
AQ8AMIIBCgKCAQEAwhEjPDJN6uQfrencNRRNy8gy8aBDS0d4008iAQOd5tiC9Lh1
uNOvtk9dq9UQ+ZNWaHJRGa+ayy7WAL4xRXpnTKMdXE15OaTnODVX684o0GAjIEqj
A2/IX0WCDJ8dXpIDHGPBYx8piykzm2Mb1mfzODnRoJfRR7xzEfJEm2fvRXetyG9e
o7IT98R2pdYx5BVaar+n2qI5VEYKRA9+IpXFsuqL/gAKgVSLgr0P2rCwrjFeFjUH
/1BbnUIdvrWMoOC75rT4aGnD+BudXFi3MP1bVE7akC952z9amvd7lzOTGFIt2TT9
DfGBMwG1pNc2nFfuxDlc/ru6r56jcnaKoxDMawIDAQABoAAwDQYJKoZIhvcNAQEL
BQADggEBAEuULrfPbF+33mRuUAlXreNqNiEcVpCsuXCJyIiYwZSx3FAzfb9969yH
aMVZjUWm1yyCoF30/yX4iT2ejyv4519TevgKA0od7qfZMa5otPQjWXeMEFVLVQ8i
RybuHk62g9udnf88bDAUKathhkHG8xSVEokikNePGZ+Z3WdCFV6dzZvKS3nJ4UMG
QetL8K/2VlpmPO6V6q3PwJkcRL4Q1FoEhEiiuBGil0NNU9xc7Xts+t9ly8Ch//jk
RmEmctQD9+ttgfHqhRYyG6fV6RkJPNGMyAo0iCB1WqWDCvVqojq7iW1aPS4sGKfx
QJp161GYY+VC/GpuiPsxSURf0Z8oRm0=
-----END CERTIFICATE REQUEST-----
```

5 - goto an Abitab and pay for the cert, wait a few days, you should get an email to download your cert.

6 - you should have a file called {fantasyName}.cer

7 - Check the certificate, but first get the [root certificate](https://www.iddigital.com.uy/media/filer_public/fe/dc/fedc3f9e-b61e-46be-9135-49373fec567c/acrn.crt) \(acrn.crt\) and the [ca certificate](https://www.iddigital.com.uy/media/filer_public/ed/5a/ed5a37ea-5fe2-457d-8584-19f10c5c102c/abitab.crt) \(abitab.crt\)

```
openssl verify -CAfile acrn.crt -untrusted abitab.crt {fantasyName}.cer
```

Output should be:

```
{fantasyName}.cer: OK
```

8 - Check validity, issuer and other stuff

```
openssl x509 -in {fantasyName}.cer -text
```

Output should be:

```
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            0c:0f:21:58:9f:e1:db:03:47:ed:41:fa:de:2d:53:0e:33:d5:b7:38
    Signature Algorithm: sha256WithRSAEncryption
        Issuer: C = UY, L = Montevideo, O = Abitab S.A., OU = ID digital, CN = Abitab
        Validity
            Not Before: Jun  5 15:38:03 2019 GMT
            Not After : Jun  4 15:38:03 2021 GMT
        Subject: C = UY, ST = Montevideo, serialNumber = RUC217875770014, O = ABC S.A., CN = Marvelous Company
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                Public-Key: (2048 bit)
                Modulus:
                    00:ca:52:4b:ea:1e:ff:ce:24:6b:a8:da:72:18:68:
                    d5:56:5d:0e:48:5a:2d:35:09:76:5a:cf:a4:c8:1c:
                    b1:a9:fe:53:89:fb:ad:34:ff:88:5b:9f:bb:e7:e8:
                    00:01:dc:35:73:75:03:ad:b3:b1:b9:a4:7d:2b:26:
                    79:ce:15:40:0a:ef:51:b8:9f:32:8c:7c:70:86:52:
                    4b:16:fe:6a:27:6b:e6:36:7a:62:50:d8:df:9a:89:
                    cc:09:29:eb:4f:29:14:88:80:0b:8f:38:1e:80:6a:
                    18:7c:1d:bd:97:3b:78:7d:45:49:36:4f:41:cd:a2:
                    e0:76:57:3c:68:31:79:64:c9:6e:d7:51:1e:66:c3:
                    a2:64:2c:79:c0:e7:65:c3:56:84:53:5a:43:6d:cb:
                    9a:02:20:d2:ef:1a:69:d1:b0:9d:73:a2:e0:2a:60:
                    65:50:31:cf:fb:b3:2f:bf:11:88:40:2e:b5:49:10:
                    0f:0a:6e:dc:97:fa:bf:2c:9f:05:39:0b:58:54:af:
                    06:96:e8:c5:8e:01:16:bc:a8:1a:4d:41:c5:93:91:
                    a2:1e:a1:8b:f2:fe:c1:88:24:49:a3:47:4b:c5:13:
                    01:dd:a7:57:12:69:62:2b:eb:fe:20:ef:69:fb:3a:
                    a5:f0:7e:29:ee:ed:96:16:f7:b1:1f:a0:e4:90:25:
                    e0:33
                Exponent: 65537 (0x10001)
        X509v3 extensions:
            Authority Information Access: 
                CA Issuers - URI:http://www.id.com.uy/resources/Abitab.crt
                OCSP - URI:http://ocsp.id.com.uy/asf/servlet/OCSPServlet

            X509v3 Key Usage: critical
                Digital Signature, Non Repudiation, Key Encipherment, Data Encipherment
            X509v3 Basic Constraints: critical
                CA:FALSE
            X509v3 CRL Distribution Points: 

                Full Name:
                  URI:http://crl.id.com.uy/resources/crl_id_digital_pki_uruguay.crl

            X509v3 Certificate Policies: 
                Policy: 2.16.858.10000157.66565.4
                  CPS: http://uce.gub.uy/informacion-tecnica/politicas/cp_persona_juridica.pdf
                Policy: 2.16.858.10000157.66565.7
                  CPS: http://www.id.com.uy/resources/cps_id_firmaElectronicaAvanzada.pdf

            X509v3 Extended Key Usage: 
                TLS Web Client Authentication
            X509v3 Subject Key Identifier: 
                40:23:91:99:38:7B:FE:C9:B4:0D:62:91:A5:59:40:ED:AD:9F:73:9B
            X509v3 Authority Key Identifier: 
                keyid:0D:13:D6:F6:C6:28:A6:E7:C5:B7:87:D1:24:9C:9F:93:F0:E8:1D:3B

    Signature Algorithm: sha256WithRSAEncryption
         1c:b7:89:96:e4:53:ed:bb:ec:db:a8:32:01:9f:2c:a3:cd:6d:
         ad:42:12:77:b3:b8:e6:c9:03:52:60:20:7b:57:27:c6:11:b5:
         3f:67:0d:99:2c:5b:5a:ca:22:0a:dd:9e:bb:1f:4b:48:3f:8f:
         02:3d:8b:21:84:45:1d:6d:f5:ff:ac:68:89:cd:64:e2:d6:d6:
         5e:40:c2:8e:2a:f7:ef:14:d3:36:a4:40:30:f5:32:15:15:92:
         76:fb:7e:9e:53:ea:c2:76:fc:39:ad:88:fe:66:92:26:e9:1c:
         c4:38:cd:49:fa:43:87:f0:5d:d6:56:4d:81:d7:7f:f1:c2:dd:
         b0:4d:fe:c3:2a:6e:7c:9f:6e:5c:ed:62:42:99:e1:f7:36:ee:
         14:8c:2c:20:e3:46:97:5a:77:03:c0:a0:c6:4a:88:fd:40:22:
         87:72:5a:18:ea:9c:a5:c7:5a:08:8c:e4:05:a4:7d:b9:84:35:
         5f:89:36:56:0e:40:3d:12:e8:bb:35:72:ed:af:08:56:4e:b0:
         bb:2e:a9:9b:e4:fb:1d:3e:0b:63:c8:9b:4b:91:44:66:57:c0:
         14:b4:96:f0:dc:2c:57:3f:52:04:ad:95:aa:7d:4d:d0:f2:0c:
         9f:9c:40:e8:d6:55:73:ba:3c:df:90:cb:00:5b:21:11:67:c2:
         ed:32:1e:de
-----BEGIN CERTIFICATE-----
MIIEXDCCA0SgAwIBAgINAeOpMBz8cgY4P5pTHTANBgkqhkiG9w0BAQsFADBMMSAw
HgYDVQQLExdHbG9iYWxTaWduIFJvb3QgQ0EgLSBSMjETMBEGA1UEChMKR2xvYmFs
U2lnbjETMBEGA1UEAxMKR2xvYmFsU2lnbjAeFw0xNzA2MTUwMDAwNDJaFw0yMTEy
MTUwMDAwNDJaMFQxCzAJBgNVBAYTAlVTMR4wHAYDVQQKExVHb29nbGUgVHJ1c3Qg
U2VydmljZXMxJTAjBgNVBAMTHEdvb2dsZSBJbnRlcm5ldCBBdXRob3JpdHkgRzMw
ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDKUkvqHv/OJGuo2nIYaNVW
XQ5IWi01CXZaz6TIHLGp/lOJ+600/4hbn7vn6AAB3DVzdQOts7G5pH0rJnnOFUAK
71G4nzKMfHCGUksW/mona+Y2emJQ2N+aicwJKetPKRSIgAuPOB6Aahh8Hb2XO3h9
RUk2T0HNouB2VzxoMXlkyW7XUR5mw6JkLHnA52XDVoRTWkNty5oCINLvGmnRsJ1z
ouAqYGVQMc/7sy+/EYhALrVJEA8KbtyX+r8snwU5C1hUrwaW6MWOARa8qBpNQcWT
kaIeoYvy/sGIJEmjR0vFEwHdp1cSaWIr6/4g72n7OqXwfinu7ZYW97EfoOSQJeAz
AgMBAAGjggEzMIIBLzAOBgNVHQ8BAf8EBAMCAYYwHQYDVR0lBBYwFAYIKwYBBQUH
AwEGCCsGAQUFBwMCMBIGA1UdEwEB/wQIMAYBAf8CAQAwHQYDVR0OBBYEFHfCuFCa
Z3Z2sS3ChtCDoH6mfrpLMB8GA1UdIwQYMBaAFJviB1dnHB7AagbeWbSaLd/cGYYu
MDUGCCsGAQUFBwEBBCkwJzAlBggrBgEFBQcwAYYZaHR0cDovL29jc3AucGtpLmdv
b2cvZ3NyMjAyBgNVHR8EKzApMCegJaAjhiFodHRwOi8vY3JsLnBraS5nb29nL2dz
cjIvZ3NyMi5jcmwwPwYDVR0gBDgwNjA0BgZngQwBAgIwKjAoBggrBgEFBQcCARYc
aHR0cHM6Ly9wa2kuZ29vZy9yZXBvc2l0b3J5LzANBgkqhkiG9w0BAQsFAAOCAQEA
HLeJluRT7bvs26gyAZ8so81trUISd7O45skDUmAge1cnxhG1P2cNmSxbWsoiCt2e
ux9LSD+PAj2LIYRFHW31/6xoic1k4tbWXkDCjir37xTTNqRAMPUyFRWSdvt+nlPq
wnb8Oa2I/maSJukcxDjNSfpDh/Bd1lZNgdd/8cLdsE3+wypufJ9uXO1iQpnh9zbu
FIwsIONGl1p3A8CgxkqI/UAih3JaGOqcpcdaCIzkBaR9uYQ1X4k2Vg5APRLouzVy
7a8IVk6wuy6pm+T7HT4LY8ibS5FEZlfAFLSW8NwsVz9SBK2Vqn1N0PIMn5xA6NZV
c7o835DLAFshEWfC7TIe3g==
-----END CERTIFICATE-----
```

#### Computing Resource

Follow [this](https://linuxconfig.org/how-to-install-ubuntu-18-04-bionic-beaver) tutorial to install Ubuntu 18.04 LTS

#### Java 8

To install:

```
sudo apt install openjdk-8-jdk-headless
```

then check

```
java -version
```

output:

```
openjdk version "1.8.0_212"
OpenJDK Runtime Environment (build 1.8.0_212-8u212-b03-0ubuntu1.18.04.1-b03)
OpenJDK 64-Bit Server VM (build 25.212-b03, mixed mode)
```

#### SBT

Based on [this](https://www.scala-sbt.org/1.0/docs/Installing-sbt-on-Linux.html) tutorial

```
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt-get update
sudo apt-get install sbt
```

#### play4jpa

```
git clone https://github.com/nicoribeiro/play4jpa.git
cd play4jpa
sbt publishLocal
```

sbt downloads all dependencies, build and publish the artifact to the local repository so other projects \(such as this\) can use the artifact. First build takes several minutes to download all deps.

#### Unlimited strength JCE policy installed

* Go to the Oracle Java SE download page [http://www.oracle.com/technetwork/java/javase/downloads/index.html](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* Scroll down ... Under "Additional Resources" section you will find "Java Cryptography Extension \(JCE\) Unlimited Strength Jurisdiction Policy File"
* Download the version that matches your installed JVM E.g. UnlimitedJCEPolicyJDK7.zip
* Unzip the downloaded zip 
* Copy local\_policy.jar and US\_export\_policy.jar to the $JAVA\_HOME/jre/lib/security \(Note: these jars will be already there so you have to overwrite them\)

#### Database

You must create a blank database with a user and a password with complete privileges over that database. Suggested databases are Postgres or Mysql

### Installing

Clone this repo

```
git clone https://github.com/nicoribeiro/java_efactura_uy.git
```

## Config

```
vi conf/application.conf
```

Config the database connection:

For postgres

```
db.default.driver=org.postgresql.Driver
db.default.url="jdbc:postgresql://{ip}:{port, default 5432}/{dbname}"
```

For Mysql

```
default.driver = com.mysql.cj.jdbc.Driver
default.url="jdbc:mysql://{ip}:{port, default 3306}/{dbname}"
```

For all

```
db.default.username={dbuser}
db.default.password="{dbpass}"
```

## Run

```
cd java_efactura_uy
```

```
./activator -jvm-debug 9999 "run 9000"
```

Te first time activator takes several minutes because of dependency download

## First Steps

To interact with the backend we recommend using [postman](https://www.getpostman.com/) and download the complete API Collection.

After install, goto Import, then Import from link, and use this link \(collection is in spanish\) [https://www.getpostman.com/collections/d7dd8b7d073379f16454](https://www.getpostman.com/collections/d7dd8b7d073379f16454)

* Create a User using `User/SignUp` POST
* SignIn using `User/SingIn` POST
* Initialize Database using `App/Init Database` POST
* Import Companies using `Empresa/Cargar Empresas desde XML DGI`POST
* Add your Company
* Configure your certificate
* Complete DGI challenge to become a valid efactura member
* Import CAEs using `Empresa/Agregar CAE` POST

## Deployment

ToDo

## Built With

* [PlayFramework](https://www.playframework.com/) - The web framework used
* [SBT](https://www.scala-sbt.org/) - Dependency Management

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags).

## Authors

* **Nicolas Ribeiro** - _Initial work_ - [Bluedot](https://github.com/nicoribeiro)

See also the list of [contributors](https://github.com/nicoribeiro/java_efactura_uy/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* [LerandGroup](https://lerandgroup.com.uy)

## Notes

A hack to work around the issue on relative namespaces used in the service. This is achieved via a modified C14nHelper class in the project's classpath \(giving it higher precedence and being loaded instead of the default one\). The only modification there is a hardcoded short-circuit in namespaceIsAbsolute method.

