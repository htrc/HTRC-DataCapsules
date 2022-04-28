FROM 		tomcat:9.0

MAINTAINER Data to Insight Center <d2i@indiana.edu>

USER root

WORKDIR $CATALINA_HOME

ADD sloan-ws*.war

# Create directory to keep DC API configuration files
RUN mkdir -p /etc/htrc/dcapi
