local function init(opts)
    -- luacheck: no unused args
    if opts.is_master then
        local space = box.schema.space.create("person", {
            if_not_exists = true,
            format = {
                { 'id', type = 'number' },
                { 'is_married', type = 'boolean', is_nullable = true },
                { 'name', type = 'string' },
                { 'bucket_id', 'unsigned' },
            },
            if_not_exists = true
        })
        space:create_index('pk', { parts = { 'id' }, if_not_exists = true })
        space:create_index('bucket_id', { parts = { 'bucket_id' }, unique = false, if_not_exists = true })
        space:create_index('is_married_idx', { parts = { 'is_married' }, unique = false, if_not_exists = true })

        local complex_space = box.schema.space.create("complex_person", {
            if_not_exists = true,
            format = {
                { 'id', type = 'number' },
                { 'second_id', type = 'uuid' },
                { 'is_married', type = 'boolean', is_nullable = true },
                { 'name', type = 'string' },
                { 'bucket_id', 'unsigned' },
            },
            if_not_exists = true
        })
        complex_space:create_index('pk', { parts = { 'id', 'second_id' }, if_not_exists = true })
        complex_space:create_index('bucket_id', { parts = { 'bucket_id' }, unique = false, if_not_exists = true })
        complex_space:create_index('is_married_idx', { parts = { 'is_married' }, unique = false, if_not_exists = true })

        local nested_space = box.schema.space.create("nested_person", {
            if_not_exists = true,
            format = {
                { 'id', type = 'number' },
                { 'name', type = 'string' },
                { 'friends', type = 'array'},
                { 'buys', type = 'map'},
                { 'husband', type = 'array'},
                { 'child', type = 'map'},
                { 'bucket_id', 'unsigned' },
            },
            if_not_exists = true
        })
        nested_space:create_index('pk', { parts = { 'id' }, if_not_exists = true })
        nested_space:create_index('bucket_id', { parts = { 'bucket_id' }, unique = false, if_not_exists = true })
    end

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
    role_name = 'app.roles.storage',
    init = init,
    stop = stop,
    validate_config = validate_config,
    apply_config = apply_config,
    dependencies = {
        'cartridge.roles.crud-storage'
    },
}
