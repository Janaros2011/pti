# HTTP Security

## 1. Introduction

Note: This file only contains some clarifications about the .pdf document. 

## 2. Tasks

### 2.1 HTTPServer

Download the HTTPServer code and execute it:

	mkdir p4
	cd p4
	wget http://docencia.ac.upc.es/FIB/grau/PTI/lab/_seg/pti-p6-codigo.tar.gz
	tar xzvf pti-p6-codigo.tar.gz
	javac *.java
	java -cp . HTTPServer &

    [1] 13795
    HTTPServer version 1.0
    HTTPServer is listening on port 8000.

Test with your browser: http://localhost:8000. The server sends the file index.html to the browser.

Test with your own client: 

	java -cp . Browser http://localhost:8000

### 2.2 Basic Authentication without SSL (only with a commercial browser as client)

1. Modify the processGetRequest method within HTTPServer.java: 
 
	 void processGetRequest(HTTPRequest request,BufferedOutputStream outStream)
	   throws IOException {  
	    if (!request.checkBasicAuthentication())
	        request.sendBasicAuthenticationUnauthorized(outStream);
	    else {
	      ...the previous code...
	    }
	 }

2. Update the following within the HTTPRequest class within HTTPServer.java: 

    Vector<String> lines = new Vector<String>(); 
 
3. Add to the HTTPRequest class within HTTPServer.java: 

    public boolean checkBasicAuthentication() {    
        for(String line : lines){        
            if(line.equalsIgnoreCase("Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==")) 
                return true;        
        }
        return false;   
    } 

4. Add to the HTTPRequest class within HTTPServer.java:         
               
     void sendBasicAuthenticationUnauthorized(BufferedOutputStream out) {
      try {   
       out.write("HTTP/1.1 401 Unauthorized\n".getBytes());
       out.write("WWW-Authenticate: Basic realm= WallyWorld\n".getBytes());    
       out.write("Content-Type: text/html\r\n\r\n".getBytes());
       out.flush();
       out.close();
       System.out.println("Response sent");   
      }catch(Exception e){   
       e.printStackTrace();
      }
     }
      
5 Test it   

Open with a browser http://localhost:8000 and type: username="Aladdin", pwd="open sesame" 

COMMENT: Don't need to apply basic authentication to Browser.java.

### 2.3 HTTPS server-side authentication (with two different clients: browser and Java class)

#### 2.3.1 Access from a web browser (SecureServer)

IMPORTANT: SecureServer inherits from the modified HTTPServer (with basic authentication). 
(You may try to use the original version for an incremental debugging (only if in trouble).)

Let's generate a new private key (with alias "server" and keypass "serverkspw") and store it within a file (a keystore) named "certs":

	keytool -genkey -alias server -keyalg RSA -keypass serverkspw -storepass serverkspw  -keystore certs
	(answer RETURN to all questions before the last one, then answer "s" or "y")

Let's now execute SecureServer, that will first load the "certs" keystore:

	java -cp . SecureServer &

Test it with your web browser https://localhost:4430/ (don't miss the httpS!)

COMMENT: The browser will not trust the certificate as you didn't install the cert. Just add a security exception (don't need to install the cert).

#### 2.3.2 Access from a java class (SecureBrowser)

IMPORTANT: Disable basic authentication for this!

Export the public key certificate to a file named "server.cer".

	keytool -export -alias server -storepass serverkspw -file server.cer -keystore certs

Import the public key certificate as a trusted certificate into a truststore, cacerts.jks.

	keytool -import -v -trustcacerts -alias server -file server.cer -keystore cacerts.jks -keypass serverkspw -storepass serverkspw

Now, assuming that SecureServer is running, let's execute the SecureBrowser:

	java -cp . -Djavax.net.ssl.trustStore=cacerts.jks -Djavax.net.ssl.trustStorePassword=serverkspw SecureBrowser https://localhost:4430

### 2.4 +Client-side authentication (mutal authentication)

IMPORTANT: Discard the changes you made in HTTPServer.java (the basic authentication)
COMMENT: You can keep the changes done in 2.3 

Let's create a certificate for the client and store it within the keystore:

	keytool -genkey -alias client -keyalg RSA -keypass serverkspw -storepass serverkspw  -keystore certs

Now let's add the certificate into the truststore:

	keytool -export -alias client -storepass serverkspw -file client.cer -keystore certs
	keytool -import -v -trustcacerts -alias client -file client.cer -keystore cacerts.jks -keypass serverkspw -storepass serverkspw

Now we need to modify SecureServer.java to make it ask for a client certificate:

	this("SecureServer", "1.0", 4430, true);

When relaunching the server, it is necessary to specify where is the trustore where the client certificate will be checked:

	javac *.java
	java -cp . -Djavax.net.ssl.trustStore=cacerts.jks -Djavax.net.ssl.trustStorePassword=serverkspw SecureServer &

When executing the client, it is necessary to specify where is the keystore which contains the client certificate:

	java -cp . -Djavax.net.ssl.trustStore=cacerts.jks -Djavax.net.ssl.trustStorePassword=serverkspw -Djavax.net.ssl.keyStore=certs -Djavax.net.ssl.keyStorePassword=serverkspw SecureBrowser https://localhost:4430


### 5. Troubleshooting

To check if the necessary ports are open (you must be root):

	sudo iptables -nL | grep 8000
	sudo iptables -nL | grep 4430

To open the necessary ports if closed (you must be root):

	sudo iptables -A INPUT -p tcp --dport 8000 -j ACCEPT
	sudo iptables -A INPUT -p tcp --dport 4430 -j ACCEPT
   


