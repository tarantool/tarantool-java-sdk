/**
 * The package contains classes, interfaces and enums designed for connection pooling.
 *
 * <p>NOTE: word "connection" here actually means iproto client, an instance of {@link
 * io.tarantool.core.IProtoClient}, but for clearer understanding it will be called as "connection".
 *
 * <p>Interface {@link io.tarantool.pool.IProtoClientPool} represents an API contract for pooling.
 * It includes pool configuration, obtaining connections, tracking availability of connections and
 * performing actions for each connection in pool. Connections within pool are divided into groups,
 * when each group has associated tag, address of host to connect and credentials. Group should
 * contain at least one connection. Connection from pool can be obtained by group tag and index of
 * connection in group. Connection is being established in lazy manner - first connect will be made
 * when connection will be requested from pool.
 *
 * <p>Class {@link io.tarantool.pool.InstanceConnectionGroup} represents a group of connections,
 * hold all necessary information for connecting: host, port, username, password, tag and count of
 * clients within group, which will be created in pool.
 *
 * <p>Class {@link io.tarantool.pool.PoolEntry} is a main unit of pool which holds a single
 * connection. It controls connection during its full life-cycle: creating, connecting, alive
 * checking and closing. For keep-alive check, dead connections detection, invalidation and
 * reconnecting heartbeats facility is used. Conditions for heartbeats are managed by {@link
 * io.tarantool.pool.HeartbeatOpts}, when user can set count of dead ping to invalidate, count of
 * dead ping to kill and reconnect, interval between ping and so on. When heartbeat is run, it can
 * be in states described by enum {@link io.tarantool.pool.HeartbeatEvent}.
 *
 * <p>Subpackage {@link io.tarantool.pool.exceptions} contains classes for pool exceptions.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see io.tarantool.pool.IProtoClientPool
 * @see io.tarantool.pool.InstanceConnectionGroup
 * @see io.tarantool.pool.PoolEntry
 * @see io.tarantool.pool.HeartbeatOpts
 * @see io.tarantool.pool.HeartbeatEvent
 */
package io.tarantool.pool;
