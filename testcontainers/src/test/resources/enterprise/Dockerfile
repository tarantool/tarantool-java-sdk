ARG TAG=7
ARG IMAGE="centos"
FROM $IMAGE:$TAG

ARG SDK_DIR="/sdk"
ARG TARANTOOL_RUNDIR="/tmp/run"
ARG TARANTOOL_WORKDIR="/app"
ARG TARANTOOL_DATADIR="/tmp/data"

ARG DOWNLOAD_HOST
ARG SDK_PATH

ENV DOWNLOAD_HOST=$DOWNLOAD_HOST
ENV SDK_PATH=$SDK_PATH
ENV SDK_DIR=$SDK_DIR
ENV TARANTOOL_RUNDIR=$TARANTOOL_RUNDIR
ENV TARANTOOL_WORKDIR=$TARANTOOL_WORKDIR
ENV TARANTOOL_DATADIR=$TARANTOOL_DATADIR

RUN curl https://curl.se/ca/cacert.pem -o /etc/pki/tls/certs/ca-bundle.crt
RUN sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*
RUN sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*
RUN yum -y install wget
RUN wget $DOWNLOAD_HOST/$SDK_PATH
RUN tar -xf ${SDK_PATH##*/}
RUN mv ./tarantool-enterprise $SDK_DIR
RUN cp $SDK_DIR/tarantool /usr/bin/tarantool && \
    cp $SDK_DIR/tt /usr/bin/tt

WORKDIR $TARANTOOL_WORKDIR
