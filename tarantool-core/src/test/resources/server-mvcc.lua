------------------------------------------------------------------------------
-- MASTER
------------------------------------------------------------------------------
if package.setsearchroot ~= nil then
    package.setsearchroot()
end

local log = require('log')
local fiber = require('fiber')
local tarantool = require('tarantool')

------------------------------------------------------------------------------
-- UTILITY FUNCTIONS
------------------------------------------------------------------------------
local function get_version()
    local version = unpack(tarantool.version:split('-'))
    return version
end

local function create_kv_space(name)
    local space = box.schema.space.create(name, {
        if_not_exists = true,
        format = {
            { 'id', type = 'string' },
            { 'value', type = 'string', is_nullable = true }
        }
    })
    space:create_index('pk', { parts = { 'id' } })
end

local function create_complex_space(name)
    local space = box.schema.space.create(name, {
        if_not_exists = true,
        format = {
            { 'id', type = 'number' },
            { 'is_married', type = 'boolean', is_nullable = true },
            { 'name', type = 'string' }
        }
    })
    space:create_index('pk', { parts = { 'id' } })
end

local function fail()
    error('Fail!')
end

get_version = get_version
fail = fail

box.cfg {
    listen = 3301,
    memtx_memory = 128 * 1024 * 1024,
    log_level = 6,
    memtx_use_mvcc_engine = true
}

function insert_c()
    fiber.yield()
    box.space.test:insert({ 'test', 'test' })
end

box.once('schema', function()
    create_kv_space('test')
    create_kv_space('space_a')
    create_kv_space('space_b')

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
    box.schema.user.grant('replicator', 'replication')

    log.info('schema created on master')
end)
