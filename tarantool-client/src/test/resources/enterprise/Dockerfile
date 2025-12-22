ARG TAG=7
ARG IMAGE="centos"
FROM $IMAGE:$TAG

ARG SDK_DIR="/sdk"
ARG TARANTOOL_RUNDIR="/tmp/run"
ARG TARANTOOL_WORKDIR="/app"
ARG TARANTOOL_DATADIR="/tmp/data"

ARG DOWNLOAD_HOST
ARG SDK_PATH
ARG TARANTOOL_DB_PATH

ENV SDK_TGT_DIR=$SDK_DIR
ENV TARANTOOL_RUNDIR=$TARANTOOL_RUNDIR
ENV TARANTOOL_WORKDIR=$TARANTOOL_WORKDIR
ENV TARANTOOL_DATADIR=$TARANTOOL_DATADIR

RUN curl https://curl.se/ca/cacert.pem -o /etc/pki/tls/certs/ca-bundle.crt
RUN sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*
RUN sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*
RUN yum -y install wget net-tools
RUN wget $DOWNLOAD_HOST/$SDK_PATH
RUN mkdir ./tmp_sdk
RUN tar -xf ${SDK_PATH##*/} -C ./tmp_sdk
RUN mv ./tmp_sdk/tarantool-enterprise $SDK_DIR
RUN cp $SDK_DIR/tarantool /usr/bin/tarantool && \
    cp $SDK_DIR/tt /usr/bin/tt

WORKDIR $TARANTOOL_WORKDIR

RUN wget "$DOWNLOAD_HOST/$TARANTOOL_DB_PATH"
RUN tar -xf ${TARANTOOL_DB_PATH##*/}

WORKDIR "$TARANTOOL_WORKDIR/tarantooldb"
COPY "cartridge" "$TARANTOOL_WORKDIR/tarantooldb"

CMD tt start tarantooldb && while true; do sleep 1; done
