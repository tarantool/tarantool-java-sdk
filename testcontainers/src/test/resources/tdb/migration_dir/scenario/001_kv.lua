local config = require('config')
local helpers = require('tt-migrations.helpers')
local fun = require('fun')

local function is_router()
  return fun.index('roles.crud-router', config:get('roles')) ~= nil
end

local function is_storage()
  return fun.index('roles.crud-storage', config:get('roles')) ~= nil
end

local function apply()
    if is_router() then
        box.schema.func.create('hello', {
            language = 'LUA',
            if_not_exists = true,
            body = [[
                function(name)
                    return string.format("hello %s", tostring(name))
                end
            ]],
        })
    end

    if is_storage() then
        box.schema.space.create('kv', { if_not_exists = true })
        box.space.kv:format({
            { name = 'id', type = 'number' },
            { name = 'bucket_id', type = 'unsigned' },
            { name = 'kv', type = 'any' },
        })
        box.space.kv:create_index('pk', { parts = {'id'}, if_not_exists = true})
        box.space.kv:create_index('bucket_id', { parts = {'bucket_id'}, unique = false, if_not_exists = true})
        helpers.register_sharding_key('kv', {'id'})
    end

    return true
end

return {
    apply = {
        scenario = apply,
    }
}
