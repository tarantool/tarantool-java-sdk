------------------------------------------------------------------------------
-- MASTER
------------------------------------------------------------------------------
if package.setsearchroot ~= nil then
    package.setsearchroot()
end

local log = require('log')
local fiber = require('fiber')

local utils = require('utils')

get_version = utils.get_version
fail = utils.fail

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
    utils.create_kv_space('test')
    utils.create_kv_space('space_a')
    utils.create_kv_space('space_b')

    log.info('schema created on master')
end)
