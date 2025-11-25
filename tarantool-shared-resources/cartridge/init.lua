#!/usr/bin/env tarantool

require('strict').on()

if package.setsearchroot ~= nil then
    package.setsearchroot()
end

local cartridge = require('cartridge')
local ok, err = cartridge.cfg({
    workdir = 'tmp/db',
    roles = {
        'cartridge.roles.crud-storage',
        'cartridge.roles.crud-router',
        'app.roles.api',
        'app.roles.storage',
    },
    cluster_cookie = 'secret-cluster-cookie',
})

assert(ok, tostring(err))
