j2trp
=====

#What is it?

J2TRP or or its full name "J2Transparent Reverse Proxy", is a reverse proxy written with the following design goals:

* 100% Java-based servlet.
* No dependencies to other net-based frameworks. It only has a compile-time dependency to SLF4J.
* Fully transparent, meaning that it doesn't create a separate HTTP session upstream. All headers (including cookies) are sent as-is.
* It comes with a simple tool-box that enables you to modify HTTP requests and responses through Servlet Filters, which gives you a nice separation from the inner mechanics of the actual proxying.
* New features and bugs should be developed/fixed using a test-driven approach. There should be plenty of unit tests in there. 
At the time of writing, code coverage is over 80%.

#What's wrong with ARP or J2EP?

In a nutshell, absolutely nothing. ARP, for instance, is the de-facto standard component for doing reverse proxying;
it's great at what it does. However, I wrote J2TRP because I had specific requirements for a project that needed integration 
with a CAS (Central Authentication Service) agent. The feature-set for the Apache module had some 
compatibility issues (mainly with old versions of OpenSSL) that were a nightmare to sort out. 

On top of that, the Apache and IIS flavors of the CAS agent didn't have all the features that were available in the Java version.

### ARP (Apache Reverse Proxy)
Good C programmers are getting harder and harder to find, and most developers out there use Java. I'd be very picky in letting someone else 
write an Apache module for me, let alone for a security application, as was the case back then. Sure, Java has its 
problems too, but I can sit down with a couple of Java developers and explain quite fast how J2TRP works, without 
getting bogged down in esoteric nuances of how to manage memory allocation and deallocation. 

### J2EP
I actually started using J2EP, but the project grinded to a crawl when I realized that it creates a separate 
HTTP session upstream, so I had to manually specify, in code, which cookies should be copied over upstream. 

Also, the fact that J2EP is a filter and not a fully-fledged servlet didn't work for me. Apart from this, 
I have no quarrel with J2EP, please use it if it better suits your needs.

#Is it really fully transparent?
Well, it as transparent as it can be, considering that it's a Java Servlet. Since the servlet *container* manages the
inbound connections, the servlet has to connect to the upstream server without HTTP keep-alives. 
This obviously incurs a performance penalty in terms of latency, 
but I think it's worth the trade-off of not writing a servlet container of my own, with all the complexity that it entails.     

