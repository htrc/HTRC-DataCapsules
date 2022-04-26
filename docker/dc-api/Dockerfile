FROM 		tomcat:9.0

MAINTAINER Data to Insight Center <d2i@indiana.edu>

USER root

WORKDIR /opt

WORKDIR /opt/tomcat/webapps/
ADD sloan-ws*.war /opt/tomcat/webapps/sloan-ws.war
RUN unzip -qq sloan-ws.war -d sloan-ws

# Create directory to keep DC API configuration files
RUN mkdir -p /etc/htrc/dcapi
