------------------------------------------------------------------------------
-- UTILS
------------------------------------------------------------------------------
local tarantool = require('tarantool')

local M = {}

function M.get_version()
    local version = unpack(tarantool.version:split('-'))
    return version
end

function M.create_kv_space(name)
    local space = box.schema.space.create(name, {
        if_not_exists = true,
        format = {
            { 'id', type = 'string' },
            { 'value', type = 'string', is_nullable = true }
        }
    })
    space:create_index('pk', { parts = { 'id' } })
end

function M.create_complex_space(name)
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

function M.fail()
    error('Fail!')
end

return M
