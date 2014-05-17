# 
# Docker file for Syncany  
#
# Build:
#  docker build -t syncany:latest .
# 
# Run:
#  docker run -t -i syncany:latest
#

FROM ubuntu
MAINTAINER Philipp Heckel <philipp.heckel@gmail.com>

RUN apt-get update

# TODO: Once 0.1.4 is 'latest', change to: openjdk-7-jre-headless
RUN apt-get install -y wget openjdk-7-jre vim

RUN wget -O syncany.deb https://syncany.org/r/latest.deb
RUN dpkg -i syncany.deb
RUN rm syncany.deb
RUN useradd -m -d/home/syncany syncany

USER syncany
ENV HOME /home/syncany
WORKDIR /home/syncany

ENTRYPOINT /bin/bash 
