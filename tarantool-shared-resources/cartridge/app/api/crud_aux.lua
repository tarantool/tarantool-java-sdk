local fiber = require('fiber')

local crud = require('crud')

local crud_methods_to_patch = {
    'insert',
    'select',
    'get',
    'delete',
    'replace',
    'update',
    'upsert',
    'insert_many',
    'replace_many',
    'upsert_many',
    'truncate',
    'count',
    'len',
    'min',
    'max'
}

local old_methods = {}

local function wrap_api(wrapper)
    for _, name in ipairs(crud_methods_to_patch) do
        local real_method
        if old_methods[name] ~= nil then
            real_method = old_methods[name]
        else
            real_method = crud[name]
            old_methods[name] = real_method
        end
        crud[name] = wrapper(name, real_method)
    end
end

local function unwrap_api()
    for _, name in ipairs(crud_methods_to_patch) do
        if old_methods[name] ~= nil then
            crud[name] = old_methods[name]
        end
    end
end

local function break_api()
    wrap_api(function(name, method)
        return function(...)
            local args = { ... }
            local counter_name = ('crud_%s_calls'):format(name)
            local counter = rawget(_G, counter_name)
            if counter == nil then
                counter = 0
            end
            counter = counter + 1
            rawset(_G, counter_name, counter)
            if counter % 3 ~= 0 then
                error('some lua error ' .. counter)
            end
            return method(...)
        end
    end)
end

local function slow_api()
    wrap_api(function(name, method)
        return function(...)
            fiber.sleep(1.5)
            return method(...)
        end
    end)
end

local function init_module()
    wrap_api(function(name, method)
        return function(...)
            local args = { ... }
            rawset(_G, ('crud_%s_opts'):format(name), args[#args])
            return method(...)
        end
    end)
end

local function reset_counters()
    for _, name in ipairs(crud_methods_to_patch) do
        rawset(_G, ('crud_%s_calls'):format(name), 0)
    end
end

return {
    init_module = init_module,
    break_api = break_api,
    unwrap_api = unwrap_api,
    reset_counters = reset_counters,
    slow_api = slow_api
}
