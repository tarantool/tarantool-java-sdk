ARG TARANTOOL_VERSION="3.5.0"
ARG IMAGE="tarantool/tarantool"
FROM --platform=linux/amd64 $IMAGE:$TARANTOOL_VERSION

ARG TARANTOOL_WORKDIR="/app"
ENV TARANTOOL_WORKDIR=$TARANTOOL_WORKDIR

WORKDIR $TARANTOOL_WORKDIR
COPY "cluster" "$TARANTOOL_WORKDIR"

# install dependencies
RUN apt-get -y update && \
    apt-get -y install build-essential cmake make gcc git unzip cartridge-cli && \
    apt-get -y clean

CMD tt init && tt build && tt start && sleep infinity
