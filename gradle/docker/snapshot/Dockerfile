# 
# Docker file for Syncany (SNAPSHOT) 
#
# Build:
#  docker build -t syncany/snapshot .
# 
# Run:
#  docker run -t -i syncany/snapshot
#

FROM ubuntu
MAINTAINER Philipp Heckel <philipp.heckel@gmail.com>

# Install Syncany and dependencies
RUN apt-get update
RUN apt-get install -y wget openjdk-7-jre-headless vim
RUN wget -O syncany.deb https://syncany.org/r/latest-snapshot.deb
RUN dpkg -i syncany.deb
RUN rm syncany.deb

# Add user 'syncany'
RUN useradd -m -d/home/syncany -s /bin/bash syncany
RUN echo 'syncany ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

USER syncany
ENV HOME /home/syncany
WORKDIR /home/syncany

ENTRYPOINT /bin/bash 
