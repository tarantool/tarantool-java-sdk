------------------------------------------------------------------------------
-- MASTER
------------------------------------------------------------------------------
if package.setsearchroot ~= nil then
    package.setsearchroot()
end

------------------------------------------------------------------------------
-- TARANTOOL CONFIGURATION
------------------------------------------------------------------------------

box.cfg {
    listen = 3301,
    memtx_memory = 128 * 1024 * 1024,
    log_level = 6
}

------------------------------------------------------------------------------
-- IMPORTS AND MODULE LOCALS
------------------------------------------------------------------------------
local log = require('log')
local fiber = require('fiber')
local socket = require('socket')

local utils = require('utils')

local LIMIT = 512
local BIND = '0.0.0.0'
local call_counter = 0
local session_counter = 0
local connects_registered = {}
local pipe_lock = false

------------------------------------------------------------------------------
-- SCHEMA
------------------------------------------------------------------------------
box.once('schema', function()
    utils.create_kv_space('test')
    utils.create_kv_space('space_a')
    utils.create_kv_space('space_b')
    utils.create_complex_space('person')

    box.space.space_b:on_replace(function(old, new, s, op)
        box.session.push(old)
    end)

    box.schema.user.create('service_user', {
        password = '',
        if_not_exists = true
    })
    box.schema.user.grant('service_user', 'super')
    box.schema.user.create('user_a', {
        password = 'secret_a',
        if_not_exists = true
    })
    box.schema.user.create('user_b', {
        password = 'secret_b',
        if_not_exists = true
    })
    box.schema.user.create('user_c', {
        password = 'secret_c',
        if_not_exists = true
    })
    box.schema.user.create('user_d', {
        password = 'secret_d',
        if_not_exists = true
    })
    box.schema.user.create('replicator', {
        password = 'password'
    })

    -- need for manual testing when tarantool is run just as
    -- `tarantool server.lua`.
    -- When this user created via env variables there will be no effect
    box.schema.user.create('api_user', {
        password = 'secret',
        if_not_exists = true
    })

    box.schema.user.grant('api_user', 'super') -- created via env variables when testcontainer is starting
    box.schema.user.grant('user_a', 'read,write', 'space', 'space_a')
    box.schema.user.grant('user_a', 'execute', 'universe')
    box.schema.user.grant('user_b', 'read,write', 'space', 'space_b')
    box.schema.user.grant('user_b', 'read', 'space', 'space_a')
    box.schema.user.revoke('user_c', 'session', 'universe')
    box.schema.user.revoke('user_d', 'usage', 'universe')
    box.schema.user.grant('user_d', 'execute', 'universe')
    box.schema.user.grant('replicator', 'replication')

    log.info('schema created on master')
end)

------------------------------------------------------------------------------
-- PRIVATE FUNCTIONS
------------------------------------------------------------------------------
local function same(data)
    return data
end

local function same_with_lock(data)
    if pipe_lock then
        return
    end
    return data
end

local function corruptor(data)
    return string.char(math.random(0, 255)):rep(#data)
end

local function pipe(stopper, from, to, convertor)
    while not stopper.active do
        if from:readable(0.1) then
            local chunk = from:sysread(LIMIT)
            if chunk == '' or chunk == nil then
                stopper.active = true
                return
            end
            local converted = convertor(chunk)
            if converted ~= nil then
                to:send(converted)
            end
        end
    end
end

-- Makes socket handler for generic proxy. It accepts two callbacks - one for
-- handling data transmitting from client ot server, the second one is for data
-- transmitting from server to client.  It allows perform different kind of
-- checks, for example delays of packets, corrupting packets (to check how
-- corrupted packed will be processed on connector side) and so on.
local function create_proxy(client2srv, srv2client)
    return function(client)
        local stopper = { active = false }
        local proxy = socket.tcp_connect('127.0.0.1', 3301)
        local p1 = fiber.create(pipe, stopper, client, proxy, client2srv)
        local p2 = fiber.create(pipe, stopper, proxy, client, srv2client)
        p1:set_joinable(true)
        p2:set_joinable(true)
        p1:join()
        p2:join()
        proxy:close()
    end
end

-- This server closes listening socket when it gets any incoming connection.
-- It needed for checks about "Connection closed by remote side / server"
-- errors on client side.
local function closing_server()
    while true do
        local srv_sock = socket('AF_INET', 'SOCK_STREAM', 'tcp')
        srv_sock:bind(BIND, 3302)
        srv_sock:listen()
        log.info('listening')
        -- waits till data arrived
        if srv_sock:wait() == 'R' then
            log.info('close suddenly socket')
            srv_sock:close()
        end
    end
end

-- This server does not call acept for any incoming connection.  It needed for
-- checks about tcp connect timeout (when we set coninect timeout for tcp
-- sockect in netty Bootstrap)
local function non_accepting_server()
    local srv_sock
    srv_sock = socket('AF_INET', 'SOCK_STREAM', 'tcp')
    srv_sock:bind(BIND, 3303)
    srv_sock:listen()
end

------------------------------------------------------------------------------
-- PUBLIC FUNCTIONS
------------------------------------------------------------------------------
get_version = utils.get_version
fail = utils.fail

function echo_with_wrapping(...)
    -- we use table packing because it's more popular than multi return value
    return { ... }
end

function echo(...)
    return ...
end

function echo_to_map(...)
    return (...):tomap()
end

function return_true()
    return true
end

function return_number()
    return 2008
end

function slow_echo(...)
    fiber.sleep(1.5)
    return ...
end

function nonslow_echo(...)
    fiber.sleep(0.5)
    return ...
end

function wrong_ret()
    return function()
        return 1
    end
end

function fail_by_box_error()
    box.error({ reason = 'fail' })
end

function wrapped_fail_by_box_error()
    local _, err = pcall(fail_by_box_error)
    local exception = box.error.new({ reason = 'wrapped failure' })
    exception:set_prev(err)
    box.error(exception)
end

function echo_with_push(...)
    box.session.push({ "push1", "out of band" })
    box.session.push({ "push2", "out of band" })
    return ...
end

function insert(space, tuple)
    local cond = fiber.cond()
    fiber.create(function()
        box.space[space]:insert(tuple)
        cond:broadcast()
    end)

    return cond:wait()
end

function inc()
    local ssid = box.session.id()
    if connects_registered[ssid] == nil then
        session_counter = session_counter + 1
        connects_registered[ssid] = true
    end
    call_counter = call_counter + 1
end

function get_call_counter()
    return call_counter
end

function get_session_counter()
    return session_counter
end

function reset_call_counter()
    call_counter = 0
end

function lock_pipe(flag)
    pipe_lock = flag
end

function stuck()
    fiber.create(function()
        fiber.sleep(1)
        while true do
        end
    end)
end

------------------------------------------------------------------------------
-- FIBERS, TASKS, SERVERS
------------------------------------------------------------------------------
fiber.create(closing_server)
fiber.create(non_accepting_server)
socket.tcp_server(BIND, 3304, create_proxy(same, corruptor))
socket.tcp_server(BIND, 3305, create_proxy(same_with_lock, same))
socket.tcp_server(BIND, 3306, create_proxy(same, same_with_lock))
