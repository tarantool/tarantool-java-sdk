local version = require('app.version')
local crud_aux = require('app.api.crud_aux')
local helpers = require('app.api.helpers')

local function init(opts)
    -- luacheck: no unused args
    rawset(_G, 'get_version', version.get_version)
    rawset(_G, 'crud_aux', crud_aux)
    crud_aux.init_module()
    helpers.init_module()
    return true
end

local function stop()
    return true
end

local function validate_config(conf_new, conf_old)
    -- luacheck: no unused args
    return true
end

local function apply_config(conf, opts)
    -- luacheck: no unused args
    return true
end

return {
    role_name = 'app.roles.api',
    init = init,
    stop = stop,
    validate_config = validate_config,
    apply_config = apply_config,
    dependencies = {
        'cartridge.roles.crud-router'
    }
}
