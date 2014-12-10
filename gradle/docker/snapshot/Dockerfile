# 
# Docker file for Syncany (SNAPSHOT) 
#
# Build:
#  docker build -t syncany/snapshot .
# 
# Run:
#  docker run -ti syncany/snapshot
#
# Tries to follow this tutorial:
#  http://container-solutions.com/2014/11/6-dockerfile-tips-official-images/
#

FROM debian:jessie
MAINTAINER Philipp Heckel <philipp.heckel@gmail.com>

# Install Syncany and dependencies, then add 'syncany' user
RUN \
	   apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys A3002F0613D342687D70AEEE3F6B7F13651D12BD \
	&& echo deb http://archive.syncany.org/apt/snapshot/ snapshot main > /etc/apt/sources.list.d/syncany.list \
	&& apt-get update \
	&& apt-get install -y syncany vim bash-completion sudo --no-install-recommends \
	&& rm -rf /var/lib/apt/lists/*debian.{org,net}* \
	&& apt-get purge -y --auto-remove \
	&& useradd -m -d/home/syncany -s /bin/bash syncany \
	&& echo 'syncany ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

USER syncany
ENV HOME /home/syncany
WORKDIR /home/syncany

ENTRYPOINT /bin/bash 
