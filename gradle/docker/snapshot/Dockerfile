# 
# Docker file for Syncany (SNAPSHOT) 
#
# Build:
#  docker build -t syncany/snapshot .
# 
# Run:
#  docker run -ti syncany/snapshot
#

FROM ubuntu
MAINTAINER Philipp Heckel <philipp.heckel@gmail.com>

# Install Syncany and dependencies
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys A3002F0613D342687D70AEEE3F6B7F13651D12BD
RUN echo deb http://archive.syncany.org/apt/snapshot/ snapshot main > /etc/apt/sources.list.d/syncany.list

RUN apt-get update
RUN apt-get install -y syncany vim bash-completion

# Add user 'syncany'
RUN useradd -m -d/home/syncany -s /bin/bash syncany
RUN echo 'syncany ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

USER syncany
ENV HOME /home/syncany
WORKDIR /home/syncany

ENTRYPOINT /bin/bash 
