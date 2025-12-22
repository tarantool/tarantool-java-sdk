box.cfg {
    listen = 3301,
    memtx_memory = 2 * 1024 * 1024 * 1024, -- 2 Gb
    -- log = 'file:/tmp/tarantool.log',
    log_level = 6,
    readahead = 10 * 1024 * 1024,
    net_msg_max = 200000,
    memtx_use_mvcc_engine = true,
}

-- Create a Metrics Client
local metrics = require('metrics')

-- Enable default metrics collections
metrics.enable_default_metrics();

-- Init Prometheus Exporter
local httpd = require('http.server')
local http_handler = require('metrics.plugins.prometheus').collect_http

httpd.new('0.0.0.0', 8081)
     :route({ path = '/metrics' }, function(...)
    return http_handler(...)
end) :start()

-- API user will be able to login with this password
box.schema.user.create('api_user', { password = 'secret' })
-- API user will be able to create spaces, add or remove data, execute functions
box.schema.user.grant('api_user', 'read,write,execute', 'universe')
box.schema.user.grant("guest", "super")

local tuple = box.tuple.new({
    1, -- field1
    'aaaaaaaa', -- field2
    box.NULL, -- field3
    'bbbbb', -- field4
    false, -- field5
    99, -- field6
    'cccccc', -- field7
    3.4654, -- field8
    'a', -- field9
    -123312, -- field10
    { 0, "asdsad", 1, false, 2.2, }, -- field11
    { hello = "world", d = 3 }, -- field12
    true, -- field13
    9223372036854775807ULL, -- field14
    -9223372036854775807LL              -- field15
})

function return_one_tuple()
    return tuple
end

tmp = box.schema.create_space('tmp')
tmp:create_index("pri")
