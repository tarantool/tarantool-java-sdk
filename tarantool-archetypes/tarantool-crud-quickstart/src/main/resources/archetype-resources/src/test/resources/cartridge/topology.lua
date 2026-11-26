-- Tarantool Cartridge cluster topology configuration.
-- We explicitly assign the vshard and crud roles so that Spring Data Repository
-- can invoke stored procedures such as crud.insert and crud.select.
local cartridge = require('cartridge')

local replicasets = {
    {
        alias = 'router',
        roles = {'vshard-router', 'crud-router'},
        join_servers = {{uri = 'localhost:3301'}}
    },
    {
        alias = 's1-storage',
        roles = {'vshard-storage', 'crud-storage'},
        join_servers = {{uri = 'localhost:3302'}}
    }
}

return cartridge.admin_edit_topology({replicasets = replicasets})
