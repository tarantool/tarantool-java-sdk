if package.setsearchroot ~= nil then
    package.setsearchroot()
end

------------------------------------------------------------------------------
-- IMPORTS AND MODULE LOCALS
------------------------------------------------------------------------------
log = require('log')
fiber = require('fiber')
socket = require('socket')
tarantool = require('tarantool')

LIMIT = 512
BIND = '0.0.0.0'
call_counter = 0
session_counter = 0
connects_registered = {}
pipe_lock = false

------------------------------------------------------------------------------
-- UTILITY FUNCTIONS
------------------------------------------------------------------------------
function get_version()
    version = unpack(tarantool.version:split('-'))
    return version
end

function create_kv_space(name)
    space = box.schema.space.create(name, {
        if_not_exists = true,
        format = {
            { 'id', type = 'string' },
            { 'value', type = 'string', is_nullable = true }
        }
    })
    space:create_index('pk', { parts = { 'id' } })
end

function create_complex_space(name)
    space = box.schema.space.create(name, {
        if_not_exists = true,
        format = {
            { 'id', type = 'number' },
            { 'is_married', type = 'boolean', is_nullable = true },
            { 'name', type = 'string' }
        }
    })
    space:create_index('pk', { parts = { 'id' } })
end

function fail()
    error('Fail!')
end

------------------------------------------------------------------------------
-- SCHEMA
------------------------------------------------------------------------------
box.once('schema', function()
    create_kv_space('test')
    create_kv_space('space_a')
    create_kv_space('space_b')
    create_complex_space('person')

    box.space.space_b:on_replace(function(old, new, s, op)
        box.session.push(old)
    end)

    box.schema.user.grant('user_a', 'read,write', 'space', 'space_a')
    box.schema.user.grant('user_b', 'read,write', 'space', 'space_b')
    box.schema.user.grant('user_b', 'read', 'space', 'space_a')
    box.schema.user.revoke('user_c', 'session', 'universe')
    box.schema.user.revoke('user_d', 'usage', 'universe')

    log.info('schema created on master')
end)

------------------------------------------------------------------------------
-- PRIVATE FUNCTIONS
------------------------------------------------------------------------------
function same(data)
    return data
end

function same_with_lock(data)
    if pipe_lock then
        return
    end
    return data
end

function corruptor(data)
    return string.char(math.random(0, 255)):rep(#data)
end

function pipe(stopper, from, to, convertor)
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
function create_proxy(client2srv, srv2client)
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
function closing_server()
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
function non_accepting_server()
    local srv_sock = socket('AF_INET', 'SOCK_STREAM', 'tcp')
    srv_sock:bind(BIND, 3303)
    srv_sock:listen()
end

------------------------------------------------------------------------------
-- PUBLIC FUNCTIONS
------------------------------------------------------------------------------
get_version = get_version
fail = fail

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
    _, err = pcall(fail_by_box_error)
    exception = box.error.new({ reason = 'wrapped failure' })
    exception:set_prev(err)
    box.error(exception)
end

function echo_with_push(...)
    box.session.push({ "push1", "out of band" })
    box.session.push({ "push2", "out of band" })
    return ...
end

function insert(space, tuple)
    cond = fiber.cond()
    fiber.create(function()
        box.space[space]:insert(tuple)
        cond:broadcast()
    end)

    return cond:wait()
end

function inc()
    ssid = box.session.id()
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
