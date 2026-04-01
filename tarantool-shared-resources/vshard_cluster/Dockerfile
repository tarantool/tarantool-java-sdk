ARG TARANTOOL_VERSION="3.5.0"
ARG IMAGE="tarantool/tarantool"
FROM --platform=linux/amd64 $IMAGE:$TARANTOOL_VERSION
HEALTHCHECK NONE

ARG TARANTOOL_WORKDIR="/app"
ENV TARANTOOL_WORKDIR=$TARANTOOL_WORKDIR
ARG APP_NAME="vshard-cluster"

WORKDIR $TARANTOOL_WORKDIR
COPY "cluster" "$TARANTOOL_WORKDIR/$APP_NAME"
WORKDIR "$TARANTOOL_WORKDIR/$APP_NAME"
RUN mkdir -p /etc/tarantool/instances.enabled && \
    ln -s "$TARANTOOL_WORKDIR/$APP_NAME" /etc/tarantool/instances.enabled/app

RUN sed -i 's|http://archive.ubuntu.com/ubuntu/|http://mirror.yandex.ru/ubuntu/|g; s|http://security.ubuntu.com/ubuntu/|http://mirror.yandex.ru/ubuntu/|g' /etc/apt/sources.list

# install dependencies
RUN apt-get -y update && \
    apt-get -y install build-essential cmake make gcc git unzip && \
    apt-get -y clean

CMD rm -f tt.yaml && \
    TT_CONFIG=./config.yaml env -u TT_APP_NAME -u TT_INSTANCE_NAME tt init && \
    TT_CONFIG=./config.yaml env -u TT_APP_NAME -u TT_INSTANCE_NAME tt build && \
    TT_CONFIG=./config.yaml env -u TT_APP_NAME -u TT_INSTANCE_NAME tt start vshard-cluster && \
    sleep infinity
