local vshard = require('vshard')
local log = require('log')
local crud = require('crud')
local crud_aux = require('crud_aux')
local helpers = require('helpers')

-- Bootstrap the vshard router.
while true do
    local ok, err = vshard.router.bootstrap({
        if_not_bootstrapped = true,
    })
    if ok then
        crud.init_router()
        rawset(_G, 'crud_aux', crud_aux)
        crud_aux.init_module()
        helpers.init_module()
        break
    end
    log.info(('Router bootstrap error: %s'):format(err))
end
