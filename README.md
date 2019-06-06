# Biblioteca de Comunicación efactura Uruguay

This project aims to wrapper the Uruguayan's efactura protocol and communication with DGI \(Direccion General Impositiva\) and provide a simple REST API with json.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

* Signing Certificate
* Computing resource \(VM, server, etc\). We would be using a VM with Ubuntu 18.04 for purpose of this howto.
* Java 8
* sbt
* play4jpa
* Database, Postgres or Mysql, we would use Postgres as a database backend.
* Unlimited strength JCE policy installed

#### Signing Certificate

First of all the system would need a certificate to sign the documents. At this moment there are 2 entities who can create a valid certificate: Abitab or Correo Uruguayo. The creation of the PKCS10 works for both but then I'll be choosing Abitab as a Certificate Authority.

1 - Create an rsa key and the PKCS10 request, I'll would be using this info for the example:

* {fantasyName} : MARVELOUS COMPANY
* {CompanyName} : ABC S.A.
* {RUC} : 217875770014
* {City} : Montevideo

replace whats inside {} with your actual company information:

```
openssl req  -new -newkey rsa:2048 -nodes -keyout mykey.pem -out myreq.pem -subj '/CN={FantasyName}/O={CompanyName},SERIALNUMBER=RUC{RUC}/ST={City}/C=UY'
```

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

You must create a blank database with a user and a password with complete privileges over that database

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



* Create a User using User/SignUp POST
* SignIn using User/SingIn POST
* Import Companies with efactura
* Add your Company
* Configure your certificate
* Complete DGI challenge to become a valid efactura member

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

